package com.claudeburp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ClaudeApiClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 4096;
    private static final int MAX_INPUT_CHARS = 50_000;

    private static final String DEFAULT_SYSTEM_PROMPT =
        "You are an expert penetration tester and web application security analyst. " +
        "Analyze the provided HTTP request/response for security vulnerabilities.\n\n" +
        "For each finding provide:\n" +
        "1. Vulnerability name and severity (Critical / High / Medium / Low / Info)\n" +
        "2. Exact location in the request/response\n" +
        "3. Technical description of the issue\n" +
        "4. Proof of concept or exploitation notes\n" +
        "5. Remediation recommendation\n\n" +
        "Cover: SQL Injection, XSS, SSRF, IDOR, Command Injection, Path Traversal, " +
        "Authentication/Authorization flaws, Information Disclosure, Insecure Headers, " +
        "Business Logic issues, and other OWASP Top 10 risks.\n\n" +
        "If no significant vulnerabilities are found, explain why and note any minor observations.";

    private final HttpClient httpClient;

    public ClaudeApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String analyze(String apiKey, String model, String systemPrompt, String content) throws Exception {
        String truncatedContent = content.length() > MAX_INPUT_CHARS
                ? content.substring(0, MAX_INPUT_CHARS) + "\n\n[... content truncated at " + MAX_INPUT_CHARS + " chars ...]"
                : content;

        String effectiveSystem = (systemPrompt == null || systemPrompt.isBlank())
                ? DEFAULT_SYSTEM_PROMPT
                : systemPrompt;

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", MAX_TOKENS);
        body.addProperty("system", effectiveSystem);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content",
                "Analyze the following HTTP traffic for security vulnerabilities:\n\n" + truncatedContent);

        JsonArray messages = new JsonArray();
        messages.add(message);
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            JsonObject errorJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String errorMsg = errorJson.has("error")
                    ? errorJson.getAsJsonObject("error").get("message").getAsString()
                    : response.body();
            throw new RuntimeException("API error " + response.statusCode() + ": " + errorMsg);
        }

        return JsonParser.parseString(response.body())
                .getAsJsonObject()
                .getAsJsonArray("content")
                .get(0)
                .getAsJsonObject()
                .get("text")
                .getAsString();
    }
}
