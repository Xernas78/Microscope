package dev.xernas.format.ttf;

import java.util.List;

public record GlyphTable(List<Glyph> glyphs) {

    public record Glyph(short numberOfContours, short xMin, short yMin, short xMax, short yMax) {

        @Override
        public String toString() {
            return "Glyph{" +
                    "numberOfContours=" + numberOfContours +
                    ", xMin=" + xMin +
                    ", yMin=" + yMin +
                    ", xMax=" + xMax +
                    ", yMax=" + yMax +
                    '}';
        }

    }

}
