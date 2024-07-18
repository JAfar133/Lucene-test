package org.example;

import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        List<QuestionInfo> infoList = new ArrayList<>();
        List<String> prompts = loadPrompts(Config.PROMPTS_FILE_PATH);
        for (String prompt : prompts) {
            QuestionInfo info = processPrompt(prompt);
            infoList.add(info);
        }
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        saveResultsToJson(infoList, String.format(Config.JSON_RESULT_FILE_PATH_FORMAT, date));
        saveResultsToXlsx(infoList, String.format(Config.XLSX_RESULT_FILE_PATH_FORMAT, date));
    }

    public static List<String> loadPrompts(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String content = Files.readString(path);
        String[] promptsArray = content.split("\\r?\\n\\r?\\n");
        System.out.println(promptsArray.length + " prompts found");
        List<String> prompts = new ArrayList<>();
        for (String prompt : promptsArray) {
            if (!prompt.trim().isEmpty()) {
                prompts.add(prompt.trim());
            }
        }
        return prompts;
    }

    public static QuestionInfo processPrompt(String prompt) throws Exception {
        long startTime = System.currentTimeMillis();
        LuceneIndexer indexer = new LuceneIndexer();
        List<String> chunks = PdfHelper.loadPdfData(Config.PDF_FILE_PATH);

        indexer.indexDocuments(chunks);
        String escapedPrompt = QueryParserBase.escape(prompt);

        String data = indexer.search(escapedPrompt, Config.NUM_HINTS);
        String request = String.format(Config.REQUEST_FORMAT, data, prompt);

        JSONObject generateResponse = OllamaClient.generate(request, Config.OLLAMA_MODEL);
        String response = generateResponse.getString("response");

        long duration = System.currentTimeMillis() - startTime;

        return new QuestionInfo(prompt, request, response, duration, data);
    }

    public static void saveResultsToJson(List<QuestionInfo> infoList, String filePath) throws IOException {
        JSONArray jsonArray = new JSONArray();
        for (QuestionInfo info : infoList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("prompt", info.getPrompt());
            jsonObject.put("request", info.getRequest());
            jsonObject.put("response", info.getResponse());
            jsonObject.put("duration", info.getDuration());
            jsonObject.put("data", info.getData());
            jsonArray.put(jsonObject);
        }

        try (FileWriter file = new FileWriter(filePath)) {
            file.write(jsonArray.toString(4));
            file.flush();
        }
    }
    public static void saveResultsToXlsx(List<QuestionInfo> infoList, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Results");

        Row headerRow = sheet.createRow(0);
        String[] columns = {"№", "Prompt", "Duration", "Response", "Request", "Data", "Hardware", "Config"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (QuestionInfo info : infoList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum-1);
            row.createCell(1).setCellValue(info.getPrompt());
            row.createCell(2).setCellValue(info.getResponse());
            row.createCell(3).setCellValue(String.format("%f.2s", (double) info.getDuration() / 1000));
            row.createCell(4).setCellValue(info.getRequest());
            row.createCell(5).setCellValue(info.getData());
            row.createCell(6).setCellValue("Intel(R) Core(TM) i5-9400:\n" +
                    "    Speed: 2.90GHz \n" +
                    "    Cores: 6\n" +
                    "    Logical processors: 6\n" +
                    "RAM:\n" +
                    "    Size: 16GB\n" +
                    "    Usage: 15.9GB");
            row.createCell(7).setCellValue(String.format("max chunk size: %d\nmin chunk size: %d\nnum hints: %d",Config.MAX_CHUNK_SIZE, Config.MIN_CHUNK_SIZE, Config.NUM_HINTS));
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}