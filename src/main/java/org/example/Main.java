package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
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
    private static final String PDF_FILE_PATH = "./files/FAQ1.pdf";
    private static final String PROMPT =
            "8.1.4 What do you consider yourself?\n" +
                    "( ) Data Controller\n" +
                    "( ) Data Processor\n" +
                    "( ) Neither";
    private static final int NUM_HINTS = 2;
    private static final int MAX_CHUNK_SIZE = 500;
    private static final int MIN_CHUNK_SIZE = 100;
    public static Info processPrompt(String prompt) throws Exception {
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
        String escapedPrompt = QueryParserBase.escape(prompt);
        // Search for the most relevant document
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        QueryParser parser = new QueryParser("content", analyzer);
        Query query = parser.parse(escapedPrompt);
        ScoreDoc[] hits = isearcher.search(query, NUM_HINTS).scoreDocs;

        StringBuilder data = new StringBuilder();
        for (ScoreDoc hit : hits) {
            Document hitDoc = isearcher.doc(hit.doc);
            data.append(hitDoc.get("content"));
        }

        // Generate a response using the retrieved document
        String request = "Using this data: " + data
                + ". Respond to this prompt: " + prompt;

        JSONObject generateResponse = OllamaClient.generate(request, "mistral");
        String response = generateResponse.getString("response");

        System.out.println("Generated response: " + response);

        // Close resources
        ireader.close();
        directory.close();
        System.out.println("Duration is: " + (System.currentTimeMillis() - ms) + "ms");
        // Clean up temporary index directory

        try {
            Files.deleteIfExists(indexPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Info(prompt, request, response, (System.currentTimeMillis() - ms), data.toString());
    }
    static class Info {
        public String prompt;
        public String request;
        public String response;
        public double duration;
        public String data;

        public Info(String prompt, String request, String response, double duration, String data) {
            this.prompt = prompt;
            this.request = request;
            this.response = response;
            this.duration = duration;
            this.data = data;
        }
    }
    public static void main(String[] args) throws Exception {
        String[] prompts = new String[] {
                "8.1.5 Where personal data is held do you have a policy/procedure in place to allow the release of this data\n" +
                        "when requested? " +
                        "( ) Yes\n" +
                        "( ) No\n" +
                        "( ) N/A",
                "8.2.1 Do staff face disciplinary procedures if data protection policies are breached?\n" +
                        "( ) Yes\n" +
                        "( ) No",
                "8.2.2 Do you have technical constraints on the system to:\n" +
                        "Prevent copying data off system to USB memory? ( ) Yes\n" +
                        "( ) No\n" +
                        "Prevent copying data off system to personal email? ( ) Yes\n" +
                        "( ) No\n" +
                        "Restrict printing off systems? ( ) Yes\n" +
                        "( ) No",
                "8.2.3 Protection in the processing area\n" +
                        "Restrictions on staff in processing area ( ) Yes\n" +
                        "( ) No\n" +
                        "No use of Smartphones? ( ) Yes\n" +
                        "( ) No\n" +
                        "No use of Cameras? ( ) Yes\n" +
                        "( ) No\n" +
                        "No use of flash devices? ( ) Yes\n" +
                        "( ) No"
        };
        List<Info> infoList = new ArrayList<>();
        for (String prompt: prompts) {
            Info info = processPrompt(prompt);
            infoList.add(info);
        }
        System.out.println(infoList);
    }

    private static List<String> loadPdfData(String filePath) throws IOException {
        PDDocument document = Loader.loadPDF(new File(filePath));
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return splitIntoParagraphs(text, MAX_CHUNK_SIZE, MIN_CHUNK_SIZE);
    }

    private static List<String> splitIntoParagraphs(String text, int maxChunkSize, int minChunkSize) {
        String[] paragraphsArray = text.split("\\r?\\n\\r?\\n");
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphsArray) {
            if (!paragraph.trim().isEmpty()) {

                    if (currentChunk.length() >= minChunkSize) {
                        paragraphs.add(currentChunk.toString());
                        currentChunk.setLength(0);
                        currentChunk.append(paragraph.trim());
                    } else {
                        currentChunk.append("\n\n").append(paragraph.trim());
                        paragraphs.addAll(splitIntoChunks(currentChunk.toString(), maxChunkSize, 60));
                        currentChunk.setLength(0);
                    }
            }
        }

        if (!currentChunk.isEmpty()) {
            paragraphs.add(currentChunk.toString());
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

    private static List<String> splitIntoChunks(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int start = 0; start < length; start += chunkSize - chunkOverlap) {
            int end = Math.min(length, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }
}