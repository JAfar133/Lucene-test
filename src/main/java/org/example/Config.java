package org.example;

public class Config {
    public static final String OLLAMA_MODEL = "mistral";
    public static final int MAX_CHUNK_SIZE = 800;
    public static final int MIN_CHUNK_SIZE = 200;
    public static final String PDF_FILE_PATH = "./files/FAQ1.pdf";
    public static final String PROMPTS_FILE_PATH = "./files/prompts.txt";
    public static final int NUM_HINTS = 2;
    public static final String REQUEST_FORMAT = "The user needs to answer the questions, you need to help him. Using this data: '%s'. Respond to this prompt: '%s'";
    public static final String JSON_RESULT_FILE_PATH_FORMAT = "./files/result-%s.json";
    public static final String XLSX_RESULT_FILE_PATH_FORMAT = "./files/result-%s.xlsx";
}
