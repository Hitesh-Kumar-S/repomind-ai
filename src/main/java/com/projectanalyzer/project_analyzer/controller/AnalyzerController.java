package com.projectanalyzer.project_analyzer.controller;

import com.projectanalyzer.project_analyzer.service.GitHubService;
import com.projectanalyzer.project_analyzer.service.LLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class AnalyzerController {

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private LLMService llmService;

    @PostMapping("/analyze")
    public String analyzeProject(@RequestBody String repoUrl) {

        // 🔴 Step 1: Validate GitHub repository URL format
        if (repoUrl == null || !repoUrl.matches("^https://github.com/[^/]+/[^/]+/?$")) {
            return """
❌ **Invalid GitHub Repository URL**

The URL provided is **not a valid GitHub repository link**.

✅ Please use the following format:
https://github.com/username/repository

⚠️ Example:
https://github.com/spring-projects/spring-boot
""";
        }

        // 🔴 Step 2: Fetch README from GitHub
        String readme = gitHubService.fetchReadme(repoUrl);

        // ❌ README missing or repository inaccessible
        if (readme == null) {
            return """
❌ **README.md Not Found**

The provided GitHub repository does **not contain a readable README.md**
or the repository could not be accessed.

⚠️ Project Analyzer relies heavily on README.md for accurate analysis.

✅ Please ensure:
- The repository exists
- It is public
- It contains a well-structured README.md
""";
        }

        // ⚠️ README exists but is weak
        if ("WEAK_README".equals(readme)) {
            return """
⚠️ **Weak README Detected**

A README.md file was found, but it appears to be **too short or incomplete**.

⚠️ The analysis may be **generic or partially inaccurate**.

💡 For best results, include:
- Project overview
- Tech stack
- Architecture or workflow
- Features
- Improvements
""";
        }

        // ✅ Step 3: Safe to analyze with LLM
        return llmService.analyzeProject(readme);
    }
}
