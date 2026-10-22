#include "font_property_keys.h"
#include <cassert>
#include <map>

int main() {
    const std::string package = "com.android.chrome";
    // These names are the Java/Kotlin/native wire contract, including the user-0 prefix.
    assert(dpis::font_property_name("forcefont", package, 0) ==
           "debug.dpis.forcefont.0.0f493ae6");
    assert(dpis::font_property_name("forcefont", package, 999, true) ==
           "persist.debug.dpis.forcefont.999.0f493ae6");
    std::map<std::string, std::string> values = {
            {"debug.dpis.forcefont.0f493ae6",                           "300"},
            {"debug.dpis.forcefont",                                    "300"},
            {dpis::font_property_name("forcefont", package, 0),         "200"},
            {dpis::font_property_name("forcefont", package, 999, true), "60"},
    };
    const auto reader = [&](const char *key) {
        const auto found = values.find(key);
        return found == values.end() ? std::string() : found->second;
    };
    assert(dpis::read_font_property("forcefont", package, 0, reader) == "200");
    assert(dpis::read_font_property("forcefont", package, 999, reader) == "60");
    assert(dpis::read_font_property("forcefont", package, 10, reader).empty());
    values[dpis::font_property_name("forcefont", package, 999)] = "0";
    assert(dpis::read_font_property("forcefont", package, 999, reader) == "0");
    assert(dpis::read_font_property("forcefont", package, 0, reader) == "200");
    values[dpis::font_property_name("rustbin", package, 999)] = "/user999/libapp.so";
    assert(dpis::read_font_property("rustbin", package, 999, reader) == "/user999/libapp.so");
    assert(dpis::read_font_property("rustbin", package, 0, reader).empty());

    const auto env = [](const char *) { return std::string("90"); };
    const auto cmdline = [](const char *) { return std::string("80"); };
    const auto empty = [](const char *) { return std::string(); };
    assert(dpis::select_font_config(package, 0, true, reader, env, cmdline).value == "200");
    assert(dpis::select_font_config(package, 999, true, reader, env, cmdline).value == "0");
    assert(dpis::select_font_config(package, 10, true, reader, env, cmdline).value.empty());
    assert(dpis::select_font_config(package, 10, false, reader, env, cmdline).value == "90");
    assert(dpis::select_font_config(package, 10, false, reader, empty, cmdline).value == "80");
    values[dpis::font_property_name("font", package, 10)] = "70";
    assert(dpis::select_font_config(package, 10, false, reader, empty, empty).value == "70");
    assert(dpis::original_rust_binary(package, 999, "/env.so", "/sibling.so", reader) == "/env.so");
    assert(dpis::original_rust_binary(package, 999, "", "/sibling.so", reader) == "/sibling.so");
    assert(dpis::original_rust_binary(package, 999, "", "", reader) == "/user999/libapp.so");
    assert(dpis::original_rust_binary(package, 0, "", "", reader).empty());
}
