#pragma once

#include <cstdint>
#include <cstdio>
#include <string>

namespace dpis {

    inline std::string font_property_name(const std::string &field,
                                          const std::string &package_name,
                                          uint32_t user_id,
                                          bool persistent = false) {
        uint32_t hash = 0;
        for (unsigned char ch: package_name) {
            hash = hash * 31u + ch;
        }
        char suffix[32] = {};
        std::snprintf(suffix, sizeof(suffix), "%u.%08x", user_id, hash);
        return std::string(persistent ? "persist.debug.dpis." : "debug.dpis.")
               + field + "." + suffix;
    }

    template<typename Reader>
    std::string read_font_property(const std::string &field,
                                   const std::string &package_name,
                                   uint32_t user_id,
                                   Reader reader) {
        auto value = reader(font_property_name(field, package_name, user_id).c_str());
        if (!value.empty()) return value;
        return reader(font_property_name(field, package_name, user_id, true).c_str());
    }

    struct FontConfigValue {
        std::string source;
        std::string value;
    };

    template<typename PropertyReader, typename EnvironmentReader, typename CommandLineReader>
    FontConfigValue select_font_config(const std::string &package_name,
                                       uint32_t user_id,
                                       bool configured_from_jni,
                                       PropertyReader property_reader,
                                       EnvironmentReader environment_reader,
                                       CommandLineReader command_line_reader) {
        FontConfigValue config = {
                font_property_name("forcefont", package_name, user_id),
                read_font_property("forcefont", package_name, user_id, property_reader),
        };
        if (config.value.empty() && !configured_from_jni) {
            config = {"env:DPIS_FONT_SCALE_PERCENT", environment_reader("DPIS_FONT_SCALE_PERCENT")};
        }
        if (config.value.empty() && !configured_from_jni) {
            config = {"cmdline:DPIS_FONT_SCALE_PERCENT",
                      command_line_reader("DPIS_FONT_SCALE_PERCENT")};
        }
        if (config.value.empty() && !configured_from_jni) {
            config = {font_property_name("font", package_name, user_id),
                      read_font_property("font", package_name, user_id, property_reader)};
        }
        return config;
    }

    template<typename PropertyReader>
    std::string original_rust_binary(const std::string &package_name,
                                     uint32_t user_id,
                                     const std::string &environment_path,
                                     const std::string &sibling_path,
                                     PropertyReader property_reader) {
        if (!environment_path.empty()) return environment_path;
        if (!sibling_path.empty()) return sibling_path;
        return read_font_property("rustbin", package_name, user_id, property_reader);
    }

}  // namespace dpis
