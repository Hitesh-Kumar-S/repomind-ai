package com.projectanalyzer.project_analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BitbucketService implements RepositoryService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String fetchReadme(String repoUrl) {
        try {
            // Example: https://bitbucket.org/workspace/repo

            String cleanUrl = repoUrl.replace("https://bitbucket.org/", "");
            String[] parts = cleanUrl.split("/");

            if (parts.length < 2) return "INVALID_URL";

            String workspace = parts[0];
            String repo = parts[1];

            // Try main branch
            String readme = fetchFromBranch(workspace, repo, "main");
            if (readme != null) return readme;

            // Fallback to master
            readme = fetchFromBranch(workspace, repo, "master");
            if (readme != null) return readme;

            return "README_NOT_FOUND";

        } catch (Exception e) {
            return "README_NOT_FOUND";
        }
    }

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

                    return restTemplate.getForObject(url, String.class);

                } catch (Exception ignored) {}
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }
}