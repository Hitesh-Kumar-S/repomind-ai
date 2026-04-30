package com.projectanalyzer.project_analyzer.controller;

import com.projectanalyzer.project_analyzer.service.BitbucketService;
import com.projectanalyzer.project_analyzer.service.ContextService;
import com.projectanalyzer.project_analyzer.service.GitHubService;
import com.projectanalyzer.project_analyzer.service.GitLabService;
import com.projectanalyzer.project_analyzer.service.GroqLLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class AnalyzerController {

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private GitLabService gitLabService;

    @Autowired
    private BitbucketService bitbucketService;

    @Autowired
    private GroqLLMService groqllmService;

    @Autowired
    private ContextService contextService;

    @PostMapping("/analyze")
    public String analyzeProject(@RequestBody String repoUrl) {

        String readme;
        String structure;
        String keyFiles;

        // ===================== PLATFORM DETECTION =====================

        if (repoUrl.contains("github.com")) {

            readme = gitHubService.fetchReadme(repoUrl);
            structure = gitHubService.fetchRepoStructure(repoUrl);
            keyFiles = gitHubService.fetchKeyFiles(repoUrl);

        } else if (repoUrl.contains("gitlab.com")) {

            readme = gitLabService.fetchReadme(repoUrl);
            structure = gitLabService.fetchRepoStructure(repoUrl);
            keyFiles = gitLabService.fetchKeyFiles(repoUrl);

        } else if (repoUrl.contains("bitbucket.org")) {

            readme = bitbucketService.fetchReadme(repoUrl);
            structure = bitbucketService.fetchRepoStructure(repoUrl);
            keyFiles = bitbucketService.fetchKeyFiles(repoUrl);

        } else {
            return """
❌ **Unsupported Repository Platform**

Currently supported platforms:
- GitHub
- GitLab
- Bitbucket

Please provide a valid repository URL.
""";
        }

        // ===================== 🔥 GLOBAL ERROR HANDLING (NEW FIX) =====================

        // Stop LLM if any service error occurs
        if (readme != null && (readme.startsWith("❌") || readme.startsWith("⚠️"))) {
            return readme;
        }

        // ===================== EXISTING VALIDATIONS =====================

        if ("INVALID_URL".equals(readme)) {
            return """
❌ **Invalid Repository URL**

Please check:
- Username
- Repository name
- URL format

Example:
https://github.com/user/repo
""";
        }

        if ("README_NOT_FOUND".equals(readme)) {
            return """
❌ **README.md Not Found**

This repository does not contain a readable README.

👉 Please ensure:
- Repository is public
- README.md exists
""";
        }

        if ("WEAK_README".equals(readme)) {
            return """
⚠️ **Weak README Detected**

A README.md file was found, but it appears to be insufficiently detailed.

⚠️ The analysis may be partially generic.

💡 Consider adding:
- Project overview
- Tech stack
- Architecture
- Features
- Improvements
""";
        }

        // ===================== SAFETY FALLBACK =====================

        if (keyFiles == null || keyFiles.isBlank()) {
            keyFiles = "No key files available.";
        }

        if (structure == null || structure.isBlank()) {
            structure = "No repository structure available.";
        }

        // ===================== STORE CONTEXT =====================

        contextService.clear();
        contextService.setReadme(readme);
        contextService.setRepoStructure(structure);
        contextService.setKeyFiles(keyFiles);

        // ===================== LLM ANALYSIS =====================

        return groqllmService.analyzeProject(readme);
    }
}