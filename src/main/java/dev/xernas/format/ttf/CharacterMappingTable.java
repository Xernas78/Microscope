package dev.xernas.format.ttf;

import java.util.ArrayList;
import java.util.List;

public class CharacterMappingTable {

    private final int format;
    private final List<EncodingRecord> encodingRecords;
    private final boolean hasMappings;

    // Format 4
    private int segCount;
    private int[] startCode;
    private int[] endCode;
    private int[] idDelta;
    private int[] idRangeOffset;
    private byte[] glyphData;
    private int glyphArrayOffset;

    public CharacterMappingTable(int format, List<EncodingRecord> encodingRecords) {
        this.format = format;
        this.encodingRecords = encodingRecords;
        this.hasMappings = false;
    }

    public CharacterMappingTable(int format, List<EncodingRecord> encodingRecords, int segCount, int[] startCode, int[] endCode, int[] idDelta, int[] idRangeOffset, byte[] glyphData, int glyphArrayOffset) {
        this.format = format;
        this.encodingRecords = encodingRecords;
        this.segCount = segCount;
        this.startCode = startCode;
        this.endCode = endCode;
        this.idDelta = idDelta;
        this.idRangeOffset = idRangeOffset;
        this.glyphData = glyphData;
        this.glyphArrayOffset = glyphArrayOffset;
        this.hasMappings = true;
    }

    public int getFormat() {
        return format;
    }

    public List<EncodingRecord> getEncodingRecords() {
        return encodingRecords;
    }

    public boolean hasMappings() {
        return hasMappings;
    }

    public int getSegCount() {
        return segCount;
    }

    public int[] getStartCode() {
        return startCode;
    }

    public int[] getEndCode() {
        return endCode;
    }

    public int[] getIdDelta() {
        return idDelta;
    }

    public int[] getIdRangeOffset() {
        return idRangeOffset;
    }

    public byte[] getGlyphData() {
        return glyphData;
    }

    public int getGlyphArrayOffset() {
        return glyphArrayOffset;
    }

    public int mapCharToGlyphForFormat4(int charCode) {
        if (!hasMappings) return 0;
        for (int i = 0; i < segCount; i++) {
            if (charCode >= startCode[i] && charCode <= endCode[i]) {
                if (idRangeOffset[i] == 0) {
                    return (charCode + idDelta[i]) & 0xFFFF;
                } else {
                    int offset = idRangeOffset[i];
                    int idx = (offset / 2 + (charCode - startCode[i])) + i - segCount;
                    if (idx * 2 >= glyphData.length - 1) return 0;
                    int glyphIndex = ((glyphData[idx * 2] & 0xFF) << 8) | (glyphData[idx * 2 + 1] & 0xFF);
                    if (glyphIndex != 0) {
                        glyphIndex = (glyphIndex + idDelta[i]) & 0xFFFF;
                    }
                    return glyphIndex;
                }
            }
        }
        return 0; // not found
    }

    public int mapCharToGlyph(int charCode) {
        return switch (format) {
            case 4 -> mapCharToGlyphForFormat4(charCode);
            default -> 0; // unsupported format
        };
    }

    public int charToGlyph(Character character) {
        return mapCharToGlyph(character);
    }

    public List<Integer> stringToGlyphs(String text) {
        List<Integer> glyphs = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(codePoint)) i++; // skip extra char
            glyphs.add(mapCharToGlyph(codePoint));
        }
        return glyphs;
    }

    public record EncodingRecord(int platformID, int encodingID, long offset) {

    }

}
