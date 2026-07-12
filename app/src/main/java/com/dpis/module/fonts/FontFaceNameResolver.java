package com.dpis.module.fonts;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads human-readable family and style names from a TTC face without loading
 * it into the Android Typeface cache. The runtime loader still decides whether
 * a face is usable; this resolver only improves catalog labels.
 */
public final class FontFaceNameResolver {
    private static final int TAG_NAME = 0x6E616D65; // name
    private static final int SFNT_HEADER_SIZE = 12;
    private static final int TABLE_RECORD_SIZE = 16;
    private static final int NAME_HEADER_SIZE = 6;
    private static final int NAME_RECORD_SIZE = 12;
    private static final int NAME_FAMILY = 1;
    private static final int NAME_STYLE = 2;
    private static final int NAME_TYPOGRAPHIC_FAMILY = 16;
    private static final int NAME_TYPOGRAPHIC_STYLE = 17;
    private static final int WINDOWS_ENGLISH_US = 0x0409;

    private FontFaceNameResolver() {
    }

    public static String resolveTtcFaceLabel(File file, int ttcIndex, String fallback) {
        if (file == null || !file.isFile() || ttcIndex < 0) {
            return fallback;
        }
        FontFileInspector.Result inspection = FontFileInspector.inspect(file);
        if (inspection.kind != FontFileKind.TTC
                || ttcIndex >= inspection.ttc.offsets.size()) {
            return fallback;
        }
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            List<NameRecord> records = readNameRecords(input, inspection.ttc.offsets.get(ttcIndex));
            String family = preferredName(records, NAME_TYPOGRAPHIC_FAMILY);
            if (family == null) {
                family = preferredName(records, NAME_FAMILY);
            }
            String style = preferredName(records, NAME_TYPOGRAPHIC_STYLE);
            if (style == null) {
                style = preferredName(records, NAME_STYLE);
            }
            if (family == null || family.isBlank()) {
                return fallback;
            }
            return style == null || style.isBlank() ? family : family + " " + style;
        } catch (IOException | RuntimeException ignored) {
            return fallback;
        }
    }

    private static List<NameRecord> readNameRecords(RandomAccessFile input, long sfntOffset)
            throws IOException {
        long fileLength = input.length();
        if (!fits(sfntOffset, SFNT_HEADER_SIZE, fileLength)) {
            return List.of();
        }
        input.seek(sfntOffset + 4);
        int tableCount = input.readUnsignedShort();
        long tableDirectoryEnd = sfntOffset + SFNT_HEADER_SIZE + (long) tableCount * TABLE_RECORD_SIZE;
        if (!fits(sfntOffset, tableDirectoryEnd - sfntOffset, fileLength)) {
            return List.of();
        }
        long nameOffset = -1;
        long nameLength = 0;
        input.seek(sfntOffset + SFNT_HEADER_SIZE);
        for (int index = 0; index < tableCount; index++) {
            int tag = input.readInt();
            input.skipBytes(4);
            long offset = Integer.toUnsignedLong(input.readInt());
            long length = Integer.toUnsignedLong(input.readInt());
            if (tag == TAG_NAME) {
                nameOffset = offset;
                nameLength = length;
                break;
            }
        }
        if (nameOffset < 0 || !fits(nameOffset, nameLength, fileLength)
                || nameLength < NAME_HEADER_SIZE) {
            return List.of();
        }
        input.seek(nameOffset + 2);
        int recordCount = input.readUnsignedShort();
        int stringOffset = input.readUnsignedShort();
        long recordsEnd = NAME_HEADER_SIZE + (long) recordCount * NAME_RECORD_SIZE;
        if (recordsEnd > nameLength || stringOffset < recordsEnd) {
            return List.of();
        }
        List<NameRecord> records = new ArrayList<>();
        input.seek(nameOffset + NAME_HEADER_SIZE);
        for (int index = 0; index < recordCount; index++) {
            int platformId = input.readUnsignedShort();
            int encodingId = input.readUnsignedShort();
            int languageId = input.readUnsignedShort();
            int nameId = input.readUnsignedShort();
            int length = input.readUnsignedShort();
            int offset = input.readUnsignedShort();
            long valueOffset = nameOffset + stringOffset + offset;
            if (length == 0 || !fits(valueOffset, length, nameOffset + nameLength)) {
                continue;
            }
            long resumeAt = input.getFilePointer();
            byte[] value = new byte[length];
            input.seek(valueOffset);
            input.readFully(value);
            input.seek(resumeAt);
            String decoded = decodeName(value, platformId, encodingId);
            if (decoded != null && !decoded.isBlank()) {
                records.add(new NameRecord(nameId, languageId, decoded.trim()));
            }
        }
        return records;
    }

    private static String preferredName(List<NameRecord> records, int requestedNameId) {
        NameRecord fallback = null;
        for (NameRecord record : records) {
            if (record.nameId != requestedNameId) {
                continue;
            }
            if (record.languageId == WINDOWS_ENGLISH_US || record.languageId == 0) {
                return record.value;
            }
            if (fallback == null) {
                fallback = record;
            }
        }
        return fallback != null ? fallback.value : null;
    }

    private static String decodeName(byte[] value, int platformId, int encodingId) {
        Charset charset;
        if (platformId == 0 || platformId == 3) {
            charset = StandardCharsets.UTF_16BE;
        } else if (platformId == 1 && encodingId == 0) {
            charset = Charset.forName("x-MacRoman");
        } else {
            return null;
        }
        return new String(value, charset).replace('\u0000', ' ').trim();
    }

    private static boolean fits(long offset, long length, long limit) {
        return offset >= 0 && length >= 0 && offset <= limit && length <= limit - offset;
    }

    private static final class NameRecord {
        final int nameId;
        final int languageId;
        final String value;

        NameRecord(int nameId, int languageId, String value) {
            this.nameId = nameId;
            this.languageId = languageId;
            this.value = value;
        }
    }
}
