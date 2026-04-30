package com.projectanalyzer.project_analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GitLabService implements RepositoryService {

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================== UTILITY =====================

    private String extractProjectPath(String repoUrl) {
        repoUrl = repoUrl.trim();

        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }

        return repoUrl.replace("https://gitlab.com/", "");
    }

    private String encodeProjectPath(String projectPath) {
        return projectPath.replace("/", "%2F");
    }

    // ===================== CHECK PROJECT EXISTS =====================

    private boolean projectExists(String encodedPath) {
        try {
            String url = "https://gitlab.com/api/v4/projects/" + encodedPath;
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
            if (repoUrl == null || !repoUrl.startsWith("https://gitlab.com/")) {
                return "INVALID_URL";
            }

            String projectPath = repoUrl
                    .replace("https://gitlab.com/", "")
                    .replaceAll("/$", "");

            String encodedPath = projectPath.replace("/", "%2F");

            Integer projectId = null;

            // 🔥 STEP 1: Try direct API
            try {
                String url = "https://gitlab.com/api/v4/projects/" + encodedPath;

                Map<String, Object> project =
                        restTemplate.getForObject(url, Map.class);

                // ✅ FIX: Safe ID casting
                Object idObj = project.get("id");

                if (idObj instanceof Integer) {
                    projectId = (Integer) idObj;
                } else if (idObj instanceof Number) {
                    projectId = ((Number) idObj).intValue();
                }

            } catch (Exception ignored) {}

            // 🔥 STEP 2: Fallback to search
            if (projectId == null) {

                String repoName = projectPath.substring(projectPath.lastIndexOf("/") + 1);

                String searchUrl =
                        "https://gitlab.com/api/v4/projects?search=" + repoName;

                List<Map<String, Object>> projects =
                        restTemplate.getForObject(searchUrl, List.class);

                if (projects != null) {
                    for (Map<String, Object> project : projects) {

                        String path = (String) project.get("path");

                        if (repoName.equalsIgnoreCase(path)) {

                            // ✅ FIX: Safe ID casting here too
                            Object idObj = project.get("id");

                            if (idObj instanceof Integer) {
                                projectId = (Integer) idObj;
                            } else if (idObj instanceof Number) {
                                projectId = ((Number) idObj).intValue();
                            }

                            break;
                        }
                    }
                }
            }

            // ❌ Still not found
            if (projectId == null) {
                return "INVALID_URL";
            }

            // 🔥 STEP 3: Fetch README
            // ✅ FIX: Added develop branch
            String[] branches = {"main", "master", "develop"};

            for (String branch : branches) {
                try {
                    String url = "https://gitlab.com/api/v4/projects/"
                            + projectId
                            + "/repository/files/README.md/raw?ref=" + branch;

                    String content = restTemplate.getForObject(url, String.class);

                    if (content != null && !content.isEmpty()) {
                        return content;
                    }

                } catch (Exception ignored) {}
            }

            return "README_NOT_FOUND";

        } catch (Exception e) {
            return "README_NOT_FOUND";
        }
    }

    // ===================== FETCH STRUCTURE =====================

    @Override
    public String fetchRepoStructure(String repoUrl) {
        try {
            if (repoUrl == null || !repoUrl.startsWith("https://gitlab.com/")) {
                return "INVALID_URL";
            }

            String projectPath = extractProjectPath(repoUrl);
            String encodedPath = encodeProjectPath(projectPath);

            if (!projectExists(encodedPath)) {
                return "INVALID_URL";
            }

            String apiUrl = "https://gitlab.com/api/v4/projects/"
                    + encodedPath
                    + "/repository/tree?per_page=100";

            List<Map<String, Object>> files =
                    restTemplate.getForObject(apiUrl, List.class);

            if (files == null || files.isEmpty()) {
                return "No repository structure available.";
            }

            StringBuilder structure = new StringBuilder();

            for (Map<String, Object> file : files) {
                String name = (String) file.get("name");
                String type = (String) file.get("type");

                structure.append("tree".equals(type) ? "[DIR] " : "[FILE] ");
                structure.append(name).append("\n");
            }

            return structure.toString();

        } catch (Exception e) {
            return "Could not fetch repository structure.";
        }
    }

    // ===================== FETCH KEY FILES =====================

    public String fetchKeyFiles(String repoUrl) {
        try {
            String projectPath = extractProjectPath(repoUrl);
            String encodedPath = encodeProjectPath(projectPath);

            if (!projectExists(encodedPath)) {
                return "No key files available.";
            }

            String[] files = {
                    "pom.xml",
                    "package.json",
                    "application.properties",
                    "Dockerfile"
            };

            // ✅ FIX: Added develop branch
            String[] branches = {"main", "master", "develop"};

            StringBuilder result = new StringBuilder();

            for (String file : files) {
                for (String branch : branches) {
                    try {
                        String url = "https://gitlab.com/api/v4/projects/"
                                + encodedPath
                                + "/repository/files/"
                                + file.replace("/", "%2F")
                                + "/raw?ref=" + branch;

                        String content = restTemplate.getForObject(url, String.class);

                        if (content != null && !content.isEmpty()) {
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
}