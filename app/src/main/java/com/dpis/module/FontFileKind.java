package com.dpis.module;

enum FontFileKind {
    TTF(".ttf"),
    OTF(".otf"),
    TTC(".ttc"),
    UNSUPPORTED(".ttf");

    final String extension;

    FontFileKind(String extension) {
        this.extension = extension;
    }
}
