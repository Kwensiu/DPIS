package com.dpis.module;

import com.dpis.module.fonts.FontFaceNameResolver;
import com.dpis.module.fonts.FontFileKind;
import com.dpis.module.fonts.FontLibraryEntry;
import com.dpis.module.fonts.FontLibraryStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class FontFaceNameResolverTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolvesEmbeddedTtcFamilyAndStyle() throws Exception {
        File file = temporaryFolder.newFile("Named.ttc");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(namedTtc("Irianis ADF Std", "Bold Italic"));
        }

        assertEquals("Irianis ADF Std Bold Italic",
                FontFaceNameResolver.resolveTtcFaceLabel(file, 0, "Named (TTC 0)"));
    }

    @Test
    public void keepsFallbackWhenNameTableCannotBeRead() throws Exception {
        File file = temporaryFolder.newFile("Unnamed.ttc");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] { 't', 't', 'c', 'f' });
        }

        assertEquals("Unnamed (TTC 0)",
                FontFaceNameResolver.resolveTtcFaceLabel(file, 0, "Unnamed (TTC 0)"));
    }

    @Test
    public void storesCollectionAliasSeparatelyFromEmbeddedFaceLabel() throws Exception {
        File file = temporaryFolder.newFile("Named.ttc");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(namedTtc("Irianis ADF Std", "Bold Italic"));
        }
        FontLibraryStore store = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("fonts"));

        List<FontLibraryEntry> faces = store.registerCopiedFontFaces(
                file,
                "Named.ttc",
                "My collection",
                FontFileKind.TTC,
                List.of(0),
                1234L);

        assertEquals("Irianis ADF Std Bold Italic", faces.get(0).displayName);
        assertEquals("My collection", faces.get(0).collectionDisplayName);
    }

    private static byte[] namedTtc(String family, String style) throws Exception {
        byte[] familyBytes = family.getBytes(StandardCharsets.UTF_16BE);
        byte[] styleBytes = style.getBytes(StandardCharsets.UTF_16BE);
        int nameTableLength = 6 + 2 * 12 + familyBytes.length + styleBytes.length;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(0x74746366); // ttcf
            output.writeInt(0x00010000);
            output.writeInt(1);
            output.writeInt(16);

            output.writeInt(0x00010000);
            output.writeShort(1);
            output.writeShort(0);
            output.writeShort(0);
            output.writeShort(0);
            output.writeInt(0x6E616D65); // name
            output.writeInt(0);
            output.writeInt(44);
            output.writeInt(nameTableLength);

            output.writeShort(0);
            output.writeShort(2);
            output.writeShort(30);
            writeNameRecord(output, 1, familyBytes.length, 0);
            writeNameRecord(output, 2, styleBytes.length, familyBytes.length);
            output.write(familyBytes);
            output.write(styleBytes);
        }
        return bytes.toByteArray();
    }

    private static void writeNameRecord(DataOutputStream output, int nameId, int length, int offset)
            throws Exception {
        output.writeShort(3);
        output.writeShort(1);
        output.writeShort(0x0409);
        output.writeShort(nameId);
        output.writeShort(length);
        output.writeShort(offset);
    }
}
