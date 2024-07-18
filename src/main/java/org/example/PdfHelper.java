package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class PdfHelper {
    public static List<String> loadPdfData(String filePath) throws IOException {
        PDDocument document = Loader.loadPDF(new File(filePath));
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return splitIntoParagraphs(text);
    }

    public static List<String> splitIntoParagraphs(String text) {
        String[] paragraphsArray = text.split("\\r?\\n\\r?\\n");
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphsArray) {
            if (!paragraph.trim().isEmpty()) {
                if (currentChunk.length() >= Config.MIN_CHUNK_SIZE) {
                    paragraphs.add(currentChunk.toString());
                    currentChunk.setLength(0);
                    currentChunk.append(paragraph.trim());
                } else {
                    currentChunk.append("\n\n").append(paragraph.trim());
                    paragraphs.addAll(splitIntoChunks(currentChunk.toString(), Config.MAX_CHUNK_SIZE, 60));
                    currentChunk.setLength(0);
                }
            }
        }

        if (!currentChunk.isEmpty()) {
            paragraphs.add(currentChunk.toString());
        }

        return paragraphs;
    }

    public static List<String> splitIntoChunks(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int start = 0; start < length; start += chunkSize - chunkOverlap) {
            int end = Math.min(length, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }
}