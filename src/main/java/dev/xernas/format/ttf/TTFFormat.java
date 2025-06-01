package dev.xernas.format.ttf;

import dev.xernas.format.IFileFormat;
import dev.xernas.reader.BinaryReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TTFFormat implements IFileFormat {

    private static final int DEFAULT_PLATFORM_ID = 0;
    private static final int DEFAULT_ENCODING_ID = 3;

    private final int platformID;
    private final int encodingID;

    private final Map<String, TableDirectory> tableDirectories = new HashMap<>();
    private HeadTable headTable;
    private MaximumProfileTable maximumProfileTable;
    private LocationTable locationTable;
    private GlyphTable glyphTable;
    private NameTable nameTable;
    private CharacterMappingTable characterMappingTable;

    public TTFFormat(int platformID, int encodingID) {
        this.platformID = platformID;
        this.encodingID = encodingID;
    }

    @Override
    public void readFile(File file) throws IOException {
        try (BinaryReader binaryReader = new BinaryReader(new FileInputStream(file), ByteOrder.BIG_ENDIAN)) {
            binaryReader.skip(4);
            int numTables = binaryReader.readUInt16();
            binaryReader.skip(6);
            for (int i = 0; i < numTables; i++) {
                String tag = new String(binaryReader.readBytes(4));
                long checkSum = binaryReader.readUInt32();
                long offset = binaryReader.readUInt32();
                long length = binaryReader.readUInt32();
                tableDirectories.put(tag, new TableDirectory(tag, checkSum, offset, length));
            }
            int basePos = binaryReader.getPos();
            this.headTable = readHeadTable(binaryReader);
            binaryReader.setPos(basePos);
            this.maximumProfileTable = readMaximumProfileTable(binaryReader);
            binaryReader.setPos(basePos);
            this.locationTable = readLocationTable(binaryReader);
            binaryReader.setPos(basePos);
            this.glyphTable = readGlyphTable(binaryReader);
            binaryReader.setPos(basePos);
            this.nameTable = readNameTable(binaryReader);
            binaryReader.setPos(basePos);
            this.characterMappingTable = readCharacterMappingTable(binaryReader, platformID, encodingID);
        }
    }

    private HeadTable readHeadTable(BinaryReader binaryReader) {
        TableDirectory tableDirectory = tableDirectories.get("head");
        binaryReader.setPos((int) tableDirectory.offset());
        binaryReader.skip(16);
        int flags = binaryReader.readUInt16();
        int unitsPerEm = binaryReader.readUInt16();
        binaryReader.skip(16); // Skipping creation and modification times
        binaryReader.skip(8); // Skipping xMin, yMin, xMax, yMax
        binaryReader.skip(4); // Skipping macStyle and lowestRecPPEM
        binaryReader.skip(2); // Skipping fontDirectionHint
        short indexToLocFormat = binaryReader.readInt16();
        return new HeadTable(flags, unitsPerEm, indexToLocFormat);
    }

    private MaximumProfileTable readMaximumProfileTable(BinaryReader binaryReader) {
        TableDirectory tableDirectory = tableDirectories.get("maxp");
        binaryReader.setPos((int) tableDirectory.offset());
        binaryReader.skip(4);
        int numGlyphs = binaryReader.readUInt16();
        return new MaximumProfileTable(numGlyphs);
    }

    private LocationTable readLocationTable(BinaryReader binaryReader) {
        if (headTable == null) throw new IllegalStateException("Head table not read yet");
        if (maximumProfileTable == null) throw new IllegalStateException("Maximum profile table not read yet");
        TableDirectory tableDirectory = tableDirectories.get("loca");
        binaryReader.setPos((int) tableDirectory.offset());

        short indexToLocFormat = headTable.indexToLocFormat();
        int numGlyphs = maximumProfileTable.numGlyphs();
        int[] glyphOffsets = new int[numGlyphs + 1];
        if (indexToLocFormat == 0) { // short format
            for (int i = 0; i <= numGlyphs; i++) {
                glyphOffsets[i] = binaryReader.readUInt16() * 2;
            }
        } else { // long format
            for (int i = 0; i <= numGlyphs; i++) {
                glyphOffsets[i] = (int) binaryReader.readUInt32();
            }
        }
        return new LocationTable(glyphOffsets);
    }

    private GlyphTable readGlyphTable(BinaryReader binaryReader) {
        if (locationTable == null) throw new IllegalStateException("Location table not read yet");
        TableDirectory tableDirectory = tableDirectories.get("glyf");

        int numGlyphs = maximumProfileTable.numGlyphs();
        int[] glyphOffsets = locationTable.offsets();
        List<GlyphTable.Glyph> glyphs = new ArrayList<>(numGlyphs);
        for (int i = 0; i < numGlyphs; i++) {
            int glyphOffset = glyphOffsets[i];
            int glyphLength = glyphOffsets[i + 1] - glyphOffset;

            if (glyphLength == 0) continue; // Empty glyph

            binaryReader.setPos((int) tableDirectory.offset() + glyphOffset);
            // Read the glyph data
            short numberOfContours = binaryReader.readInt16();
            short xMin = binaryReader.readInt16();
            short yMin = binaryReader.readInt16();
            short xMax = binaryReader.readInt16();
            short yMax = binaryReader.readInt16();
            // Skip the rest of the glyph data for now
            binaryReader.skip(glyphLength - 10); // 10 bytes for the header
            glyphs.add(new GlyphTable.Glyph(numberOfContours, xMin, yMin, xMax, yMax));
        }
        return new GlyphTable(glyphs);
    }

    private NameTable readNameTable(BinaryReader binaryReader) {
        TableDirectory nameTable = tableDirectories.get("name");
        binaryReader.setPos((int) nameTable.offset());
        binaryReader.skip(2);
        int nameCount = binaryReader.readUInt16();
        int stringOffset = binaryReader.readUInt16();
        List<NameTable.NameRecord> nameRecords = new ArrayList<>();
        for (int i = 0; i < nameCount; i++) {
            int platformId = binaryReader.readUInt16();
            int encodingId = binaryReader.readUInt16();
            int languageId = binaryReader.readUInt16();
            int nameId = binaryReader.readUInt16();
            int length = binaryReader.readUInt16();
            int offset = binaryReader.readUInt16();

            int lastPos = binaryReader.getPos();
            binaryReader.setPos((int) nameTable.offset() + stringOffset + offset);
            byte[] nameBytes = binaryReader.readBytes(length);
            String name = new String(nameBytes, (platformId == 0 || platformId == 3) ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_8);
            nameRecords.add(new NameTable.NameRecord(platformId, encodingId, languageId, nameId, name));
            binaryReader.setPos(lastPos);
        }
        return new NameTable(nameRecords);
    }

    private CharacterMappingTable readCharacterMappingTable(BinaryReader binaryReader, int platformID, int encodingID) {
        TableDirectory cmapTable = tableDirectories.get("cmap");
        binaryReader.setPos((int) cmapTable.offset());

        int version = binaryReader.readUInt16();
        int numTables = binaryReader.readUInt16();
        List<CharacterMappingTable.EncodingRecord> records = new ArrayList<>();
        for (int i = 0; i < numTables; i++) {
            int platformId = binaryReader.readUInt16();
            int encodingId = binaryReader.readUInt16();
            long offset = binaryReader.readUInt32();
            records.add(new CharacterMappingTable.EncodingRecord(platformId, encodingId, offset));
        }

        for (CharacterMappingTable.EncodingRecord record : records) {
            int absoluteOffset = (int) (cmapTable.offset() + record.offset());
            binaryReader.setPos(absoluteOffset);
            int format = binaryReader.readUInt16();
            if (record.platformID() != platformID || record.encodingID() != encodingID) continue; // Skip if not matching
            return switch (format) {
                case 4 -> readCmapFormat4(binaryReader, absoluteOffset, records);
                default -> new CharacterMappingTable(format, records); // Unsupported format
            };
        }
        return new CharacterMappingTable(-1, records); // No matching format found with the given platform and encoding
    }

    public CharacterMappingTable readCmapFormat4(BinaryReader binaryReader, int offset, List<CharacterMappingTable.EncodingRecord> records) {
        int format = 4; // already read
        int length = binaryReader.readUInt16();
        int language = binaryReader.readUInt16();

        int segCountX2 = binaryReader.readUInt16();
        int segCount = segCountX2 / 2;

        int searchRange = binaryReader.readUInt16();
        int entrySelector = binaryReader.readUInt16();
        int rangeShift = binaryReader.readUInt16();

        int[] endCode = new int[segCount];
        for (int i = 0; i < segCount; i++) endCode[i] = binaryReader.readUInt16();

        int reservedPad = binaryReader.readUInt16();

        int[] startCode = new int[segCount];
        for (int i = 0; i < segCount; i++) startCode[i] = binaryReader.readUInt16();

        int[] idDelta = new int[segCount];
        for (int i = 0; i < segCount; i++) idDelta[i] = binaryReader.readInt16();

        int[] idRangeOffset = new int[segCount];
        for (int i = 0; i < segCount; i++) idRangeOffset[i] = binaryReader.readUInt16();

        int glyphArrayStart = binaryReader.getPos();
        int remaining = length - (glyphArrayStart - offset);
        byte[] glyphData = binaryReader.readBytes(remaining);

        return new CharacterMappingTable(format, records, segCount, startCode, endCode, idDelta, idRangeOffset, glyphData, glyphArrayStart);
    }

    public static TTFFormat create(File file) throws IOException {
        return create(file, DEFAULT_PLATFORM_ID, DEFAULT_ENCODING_ID);
    }

    public static TTFFormat create(File file, int platformID, int encodingID) throws IOException {
        TTFFormat format = new TTFFormat(platformID, encodingID);
        format.readFile(file);
        return format;
    }

    public boolean isValid() {
        return headTable != null && maximumProfileTable != null && locationTable != null && glyphTable != null && nameTable != null && characterMappingTable != null && characterMappingTable.hasMappings();
    }

    public GlyphTable.Glyph glyphForChar(Character character) {
        int glyphIndex = characterMappingTable.charToGlyph(character);
        if (glyphIndex < 0 || glyphIndex >= glyphTable.glyphs().size()) return null;
        return glyphTable.glyphs().get(glyphIndex);
    }

    public List<GlyphTable.Glyph> strToGlyphs(String text) {
        List<Integer> glyphIndices = characterMappingTable.stringToGlyphs(text);
        List<GlyphTable.Glyph> glyphs = new ArrayList<>();
        for (int glyphIndex : glyphIndices) glyphs.add(glyphTable.glyphs().get(glyphIndex));
        return glyphs;
    }

    public Map<Integer, List<Integer>> getAllAvailableEncodings() {
        Map<Integer, List<Integer>> encodings = new HashMap<>();
        for (Integer platform : getAvailablePlatforms()) {
            List<Integer> encodingList = getAvailableEncodings(platform);
            if (!encodingList.isEmpty()) encodings.put(platform, encodingList);
        }
        return encodings;
    }

    public List<Integer> getAvailablePlatforms() {
        List<Integer> platforms = new ArrayList<>();
        for (CharacterMappingTable.EncodingRecord record : characterMappingTable.getEncodingRecords()) {
            if (!platforms.contains(record.platformID())) platforms.add(record.platformID());
        }
        return platforms;
    }

    public List<Integer> getAvailableEncodings(int platformID) {
        List<Integer> encodings = new ArrayList<>();
        for (CharacterMappingTable.EncodingRecord record : characterMappingTable.getEncodingRecords()) {
            if (record.platformID() == platformID && !encodings.contains(record.encodingID())) {
                encodings.add(record.encodingID());
            }
        }
        return encodings;
    }

    public HeadTable getHeadTable() {
        return headTable;
    }

    public MaximumProfileTable getMaximumProfileTable() {
        return maximumProfileTable;
    }

    public LocationTable getLocationTable() {
        return locationTable;
    }

    public GlyphTable getGlyphTable() {
        return glyphTable;
    }

    public NameTable getNameTable() {
        return nameTable;
    }

    public CharacterMappingTable getCharacterMappingTable() {
        return characterMappingTable;
    }

    public TableDirectory getTableDirectory(String tag) {
        return tableDirectories.get(tag);
    }

}
