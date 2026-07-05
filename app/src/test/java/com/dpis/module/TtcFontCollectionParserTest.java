package com.dpis.module;

import com.dpis.module.fonts.FontFileInspector;
import com.dpis.module.fonts.FontFileKind;
import com.dpis.module.fonts.TtcFontCollectionParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TtcFontCollectionParserTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void parsesValidTtcHeader() throws Exception {
        File file = writeTtc(0x00010000, 48, 96);

        TtcFontCollectionParser.Result result = TtcFontCollectionParser.parse(file);

        assertTrue(result.valid);
        assertEquals(0x00010000, result.version);
        assertEquals(List.of(48L, 96L), result.offsets);
    }

    @Test
    public void rejectsNonTtcSignature() throws Exception {
        File file = temporaryFolder.newFile("Example.ttf");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] {0x00, 0x01, 0x00, 0x00});
        }

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void rejectsZeroFaceCount() throws Exception {
        File file = writeRawHeader(0x74746366, 0x00010000, 0);

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void rejectsOversizedFaceCount() throws Exception {
        File file = writeRawHeader(0x74746366, 0x00010000, 129);

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void rejectsOffsetsOutsideFile() throws Exception {
        File file = writeRawTtcWithOffsets(0x00010000, 4096);

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void inspectorClassifiesRenamedTtcBySignature() throws Exception {
        File file = writeTtcNamed("Renamed.ttf", 0x00010000, 48);

        FontFileInspector.Result result = FontFileInspector.inspect(file);

        assertEquals(FontFileKind.TTC, result.kind);
        assertTrue(result.ttc.valid);
        assertEquals(List.of(48L), result.ttc.offsets);
    }

    @Test
    public void inspectorClassifiesOpenTypeBySignature() throws Exception {
        File file = temporaryFolder.newFile("Example.bin");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] {'O', 'T', 'T', 'O', 0, 0, 0, 0});
        }

        assertEquals(FontFileKind.OTF, FontFileInspector.inspect(file).kind);
    }

    private File writeTtc(int version, long... offsets) throws Exception {
        return writeTtcNamed("Collection.ttc", version, offsets);
    }

    private File writeTtcNamed(String name, int version, long... offsets) throws Exception {
        File file = temporaryFolder.newFile(name);
        int size = (int) Math.max(128, max(offsets) + 16);
        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x74746366);
        buffer.putInt(version);
        buffer.putInt(offsets.length);
        for (long offset : offsets) {
            buffer.putInt((int) offset);
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(buffer.array());
        }
        return file;
    }

    private File writeRawHeader(int signature, int version, int count) throws Exception {
        File file = temporaryFolder.newFile("Broken.ttc");
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(signature);
        buffer.putInt(version);
        buffer.putInt(count);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(buffer.array());
        }
        return file;
    }

    private File writeRawTtcWithOffsets(int version, long... offsets) throws Exception {
        File file = temporaryFolder.newFile("BrokenOffset.ttc");
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x74746366);
        buffer.putInt(version);
        buffer.putInt(offsets.length);
        for (long offset : offsets) {
            buffer.putInt((int) offset);
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(buffer.array());
        }
        return file;
    }

    private static long max(long[] values) {
        long result = 0L;
        for (long value : values) {
            result = Math.max(result, value);
        }
        return result;
    }
}
