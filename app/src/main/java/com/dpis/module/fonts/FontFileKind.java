package com.dpis.module.fonts;

public enum FontFileKind {
    TTF(".ttf"),
    OTF(".otf"),
    TTC(".ttc"),
    UNSUPPORTED("");

    public final String extension;

    FontFileKind(String extension) {
        this.extension = extension;
    }
}
