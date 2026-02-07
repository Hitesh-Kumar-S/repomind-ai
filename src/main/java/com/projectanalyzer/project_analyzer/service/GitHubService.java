package com.projectanalyzer.project_analyzer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class GitHubService {

    private static final int MIN_README_LENGTH = 200;

    @Value("${github.token:}")
    private String githubToken;

    public String fetchReadme(String repoUrl) {
        try {
            if (repoUrl == null || repoUrl.trim().isEmpty()) {
                return "INVALID_URL";
            }

            // Normalize URL
            repoUrl = repoUrl.trim();
            if (repoUrl.endsWith("/")) {
                repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
            }
            if (repoUrl.endsWith(".git")) {
                repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
            }
            if (!repoUrl.startsWith("https://github.com/")) {
                return "INVALID_URL";
            }

            String[] parts = repoUrl.replace("https://github.com/", "").split("/");
            if (parts.length < 2) {
                return "INVALID_URL";
            }

            String owner = parts[0];
            String repo = parts[1];

            String apiUrl =
                    "https://api.github.com/repos/" + owner + "/" + repo + "/readme";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Project-Analyzer-App");
            headers.set("Accept", "application/vnd.github.v3+json");

            // ✅ Add GitHub token if present (prevents rate limiting)
            if (githubToken != null && !githubToken.isBlank()) {
                headers.setBearerAuth(githubToken);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JSONObject json = new JSONObject(response.getBody());
            String encodedContent = json.getString("content");

            String decodedReadme = new String(
                    Base64.getDecoder().decode(encodedContent.replaceAll("\\s", ""))
            ).trim();

            if (decodedReadme.length() < MIN_README_LENGTH) {
                return "WEAK_README";
            }

            return decodedReadme;

        } catch (Exception e) {
            return "README_NOT_FOUND";
        }
    }
}
