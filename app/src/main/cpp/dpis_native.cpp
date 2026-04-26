#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <link.h>
#include <sys/mman.h>
#include <sys/system_properties.h>
#include <unistd.h>

#include <atomic>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <cstring>
#include <cerrno>
#include <string>

namespace {

constexpr const char *kLogTag = "DPIS_NATIVE";
constexpr const char *kTargetLibrary = "libhyper_os_flutter.so";
constexpr const char *kHyperOsAppPublicLibrary = "libhyper_os_app_public.so";
constexpr uintptr_t kParagraphBuilderCreateOffset = 0x81c368;
constexpr uintptr_t kParagraphBuilderPushStyleOffset = 0x82370c;
constexpr double kMinScale = 0.25;
constexpr double kMaxScale = 8.0;
#if defined(__aarch64__)
constexpr size_t kInlineHookPatchBytes = 20;
#endif

using HookFunType = int (*)(void *func, void *replace, void **backup);
using UnhookFunType = int (*)(void *func);
using NativeOnModuleLoaded = void (*)(const char *name, void *handle);
using HyperOsLaunchMainThread = void (*)();
using HyperOsAppEntryPoint = void (*)();

struct NativeAPIEntries {
    uint32_t version;
    HookFunType hook_func;
    UnhookFunType unhook_func;
};

HookFunType g_hook_func = nullptr;
void *g_backup_create = nullptr;
void *g_backup_push_style = nullptr;
std::atomic<int> g_target_font_percent{100};
std::atomic<bool> g_enabled{false};
std::atomic<bool> g_configured_from_jni{false};
std::atomic<bool> g_create_hooked{false};
std::atomic<bool> g_push_style_hooked{false};
std::atomic<int> g_property_refresh_budget{256};
std::atomic<int> g_replace_create_log_budget{16};
std::atomic<int> g_replace_push_style_log_budget{16};

std::atomic<int> g_last_observed_scale_milli{1000};

void log_info(const std::string &message);
extern "C" void replace_create_trampoline();
extern "C" void replace_push_style_trampoline();

#if defined(__aarch64__)
void emit_mov_abs(uint32_t *code, size_t &index, uintptr_t address) {
    uint64_t value = static_cast<uint64_t>(address);
    uint16_t part0 = static_cast<uint16_t>(value & 0xffffu);
    uint16_t part1 = static_cast<uint16_t>((value >> 16u) & 0xffffu);
    uint16_t part2 = static_cast<uint16_t>((value >> 32u) & 0xffffu);
    uint16_t part3 = static_cast<uint16_t>((value >> 48u) & 0xffffu);
    code[index++] = 0xd2800000u | (static_cast<uint32_t>(part0) << 5u) | 17u;
    code[index++] = 0xf2a00000u | (static_cast<uint32_t>(part1) << 5u) | (1u << 21u) | 17u;
    code[index++] = 0xf2c00000u | (static_cast<uint32_t>(part2) << 5u) | (2u << 21u) | 17u;
    code[index++] = 0xf2e00000u | (static_cast<uint32_t>(part3) << 5u) | (3u << 21u) | 17u;
}

void emit_abs_branch(uint32_t *code, size_t &index, uintptr_t address, bool link) {
    emit_mov_abs(code, index, address);
    code[index++] = link ? 0xd63f0220u : 0xd61f0220u;
}

bool is_bl(uint32_t instruction) {
    return (instruction & 0xfc000000u) == 0x94000000u;
}

bool is_b(uint32_t instruction) {
    return (instruction & 0xfc000000u) == 0x14000000u;
}

uintptr_t decode_branch_target(uintptr_t pc, uint32_t instruction) {
    int32_t imm26 = static_cast<int32_t>(instruction & 0x03ffffffu);
    if ((imm26 & 0x02000000) != 0) {
        imm26 |= static_cast<int32_t>(0xfc000000u);
    }
    return pc + (static_cast<int64_t>(imm26) << 2);
}

bool make_writable_executable(void *address, size_t length) {
    long page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0) {
        page_size = 4096;
    }
    uintptr_t start = reinterpret_cast<uintptr_t>(address) & ~(static_cast<uintptr_t>(page_size) - 1u);
    uintptr_t end = (reinterpret_cast<uintptr_t>(address) + length + page_size - 1u)
            & ~(static_cast<uintptr_t>(page_size) - 1u);
    if (mprotect(reinterpret_cast<void *>(start), end - start,
            PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        log_info("mprotect failed errno=" + std::to_string(errno));
        return false;
    }
    return true;
}

#endif

int inline_hook_arm64(void *target, void *replacement, void **backup) {
#if defined(__aarch64__)
    if (target == nullptr || replacement == nullptr || backup == nullptr) {
        return -1;
    }
    auto *trampoline = static_cast<uint32_t *>(mmap(nullptr, 4096,
            PROT_READ | PROT_WRITE | PROT_EXEC,
            MAP_PRIVATE | MAP_ANONYMOUS, -1, 0));
    if (trampoline == MAP_FAILED) {
        log_info("inline hook mmap failed errno=" + std::to_string(errno));
        return -2;
    }
    auto *source = reinterpret_cast<uint32_t *>(target);
    size_t out = 0;
    for (size_t offset = 0; offset < kInlineHookPatchBytes; offset += sizeof(uint32_t)) {
        uint32_t instruction = source[offset / sizeof(uint32_t)];
        uintptr_t pc = reinterpret_cast<uintptr_t>(target) + offset;
        if (is_bl(instruction)) {
            emit_abs_branch(trampoline, out, decode_branch_target(pc, instruction), true);
        } else if (is_b(instruction)) {
            emit_abs_branch(trampoline, out, decode_branch_target(pc, instruction), false);
        } else {
            trampoline[out++] = instruction;
        }
    }
    emit_abs_branch(trampoline, out,
            reinterpret_cast<uintptr_t>(target) + kInlineHookPatchBytes, false);
    __builtin___clear_cache(reinterpret_cast<char *>(trampoline),
            reinterpret_cast<char *>(trampoline + out));

    if (!make_writable_executable(target, kInlineHookPatchBytes)) {
        munmap(trampoline, 4096);
        return -3;
    }
    uint32_t patch[5] = {};
    size_t patch_index = 0;
    emit_abs_branch(patch, patch_index, reinterpret_cast<uintptr_t>(replacement), false);
    std::memcpy(target, patch, sizeof(patch));
    __builtin___clear_cache(reinterpret_cast<char *>(target),
            reinterpret_cast<char *>(target) + sizeof(patch));
    *backup = trampoline;
    return 0;
#else
    (void) target;
    (void) replacement;
    (void) backup;
    return -10;
#endif
}

void log_info(const char *message) {
    __android_log_write(ANDROID_LOG_INFO, kLogTag, message);
}

void log_info(const std::string &message) {
    log_info(message.c_str());
}

bool ends_with(const char *text, const char *suffix) {
    if (text == nullptr || suffix == nullptr) {
        return false;
    }
    size_t text_len = std::strlen(text);
    size_t suffix_len = std::strlen(suffix);
    return text_len >= suffix_len
            && std::strcmp(text + text_len - suffix_len, suffix) == 0;
}

struct BaseLookup {
    const char *name;
    uintptr_t base;
};

int find_base_callback(struct dl_phdr_info *info, size_t, void *data) {
    auto *lookup = static_cast<BaseLookup *>(data);
    if (info == nullptr || lookup == nullptr || !ends_with(info->dlpi_name, lookup->name)) {
        return 0;
    }
    lookup->base = static_cast<uintptr_t>(info->dlpi_addr);
    return 1;
}

uintptr_t find_library_base(const char *name) {
    BaseLookup lookup{name, 0};
    dl_iterate_phdr(find_base_callback, &lookup);
    return lookup.base;
}

double clamp(double value, double min_value, double max_value) {
    if (value < min_value) {
        return min_value;
    }
    if (value > max_value) {
        return max_value;
    }
    return value;
}

double target_scale() {
    int percent = g_target_font_percent.load(std::memory_order_relaxed);
    if (percent <= 0) {
        return 1.0;
    }
    return static_cast<double>(percent) / 100.0;
}

std::string current_process_name() {
    FILE *file = std::fopen("/proc/self/cmdline", "re");
    if (file == nullptr) {
        return {};
    }
    char buffer[256] = {};
    size_t read = std::fread(buffer, 1, sizeof(buffer) - 1, file);
    std::fclose(file);
    if (read == 0) {
        return {};
    }
    return std::string(buffer);
}

std::string read_proc_cmdline_value(const char *key) {
    if (key == nullptr || key[0] == '\0') {
        return {};
    }
    FILE *file = std::fopen("/proc/self/cmdline", "re");
    if (file == nullptr) {
        return {};
    }
    char buffer[4096] = {};
    size_t read = std::fread(buffer, 1, sizeof(buffer) - 1, file);
    std::fclose(file);
    if (read == 0) {
        return {};
    }
    std::string prefix = std::string(key) + "=";
    size_t index = 0;
    while (index < read) {
        const char *entry = buffer + index;
        size_t length = std::strlen(entry);
        if (length == 0) {
            index++;
            continue;
        }
        std::string item(entry, length);
        size_t start = 0;
        while (start < item.size()) {
            size_t end = item.find(' ', start);
            std::string token = item.substr(start,
                    end == std::string::npos ? std::string::npos : end - start);
            if (token.rfind(prefix, 0) == 0) {
                return token.substr(prefix.size());
            }
            if (end == std::string::npos) {
                break;
            }
            start = end + 1;
        }
        index += length + 1;
    }
    return {};
}

std::string read_system_property(const char *key) {
    if (key == nullptr || key[0] == '\0') {
        return {};
    }
    char value[PROP_VALUE_MAX] = {};
    int length = __system_property_get(key, value);
    if (length <= 0) {
        return {};
    }
    return std::string(value, static_cast<size_t>(length));
}

std::string read_environment(const char *key) {
    if (key == nullptr || key[0] == '\0') {
        return {};
    }
    const char *value = std::getenv(key);
    if (value == nullptr || value[0] == '\0') {
        return {};
    }
    return std::string(value);
}

uint32_t java_string_hash(const std::string &text) {
    uint32_t hash = 0;
    for (unsigned char ch : text) {
        hash = hash * 31u + ch;
    }
    return hash;
}

void refresh_property_config() {
    if (g_configured_from_jni.load(std::memory_order_relaxed)) {
        return;
    }
    int remaining = g_property_refresh_budget.load(std::memory_order_relaxed);
    if (remaining <= 0) {
        return;
    }
    g_property_refresh_budget.store(remaining - 1, std::memory_order_relaxed);
    std::string process = current_process_name();
    if (process.empty()) {
        return;
    }
    char key[PROP_NAME_MAX] = {};
    uint32_t process_hash = java_string_hash(process);
    std::snprintf(key, sizeof(key), "debug.dpis.forcefont.%08x", process_hash);
    std::string value = read_system_property(key);
    if (value.empty()) {
        value = read_system_property("debug.dpis.forcefont");
    }
    if (value.empty()) {
        value = read_environment("DPIS_FONT_SCALE_PERCENT");
    }
    if (value.empty()) {
        value = read_proc_cmdline_value("DPIS_FONT_SCALE_PERCENT");
    }
    if (value.empty()) {
        std::snprintf(key, sizeof(key), "debug.dpis.font.%08x", process_hash);
        value = read_system_property(key);
    }
    if (value.empty()) {
        return;
    }
    int percent = std::atoi(value.c_str());
    if (percent <= 0) {
        g_enabled.store(false, std::memory_order_relaxed);
        return;
    }
    g_target_font_percent.store(percent, std::memory_order_relaxed);
    g_enabled.store(true, std::memory_order_relaxed);
}

double multiplier_for(double observed_scale) {
    refresh_property_config();
    if (!g_enabled.load(std::memory_order_relaxed)
            || observed_scale <= 0.0
            || !std::isfinite(observed_scale)) {
        return 1.0;
    }
    int observed_milli = static_cast<int>(observed_scale * 1000.0 + 0.5);
    if (observed_milli > 0) {
        g_last_observed_scale_milli.store(observed_milli, std::memory_order_relaxed);
    }
    return clamp(target_scale() / observed_scale, kMinScale, kMaxScale);
}

extern "C" double dpis_create_multiplier(double d0, double d1, double d2) {
    double multiplier = multiplier_for(d1);
    int log_budget = g_replace_create_log_budget.load(std::memory_order_relaxed);
    if (log_budget > 0) {
        g_replace_create_log_budget.store(log_budget - 1, std::memory_order_relaxed);
        log_info("HyperOS Flutter ParagraphBuilder::Create override: d0="
                + std::to_string(d0)
                + " d1=" + std::to_string(d1)
                + " d2=" + std::to_string(d2)
                + " multiplier=" + std::to_string(multiplier));
    }
    return multiplier;
}

extern "C" uintptr_t dpis_create_backup_address() {
    return reinterpret_cast<uintptr_t>(g_backup_create);
}

bool is_push_style_experiment_enabled() {
    std::string value = read_system_property("debug.dpis.pushstyle");
    if (value == "1" || value == "true" || value == "enabled") {
        return true;
    }
    if (value == "false" || value == "disabled") {
        return false;
    }
    refresh_property_config();
    return g_enabled.load(std::memory_order_relaxed);
}

extern "C" double dpis_push_style_multiplier(double observed_scale, double font_size) {
    double multiplier = multiplier_for(observed_scale);
    int log_budget = g_replace_push_style_log_budget.load(std::memory_order_relaxed);
    if (log_budget > 0) {
        g_replace_push_style_log_budget.store(log_budget - 1, std::memory_order_relaxed);
        log_info("HyperOS Flutter ParagraphBuilder::pushStyle override: font="
                + std::to_string(font_size)
                + " observed=" + std::to_string(observed_scale)
                + " multiplier=" + std::to_string(multiplier));
    }
    return multiplier;
}

extern "C" uintptr_t dpis_push_style_backup_address() {
    return reinterpret_cast<uintptr_t>(g_backup_push_style);
}

void try_hook_flutter(void *handle) {
    if (handle == nullptr) {
        return;
    }
    uintptr_t base = find_library_base(kTargetLibrary);
    if (base == 0) {
        log_info("HyperOS Flutter font hook skipped: base not found");
        return;
    }
    if (!g_create_hooked.exchange(true, std::memory_order_acq_rel)) {
        void *target = reinterpret_cast<void *>(base + kParagraphBuilderCreateOffset);
        int result = g_hook_func != nullptr
                ? g_hook_func(target,
                        reinterpret_cast<void *>(replace_create_trampoline),
                        &g_backup_create)
                : inline_hook_arm64(target,
                        reinterpret_cast<void *>(replace_create_trampoline),
                        &g_backup_create);
        log_info("HyperOS Flutter ParagraphBuilder::Create hook result=" + std::to_string(result));
    }
    if (!g_push_style_hooked.exchange(true, std::memory_order_acq_rel)) {
        if (!is_push_style_experiment_enabled()) {
            log_info("HyperOS Flutter ParagraphBuilder::pushStyle hook skipped: debug.dpis.pushstyle disabled");
        } else {
            void *target = reinterpret_cast<void *>(base + kParagraphBuilderPushStyleOffset);
            int result = g_hook_func != nullptr
                    ? g_hook_func(target,
                            reinterpret_cast<void *>(replace_push_style_trampoline),
                            &g_backup_push_style)
                    : inline_hook_arm64(target,
                            reinterpret_cast<void *>(replace_push_style_trampoline),
                            &g_backup_push_style);
            log_info("HyperOS Flutter ParagraphBuilder::pushStyle hook result=" + std::to_string(result));
        }
    }
}

void on_library_loaded(const char *name, void *handle) {
    if (ends_with(name, kTargetLibrary)) {
        try_hook_flutter(handle);
    }
}

void *load_original_rust_binary() {
    std::string process = current_process_name();
    if (process.empty()) {
        log_info("HyperOS proxy original load skipped: empty process");
        return nullptr;
    }
    char key[PROP_NAME_MAX] = {};
    std::snprintf(key, sizeof(key), "debug.dpis.rustbin.%08x", java_string_hash(process));
    std::string path = read_system_property(key);
    if (path.empty()) {
        log_info("HyperOS proxy original load skipped: missing property " + std::string(key));
        return nullptr;
    }
    void *handle = dlopen(path.c_str(), RTLD_NOW | RTLD_GLOBAL);
    const char *error = dlerror();
    log_info("HyperOS proxy original load: process=" + process
            + " path=" + path
            + " handle=" + std::to_string(reinterpret_cast<uintptr_t>(handle))
            + " error=" + (error == nullptr ? "" : error));
    return handle;
}

HyperOsAppEntryPoint find_original_app_entry_point() {
    void *handle = load_original_rust_binary();
    if (handle == nullptr) {
        return nullptr;
    }
    auto entry = reinterpret_cast<HyperOsAppEntryPoint>(dlsym(handle, "app_entry_point"));
    const char *error = dlerror();
    log_info("HyperOS proxy app_entry_point lookup: entry="
            + std::to_string(reinterpret_cast<uintptr_t>(entry))
            + " error=" + (error == nullptr ? "" : error));
    return entry;
}

void try_hook_flutter_without_lsposed() {
    void *local_handle = dlopen(kTargetLibrary, RTLD_NOW | RTLD_NOLOAD);
    if (local_handle == nullptr) {
        local_handle = dlopen(kTargetLibrary, RTLD_NOW | RTLD_GLOBAL);
    }
    const char *error = dlerror();
    log_info("HyperOS proxy flutter lookup: handle="
            + std::to_string(reinterpret_cast<uintptr_t>(local_handle))
            + " error=" + (error == nullptr ? "" : error));
    try_hook_flutter(local_handle);
}

[[gnu::constructor]]
void proxy_constructor() {
    log_info("HyperOS proxy constructor: process=" + current_process_name());
    refresh_property_config();
    load_original_rust_binary();
    try_hook_flutter_without_lsposed();
}

} // namespace

#if defined(__aarch64__)
extern "C" [[gnu::visibility("default")]] [[gnu::used]] [[gnu::naked]]
void replace_create_trampoline() {
    __asm__ volatile(
            "sub sp, sp, #256\n"
            "stp x0, x1, [sp, #0]\n"
            "stp x2, x3, [sp, #16]\n"
            "stp x4, x5, [sp, #32]\n"
            "stp x6, x7, [sp, #48]\n"
            "str x8, [sp, #64]\n"
            "str x30, [sp, #72]\n"
            "stp q0, q1, [sp, #80]\n"
            "stp q2, q3, [sp, #112]\n"
            "stp q4, q5, [sp, #144]\n"
            "stp q6, q7, [sp, #176]\n"
            "bl dpis_create_multiplier\n"
            "str d0, [sp, #208]\n"
            "bl dpis_create_backup_address\n"
            "str x0, [sp, #216]\n"
            "ldp q0, q1, [sp, #80]\n"
            "ldp q2, q3, [sp, #112]\n"
            "ldp q4, q5, [sp, #144]\n"
            "ldp q6, q7, [sp, #176]\n"
            "ldr d16, [sp, #208]\n"
            "fmul d0, d0, d16\n"
            "fmul d2, d2, d16\n"
            "ldp x0, x1, [sp, #0]\n"
            "ldp x2, x3, [sp, #16]\n"
            "ldp x4, x5, [sp, #32]\n"
            "ldp x6, x7, [sp, #48]\n"
            "ldr x8, [sp, #64]\n"
            "ldr x30, [sp, #72]\n"
            "ldr x9, [sp, #216]\n"
            "add sp, sp, #256\n"
            "cbz x9, 1f\n"
            "br x9\n"
            "1:\n"
            "ret\n");
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]] [[gnu::naked]]
void replace_push_style_trampoline() {
    __asm__ volatile(
            "sub sp, sp, #256\n"
            "stp x0, x1, [sp, #0]\n"
            "stp x2, x3, [sp, #16]\n"
            "stp x4, x5, [sp, #32]\n"
            "stp x6, x7, [sp, #48]\n"
            "str x8, [sp, #64]\n"
            "str x30, [sp, #72]\n"
            "stp q0, q1, [sp, #80]\n"
            "stp q2, q3, [sp, #112]\n"
            "stp q4, q5, [sp, #144]\n"
            "stp q6, q7, [sp, #176]\n"
            "fmov d0, d3\n"
            "ldr d1, [sp, #80]\n"
            "bl dpis_push_style_multiplier\n"
            "str d0, [sp, #208]\n"
            "bl dpis_push_style_backup_address\n"
            "str x0, [sp, #216]\n"
            "ldp q0, q1, [sp, #80]\n"
            "ldp q2, q3, [sp, #112]\n"
            "ldp q4, q5, [sp, #144]\n"
            "ldp q6, q7, [sp, #176]\n"
            "ldr d16, [sp, #208]\n"
            "fmul d0, d0, d16\n"
            "ldp x0, x1, [sp, #0]\n"
            "ldp x2, x3, [sp, #16]\n"
            "ldp x4, x5, [sp, #32]\n"
            "ldp x6, x7, [sp, #48]\n"
            "ldr x8, [sp, #64]\n"
            "ldr x30, [sp, #72]\n"
            "ldr x9, [sp, #216]\n"
            "add sp, sp, #256\n"
            "cbz x9, 1f\n"
            "br x9\n"
            "1:\n"
            "ret\n");
}
#else
extern "C" [[gnu::visibility("default")]] [[gnu::used]]
void replace_create_trampoline() {
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
void replace_push_style_trampoline() {
}
#endif

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
uintptr_t dpis_resolve_app_entry_point(void *arg0, void *arg1, void *arg8) {
    log_info("HyperOS proxy app_entry_point entered: process=" + current_process_name()
            + " x0=" + std::to_string(reinterpret_cast<uintptr_t>(arg0))
            + " x1=" + std::to_string(reinterpret_cast<uintptr_t>(arg1))
            + " x8=" + std::to_string(reinterpret_cast<uintptr_t>(arg8)));
    try_hook_flutter_without_lsposed();
    HyperOsAppEntryPoint entry = find_original_app_entry_point();
    return reinterpret_cast<uintptr_t>(entry);
}

#if defined(__aarch64__)
extern "C" [[gnu::visibility("default")]] [[gnu::used]] [[gnu::naked]]
void app_entry_point() {
    __asm__ volatile(
            "sub sp, sp, #96\n"
            "stp x0, x1, [sp, #0]\n"
            "stp x2, x3, [sp, #16]\n"
            "stp x4, x5, [sp, #32]\n"
            "stp x6, x7, [sp, #48]\n"
            "str x8, [sp, #64]\n"
            "str x30, [sp, #72]\n"
            "mov x2, x8\n"
            "bl dpis_resolve_app_entry_point\n"
            "mov x9, x0\n"
            "ldp x0, x1, [sp, #0]\n"
            "ldp x2, x3, [sp, #16]\n"
            "ldp x4, x5, [sp, #32]\n"
            "ldp x6, x7, [sp, #48]\n"
            "ldr x8, [sp, #64]\n"
            "ldr x30, [sp, #72]\n"
            "add sp, sp, #96\n"
            "cbz x9, 1f\n"
            "br x9\n"
            "1:\n"
            "ret\n");
}
#else
extern "C" [[gnu::visibility("default")]] [[gnu::used]]
void app_entry_point() {
    HyperOsAppEntryPoint entry = find_original_app_entry_point();
    if (entry != nullptr) {
        entry();
    }
}
#endif

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
void launch_main_thread() {
    log_info("HyperOS proxy launch_main_thread entered: process=" + current_process_name());
    try_hook_flutter_without_lsposed();
    void *public_handle = dlopen(kHyperOsAppPublicLibrary, RTLD_NOW | RTLD_GLOBAL);
    if (public_handle == nullptr) {
        public_handle = dlopen("/system_ext/lib64/libhyper_os_app_public.so", RTLD_NOW | RTLD_GLOBAL);
    }
    const char *open_error = dlerror();
    auto original = reinterpret_cast<HyperOsLaunchMainThread>(
            dlsym(public_handle, "launch_main_thread"));
    const char *symbol_error = dlerror();
    log_info("HyperOS proxy forwarding: publicHandle="
            + std::to_string(reinterpret_cast<uintptr_t>(public_handle))
            + " original=" + std::to_string(reinterpret_cast<uintptr_t>(original))
            + " openError=" + (open_error == nullptr ? "" : open_error)
            + " symbolError=" + (symbol_error == nullptr ? "" : symbol_error));
    if (original != nullptr) {
        original();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_dpis_module_HyperOsFlutterFontHookInstaller_configure(JNIEnv *env,
                                                               jclass,
                                                               jstring package_name,
                                                               jint target_font_scale_percent,
                                                               jboolean enabled) {
    const char *package_chars = package_name != nullptr
            ? env->GetStringUTFChars(package_name, nullptr)
            : nullptr;
    g_target_font_percent.store(target_font_scale_percent, std::memory_order_relaxed);
    g_enabled.store(enabled == JNI_TRUE, std::memory_order_relaxed);
    g_configured_from_jni.store(true, std::memory_order_relaxed);
    std::string package_text = package_chars != nullptr ? package_chars : "unknown";
    if (package_chars != nullptr) {
        env->ReleaseStringUTFChars(package_name, package_chars);
    }
    log_info("configured package=" + package_text
            + " targetFontScalePercent=" + std::to_string(target_font_scale_percent)
            + " enabled=" + std::to_string(enabled == JNI_TRUE));
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    if (entries != nullptr) {
        g_hook_func = entries->hook_func;
    }
    log_info("native_init ready");
    return on_library_loaded;
}
