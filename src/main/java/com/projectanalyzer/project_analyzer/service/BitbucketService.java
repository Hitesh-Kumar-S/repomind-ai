package com.projectanalyzer.project_analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

    // ===================== CHECK REPO EXISTS =====================

    private boolean repoExists(String workspace, String repo) {
        try {
            String url = "https://api.bitbucket.org/2.0/repositories/"
                    + workspace + "/" + repo;

            restTemplate.getForObject(url, Map.class);
            return true;

        } catch (HttpClientErrorException e) {
            return false;
        }
    }

    // ===================== FETCH README =====================

    @Override
    public String fetchReadme(String repoUrl) {
        try {

            // ❌ Block invalid /src URLs
            if (repoUrl != null && repoUrl.contains("/src/")) {
                return "❌ Invalid Bitbucket URL. Remove '/src/branch' from the link.\nExample: https://bitbucket.org/workspace/repository";
            }

            if (repoUrl == null || !repoUrl.startsWith("https://bitbucket.org/")) {
                return "❌ Invalid Bitbucket URL.\nExpected format:\nhttps://bitbucket.org/workspace/repository";
            }

            String[] parts = extractParts(repoUrl);
            if (parts.length < 2) {
                return "❌ Invalid Bitbucket URL.\nExpected format:\nhttps://bitbucket.org/workspace/repository";
            }

            String workspace = parts[0];
            String repo = parts[1];

            if (!repoExists(workspace, repo)) {
                return "❌ Repository not found or is private.";
            }

            // ✅ Added develop branch
            String[] branches = {"main", "master", "develop"};

            for (String branch : branches) {
                String readme = fetchFromBranch(workspace, repo, branch);
                if (readme != null) return readme;
            }

            return "❌ README not found in this repository.";

        } catch (Exception e) {
            return "❌ README not found in this repository.";
        }
    }

    // ===================== FETCH STRUCTURE =====================

    @Override
    public String fetchRepoStructure(String repoUrl) {
        try {

            if (repoUrl != null && repoUrl.contains("/src/")) {
                return "❌ Invalid Bitbucket URL. Remove '/src/branch' from the link.";
            }

            if (repoUrl == null || !repoUrl.startsWith("https://bitbucket.org/")) {
                return "❌ Invalid Bitbucket URL.";
            }

            String[] parts = extractParts(repoUrl);
            if (parts.length < 2) return "❌ Invalid Bitbucket URL.";

            String workspace = parts[0];
            String repo = parts[1];

            if (!repoExists(workspace, repo)) {
                return "❌ Repository not found.";
            }

            String baseUrl = "https://api.bitbucket.org/2.0/repositories/"
                    + workspace + "/" + repo + "/src";

            String structure = fetchStructureFromBranch(baseUrl, "main");
            if (structure != null) return structure;

            structure = fetchStructureFromBranch(baseUrl, "master");
            if (structure != null) return structure;

            structure = fetchStructureFromBranch(baseUrl, "develop");
            if (structure != null) return structure;

            return "Could not fetch repository structure.";

        } catch (Exception e) {
            return "Could not fetch repository structure.";
        }
    }

    private String fetchStructureFromBranch(String baseUrl, String branch) {
        try {
            String url = baseUrl + "/" + branch;

            Map<String, Object> response =
                    restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("values")) {
                return null;
            }

            List<Map<String, Object>> values =
                    (List<Map<String, Object>>) response.get("values");

            StringBuilder structure = new StringBuilder();

            for (Map<String, Object> item : values) {
                String type = (String) item.get("type");
                Map<String, Object> pathObj = (Map<String, Object>) item.get("path");

                String name = (String) pathObj.get("name");

                structure.append("commit_directory".equals(type) ? "[DIR] " : "[FILE] ");
                structure.append(name).append("\n");
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
                return "❌ Invalid Bitbucket URL.";
            }

            String[] parts = extractParts(repoUrl);
            if (parts.length < 2) return "No key files available.";

            String workspace = parts[0];
            String repo = parts[1];

            if (!repoExists(workspace, repo)) {
                return "No key files available.";
            }

            String[] files = {
                    "pom.xml",
                    "package.json",
                    "application.properties",
                    "Dockerfile"
            };

            String[] branches = {"main", "master", "develop"};

            StringBuilder result = new StringBuilder();

            for (String file : files) {
                for (String branch : branches) {
                    try {
                        String url = "https://api.bitbucket.org/2.0/repositories/"
                                + workspace + "/"
                                + repo + "/src/"
                                + branch + "/"
                                + file;

                        String content = restTemplate.getForObject(url, String.class);

                        if (content != null && !content.trim().isEmpty()) {

                            if (content.length() < 50) {
                                return "⚠️ README exists but content is too short.";
                            }

                            content = content.substring(0, Math.min(content.length(), 1500));

                            result.append("=== ").append(file).append(" ===\n");
                            result.append(content).append("\n\n");

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

    private String fetchFromBranch(String workspace, String repo, String branch) {
        try {
            String[] fileNames = {
                    "README.md",
                    "readme.md",
                    "Readme.md"
            };

            for (String file : fileNames) {
                try {
                    String url = "https://api.bitbucket.org/2.0/repositories/"
                            + workspace + "/"
                            + repo + "/src/"
                            + branch + "/"
                            + file;

                    String content = restTemplate.getForObject(url, String.class);

                    if (content != null && !content.trim().isEmpty()) {
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