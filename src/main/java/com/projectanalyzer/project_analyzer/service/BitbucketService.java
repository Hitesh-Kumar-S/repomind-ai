package com.projectanalyzer.project_analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@Service
public class BitbucketService implements RepositoryService {

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================== UTIL =====================

    private String[] extractParts(String repoUrl) {
        repoUrl = repoUrl.trim();

        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }

        String clean = repoUrl.replace("https://bitbucket.org/", "");
        String[] parts = clean.split("/");

        // ✅ Always take only workspace + repo
        if (parts.length >= 2) {
            return new String[]{parts[0], parts[1]};
        }

        return parts;
    }

    // ===================== FETCH README =====================

    @Override
public String fetchReadme(String repoUrl) {

    try {

        // ===================== INVALID URL CHECK =====================

        if (repoUrl != null && repoUrl.contains("/src/")) {
            return "FETCH_FAILED";
        }

        if (repoUrl == null
                || !repoUrl.startsWith("https://bitbucket.org/")) {

            return "FETCH_FAILED";
        }

        String[] parts = extractParts(repoUrl);

        if (parts.length < 2) {
            return "FETCH_FAILED";
        }

        String workspace = parts[0];
        String repo = parts[1];

        // ===================== BRANCHES =====================

        String[] branches = {"main", "master", "develop"};

        boolean weakReadmeFound = false;

        // ===================== README FETCH =====================

        for (String branch : branches) {

            String readme =
                    fetchFromBranch(workspace, repo, branch);

            if (readme != null) {

                if ("WEAK_README".equals(readme)) {

                    weakReadmeFound = true;

                    continue;
                }

                return readme;
            }
        }

        // ===================== FINAL RESULT =====================

        if (weakReadmeFound) {
            return "WEAK_README";
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

        if (repoUrl != null && repoUrl.contains("/src/")) {
            return "Could not fetch repository structure.";
        }

        if (repoUrl == null
                || !repoUrl.startsWith("https://bitbucket.org/")) {

            return "Could not fetch repository structure.";
        }

        String[] parts = extractParts(repoUrl);

        if (parts.length < 2) {
            return "Could not fetch repository structure.";
        }

        String workspace = parts[0];
        String repo = parts[1];

        String baseUrl =
                "https://api.bitbucket.org/2.0/repositories/"
                        + workspace + "/"
                        + repo + "/src";

        String[] branches = {"main", "master", "develop"};

        for (String branch : branches) {

            String structure =
                    fetchStructureFromBranch(baseUrl, branch);

            if (structure != null) {
                return structure;
            }
        }

        return "Could not fetch repository structure.";

    } catch (Exception e) {

        return "Could not fetch repository structure.";
    }
}

private String fetchStructureFromBranch(
        String baseUrl,
        String branch
) {

    try {

        String url = baseUrl + "/" + branch;

        Map<String, Object> response =
                restTemplate.getForObject(url, Map.class);

        if (response == null
                || !response.containsKey("values")) {

            return null;
        }

        List<Map<String, Object>> values =
                (List<Map<String, Object>>) response.get("values");

        StringBuilder structure =
                new StringBuilder();

        for (Map<String, Object> item : values) {

            String type =
                    (String) item.get("type");

            Map<String, Object> pathObj =
                    (Map<String, Object>) item.get("path");

            String name =
                    (String) pathObj.get("name");

            structure.append(
                    "commit_directory".equals(type)
                            ? "[DIR] "
                            : "[FILE] "
            );

            structure.append(name)
                    .append("\n");
        }

        return structure.toString();

    } catch (Exception e) {

        return null;
    }
}

    // ===================== FETCH KEY FILES =====================

public String fetchKeyFiles(String repoUrl) {

    try {

        if (repoUrl != null && repoUrl.contains("/src/")) {
            return "No key files available.";
        }

        String[] parts = extractParts(repoUrl);

        if (parts.length < 2) {
            return "No key files available.";
        }

        String workspace = parts[0];
        String repo = parts[1];

        String[] files = {
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

        String[] readmeFiles = {
                "README.md",
                "readme.md",
                "Readme.md",
                "README.MD",
                "README",
                "readme",
                "ReadMe.md"
        };

        String[] branches = {"main", "master", "develop"};

        StringBuilder result = new StringBuilder();

        // ===================== NORMAL KEY FILES =====================

        for (String file : files) {

            for (String branch : branches) {

                try {

                    String url =
                            "https://api.bitbucket.org/2.0/repositories/"
                                    + workspace + "/"
                                    + repo + "/src/"
                                    + branch + "/"
                                    + file;

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Accept", "text/plain");

                    HttpEntity<String> entity =
                            new HttpEntity<>(headers);

                    ResponseEntity<String> response =
                            restTemplate.exchange(
                                    url,
                                    HttpMethod.GET,
                                    entity,
                                    String.class
                            );

                    String content = response.getBody();

                    if (content != null && !content.trim().isEmpty()) {

                        content = content.substring(
                                0,
                                Math.min(content.length(), 1000)
                        );

                        result.append("=== ")
                                .append(file)
                                .append(" ===\n");

                        result.append(content)
                                .append("\n\n");

                        break;
                    }

                } catch (Exception ignored) {}
            }
        }

        // ===================== README VARIATIONS =====================

        for (String readmeFile : readmeFiles) {

            for (String branch : branches) {

                try {

                    String url =
                            "https://api.bitbucket.org/2.0/repositories/"
                                    + workspace + "/"
                                    + repo + "/src/"
                                    + branch + "/"
                                    + readmeFile;

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Accept", "text/plain");

                    HttpEntity<String> entity =
                            new HttpEntity<>(headers);

                    ResponseEntity<String> response =
                            restTemplate.exchange(
                                    url,
                                    HttpMethod.GET,
                                    entity,
                                    String.class
                            );

                    String content = response.getBody();

                    if (content != null && !content.trim().isEmpty()) {

                        content = content.substring(
                                0,
                                Math.min(content.length(), 1000)
                        );

                        result.append("=== ")
                                .append(readmeFile)
                                .append(" ===\n");

                        result.append(content)
                                .append("\n\n");

                        break;
                    }

                } catch (Exception ignored) {}
            }
        }

        return result.toString().isEmpty()
                ? "No key files available."
                : result.toString();

    } catch (Exception e) {

        return "No key files available.";
    }
}

// ===================== README HELPER =====================

private String fetchFromBranch(
        String workspace,
        String repo,
        String branch
) {

    try {

        String[] fileNames = {
                "README.md",
                "readme.md",
                "Readme.md",
                "README.MD",
                "README",
                "readme",
                "ReadMe.md"
        };

        for (String file : fileNames) {

            try {

                String url =
                        "https://api.bitbucket.org/2.0/repositories/"
                                + workspace + "/"
                                + repo + "/src/"
                                + branch + "/"
                                + file;

                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "text/plain");

                HttpEntity<String> entity =
                        new HttpEntity<>(headers);

                ResponseEntity<String> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                String.class
                        );

                String content = response.getBody();

                if (content != null && !content.trim().isEmpty()) {

                    if (content.trim().length() < 50) {
                        return "WEAK_README";
                    }

                    return content;
                }

            } catch (Exception ignored) {}
        }

        return null;

    } catch (Exception e) {

        return null;
    }
}
}