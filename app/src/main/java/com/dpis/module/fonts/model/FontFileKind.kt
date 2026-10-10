package com.dpis.module.fonts

enum class FontFileKind(@JvmField val extension: String) {
    TTF(".ttf"),
    OTF(".otf"),
    TTC(".ttc"),
    UNSUPPORTED("")
}
