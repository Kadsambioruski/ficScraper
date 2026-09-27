package com.example.archive;

import com.example.model.Fiction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.IOException;

import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

public class PdfGenerator {

    private static final Path ARCHIVE_DIR = Path.of("data", "archive");

    private static final float MARGIN = 50;

    private static final float FONT_SIZE = 11;
    private static final float LEADING = 16;

    private static final float TITLE_SIZE = 16;
    private static final float HEADING_SIZE = 14;

    private static final float PARAGRAPH_SPACING = 10;
    private static final float BLOCK_SPACING = 12;

    public void generate(
            Fiction fiction,
            List<String> chapterNames,
            List<String> chapterContents
    ) throws IOException {

        Files.createDirectories(ARCHIVE_DIR);

        Path outputPath = ARCHIVE_DIR.resolve(
                fiction.getId() + ".pdf"
        );

        try (PDDocument document = new PDDocument()) {
            PdfFontManager fonts = new PdfFontManager(document);

            for (int i = 0; i < chapterContents.size(); i++) {
                addChapter(
                        document,
                        fonts,
                        chapterNames.get(i),
                        chapterContents.get(i)
                );
            }

            document.save(outputPath.toFile());
        }
    }

    private void addChapter(
            PDDocument document,
            PdfFontManager fonts,
            String chapterName,
            String html
    ) throws IOException {

        PageState page = createPage(document);

        writeTitle(page, fonts, chapterName);

        Element root = Jsoup.parseBodyFragment(html).body();

        for (Node node : root.childNodes()) {
            if (!(node instanceof Element element)) {
                continue;
            }

            renderBlock(document, fonts, page, element);
        }

        page.close();
    }

    private static String normalizeText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("\u3164", " ")
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\uFEFF", "");
    }

    private void renderBlock(
            PDDocument document,
            PdfFontManager fonts,
            PageState page,
            Element element
    ) throws IOException {

        String tag = element.tagName();

        switch (tag) {
            case "p":
                renderParagraph(page, fonts, element, FONT_SIZE);
                break;

            case "h1":
                renderParagraph(page, fonts, element, 18);
                break;

            case "h2":
                renderParagraph(page, fonts, element, HEADING_SIZE);
                break;

            case "h3":
                renderParagraph(page, fonts, element, 13);
                break;

            case "h4":
            case "h5":
            case "h6":
                renderParagraph(page, fonts, element, FONT_SIZE);
                break;

            case "hr":
                renderHorizontalRule(page);
                break;

            case "table":
                renderTable(document, fonts, page, element);
                break;

            case "img":
                renderImage(document, page, element);
                break;

            default:
                renderContainer(document, fonts, page, element);
                break;
        }
    }

    private void renderContainer(
            PDDocument document,
            PdfFontManager fonts,
            PageState page,
            Element element
    ) throws IOException {

        for (Node child : element.childNodes()) {
            if (child instanceof Element childElement) {
                renderBlock(document, fonts, page, childElement);
            }
        }
    }

    private void renderParagraph(
            PageState page,
            PdfFontManager fonts,
            Element element,
            float fontSize
    ) throws IOException {

        List<TextRun> runs = new ArrayList<>();

        collectTextRuns(
                element,
                fonts,
                false,
                false,
                runs
        );

        List<List<TextRun>> lines = wrapRuns(
                runs,
                fonts,
                fontSize,
                page.usableWidth()
        );

        for (List<TextRun> line : lines) {
            page.ensureSpace(LEADING);
            page.writeLine(line, fonts, fontSize);
        }

        page.moveDown(PARAGRAPH_SPACING);
    }

    private void collectTextRuns(
            Node node,
            PdfFontManager fonts,
            boolean bold,
            boolean italic,
            List<TextRun> runs
    ) {

        if (node instanceof TextNode textNode) {
            String text = normalizeWhitespace(
                    textNode.getWholeText()
            );

            if (!text.isEmpty()) {
                runs.add(new TextRun(text, bold, italic));
            }

            return;
        }

        if (!(node instanceof Element element)) {
            return;
        }

        String tag = element.tagName();

        if (tag.equals("br")) {
            runs.add(new TextRun("\n", bold, italic));
            return;
        }

        boolean childBold =
                bold || tag.equals("strong") || tag.equals("b");

        boolean childItalic =
                italic || tag.equals("em") || tag.equals("i");

        for (Node child : element.childNodes()) {
            collectTextRuns(
                    child,
                    fonts,
                    childBold,
                    childItalic,
                    runs
            );
        }
    }

    private String normalizeWhitespace(String text) {
        return normalizeText(text)
                .replaceAll("[\\t\\r\\n]+", " ")
                .replaceAll(" {2,}", " ");
    }

    private List<List<TextRun>> wrapRuns(
            List<TextRun> runs,
            PdfFontManager fonts,
            float fontSize,
            float maxWidth
    ) throws IOException {

        List<List<TextRun>> lines = new ArrayList<>();
        List<TextRun> currentLine = new ArrayList<>();

        float currentWidth = 0;

        for (TextRun run : runs) {
            if (run.text().equals("\n")) {
                lines.add(currentLine);
                currentLine = new ArrayList<>();
                currentWidth = 0;
                continue;
            }

            String[] words = run.text().split(" ");

            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }

                String prefix = currentLine.isEmpty() ? "" : " ";

                TextRun candidate = new TextRun(
                        prefix + word,
                        run.bold(),
                        run.italic()
                );

                float candidateWidth = getRunWidth(
                        candidate,
                        fonts,
                        fontSize
                );

                if (currentWidth + candidateWidth > maxWidth
                        && !currentLine.isEmpty()) {

                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                    currentWidth = 0;

                    candidate = new TextRun(
                            word,
                            run.bold(),
                            run.italic()
                    );

                    candidateWidth = getRunWidth(
                            candidate,
                            fonts,
                            fontSize
                    );
                }

                currentLine.add(candidate);
                currentWidth += candidateWidth;
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    private float getRunWidth(
            TextRun run,
            PdfFontManager fonts,
            float fontSize
    ) throws IOException {

        PDFont primaryFont = getPrimaryFont(
                fonts,
                run.bold(),
                run.italic()
        );

        float width = 0;
        String text = normalizeText(run.text());

        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);

            String character = new String(
                    Character.toChars(codePoint)
            );

            PDFont font = fonts.getFontForCharacter(
                    character,
                    primaryFont
            );

            width += font.getStringWidth(character)
                    / 1000
                    * fontSize;

            i += Character.charCount(codePoint);
        }

        return width;
    }

    private PDFont getPrimaryFont(
            PdfFontManager fonts,
            boolean bold,
            boolean italic
    ) {

        if (bold && italic) {
            return fonts.getBoldItalic();
        }

        if (bold) {
            return fonts.getBold();
        }

        if (italic) {
            return fonts.getItalic();
        }

        return fonts.getRegular();
    }

    private void renderHorizontalRule(PageState page)
            throws IOException {

        page.ensureSpace(20);

        page.stream().moveTo(
                page.margin(),
                page.y()
        );

        page.stream().lineTo(
                page.pageWidth() - page.margin(),
                page.y()
        );

        page.stream().stroke();

        page.moveDown(BLOCK_SPACING);
    }

    private void renderTable(
            PDDocument document,
            PdfFontManager fonts,
            PageState page,
            Element table
    ) throws IOException {

        List<Element> rows = table.select("> tbody > tr");

        if (rows.isEmpty()) {
            rows = table.select("> tr");
        }

        if (rows.isEmpty()) {
            return;
        }

        float tableWidth = page.usableWidth();

        int columnCount = 1;

        for (Element row : rows) {
            int count = row.select("> td, > th").size();

            columnCount = Math.max(columnCount, count);
        }

        float cellWidth = tableWidth / columnCount;

        for (Element row : rows) {
            ElementsWrapper cells = new ElementsWrapper(
                    row.select("> td, > th")
            );

            List<List<List<TextRun>>> cellLines =
                    new ArrayList<>();

            float rowHeight = LEADING + 8;

            for (Element cell : cells.elements()) {
                List<TextRun> runs = new ArrayList<>();

                collectTextRuns(
                        cell,
                        fonts,
                        false,
                        false,
                        runs
                );

                List<List<TextRun>> lines = wrapRuns(
                        runs,
                        fonts,
                        FONT_SIZE,
                        cellWidth - 10
                );

                cellLines.add(lines);

                rowHeight = Math.max(
                        rowHeight,
                        lines.size() * LEADING + 8
                );
            }

            page.ensureSpace(rowHeight);

            float startX = page.margin();
            float startY = page.y();

            for (int i = 0; i < cells.size(); i++) {
                List<List<TextRun>> lines = cellLines.get(i);

                float x = startX + i * cellWidth;

                page.stream().addRect(
                        x,
                        startY - rowHeight,
                        cellWidth,
                        rowHeight
                );

                page.stream().stroke();

                float textY = startY - 14;

                for (List<TextRun> line : lines) {
                    page.stream().beginText();

                    page.stream().newLineAtOffset(
                            x + 5,
                            textY
                    );

                    writeRuns(
                            page.stream(),
                            line,
                            fonts,
                            FONT_SIZE
                    );

                    page.stream().endText();

                    textY -= LEADING;
                }
            }

            page.setY(startY - rowHeight - 4);
        }

        page.moveDown(BLOCK_SPACING);
    }

    private void writeRuns(
            PDPageContentStream stream,
            List<TextRun> runs,
            PdfFontManager fonts,
            float fontSize
    ) throws IOException {

        for (TextRun run : runs) {
            PDFont primaryFont = getPrimaryFont(
                    fonts,
                    run.bold(),
                    run.italic()
            );

            String text = normalizeText(run.text());

            for (int i = 0; i < text.length();) {
                int codePoint = text.codePointAt(i);

                String character = new String(
                        Character.toChars(codePoint)
                );

                PDFont font = fonts.getFontForCharacter(
                        character,
                        primaryFont
                );

                stream.setFont(font, fontSize);
                stream.showText(character);

                i += Character.charCount(codePoint);
            }
        }
    }

    private void renderImage(
            PDDocument document,
            PageState page,
            Element image
    ) throws IOException {

        String source = image.absUrl("src");

        if (source.isBlank()) {
            return;
        }

        BufferedImage bufferedImage;

        try {
            bufferedImage = ImageIO.read(
                    URI.create(source).toURL()
            );
        } catch (Exception e) {
            return;
        }

        if (bufferedImage == null) {
            return;
        }

        PDImageXObject pdfImage =
                LosslessFactory.createFromImage(
                        document,
                        bufferedImage
                );

        float maxWidth = page.usableWidth();
        float maxHeight = 400;

        float width = bufferedImage.getWidth();
        float height = bufferedImage.getHeight();

        float scale = Math.min(
                maxWidth / width,
                maxHeight / height
        );

        width *= scale;
        height *= scale;

        page.ensureSpace(height + 10);

        page.stream().drawImage(
                pdfImage,
                page.margin(),
                page.y() - height,
                width,
                height
        );

        page.setY(
                page.y() - height - BLOCK_SPACING
        );
    }

    private void writeTitle(
            PageState page,
            PdfFontManager fonts,
            String title
    ) throws IOException {

        page.stream().beginText();

        page.stream().newLineAtOffset(
                page.margin(),
                page.y()
        );

        writeRuns(
                page.stream(),
                List.of(
                        new TextRun(
                                title,
                                true,
                                false
                        )
                ),
                fonts,
                TITLE_SIZE
        );

        page.stream().endText();

        page.moveDown(30);
    }

    private PageState createPage(PDDocument document)
            throws IOException {

        return new PageState(document);
    }

    private record TextRun(
            String text,
            boolean bold,
            boolean italic
    ) {
    }

    private static class ElementsWrapper {

        private final List<Element> elements;

        ElementsWrapper(org.jsoup.select.Elements elements) {
            this.elements = new ArrayList<>(elements);
        }

        List<Element> elements() {
            return elements;
        }

        int size() {
            return elements.size();
        }
    }

    private static class PageState {

        private static final float PAGE_WIDTH =
                PDRectangle.A4.getWidth();

        private static final float PAGE_HEIGHT =
                PDRectangle.A4.getHeight();

        private final PDDocument document;
        private final float margin = MARGIN;

        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PageState(PDDocument document) throws IOException {
            this.document = document;
            createNewPage();
        }

        private void createNewPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            stream = new PDPageContentStream(
                    document,
                    page
            );

            y = PAGE_HEIGHT - margin;
        }

        void ensureSpace(float requiredHeight)
                throws IOException {

            if (y - requiredHeight < margin) {
                stream.close();
                createNewPage();
            }
        }

        void moveDown(float amount) {
            y -= amount;
        }

        float y() {
            return y;
        }

        void setY(float y) {
            this.y = y;
        }

        float margin() {
            return margin;
        }

        float pageWidth() {
            return PAGE_WIDTH;
        }

        float usableWidth() {
            return PAGE_WIDTH - margin * 2;
        }

        PDPageContentStream stream() {
            return stream;
        }

        void close() throws IOException {
            stream.close();
        }

        void writeLine(
                List<TextRun> line,
                PdfFontManager fonts,
                float fontSize
        ) throws IOException {

            stream.beginText();

            stream.newLineAtOffset(
                    margin,
                    y
            );

            for (TextRun run : line) {
                PDFont primaryFont =
                        getPrimaryFontStatic(
                                fonts,
                                run.bold(),
                                run.italic()
                        );

                String text = normalizeText(run.text());

                for (int i = 0; i < text.length();) {
                    int codePoint = text.codePointAt(i);

                    String character = new String(
                            Character.toChars(codePoint)
                    );

                    PDFont font =
                            fonts.getFontForCharacter(
                                    character,
                                    primaryFont
                            );

                    stream.setFont(font, fontSize);
                    stream.showText(character);

                    i += Character.charCount(codePoint);
                }
            }

            stream.endText();

            y -= LEADING;
        }

        private static PDFont getPrimaryFontStatic(
                PdfFontManager fonts,
                boolean bold,
                boolean italic
        ) {

            if (bold && italic) {
                return fonts.getBoldItalic();
            }

            if (bold) {
                return fonts.getBold();
            }

            if (italic) {
                return fonts.getItalic();
            }

            return fonts.getRegular();
        }
    }
}