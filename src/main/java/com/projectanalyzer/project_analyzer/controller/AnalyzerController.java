package com.projectanalyzer.project_analyzer.controller;

import jakarta.servlet.http.HttpSession;
import com.projectanalyzer.project_analyzer.model.ContextData;
import com.projectanalyzer.project_analyzer.service.BitbucketService;
import com.projectanalyzer.project_analyzer.service.ContextService;
import com.projectanalyzer.project_analyzer.service.GitHubService;
import com.projectanalyzer.project_analyzer.service.GitLabService;
import com.projectanalyzer.project_analyzer.service.GroqLLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

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
    public String analyzeProject(
            @RequestBody String repoUrl,
            HttpSession session
    ) {

        contextService.clear();

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

        // Stop LLM if any service error occurs
        if (readme != null && (readme.startsWith("❌") || readme.startsWith("⚠️"))) {
            return readme;
        }

        // ===================== EXISTING VALIDATIONS =====================

        if ("FETCH_FAILED".equals(readme)) {
    return """
❌ Unable to fetch README from this repository.

Possible reasons:
- Repository does not exist
- Repository is private
- README is missing
- Invalid username or repository name
- API temporarily unavailable

Please verify the repository URL and try again later.
""";
}
        
        if ("RATE_LIMIT".equals(readme)) {
    return """
⚠️ GitHub API rate limit exceeded.

Please try again later.

💡 Tip:
You can configure a GitHub Personal Access Token
to increase API limits.
""";
}

if ("AUTH_FAILED".equals(readme)) {
    return """
❌ GitHub authentication failed.

Please check your GitHub API token configuration.
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

        ContextData context =
        new ContextData(
                readme,
                structure,
                keyFiles
        );

session.setAttribute(
        "repoContext",
        context
);

        // ===================== LLM ANALYSIS =====================

        return groqllmService.analyzeProject(
            context.buildContext()
        );
    }

    @PostMapping("/clear-context")
public ResponseEntity<Void> clearContext() {

    contextService.clear();

    return ResponseEntity.ok().build();
}
}