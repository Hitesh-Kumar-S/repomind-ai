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

    // ===================== FETCH README =====================

@Override
public String fetchReadme(String repoUrl) {

    try {

        if (repoUrl == null || !repoUrl.startsWith("https://gitlab.com/")) {
            return "FETCH_FAILED";
        }

        String projectPath = repoUrl
                .replace("https://gitlab.com/", "")
                .replaceAll("/$", "");

        String encodedPath = projectPath.replace("/", "%2F");

        Integer projectId = null;

        // ===================== STEP 1: DIRECT API =====================

        try {

            String url =
                    "https://gitlab.com/api/v4/projects/" + encodedPath;

            Map<String, Object> project =
                    restTemplate.getForObject(url, Map.class);

            if (project != null) {

                Object idObj = project.get("id");

                if (idObj instanceof Integer) {
                    projectId = (Integer) idObj;

                } else if (idObj instanceof Number) {
                    projectId = ((Number) idObj).intValue();
                }
            }

        } catch (Exception ignored) {}

        // ===================== STEP 2: SEARCH FALLBACK =====================

        if (projectId == null) {

            String repoName =
                    projectPath.substring(projectPath.lastIndexOf("/") + 1);

            String searchUrl =
                    "https://gitlab.com/api/v4/projects?search=" + repoName;

            List<Map<String, Object>> projects =
                    restTemplate.getForObject(searchUrl, List.class);

            if (projects != null) {

                for (Map<String, Object> project : projects) {

                    String pathWithNamespace =
                            (String) project.get("path_with_namespace");

                    if (projectPath.equalsIgnoreCase(pathWithNamespace)) {

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

        // ===================== PROJECT NOT FOUND =====================

        if (projectId == null) {
            return "FETCH_FAILED";
        }

        // ===================== README FETCH =====================

        String[] branches = {"main", "master", "develop"};

        String[] readmeFiles = {
                "README.md",
                "readme.md",
                "Readme.md",
                "README.MD",
                "README",
                "readme",
                "ReadMe.md"
        };

        boolean weakReadmeFound = false;

        for (String branch : branches) {

            for (String readmeFile : readmeFiles) {

                try {

                    String url =
                            "https://gitlab.com/api/v4/projects/"
                                    + projectId
                                    + "/repository/files/"
                                    + readmeFile.replace("/", "%2F")
                                    + "/raw?ref=" + branch;

                    String content =
                            restTemplate.getForObject(url, String.class);

                    if (content != null && !content.isEmpty()) {

                        if (content.trim().length() < 50) {

                            weakReadmeFound = true;

                            continue;
                        }

                        return content;
                    }

                } catch (Exception ignored) {}
            }
        }

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

        if (repoUrl == null
                || !repoUrl.startsWith("https://gitlab.com/")) {

            return "Could not fetch repository structure.";
        }

        String projectPath =
                extractProjectPath(repoUrl);

        String encodedPath =
                encodeProjectPath(projectPath);

        String apiUrl =
                "https://gitlab.com/api/v4/projects/"
                        + encodedPath
                        + "/repository/tree?per_page=100";

        List<Map<String, Object>> files =
                restTemplate.getForObject(apiUrl, List.class);

        if (files == null || files.isEmpty()) {

            return "No repository structure available.";
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
                    "tree".equals(type)
                            ? "[DIR] "
                            : "[FILE] "
            );

            structure.append(name)
                    .append("\n");

            count++;
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

                    String url = "https://gitlab.com/api/v4/projects/"
                            + encodedPath
                            + "/repository/files/"
                            + file.replace("/", "%2F")
                            + "/raw?ref=" + branch;

                    String content =
                            restTemplate.getForObject(url, String.class);

                    if (content != null && !content.isEmpty()) {

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

                    String url = "https://gitlab.com/api/v4/projects/"
                            + encodedPath
                            + "/repository/files/"
                            + readmeFile.replace("/", "%2F")
                            + "/raw?ref=" + branch;

                    String content =
                            restTemplate.getForObject(url, String.class);

                    if (content != null && !content.isEmpty()) {

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
}