package com.projectanalyzer.project_analyzer.service;

import org.springframework.stereotype.Service;

@Service
public class ContextService {

    private String currentReadme;
    private String repoStructure;
    private String keyFiles;

    // ===================== README =====================

    public void setReadme(String readme) {
        this.currentReadme = readme;
    }

    public String getReadme() {
        return currentReadme;
    }

    // ===================== REPO STRUCTURE =====================

    public void setRepoStructure(String structure) {
        this.repoStructure = structure;
    }

    public String getRepoStructure() {
        return repoStructure;
    }

    // ===================== KEY FILES =====================

    public void setKeyFiles(String keyFiles) {
        this.keyFiles = keyFiles;
    }

    // 🔥 FIX: Missing getter (this caused your error)
    public String getKeyFiles() {
        return keyFiles;
    }

    // ===================== CONTEXT CHECK =====================

    public boolean hasContext() {
        return currentReadme != null && !currentReadme.isEmpty();
    }

    public void clear() {
        this.currentReadme = null;
        this.repoStructure = null;
        this.keyFiles = null; // 🔥 FIX: reset properly
    }

    // ===================== BUILD CONTEXT =====================

    public String buildContext() {

        String readme = currentReadme != null ? currentReadme : "No README available";
        String structure = repoStructure != null ? repoStructure : "No structure available";
        String files = keyFiles != null ? keyFiles : "No key files available";

        return """
PROJECT README:
%s

PROJECT STRUCTURE:
%s

IMPORTANT FILES:
%s
""".formatted(readme, structure, files);
    }
}