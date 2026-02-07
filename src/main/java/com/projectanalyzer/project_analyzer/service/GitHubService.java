package com.projectanalyzer.project_analyzer.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

import java.util.Base64;

@Service
public class GitHubService {

    // Threshold to decide whether README is meaningful
    private static final int MIN_README_LENGTH = 200;

    public String fetchReadme(String repoUrl) {
        try {
            // Extract owner and repo name
            String cleanUrl = repoUrl.replace("https://github.com/", "");
            String owner = cleanUrl.split("/")[0];
            String repo = cleanUrl.split("/")[1];

            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/readme";

            RestTemplate restTemplate = new RestTemplate();

            // Required GitHub headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Project-Analyzer-App");
            headers.set("Accept", "application/vnd.github.v3+json");

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

            // ⚠️ Weak README detection
            if (decodedReadme.length() < MIN_README_LENGTH) {
                return "WEAK_README";
            }

            // ✅ Valid README
            return decodedReadme;

        } catch (Exception e) {
            // ❌ README not found or inaccessible
            return null;
        }
    }
}
