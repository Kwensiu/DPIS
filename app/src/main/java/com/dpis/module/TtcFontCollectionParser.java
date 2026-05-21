package com.dpis.module;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TtcFontCollectionParser {
    private static final int SIGNATURE_TTC = 0x74746366; // ttcf
    private static final int MAX_FACE_COUNT = 128;
    private static final long HEADER_SIZE = 12L;

    private TtcFontCollectionParser() {
    }

    static Result parse(File file) {
        if (file == null || !file.isFile()) {
            return Result.invalid();
        }
        long fileLength = file.length();
        if (fileLength < HEADER_SIZE) {
            return Result.invalid();
        }
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            int signature = input.readInt();
            if (signature != SIGNATURE_TTC) {
                return Result.invalid();
            }
            int version = input.readInt();
            long count = Integer.toUnsignedLong(input.readInt());
            if (count <= 0 || count > MAX_FACE_COUNT) {
                return Result.invalid();
            }
            long offsetTableLength = HEADER_SIZE + count * 4L;
            if (offsetTableLength > fileLength) {
                return Result.invalid();
            }
            List<Long> offsets = new ArrayList<>((int) count);
            for (int index = 0; index < count; index++) {
                long offset = Integer.toUnsignedLong(input.readInt());
                if (offset < offsetTableLength || offset >= fileLength) {
                    return Result.invalid();
                }
                offsets.add(offset);
            }
            return new Result(true, version, Collections.unmodifiableList(offsets));
        } catch (IOException ignored) {
            return Result.invalid();
        }
    }

    static final class Result {
        final boolean valid;
        final int version;
        final List<Long> offsets;

        private Result(boolean valid, int version, List<Long> offsets) {
            this.valid = valid;
            this.version = version;
            this.offsets = offsets;
        }

        static Result invalid() {
            return new Result(false, 0, List.of());
        }
    }
}
