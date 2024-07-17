package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String PDF_FILE_PATH = "./files/Nietzsche.pdf";
    private static final String PROMPT = "Who is over-man?";
    private static final int NUM_HINTS = 3;
    private static final int MAX_CHUNK_SIZE = 1000;
    public static void main(String[] args) throws Exception {
        long ms = System.currentTimeMillis();
        // Initialize Lucene analyzer and index writer configuration
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Path indexPath = Files.createTempDirectory("tempIndex");
        Directory directory = FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);

        // Read text from PDF file
        List<String> chunks = loadPdfData(PDF_FILE_PATH);

        // Create a document for each chunk and add it to the index
        for (String chunk : chunks) {
            Document doc = new Document();
            doc.add(new TextField("content", chunk, Field.Store.YES));
            iwriter.addDocument(doc);
        }
        iwriter.close();

        System.out.println("Documents indexed");

        // Search for the most relevant document
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        QueryParser parser = new QueryParser("content", analyzer);
        Query query = parser.parse(PROMPT);
        ScoreDoc[] hits = isearcher.search(query, NUM_HINTS).scoreDocs;

        StringBuilder data = new StringBuilder();
        for (ScoreDoc hit : hits) {
            Document hitDoc = isearcher.doc(hit.doc);
            data.append(hitDoc.get("content"));
        }
        System.out.println("Search duration is: " + (System.currentTimeMillis() - ms) + "ms");
        // Generate a response using the retrieved document
        JSONObject generateResponse = OllamaClient.generate("Using this data: " + data + ". Respond to this prompt: " + PROMPT, "mistral");
        String response = generateResponse.getString("response");

        System.out.println("Generated response: " + response);

        // Close resources
        ireader.close();
        directory.close();

        // Clean up temporary index directory
        try {
            Files.deleteIfExists(indexPath);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static List<String> loadPdfData(String filePath) throws IOException {
        PDDocument document = Loader.loadPDF(new File(filePath));
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return splitIntoParagraphs(text, MAX_CHUNK_SIZE);
    }

    private static List<String> splitIntoParagraphs(String text, int maxChunkSize) {
        String[] paragraphsArray = text.split("\\r?\\n\\r?\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String paragraph : paragraphsArray) {
            if (!paragraph.trim().isEmpty()) {
                if (paragraph.length() <= maxChunkSize) {
                    paragraphs.add(paragraph.trim());
                } else {
                    paragraphs.addAll(splitIntoChunks(paragraph.trim(), maxChunkSize));
                }
            }
        }
        return paragraphs;
    }

    private static List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int start = 0; start < length; start += chunkSize) {
            int end = Math.min(length, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }
}