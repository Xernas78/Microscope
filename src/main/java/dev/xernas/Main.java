package dev.xernas;

import dev.xernas.format.ttf.GlyphTable;
import dev.xernas.format.ttf.TTFFormat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class Main {

    public static void main(String[] args) throws IOException, URISyntaxException {
        File ttfFile = new File(Objects.requireNonNull(Main.class.getClassLoader().getResource("JetBrainsMono-Bold.ttf")).toURI());
        TTFFormat format = TTFFormat.create(ttfFile);
        if (!format.isValid()) {
            System.err.println("Invalid TTF file.");
            return;
        }
        String text = "Hello, World!";
        List<GlyphTable.Glyph> glyphs = format.strToGlyphs(text);
        for (GlyphTable.Glyph glyph : glyphs) System.out.println(glyph);
    }

}