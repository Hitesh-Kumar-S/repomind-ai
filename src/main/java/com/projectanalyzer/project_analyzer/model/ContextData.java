package com.projectanalyzer.project_analyzer.model;

public class ContextData {

    private String readme;
    private String repoStructure;
    private String keyFiles;

    public ContextData() {
    }

    public ContextData(
            String readme,
            String repoStructure,
            String keyFiles
    ) {
        this.readme = readme;
        this.repoStructure = repoStructure;
        this.keyFiles = keyFiles;
    }

    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }

    public String getRepoStructure() {
        return repoStructure;
    }

    public void setRepoStructure(String repoStructure) {
        this.repoStructure = repoStructure;
    }

    public String getKeyFiles() {
        return keyFiles;
    }

    public void setKeyFiles(String keyFiles) {
        this.keyFiles = keyFiles;
    }

    public String buildContext() {

        String r = readme != null
                ? readme
                : "No README available";

        String s = repoStructure != null
                ? repoStructure
                : "No structure available";

        String k = keyFiles != null
                ? keyFiles
                : "No key files available";

        return """
PROJECT README:
%s

PROJECT STRUCTURE:
%s

IMPORTANT FILES:
%s
""".formatted(r, s, k);
    }
}