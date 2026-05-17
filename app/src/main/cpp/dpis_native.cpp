#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <link.h>
#include <mutex>
#include <pthread.h>
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
constexpr const char *kGenericFlutterLibrary = "libflutter.so";
constexpr const char *kGenericFlutterAppLibrary = "libapp.so";
constexpr const char *kHyperOsAppPublicLibrary = "libhyper_os_app_public.so";
#if defined(__aarch64__)
constexpr const char *kWeatherRustLibrary = "libweather_app.so";
#endif
constexpr uintptr_t kParagraphBuilderCreateOffset = 0x81c368;
constexpr uintptr_t kParagraphBuilderPushStyleOffset = 0x82370c;
// Hook after a verified libflutter wrapper has decoded TextStyle.fontSize into
// d11. The function entry receives decorationThickness in d4, so changing the
// entry register does not affect visible text size.
constexpr uintptr_t kGenericParagraphBuilderPushStyleOffset = 0x82d470;
#if defined(__aarch64__)
constexpr uintptr_t kWeatherConfigurationFontScaleGotOffset = 0x44c0d8;
#endif
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

enum class GenericFlutterFontRoute : int {
    kNone = 0,
    kVerifiedPushStyleD11 = 1,
};

struct NativeAPIEntries {
    uint32_t version;
    HookFunType hook_func;
    UnhookFunType unhook_func;
};

HookFunType g_hook_func = nullptr;
void *g_backup_create = nullptr;
void *g_backup_push_style = nullptr;
void *g_backup_generic_get_scaled_font_size = nullptr;
void *g_backup_generic_create = nullptr;
void *g_backup_generic_push_style = nullptr;
JavaVM *g_java_vm = nullptr;
jclass g_dpis_log_class = nullptr;
jmethodID g_dpis_log_info_method = nullptr;
std::mutex g_dpis_log_bridge_mutex;
#if defined(__aarch64__)
void *g_original_weather_configuration_font_scale = nullptr;
#endif
std::atomic<int> g_target_font_percent{100};
std::atomic<bool> g_enabled{false};
std::atomic<bool> g_configured_from_jni{false};
std::atomic<bool> g_create_hooked{false};
std::atomic<bool> g_push_style_hooked{false};
std::atomic<bool> g_generic_get_scaled_font_size_hooked{false};
std::atomic<bool> g_generic_create_hooked{false};
std::atomic<bool> g_generic_push_style_hooked{false};
#if defined(__aarch64__)
std::atomic<bool> g_weather_configuration_font_scale_hooked{false};
#endif
std::atomic<int> g_property_refresh_budget{256};
std::atomic<int> g_replace_create_log_budget{16};
std::atomic<int> g_replace_push_style_log_budget{16};
std::atomic<int> g_weather_configuration_font_scale_log_budget{16};
std::atomic<int> g_weather_create_d0_remap_log_budget{16};
std::atomic<int> g_property_source_log_budget{24};
std::atomic<int> g_generic_flutter_probe_log_budget{16};
std::atomic<int> g_generic_flutter_string_probe_log_budget{12};
std::atomic<bool> g_generic_flutter_poll_started{false};
std::atomic<bool> g_generic_flutter_status_started{false};
std::atomic<uintptr_t> g_last_generic_flutter_poll_base{0};
std::atomic<uintptr_t> g_last_reported_generic_flutter_base{0};
std::atomic<int> g_last_generic_flutter_poll_index{-1};
std::atomic<int> g_generic_get_scaled_font_size_attempts{0};
std::atomic<int> g_generic_get_scaled_font_size_calls{0};
std::atomic<int> g_generic_get_scaled_font_size_log_budget{24};
std::atomic<int> g_last_generic_get_scaled_input_milli{0};
std::atomic<int> g_last_generic_get_scaled_output_milli{0};
std::atomic<int> g_last_generic_get_scaled_config_id{0};
std::atomic<int> g_generic_create_attempts{0};
std::atomic<int> g_generic_create_calls{0};
std::atomic<int> g_generic_create_log_budget{24};
std::atomic<int> g_generic_push_style_attempts{0};
std::atomic<int> g_generic_push_style_calls{0};
std::atomic<int> g_generic_push_style_log_budget{24};
std::atomic<int> g_last_generic_push_style_input_milli{0};
std::atomic<int> g_last_generic_push_style_output_milli{0};
std::atomic<int> g_generic_flutter_route{static_cast<int>(GenericFlutterFontRoute::kNone)};
std::atomic<int> g_generic_flutter_route_log_budget{8};

std::atomic<int> g_last_observed_scale_milli{1000};

void log_info(const std::string &message);
void bridge_log_info(const std::string &message);
std::string current_process_name();
uintptr_t parse_maps_start_address(const char *line);
void probe_generic_flutter(void *handle, const std::string &source);
void probe_flutter_text_strings(const char *library_name, void *handle, const std::string &source);
bool is_debug_build();
bool is_generic_flutter_font_hook_experiment_enabled();
extern "C" void replace_create_trampoline();
extern "C" void replace_push_style_trampoline();
extern "C" void replace_generic_get_scaled_font_size_trampoline();
extern "C" void replace_generic_create_trampoline();
extern "C" void replace_generic_push_style_trampoline();
extern "C" float Configuration_get_font_scale(void *configuration);
extern "C" void app_entry_point();

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


bool make_writable_data(void *address, size_t length) {
    long page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0) {
        page_size = 4096;
    }
    uintptr_t start = reinterpret_cast<uintptr_t>(address) & ~(static_cast<uintptr_t>(page_size) - 1u);
    uintptr_t end = (reinterpret_cast<uintptr_t>(address) + length + page_size - 1u)
            & ~(static_cast<uintptr_t>(page_size) - 1u);
    if (mprotect(reinterpret_cast<void *>(start), end - start,
            PROT_READ | PROT_WRITE) != 0) {
        log_info("mprotect data failed errno=" + std::to_string(errno));
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

bool is_debug_build() {
#ifdef NDEBUG
    return false;
#else
    return true;
#endif
}

void bridge_log_info(const std::string &message) {
    JavaVM *java_vm = nullptr;
    jclass log_class = nullptr;
    jmethodID log_method = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_dpis_log_bridge_mutex);
        java_vm = g_java_vm;
        log_class = g_dpis_log_class;
        log_method = g_dpis_log_info_method;
    }
    if (java_vm == nullptr || log_class == nullptr || log_method == nullptr) {
        return;
    }
    JNIEnv *env = nullptr;
    bool detach = false;
    jint get_env = java_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    if (get_env == JNI_EDETACHED) {
        if (java_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        detach = true;
    } else if (get_env != JNI_OK || env == nullptr) {
        return;
    }
    jstring text = env->NewStringUTF(message.c_str());
    if (text != nullptr) {
        env->CallStaticVoidMethod(log_class, log_method, text);
        env->DeleteLocalRef(text);
    }
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
    if (detach) {
        java_vm->DetachCurrentThread();
    }
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
    if (lookup.base != 0) {
        return lookup.base;
    }
    FILE *maps = std::fopen("/proc/self/maps", "r");
    if (maps == nullptr) {
        return 0;
    }
    char line[1024] = {};
    uintptr_t base = 0;
    while (std::fgets(line, sizeof(line), maps) != nullptr) {
        if (std::strstr(line, name) == nullptr || std::strstr(line, "r-xp") == nullptr) {
            continue;
        }
        uintptr_t start = parse_maps_start_address(line);
        if (start != 0) {
            base = start;
            break;
        }
    }
    std::fclose(maps);
    return base;
}

uintptr_t parse_maps_start_address(const char *line) {
    if (line == nullptr) {
        return 0;
    }
    uintptr_t value = 0;
    for (const char *cursor = line; *cursor != '\0'; cursor++) {
        char ch = *cursor;
        if (ch == '-') {
            return value;
        }
        int digit = -1;
        if (ch >= '0' && ch <= '9') {
            digit = ch - '0';
        } else if (ch >= 'a' && ch <= 'f') {
            digit = ch - 'a' + 10;
        } else if (ch >= 'A' && ch <= 'F') {
            digit = ch - 'A' + 10;
        } else {
            return 0;
        }
        value = (value << 4u) | static_cast<uintptr_t>(digit);
    }
    return 0;
}

GenericFlutterFontRoute resolve_generic_flutter_font_route(uintptr_t base) {
    if (base == 0) {
        return GenericFlutterFontRoute::kNone;
    }
    if (!is_generic_flutter_font_hook_experiment_enabled()) {
        return GenericFlutterFontRoute::kNone;
    }
    return GenericFlutterFontRoute::kVerifiedPushStyleD11;
}

const char *generic_flutter_font_route_name(GenericFlutterFontRoute route) {
    switch (route) {
        case GenericFlutterFontRoute::kVerifiedPushStyleD11:
            return "GENERIC_PUSH_STYLE_D11";
        case GenericFlutterFontRoute::kNone:
        default:
            return "NONE";
    }
}

#if defined(__aarch64__)
bool is_weather_font_scale_symbol(const Dl_info &info) {
    // On tested Weather builds, the GOT entry resolves to the HyperOS public API
    // implementation instead of a symbol inside libweather_app.so.
    return info.dli_sname != nullptr
            && std::strcmp(info.dli_sname, "Configuration_get_font_scale") == 0
            && (ends_with(info.dli_fname, kWeatherRustLibrary)
                    || ends_with(info.dli_fname, kHyperOsAppPublicLibrary));
}

bool is_weather_configuration_font_scale_slot(void *value) {
    if (value == nullptr) {
        return false;
    }
    Dl_info info = {};
    if (dladdr(value, &info) == 0 || info.dli_fname == nullptr) {
        return false;
    }
    if (is_weather_font_scale_symbol(info)) {
        return true;
    }
    if (!ends_with(info.dli_fname, kWeatherRustLibrary)) {
        return false;
    }
    uintptr_t weather_base = find_library_base(kWeatherRustLibrary);
    uintptr_t address = reinterpret_cast<uintptr_t>(value);
    return weather_base != 0 && address >= weather_base;
}

std::string describe_symbol(void *value) {
    if (value == nullptr) {
        return "null";
    }
    Dl_info info = {};
    if (dladdr(value, &info) == 0) {
        return "dladdr-failed address=" + std::to_string(reinterpret_cast<uintptr_t>(value));
    }
    return "address=" + std::to_string(reinterpret_cast<uintptr_t>(value))
            + " file=" + (info.dli_fname == nullptr ? "" : info.dli_fname)
            + " symbol=" + (info.dli_sname == nullptr ? "" : info.dli_sname)
            + " symbolAddress=" + std::to_string(reinterpret_cast<uintptr_t>(info.dli_saddr));
}
#endif

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

bool is_enabled_value(const std::string &value) {
    return value == "1" || value == "true" || value == "enabled";
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

bool is_generic_flutter_font_hook_experiment_enabled() {
    if (!is_debug_build()) {
        return false;
    }
    return is_enabled_value(read_environment("DPIS_GENERIC_FLUTTER_FONT_HOOK"))
            || is_enabled_value(read_system_property("debug.dpis.generic_flutter_font_hook"));
}

std::string sibling_original_rust_binary_path() {
#if defined(__aarch64__)
    if (current_process_name() != "com.miui.weather2") {
        return {};
    }
    Dl_info info = {};
    if (dladdr(reinterpret_cast<void *>(app_entry_point), &info) == 0
            || info.dli_fname == nullptr
            || info.dli_fname[0] == '\0') {
        return {};
    }
    std::string self_path(info.dli_fname);
    size_t slash = self_path.rfind('/');
    if (slash == std::string::npos) {
        return {};
    }
    return self_path.substr(0, slash + 1) + kWeatherRustLibrary;
#else
    return {};
#endif
}

uint32_t java_string_hash(const std::string &text) {
    uint32_t hash = 0;
    for (unsigned char ch : text) {
        hash = hash * 31u + ch;
    }
    return hash;
}

void log_property_config_source(const std::string &process,
                                const std::string &source,
                                const std::string &value,
                                int percent,
                                bool configured_from_jni) {
    int log_budget = g_property_source_log_budget.load(std::memory_order_relaxed);
    if (log_budget <= 0) {
        return;
    }
    g_property_source_log_budget.store(log_budget - 1, std::memory_order_relaxed);
    log_info("HyperOS font config source: process=" + process
            + " source=" + source
            + " value=" + value
            + " percent=" + std::to_string(percent)
            + " configuredFromJni=" + std::to_string(configured_from_jni));
}

void refresh_property_config() {
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
    std::string source = key;
    std::string value = read_system_property(key);
    if (value.empty()) {
        source = "debug.dpis.forcefont";
        value = read_system_property("debug.dpis.forcefont");
    }
    bool configured_from_jni = g_configured_from_jni.load(std::memory_order_relaxed);
    if (value.empty() && !configured_from_jni) {
        source = "env:DPIS_FONT_SCALE_PERCENT";
        value = read_environment("DPIS_FONT_SCALE_PERCENT");
    }
    if (value.empty() && !configured_from_jni) {
        source = "cmdline:DPIS_FONT_SCALE_PERCENT";
        value = read_proc_cmdline_value("DPIS_FONT_SCALE_PERCENT");
    }
    if (value.empty() && !configured_from_jni) {
        std::snprintf(key, sizeof(key), "debug.dpis.font.%08x", process_hash);
        source = key;
        value = read_system_property(key);
    }
    if (value.empty()) {
        return;
    }
    int percent = std::atoi(value.c_str());
    log_property_config_source(process, source, value, percent, configured_from_jni);
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

double create_observed_scale(double observed_scale) {
    if (observed_scale > 0.0 && std::isfinite(observed_scale)) {
        return observed_scale;
    }
    return 1.0;
}

extern "C" double dpis_create_multiplier(double d0, double d1, double d2) {
    double multiplier = multiplier_for(create_observed_scale(d1));
    int log_budget = g_replace_create_log_budget.load(std::memory_order_relaxed);
    if (log_budget > 0) {
        g_replace_create_log_budget.store(log_budget - 1, std::memory_order_relaxed);
        log_info("HyperOS Flutter ParagraphBuilder::Create override: process=" + current_process_name()
                + " d0="
                + std::to_string(d0)
                + " d1=" + std::to_string(d1)
                + " d2=" + std::to_string(d2)
                + " multiplier=" + std::to_string(multiplier));
    }
    return multiplier;
}

extern "C" double dpis_create_scaled_d0(double original_d0,
                                        double original_d2,
                                        double multiplier) {
    if (current_process_name() == "com.miui.weather2"
            && original_d0 <= 0.0
            && original_d2 > 0.0
            && std::isfinite(original_d2)) {
        double scaled = original_d2 * multiplier;
        int log_budget = g_weather_create_d0_remap_log_budget.load(std::memory_order_relaxed);
        if (log_budget > 0) {
            g_weather_create_d0_remap_log_budget.store(log_budget - 1, std::memory_order_relaxed);
            log_info("HyperOS Weather Create d0 remap: d0="
                    + std::to_string(original_d0)
                    + " d2=" + std::to_string(original_d2)
                    + " scaled=" + std::to_string(scaled));
        }
        return scaled;
    }
    return original_d0 * multiplier;
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
        log_info("HyperOS Flutter ParagraphBuilder::pushStyle override: process=" + current_process_name()
                + " font="
                + std::to_string(font_size)
                + " observed=" + std::to_string(observed_scale)
                + " multiplier=" + std::to_string(multiplier));
    }
    return multiplier;
}

extern "C" uintptr_t dpis_push_style_backup_address() {
    return reinterpret_cast<uintptr_t>(g_backup_push_style);
}

extern "C" double dpis_generic_scaled_font_size_input(double unscaled_font_size,
                                                      int configuration_id) {
    refresh_property_config();
    double scale = target_scale();
    if (!g_enabled.load(std::memory_order_relaxed)
            || unscaled_font_size <= 0.0
            || !std::isfinite(unscaled_font_size)) {
        scale = 1.0;
    }
    double adjusted = clamp(unscaled_font_size * scale, 0.01, 10000.0);
    g_generic_get_scaled_font_size_calls.fetch_add(1, std::memory_order_relaxed);
    g_last_generic_get_scaled_input_milli.store(
            static_cast<int>(std::lround(unscaled_font_size * 1000.0)),
            std::memory_order_relaxed);
    g_last_generic_get_scaled_output_milli.store(
            static_cast<int>(std::lround(adjusted * 1000.0)),
            std::memory_order_relaxed);
    g_last_generic_get_scaled_config_id.store(configuration_id, std::memory_order_relaxed);
    int log_budget = g_generic_get_scaled_font_size_log_budget.load(std::memory_order_relaxed);
    if (log_budget > 0) {
        g_generic_get_scaled_font_size_log_budget.store(log_budget - 1, std::memory_order_relaxed);
        std::string message = "Generic Flutter GetScaledFontSize override: process="
                + current_process_name()
                + " font=" + std::to_string(unscaled_font_size)
                + " adjusted=" + std::to_string(adjusted)
                + " scale=" + std::to_string(scale)
                + " configurationId=" + std::to_string(configuration_id)
                + " calls=" + std::to_string(
                        g_generic_get_scaled_font_size_calls.load(std::memory_order_relaxed));
        log_info(message);
        bridge_log_info("DPIS_FONT " + message);
    }
    return adjusted;
}

extern "C" uintptr_t dpis_generic_get_scaled_font_size_backup_address() {
    return reinterpret_cast<uintptr_t>(g_backup_generic_get_scaled_font_size);
}

extern "C" double dpis_generic_create_scale_probe(double observed_scale) {
    refresh_property_config();
    double scale = target_scale();
    if (!g_enabled.load(std::memory_order_relaxed)) {
        scale = 1.0;
    }
    g_generic_create_calls.fetch_add(1, std::memory_order_relaxed);
    int log_budget = g_generic_create_log_budget.load(std::memory_order_relaxed);
    if (log_budget > 0) {
        g_generic_create_log_budget.store(log_budget - 1, std::memory_order_relaxed);
        std::string message = "Generic Flutter ParagraphBuilder::Create override: process="
                + current_process_name()
                + " observedD0=" + std::to_string(observed_scale)
                + " targetScale=" + std::to_string(scale)
                + " calls=" + std::to_string(
                        g_generic_create_calls.load(std::memory_order_relaxed));
        log_info(message);
        bridge_log_info("DPIS_FONT " + message);
    }
    return scale;
}

extern "C" uintptr_t dpis_generic_create_backup_address() {
    return reinterpret_cast<uintptr_t>(g_backup_generic_create);
}

extern "C" double dpis_generic_push_style_font_size_input(double font_size) {
    refresh_property_config();
    double scale = target_scale();
    if (!g_enabled.load(std::memory_order_relaxed)
            || font_size <= 0.0
            || !std::isfinite(font_size)) {
        scale = 1.0;
    }
    double adjusted = clamp(font_size * scale, 0.01, 10000.0);
    g_generic_push_style_calls.fetch_add(1, std::memory_order_relaxed);
    g_last_generic_push_style_input_milli.store(
            static_cast<int>(std::lround(font_size * 1000.0)),
            std::memory_order_relaxed);
    g_last_generic_push_style_output_milli.store(
            static_cast<int>(std::lround(adjusted * 1000.0)),
            std::memory_order_relaxed);
    int log_budget = g_generic_push_style_log_budget.load(std::memory_order_relaxed);
    if (log_budget > 0) {
        g_generic_push_style_log_budget.store(log_budget - 1, std::memory_order_relaxed);
        std::string message = "Generic Flutter ParagraphBuilder::pushStyle fontSize override: process="
                + current_process_name()
                + " fontSize=" + std::to_string(font_size)
                + " adjusted=" + std::to_string(adjusted)
                + " scale=" + std::to_string(scale)
                + " calls=" + std::to_string(
                        g_generic_push_style_calls.load(std::memory_order_relaxed));
        log_info(message);
        bridge_log_info("DPIS_FONT " + message);
    }
    return adjusted;
}

extern "C" uintptr_t dpis_generic_push_style_backup_address() {
    return reinterpret_cast<uintptr_t>(g_backup_generic_push_style);
}

extern "C" [[gnu::visibility("default")]] float Configuration_get_font_scale(void *configuration) {
    refresh_property_config();
    float value = static_cast<float>(target_scale());
    int log_budget = g_weather_configuration_font_scale_log_budget.load(std::memory_order_relaxed);
    if (log_budget > 0) {
        g_weather_configuration_font_scale_log_budget.store(log_budget - 1, std::memory_order_relaxed);
        log_info("HyperOS Configuration_get_font_scale override: process=" + current_process_name()
                + " value=" + std::to_string(value)
                + " config=" + std::to_string(reinterpret_cast<uintptr_t>(configuration)));
    }
    return value;
}

void try_hook_weather_configuration_font_scale() {
#if defined(__aarch64__)
    if (current_process_name() != "com.miui.weather2") {
        return;
    }
    if (g_weather_configuration_font_scale_hooked.load(std::memory_order_acquire)) {
        return;
    }
    uintptr_t base = find_library_base(kWeatherRustLibrary);
    if (base == 0) {
        log_info("HyperOS Weather Configuration_get_font_scale GOT hook skipped: base not found");
        return;
    }
    auto *slot = reinterpret_cast<void **>(base + kWeatherConfigurationFontScaleGotOffset);
    if (!is_weather_configuration_font_scale_slot(*slot)) {
        log_info("HyperOS Weather Configuration_get_font_scale GOT hook skipped: unexpected slot="
                + describe_symbol(*slot)
                + " base=" + std::to_string(base)
                + " slot=" + std::to_string(reinterpret_cast<uintptr_t>(slot)));
        return;
    }
    if (!make_writable_data(slot, sizeof(void *))) {
        log_info("HyperOS Weather Configuration_get_font_scale GOT hook failed: mprotect");
        return;
    }
    g_original_weather_configuration_font_scale = *slot;
    *slot = reinterpret_cast<void *>(Configuration_get_font_scale);
    __builtin___clear_cache(reinterpret_cast<char *>(slot),
            reinterpret_cast<char *>(slot) + sizeof(void *));
    g_weather_configuration_font_scale_hooked.store(true, std::memory_order_release);
    log_info("HyperOS Weather Configuration_get_font_scale GOT hook installed: base="
            + std::to_string(base)
            + " slot=" + std::to_string(reinterpret_cast<uintptr_t>(slot))
            + " original=" + std::to_string(
                    reinterpret_cast<uintptr_t>(g_original_weather_configuration_font_scale)));
#else
    log_info("HyperOS Weather Configuration_get_font_scale GOT hook skipped: unsupported arch");
#endif
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

void try_hook_generic_flutter(void *handle, const std::string &source) {
    uintptr_t base = find_library_base(kGenericFlutterLibrary);
    if (base == 0) {
        return;
    }
    GenericFlutterFontRoute route = resolve_generic_flutter_font_route(base);
    if (route == GenericFlutterFontRoute::kNone
            && g_generic_push_style_hooked.load(std::memory_order_acquire)) {
        route = static_cast<GenericFlutterFontRoute>(
                g_generic_flutter_route.load(std::memory_order_relaxed));
    }
    g_generic_flutter_route.store(static_cast<int>(route), std::memory_order_relaxed);
    uintptr_t previous_reported_base = g_last_reported_generic_flutter_base.exchange(
            base, std::memory_order_acq_rel);
    int route_log_budget = g_generic_flutter_route_log_budget.load(std::memory_order_relaxed);
    if (previous_reported_base != base || route_log_budget > 0) {
        if (route_log_budget > 0) {
            g_generic_flutter_route_log_budget.store(route_log_budget - 1,
                    std::memory_order_relaxed);
        }
        std::string mapped = "Generic Flutter mapped: process=" + current_process_name()
                + " source=" + source
                + " handle=" + std::to_string(reinterpret_cast<uintptr_t>(handle))
                + " base=" + std::to_string(base)
                + " route=" + generic_flutter_font_route_name(route);
        log_info(mapped);
        bridge_log_info("DPIS_FONT " + mapped);
    }
    if (route == GenericFlutterFontRoute::kNone) {
        return;
    }
    if (g_generic_push_style_hooked.load(std::memory_order_acquire)) {
        return;
    }
    int push_attempts = g_generic_push_style_attempts.fetch_add(
            1, std::memory_order_acq_rel);
    void *push_target = reinterpret_cast<void *>(
            base + kGenericParagraphBuilderPushStyleOffset);
    int push_result = g_hook_func != nullptr
            ? g_hook_func(push_target,
                    reinterpret_cast<void *>(replace_generic_push_style_trampoline),
                    &g_backup_generic_push_style)
            : inline_hook_arm64(push_target,
                    reinterpret_cast<void *>(replace_generic_push_style_trampoline),
                    &g_backup_generic_push_style);
    if (push_result == 0) {
        g_generic_push_style_hooked.store(true, std::memory_order_release);
    }
    std::string push_message = "Generic Flutter ParagraphBuilder::pushStyle hook result="
            + std::to_string(push_result)
            + " process=" + current_process_name()
            + " source=" + source
            + " route=" + generic_flutter_font_route_name(route)
            + " base=" + std::to_string(base)
            + " target=" + std::to_string(reinterpret_cast<uintptr_t>(push_target))
            + " backup="
            + std::to_string(reinterpret_cast<uintptr_t>(g_backup_generic_push_style))
            + " attempt=" + std::to_string(push_attempts + 1)
            + " handle=" + std::to_string(reinterpret_cast<uintptr_t>(handle));
    log_info(push_message);
    bridge_log_info("DPIS_FONT " + push_message);
}

int parse_native_poll_index(const std::string &source) {
    constexpr const char *prefix = "native-poll-";
    if (source.rfind(prefix, 0) != 0) {
        return -1;
    }
    char *end = nullptr;
    long value = std::strtol(source.c_str() + std::strlen(prefix), &end, 10);
    if (end == source.c_str() + std::strlen(prefix) || value < 0 || value > 1000) {
        return -1;
    }
    return static_cast<int>(value);
}

void probe_generic_flutter(void *handle, const std::string &source) {
    uintptr_t base = find_library_base(kGenericFlutterLibrary);
    try_hook_generic_flutter(handle, source);
    int poll_index = parse_native_poll_index(source);
    if (poll_index >= 0) {
        g_last_generic_flutter_poll_base.store(base, std::memory_order_relaxed);
        g_last_generic_flutter_poll_index.store(poll_index, std::memory_order_relaxed);
    }
    int log_budget = g_generic_flutter_probe_log_budget.load(std::memory_order_relaxed);
    if (log_budget <= 0 && base == 0) {
        return;
    }
    if (log_budget > 0) {
        g_generic_flutter_probe_log_budget.store(log_budget - 1, std::memory_order_relaxed);
    }
    log_info("Generic Flutter font probe: process=" + current_process_name()
            + " source=" + source
            + " handle=" + std::to_string(reinterpret_cast<uintptr_t>(handle))
            + " base=" + std::to_string(base)
            + " configured=" + std::to_string(g_configured_from_jni.load(std::memory_order_relaxed))
            + " enabled=" + std::to_string(g_enabled.load(std::memory_order_relaxed))
            + " lastPollIndex="
            + std::to_string(g_last_generic_flutter_poll_index.load(std::memory_order_relaxed))
            + " lastPollBase="
            + std::to_string(g_last_generic_flutter_poll_base.load(std::memory_order_relaxed))
            + " targetFontScalePercent="
            + std::to_string(g_target_font_percent.load(std::memory_order_relaxed))
            + " route="
            + generic_flutter_font_route_name(static_cast<GenericFlutterFontRoute>(
                    g_generic_flutter_route.load(std::memory_order_relaxed)))
            + " status="
            + (g_generic_push_style_hooked.load(std::memory_order_relaxed)
                    ? "push-style-d11-hooked" : "detected-not-hooked"));
    if (is_debug_build() && poll_index >= 0) {
        bridge_log_info("DPIS_FONT Generic Flutter native poll: process=" + current_process_name()
                + " source=" + source
                + " handle=" + std::to_string(reinterpret_cast<uintptr_t>(handle))
                + " base=" + std::to_string(base)
                + " targetFontScalePercent="
                + std::to_string(g_target_font_percent.load(std::memory_order_relaxed))
                + " route="
                + generic_flutter_font_route_name(static_cast<GenericFlutterFontRoute>(
                        g_generic_flutter_route.load(std::memory_order_relaxed)))
                + " status="
                + (g_generic_push_style_hooked.load(std::memory_order_relaxed)
                        ? "push-style-d11-hooked" : "detected-not-hooked"));
    }
    if (is_debug_build()) {
        probe_flutter_text_strings(kGenericFlutterLibrary, handle, source);
        probe_flutter_text_strings(kGenericFlutterAppLibrary, handle, source);
    }
}

const char *find_bytes(const char *haystack, size_t haystack_length,
                       const char *needle, size_t needle_length) {
    if (haystack == nullptr || needle == nullptr || needle_length == 0
            || haystack_length < needle_length) {
        return nullptr;
    }
    const char first = needle[0];
    const char *end = haystack + haystack_length - needle_length;
    for (const char *cursor = haystack; cursor <= end; cursor++) {
        if (*cursor == first && std::memcmp(cursor, needle, needle_length) == 0) {
            return cursor;
        }
    }
    return nullptr;
}

void probe_flutter_text_strings(const char *library_name, void *handle, const std::string &source) {
    if (library_name == nullptr || library_name[0] == '\0') {
        return;
    }
    uintptr_t base = find_library_base(library_name);
    if (base == 0) {
        return;
    }
    int log_budget = g_generic_flutter_string_probe_log_budget.load(std::memory_order_relaxed);
    if (log_budget <= 0) {
        return;
    }
    Dl_info info = {};
    if (handle != nullptr && dladdr(handle, &info) == 0) {
        info = {};
    }
    char line[1024] = {};
    FILE *maps = std::fopen("/proc/self/maps", "r");
    if (maps == nullptr) {
        return;
    }
    int matches = 0;
    while (std::fgets(line, sizeof(line), maps) != nullptr) {
        if (std::strstr(line, library_name) == nullptr) {
            continue;
        }
        if (std::strstr(line, "r--p") == nullptr && std::strstr(line, "r-xp") == nullptr) {
            continue;
        }
        uintptr_t start = parse_maps_start_address(line);
        if (start == 0) {
            continue;
        }
        uintptr_t end = 0;
        const char *dash = std::strchr(line, '-');
        if (dash != nullptr) {
            char *tail = nullptr;
            end = static_cast<uintptr_t>(std::strtoull(dash + 1, &tail, 16));
        }
        if (end <= start) {
            continue;
        }
        size_t length = static_cast<size_t>(end - start);
        const char *cursor = reinterpret_cast<const char *>(start);
        const char *limit = cursor + length;
        for (const char *needle : {"textScaleFactor", "setTextScaleFactor",
                                   "flutter/settings", "ParagraphBuilder",
                                   "pushStyle", "FontCollection"}) {
            const char *found = find_bytes(cursor, length, needle, std::strlen(needle));
            if (found == nullptr || found >= limit) {
                continue;
            }
            matches++;
            std::string message = "Flutter text string probe: process=" + current_process_name()
                    + " source=" + source
                    + " library=" + library_name
                    + " needle=" + needle
                    + " offset=" + std::to_string(
                            static_cast<uintptr_t>(found - cursor))
                    + " base=" + std::to_string(start);
            log_info(message);
            bridge_log_info("DPIS_FONT " + message);
            if (--log_budget <= 0) {
                break;
            }
        }
        if (log_budget <= 0) {
            break;
        }
    }
    std::fclose(maps);
    if (matches > 0) {
        g_generic_flutter_string_probe_log_budget.store(log_budget, std::memory_order_relaxed);
    }
}

void *generic_flutter_poll_thread(void *) {
    for (int index = 0; index < 60; index++) {
        char source[32] = {};
        std::snprintf(source, sizeof(source), "native-poll-%d", index);
        void *handle = dlopen(kGenericFlutterLibrary, RTLD_NOW | RTLD_NOLOAD);
        probe_generic_flutter(handle, source);
        if (g_generic_push_style_hooked.load(std::memory_order_acquire)) {
            break;
        }
        if (!is_debug_build() && index >= 7) {
            break;
        }
        usleep(1000000);
    }
    return nullptr;
}

void *generic_flutter_status_thread(void *) {
    for (int index = 0; index < 90; index++) {
        uintptr_t base = find_library_base(kGenericFlutterLibrary);
        if (base != 0) {
            std::string message = "Generic Flutter status tick: process=" + current_process_name()
                    + " index=" + std::to_string(index)
                    + " base=" + std::to_string(base)
                    + " route="
                    + generic_flutter_font_route_name(static_cast<GenericFlutterFontRoute>(
                            g_generic_flutter_route.load(std::memory_order_relaxed)))
                    + " getScaledHooked="
                    + std::to_string(g_generic_get_scaled_font_size_hooked.load(
                            std::memory_order_relaxed))
                    + " createHooked="
                    + std::to_string(g_generic_create_hooked.load(std::memory_order_relaxed))
                    + " pushStyleHooked="
                    + std::to_string(g_generic_push_style_hooked.load(std::memory_order_relaxed))
                    + " overrideCalls="
                    + std::to_string(g_generic_get_scaled_font_size_calls.load(
                            std::memory_order_relaxed))
                    + " createCalls="
                    + std::to_string(g_generic_create_calls.load(std::memory_order_relaxed))
                    + " pushStyleCalls="
                    + std::to_string(g_generic_push_style_calls.load(std::memory_order_relaxed));
            log_info(message);
            bridge_log_info("DPIS_FONT " + message);
        }
        usleep(1000000);
    }
    return nullptr;
}

void schedule_generic_flutter_poll() {
    if (g_generic_flutter_poll_started.exchange(true, std::memory_order_acq_rel)) {
        return;
    }
    pthread_t thread{};
    pthread_attr_t attr{};
    if (pthread_attr_init(&attr) == 0) {
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
        int result = pthread_create(&thread, &attr, generic_flutter_poll_thread, nullptr);
        pthread_attr_destroy(&attr);
        log_info("Generic Flutter poll thread start result=" + std::to_string(result));
        if (result != 0) {
            g_generic_flutter_poll_started.store(false, std::memory_order_relaxed);
        }
        return;
    }
    int result = pthread_create(&thread, nullptr, generic_flutter_poll_thread, nullptr);
    log_info("Generic Flutter poll thread start result=" + std::to_string(result));
    if (result != 0) {
        g_generic_flutter_poll_started.store(false, std::memory_order_relaxed);
    }
}

void schedule_generic_flutter_status() {
    if (!is_debug_build()) {
        return;
    }
    if (g_generic_flutter_status_started.exchange(true, std::memory_order_acq_rel)) {
        return;
    }
    pthread_t thread{};
    pthread_attr_t attr{};
    if (pthread_attr_init(&attr) == 0) {
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
        int result = pthread_create(&thread, &attr, generic_flutter_status_thread, nullptr);
        pthread_attr_destroy(&attr);
        log_info("Generic Flutter status thread start result=" + std::to_string(result));
        if (result != 0) {
            g_generic_flutter_status_started.store(false, std::memory_order_relaxed);
        }
        return;
    }
    int result = pthread_create(&thread, nullptr, generic_flutter_status_thread, nullptr);
    log_info("Generic Flutter status thread start result=" + std::to_string(result));
    if (result != 0) {
        g_generic_flutter_status_started.store(false, std::memory_order_relaxed);
    }
}

std::string generic_flutter_probe_status(void *handle, const std::string &package_name,
                                         const std::string &source) {
    uintptr_t base = find_library_base(kGenericFlutterLibrary);
    if (base != 0 && !g_generic_get_scaled_font_size_hooked.load(std::memory_order_acquire)) {
        try_hook_generic_flutter(handle, "status-probe " + source);
    }
    if (is_debug_build()) {
        probe_flutter_text_strings(kGenericFlutterLibrary, handle, source);
        probe_flutter_text_strings(kGenericFlutterAppLibrary, handle, source);
    }
    return "Generic Flutter font probe: process=" + current_process_name()
            + " package=" + package_name
            + " source=" + source
            + " handle=" + std::to_string(reinterpret_cast<uintptr_t>(handle))
            + " base=" + std::to_string(base)
            + " configured=" + std::to_string(g_configured_from_jni.load(std::memory_order_relaxed))
            + " enabled=" + std::to_string(g_enabled.load(std::memory_order_relaxed))
            + " lastPollIndex="
            + std::to_string(g_last_generic_flutter_poll_index.load(std::memory_order_relaxed))
            + " lastPollBase="
            + std::to_string(g_last_generic_flutter_poll_base.load(std::memory_order_relaxed))
            + " targetFontScalePercent="
            + std::to_string(g_target_font_percent.load(std::memory_order_relaxed))
            + " route="
            + generic_flutter_font_route_name(static_cast<GenericFlutterFontRoute>(
                    g_generic_flutter_route.load(std::memory_order_relaxed)))
            + " hookAttempts="
            + std::to_string(g_generic_get_scaled_font_size_attempts.load(std::memory_order_relaxed))
            + " overrideCalls="
            + std::to_string(g_generic_get_scaled_font_size_calls.load(std::memory_order_relaxed))
            + " lastInputMilli="
            + std::to_string(g_last_generic_get_scaled_input_milli.load(std::memory_order_relaxed))
            + " lastOutputMilli="
            + std::to_string(g_last_generic_get_scaled_output_milli.load(std::memory_order_relaxed))
            + " lastConfigId="
            + std::to_string(g_last_generic_get_scaled_config_id.load(std::memory_order_relaxed))
            + " createAttempts="
            + std::to_string(g_generic_create_attempts.load(std::memory_order_relaxed))
            + " createCalls="
            + std::to_string(g_generic_create_calls.load(std::memory_order_relaxed))
            + " pushStyleAttempts="
            + std::to_string(g_generic_push_style_attempts.load(std::memory_order_relaxed))
            + " pushStyleCalls="
            + std::to_string(g_generic_push_style_calls.load(std::memory_order_relaxed))
            + " lastPushInputMilli="
            + std::to_string(g_last_generic_push_style_input_milli.load(std::memory_order_relaxed))
            + " lastPushOutputMilli="
            + std::to_string(g_last_generic_push_style_output_milli.load(std::memory_order_relaxed))
            + " status="
            + (g_generic_get_scaled_font_size_hooked.load(std::memory_order_relaxed)
                    || g_generic_create_hooked.load(std::memory_order_relaxed)
                    || g_generic_push_style_hooked.load(std::memory_order_relaxed)
                    ? "push-style-d11-hooked" : "detected-not-hooked");
}

void on_library_loaded(const char *name, void *handle) {
    if (ends_with(name, kTargetLibrary)) {
        log_info("native_init on_library_loaded target: process=" + current_process_name()
                + " name=" + (name == nullptr ? std::string("") : std::string(name)));
        try_hook_flutter(handle);
    } else if (ends_with(name, kGenericFlutterLibrary)) {
        probe_generic_flutter(handle, name == nullptr ? "native-init" : std::string(name));
    }
}

void *load_original_rust_binary() {
    std::string process = current_process_name();
    if (process.empty()) {
        log_info("HyperOS proxy original load skipped: empty process");
        return nullptr;
    }
    std::string path = read_environment("DPIS_RUST_BINARY");
    if (path.empty()) {
        path = sibling_original_rust_binary_path();
    }
    if (path.empty()) {
        char key[PROP_NAME_MAX] = {};
        std::snprintf(key, sizeof(key), "debug.dpis.rustbin.%08x", java_string_hash(process));
        path = read_system_property(key);
        if (path.empty() || path == "0") {
            log_info("HyperOS proxy original load skipped: missing property " + std::string(key));
            return nullptr;
        }
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
    void *flutter_handle = dlopen(kGenericFlutterLibrary, RTLD_NOW | RTLD_NOLOAD);
    const char *flutter_error = dlerror();
    log_info("Generic Flutter font lookup: handle="
            + std::to_string(reinterpret_cast<uintptr_t>(flutter_handle))
            + " error=" + (flutter_error == nullptr ? "" : flutter_error));
    probe_generic_flutter(flutter_handle, "direct-lookup");
    try_hook_weather_configuration_font_scale();
}

[[gnu::constructor]]
void proxy_constructor() {
    log_info("HyperOS proxy constructor: process=" + current_process_name());
    refresh_property_config();
    if (is_enabled_value(read_environment("DPIS_NATIVE_SKIP_ORIGINAL"))
            || is_enabled_value(read_system_property("debug.dpis.native.skip_original"))) {
        log_info("HyperOS proxy original load skipped: preload mode");
    } else {
        load_original_rust_binary();
    }
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
            "ldr d0, [sp, #80]\n"
            "ldr d1, [sp, #112]\n"
            "ldr d2, [sp, #208]\n"
            "bl dpis_create_scaled_d0\n"
            "str d0, [sp, #224]\n"
            "ldr d0, [sp, #112]\n"
            "ldr d1, [sp, #208]\n"
            "fmul d0, d0, d1\n"
            "str d0, [sp, #232]\n"
            "bl dpis_create_backup_address\n"
            "str x0, [sp, #216]\n"
            "ldp q0, q1, [sp, #80]\n"
            "ldp q2, q3, [sp, #112]\n"
            "ldp q4, q5, [sp, #144]\n"
            "ldp q6, q7, [sp, #176]\n"
            "ldr d0, [sp, #224]\n"
            "ldr d2, [sp, #232]\n"
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

extern "C" [[gnu::visibility("default")]] [[gnu::used]] [[gnu::naked]]
void replace_generic_get_scaled_font_size_trampoline() {
    __asm__ volatile(
            "sub sp, sp, #96\n"
            "str x30, [sp, #0]\n"
            "stp x0, x1, [sp, #8]\n"
            "stp x2, x3, [sp, #24]\n"
            "stp q0, q1, [sp, #48]\n"
            "str w0, [sp, #80]\n"
            "bl dpis_generic_scaled_font_size_input\n"
            "str d0, [sp, #88]\n"
            "bl dpis_generic_get_scaled_font_size_backup_address\n"
            "mov x9, x0\n"
            "ldr x30, [sp, #0]\n"
            "ldp x0, x1, [sp, #8]\n"
            "ldp x2, x3, [sp, #24]\n"
            "ldp q0, q1, [sp, #48]\n"
            "ldr d0, [sp, #88]\n"
            "add sp, sp, #96\n"
            "cbz x9, 1f\n"
            "br x9\n"
            "1:\n"
            "ret\n");
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]] [[gnu::naked]]
void replace_generic_create_trampoline() {
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
            "bl dpis_generic_create_scale_probe\n"
            "str d0, [sp, #208]\n"
            "bl dpis_generic_create_backup_address\n"
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

extern "C" [[gnu::visibility("default")]] [[gnu::used]] [[gnu::naked]]
void replace_generic_push_style_trampoline() {
    __asm__ volatile(
            "sub sp, sp, #320\n"
            "str x30, [sp, #0]\n"
            "stp x0, x1, [sp, #16]\n"
            "stp x2, x3, [sp, #32]\n"
            "stp x4, x5, [sp, #48]\n"
            "stp x6, x7, [sp, #64]\n"
            "stp x8, x9, [sp, #80]\n"
            "stp q0, q1, [sp, #96]\n"
            "stp q2, q3, [sp, #128]\n"
            "stp q4, q5, [sp, #160]\n"
            "stp q6, q7, [sp, #192]\n"
            "stp d8, d9, [sp, #224]\n"
            "stp d10, d12, [sp, #240]\n"
            "fmov d0, d11\n"
            "bl dpis_generic_push_style_font_size_input\n"
            "str d0, [sp, #256]\n"
            "bl dpis_generic_push_style_backup_address\n"
            "str x0, [sp, #264]\n"
            "ldr x30, [sp, #0]\n"
            "ldp x0, x1, [sp, #16]\n"
            "ldp x2, x3, [sp, #32]\n"
            "ldp x4, x5, [sp, #48]\n"
            "ldp x6, x7, [sp, #64]\n"
            "ldp x8, x9, [sp, #80]\n"
            "ldp q0, q1, [sp, #96]\n"
            "ldp q2, q3, [sp, #128]\n"
            "ldp q4, q5, [sp, #160]\n"
            "ldp q6, q7, [sp, #192]\n"
            "ldp d8, d9, [sp, #224]\n"
            "ldp d10, d12, [sp, #240]\n"
            "ldr d11, [sp, #256]\n"
            "ldr x17, [sp, #264]\n"
            "add sp, sp, #320\n"
            "cbz x17, 1f\n"
            "br x17\n"
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

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
void replace_generic_get_scaled_font_size_trampoline() {
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
void replace_generic_create_trampoline() {
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
void replace_generic_push_style_trampoline() {
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
    {
        std::lock_guard<std::mutex> lock(g_dpis_log_bridge_mutex);
        if (g_java_vm == nullptr) {
            env->GetJavaVM(&g_java_vm);
        }
        if (g_dpis_log_class == nullptr || g_dpis_log_info_method == nullptr) {
            jclass local_log_class = env->FindClass("com/dpis/module/DpisLog");
            if (local_log_class != nullptr) {
                jclass global_log_class =
                        reinterpret_cast<jclass>(env->NewGlobalRef(local_log_class));
                env->DeleteLocalRef(local_log_class);
                if (global_log_class != nullptr) {
                    jmethodID log_info_method = env->GetStaticMethodID(
                            global_log_class, "i", "(Ljava/lang/String;)V");
                    if (log_info_method != nullptr) {
                        g_dpis_log_class = global_log_class;
                        g_dpis_log_info_method = log_info_method;
                    } else {
                        env->DeleteGlobalRef(global_log_class);
                    }
                }
            }
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
        }
    }
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
    void *flutter_handle = dlopen(kGenericFlutterLibrary, RTLD_NOW | RTLD_NOLOAD);
    probe_generic_flutter(flutter_handle, "jni-configure");
    schedule_generic_flutter_poll();
    schedule_generic_flutter_status();
}

extern "C" JNIEXPORT void JNICALL
Java_com_dpis_module_HyperOsFlutterFontHookInstaller_onRuntimeLibraryLoaded(JNIEnv *env,
                                                                            jclass,
                                                                            jstring package_name,
                                                                            jstring library_name) {
    const char *package_chars = package_name != nullptr
            ? env->GetStringUTFChars(package_name, nullptr)
            : nullptr;
    const char *library_chars = library_name != nullptr
            ? env->GetStringUTFChars(library_name, nullptr)
            : nullptr;
    std::string package_text = package_chars != nullptr ? package_chars : "unknown";
    std::string library_text = library_chars != nullptr ? library_chars : "unknown";
    if (package_chars != nullptr) {
        env->ReleaseStringUTFChars(package_name, package_chars);
    }
    if (library_chars != nullptr) {
        env->ReleaseStringUTFChars(library_name, library_chars);
    }
    void *handle = nullptr;
    if (library_text == "flutter") {
        handle = dlopen(kGenericFlutterLibrary, RTLD_NOW | RTLD_NOLOAD);
    } else if (library_text == "libflutter.so") {
        handle = dlopen(kGenericFlutterLibrary, RTLD_NOW | RTLD_NOLOAD);
    } else if (ends_with(library_text.c_str(), kGenericFlutterLibrary)) {
        handle = dlopen(library_text.c_str(), RTLD_NOW | RTLD_NOLOAD);
    } else if (library_text == kTargetLibrary || ends_with(library_text.c_str(), kTargetLibrary)) {
        handle = dlopen(kTargetLibrary, RTLD_NOW | RTLD_NOLOAD);
        try_hook_flutter(handle);
    }
    probe_generic_flutter(handle, "runtime-load package=" + package_text
            + " library=" + library_text);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dpis_module_HyperOsFlutterFontHookInstaller_genericFlutterProbeStatus(JNIEnv *env,
                                                                               jclass,
                                                                               jstring package_name,
                                                                               jstring source) {
    const char *package_chars = package_name != nullptr
            ? env->GetStringUTFChars(package_name, nullptr)
            : nullptr;
    const char *source_chars = source != nullptr
            ? env->GetStringUTFChars(source, nullptr)
            : nullptr;
    std::string package_text = package_chars != nullptr ? package_chars : "unknown";
    std::string source_text = source_chars != nullptr ? source_chars : "unknown";
    if (package_chars != nullptr) {
        env->ReleaseStringUTFChars(package_name, package_chars);
    }
    if (source_chars != nullptr) {
        env->ReleaseStringUTFChars(source, source_chars);
    }
    void *handle = dlopen(kGenericFlutterLibrary, RTLD_NOW | RTLD_NOLOAD);
    std::string status = generic_flutter_probe_status(handle, package_text, source_text);
    return env->NewStringUTF(status.c_str());
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    if (entries != nullptr) {
        g_hook_func = entries->hook_func;
    }
    log_info("native_init ready: process=" + current_process_name()
            + " entries=" + std::to_string(reinterpret_cast<uintptr_t>(entries))
            + " hook=" + std::to_string(reinterpret_cast<uintptr_t>(g_hook_func)));
    try_hook_flutter_without_lsposed();
    return on_library_loaded;
}
