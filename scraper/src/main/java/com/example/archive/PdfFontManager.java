package com.example.archive;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfFontManager {

    private final PDType0Font regular;
    private final PDType0Font bold;
    private final PDType0Font italic;
    private final PDType0Font boldItalic;

    private final List<PDFont> fallbackFonts = new ArrayList<>();

    public PdfFontManager(PDDocument document) throws IOException {

        regular = loadFont(
                document,
                "NotoSans-Regular.ttf"
        );

        bold = loadFont(
                document,
                "NotoSans-Bold.ttf"
        );

        italic = loadFont(
                document,
                "NotoSans-Italic.ttf"
        );

        boldItalic = loadFont(
                document,
                "NotoSans-BoldItalic.ttf"
        );

        fallbackFonts.add(
                loadFont(
                        document,
                        "NotoSansMath-Regular.ttf"
                )
        );

        fallbackFonts.add(
                loadFont(
                        document,
                        "NotoSansSymbols-Regular.ttf"
                )
        );

        fallbackFonts.add(
                loadFont(
                        document,
                        "NotoSansSymbols2-Regular.ttf"
                )
        );
    }

    public PDFont getRegular() {
        return regular;
    }

    public PDFont getBold() {
        return bold;
    }

    public PDFont getItalic() {
        return italic;
    }

    public PDFont getBoldItalic() {
        return boldItalic;
    }

    public PDFont getFontForCharacter(
        String character,
        PDFont primaryFont
    ) throws IOException {

        if (isWhitespace(character)) {
            return primaryFont;
        }

        if (supportsCharacter(primaryFont, character)) {
            return primaryFont;
        }

        for (PDFont fallbackFont : fallbackFonts) {

            if (supportsCharacter(fallbackFont, character)) {
                return fallbackFont;
            }
        }

        throw new IllegalArgumentException(
                "No available font can render character: "
                        + character
        );
    }

    private boolean isWhitespace(String character) {

        if (character == null || character.isEmpty()) {
            return true;
        }

        int codePoint = character.codePointAt(0);

        return Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || codePoint == 0x3164; // Hangul Filler
    }
    private boolean supportsCharacter(
            PDFont font,
            String character
    ) {

        try {
            font.getStringWidth(character);
            return true;

        } catch (IllegalArgumentException | IOException e) {
            return false;
        }
    }

    private PDType0Font loadFont(
            PDDocument document,
            String fontName
    ) throws IOException {

        var inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "fonts/" + fontName
                );

        if (inputStream == null) {
            throw new IOException(
                    "Could not find font: " + fontName
            );
        }

        return PDType0Font.load(
                document,
                inputStream
        );
    }
}