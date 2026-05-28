package com.projectanalyzer.project_analyzer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService implements RepositoryService {

    private static final int MIN_README_LENGTH = 50;

    @Value("${github.token:}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================== RETRY WRAPPER =====================

    private <T> ResponseEntity<T> exchangeWithRetry(
            String url, HttpMethod method, HttpEntity<?> entity, Class<T> type) {

        int attempts = 0;

        while (true) {
            try {
                return restTemplate.exchange(url, method, entity, type);

            } catch (HttpClientErrorException e) {

                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempts < 2) {
                    sleep(1500);
                    attempts++;
                    continue;
                }

                throw e;

            } catch (Exception e) {
                if (attempts < 2) {
                    sleep(1000);
                    attempts++;
                    continue;
                }
                throw e;
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ===================== FETCH README =====================

    @Override
    public String fetchReadme(String repoUrl) {

        try {
            if (repoUrl == null || repoUrl.trim().isEmpty()) {
                return "FETCH_FAILED";
            }

            repoUrl = normalizeUrl(repoUrl);

            if (!repoUrl.startsWith("https://github.com/")) {
                return "FETCH_FAILED";
            }

            String[] parts = repoUrl.replace("https://github.com/", "").split("/");
            if (parts.length < 2) return "FETCH_FAILED";;

            String owner = parts[0];
            String repo = parts[1];

            // ================= PRIMARY =================

            try {
                String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/readme";

                HttpHeaders headers = buildHeaders();
                headers.set("Accept", "application/vnd.github.v3+json");

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = exchangeWithRetry(
                        apiUrl,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                JSONObject json = new JSONObject(response.getBody());

                if (!json.has("content")) {
                    return "FETCH_FAILED";
                }

                String encoded = json.getString("content");

                String decoded = new String(
                        Base64.getDecoder().decode(encoded.replaceAll("\\s", ""))
                ).trim();

                if (decoded.length() < MIN_README_LENGTH) {
                    return "WEAK_README";
                }

                return decoded;

            } catch (HttpClientErrorException e) {
 
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // fallback
                } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    return "FETCH_FAILED";
                } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    return "AUTH_FAILED";
                } else {
                    return "FETCH_FAILED";
                }
            }

            // ================= FALLBACK =================

            String[] branches = {"main", "master", "develop"};
            
            String[] files = {
    "README.md",
    "readme.md",
    "Readme.md",
    "README.MD",
    "README",
    "readme",
    "ReadMe.md"
};

            for (String branch : branches) {
                for (String file : files) {
                    try {

                        String url = "https://api.github.com/repos/"
                                + owner + "/" + repo
                                + "/contents/" + file + "?ref=" + branch;

                        HttpEntity<String> entity = new HttpEntity<>(buildHeaders());

                        ResponseEntity<Map> response = exchangeWithRetry(
                                url,
                                HttpMethod.GET,
                                entity,
                                Map.class
                        );

                        if (response.getBody() == null) continue;

                        Object contentObj = response.getBody().get("content");
                        if (contentObj == null) continue;

                        String content = (String) contentObj;

                        String decoded = new String(
                                Base64.getDecoder().decode(content.replaceAll("\\s", ""))
                        ).trim();

                        if (decoded.length() < MIN_README_LENGTH) {
                            return "WEAK_README";
                        }

                        return decoded;

                    } catch (HttpClientErrorException e) {
    
                        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                            return "FETCH_FAILED";
                        }

                    } catch (Exception ignored) {}
                }
            }

            return "FETCH_FAILED";

        } catch (Exception e) {
            return "FETCH_FAILED";
        }
    }

    // ===================== FETCH STRUCTURE =====================

@Override
public String fetchRepoStructure(String repoUrl) {

    try {

        repoUrl = normalizeUrl(repoUrl);

        String[] parts =
                repoUrl.replace("https://github.com/", "")
                        .split("/");

        if (parts.length < 2) {
            return "Invalid URL";
        }

        String owner = parts[0];
        String repo = parts[1];

        String apiUrl =
                "https://api.github.com/repos/"
                        + owner + "/"
                        + repo
                        + "/contents";

        HttpEntity<String> entity =
                new HttpEntity<>(buildHeaders());

        ResponseEntity<List> response =
                exchangeWithRetry(
                        apiUrl,
                        HttpMethod.GET,
                        entity,
                        List.class
                );

        List<Map<String, Object>> files =
                response.getBody();

        if (files == null) {
            return "No structure available.";
        }

        StringBuilder structure =
                new StringBuilder();

        // ===================== LIMIT STRUCTURE SIZE =====================

        int count = 0;

        for (Map<String, Object> file : files) {

            // ✅ Prevent huge prompts
            if (count >= 40) {
                break;
            }

            String name =
                    (String) file.get("name");

            String type =
                    (String) file.get("type");

            structure.append(
                    "dir".equals(type)
                            ? "[DIR] "
                            : "[FILE] "
            );

            structure.append(name)
                    .append("\n");

            count++;
        }

        return structure.toString();

    } catch (Exception e) {

        return "Could not fetch structure.";
    }
}

    // ===================== FETCH KEY FILES =====================

    public String fetchKeyFiles(String repoUrl) {

    try {

        repoUrl = normalizeUrl(repoUrl);

        String[] parts =
                repoUrl.replace("https://github.com/", "").split("/");

        if (parts.length < 2) {
            return "No key files.";
        }

        String owner = parts[0];
        String repo = parts[1];

        String apiBase =
                "https://api.github.com/repos/" + owner + "/" + repo;

        // ===================== IMPORTANT FILES =====================

        String[] importantFiles = {
                "pom.xml",
                "package.json",
                "package-lock.json",
                "requirements.txt",
                "build.gradle",
                "settings.gradle",
                "Dockerfile",
                "docker-compose.yml",
                "application.properties",
                "application.yml"
        };

        // ===================== README VARIATIONS =====================

        String[] readmeFiles = {
                "README.md",
                "readme.md",
                "Readme.md",
                "README.MD",
                "README",
                "readme",
                "ReadMe.md"
        };

        HttpEntity<String> entity =
                new HttpEntity<>(buildHeaders());

        StringBuilder result = new StringBuilder();

        // ===================== NORMAL FILES =====================

        for (String fileName : importantFiles) {

            try {

                String url =
                        apiBase + "/contents/" + fileName;

                ResponseEntity<Map> response =
                        exchangeWithRetry(
                                url,
                                HttpMethod.GET,
                                entity,
                                Map.class
                        );

                if (response.getBody() == null) {
                    continue;
                }

                Object contentObj =
                        response.getBody().get("content");

                if (contentObj == null) {
                    continue;
                }

                String content = (String) contentObj;

                String decoded = new String(
                        Base64.getDecoder().decode(
                                content.replaceAll("\\s", "")
                        )
                );

                decoded = decoded.substring(
                        0,
                        Math.min(decoded.length(), 1000)
                );

                result.append("=== ")
                        .append(fileName)
                        .append(" ===\n");

                result.append(decoded)
                        .append("\n\n");

            } catch (Exception ignored) {}
        }

        // ===================== README FILES =====================

        for (String readmeFile : readmeFiles) {

            try {

                String url =
                        apiBase + "/contents/" + readmeFile;

                ResponseEntity<Map> response =
                        exchangeWithRetry(
                                url,
                                HttpMethod.GET,
                                entity,
                                Map.class
                        );

                if (response.getBody() == null) {
                    continue;
                }

                Object contentObj =
                        response.getBody().get("content");

                if (contentObj == null) {
                    continue;
                }

                String content = (String) contentObj;

                String decoded = new String(
                        Base64.getDecoder().decode(
                                content.replaceAll("\\s", "")
                        )
                );

                decoded = decoded.substring(
                        0,
                        Math.min(decoded.length(), 1000)
                );

                result.append("=== ")
                        .append(readmeFile)
                        .append(" ===\n");

                result.append(decoded)
                        .append("\n\n");

                break;

            } catch (Exception ignored) {}
        }

        return result.toString().isEmpty()
                ? "No key files."
                : result.toString();

    } catch (Exception e) {
        return "Could not fetch key files.";
    }
}

    // ===================== HELPERS =====================

private String normalizeUrl(String repoUrl) {

    repoUrl = repoUrl.trim();

    if (repoUrl.endsWith("/")) {
        repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
    }

    if (repoUrl.endsWith(".git")) {
        repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
    }

    return repoUrl;
}

private HttpHeaders buildHeaders() {

    HttpHeaders headers = new HttpHeaders();

    headers.set("User-Agent", "RepoMind-AI");

    if (githubToken != null) {

        githubToken = githubToken.trim();

        if (!githubToken.isEmpty()
                && !githubToken.equalsIgnoreCase("null")
                && !githubToken.equalsIgnoreCase("your_token_here")) {

            headers.setBearerAuth(githubToken);
        }
    }

    return headers;
}
}