package com.projectanalyzer.project_analyzer.controller;

import com.projectanalyzer.project_analyzer.service.GitHubService;
import com.projectanalyzer.project_analyzer.service.GitLabService;
import com.projectanalyzer.project_analyzer.service.BitbucketService;
import com.projectanalyzer.project_analyzer.service.LLMService;
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
    private LLMService llmService;

    @PostMapping("/analyze")
    public String analyzeProject(@RequestBody String repoUrl) {

        String readme;

if (repoUrl.contains("github.com")) {
    readme = gitHubService.fetchReadme(repoUrl);

} else if (repoUrl.contains("gitlab.com")) {
    readme = gitLabService.fetchReadme(repoUrl);

} else if (repoUrl.contains("bitbucket.org")) {
    readme = bitbucketService.fetchReadme(repoUrl);

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

        // ❌ Invalid URL (only applies to GitHub for now)
        if ("INVALID_URL".equals(readme)) {
            return """
❌ **Invalid Repository URL**

The URL provided is not valid.

✅ Please use:
- https://github.com/username/repository
- https://gitlab.com/username/repository
""";
        }

        // ❌ README missing
        if ("README_NOT_FOUND".equals(readme)) {
            return """
❌ **README.md Not Found**

The repository does not contain a readable README.md
or could not be accessed.

⚠️ Project Analyzer relies on README.md for accurate analysis.

✅ Please ensure:
- The repository exists
- It is public
- README.md is present
""";
        }

        // ⚠️ Weak README
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

        // ✅ LLM analysis
        return llmService.analyzeProject(readme);
    }
}