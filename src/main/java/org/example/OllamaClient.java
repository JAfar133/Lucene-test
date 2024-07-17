package org.example;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class OllamaClient {

    private static final String OLLAMA_URL = "http://localhost:11434/api";

    public static JSONObject post(String endpoint, JSONObject payload) throws Exception {
        URL url = new URL(OLLAMA_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (java.io.InputStream is = connection.getInputStream()) {
            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONObject(response);
        }
    }

    public static JSONObject getEmbeddings(String prompt, String model) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("prompt", prompt);
        payload.put("model", model);
        return post("/embeddings", payload);
    }

    public static JSONObject generate(String prompt, String model) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("prompt", prompt);
        payload.put("model", model);
        payload.put("stream", false);
        return post("/generate", payload);
    }
}
