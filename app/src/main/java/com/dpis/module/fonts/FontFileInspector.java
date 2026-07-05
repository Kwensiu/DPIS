package com.dpis.module.fonts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FontFileInspector {
    private static final int FONT_HEADER_TRUE_TYPE = 0x00010000;
    private static final int FONT_HEADER_OPEN_TYPE = 0x4F54544F; // OTTO
    private static final int FONT_HEADER_APPLE_TRUE_TYPE = 0x74727565; // true
    private static final int FONT_HEADER_TTC = 0x74746366; // ttcf

    private FontFileInspector() {
    }

    public static Result inspect(File file) {
        int signature = readSignature(file);
        if (signature == FONT_HEADER_TTC) {
            TtcFontCollectionParser.Result ttc = TtcFontCollectionParser.parse(file);
            return new Result(ttc.valid ? FontFileKind.TTC : FontFileKind.UNSUPPORTED, ttc);
        }
        if (signature == FONT_HEADER_OPEN_TYPE) {
            return new Result(FontFileKind.OTF, TtcFontCollectionParser.Result.invalid());
        }
        if (signature == FONT_HEADER_TRUE_TYPE || signature == FONT_HEADER_APPLE_TRUE_TYPE) {
            return new Result(FontFileKind.TTF, TtcFontCollectionParser.Result.invalid());
        }
        return new Result(FontFileKind.UNSUPPORTED, TtcFontCollectionParser.Result.invalid());
    }

    private static int readSignature(File file) {
        if (file == null || !file.isFile()) {
            return 0;
        }
        try (InputStream input = new FileInputStream(file)) {
            byte[] header = new byte[4];
            if (input.read(header) != header.length) {
                return 0;
            }
            return ((header[0] & 0xFF) << 24)
                    | ((header[1] & 0xFF) << 16)
                    | ((header[2] & 0xFF) << 8)
                    | (header[3] & 0xFF);
        } catch (IOException ignored) {
            return 0;
        }
    }

    public static final class Result {
        public final FontFileKind kind;
        public final TtcFontCollectionParser.Result ttc;

        Result(FontFileKind kind, TtcFontCollectionParser.Result ttc) {
            this.kind = kind;
            this.ttc = ttc;
        }
    }
}
