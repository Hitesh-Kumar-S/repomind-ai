package com.projectanalyzer.project_analyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    @Autowired
    private ContextService contextService;

    @Autowired
    private GroqLLMService groqService;

    @Autowired
    private OpenRouterLLMService openRouterService;

    // 🔥 PRIORITY LIMITS
    private static final int README_LIMIT = 12000;
    private static final int STRUCTURE_LIMIT = 3000;
    private static final int KEYFILES_LIMIT = 3000;

    // 🔧 Trim helper
    private String trim(String text, int limit) {
        if (text == null) return "";
        return text.length() > limit ? text.substring(0, limit) : text;
    }

    // 🔥 Build SMART context (NEW)
    private String buildSmartContext() {
        String readme = trim(contextService.getReadme(), README_LIMIT);
        String structure = trim(contextService.getRepoStructure(), STRUCTURE_LIMIT);
        String keyFiles = trim(contextService.getKeyFiles(), KEYFILES_LIMIT);

        return """
README:
%s

STRUCTURE:
%s

KEY FILES:
%s
""".formatted(readme, structure, keyFiles);
    }

    // 🔥 Clean response
    private String cleanResponse(String response) {
        if (response == null) return "";

        if (response.length() > 5000) {
            response = response.substring(0, 5000);
        }

        return response.trim();
    }

    public String chat(String question, boolean strictMode, String context) {

        String prompt = strictMode
                ? buildStrictPrompt(context, question)
                : buildSmartPrompt(context, question);

        // 🔒 STRICT MODE → Groq
        if (strictMode) {
            try {
                return cleanResponse(groqService.generateResponse(prompt));
            } catch (Exception e) {
                return "⚠️ Unable to process request. Please try again.";
            }
        }

        // 🧠 SMART MODE → OpenRouter → fallback Groq
        try {
            return cleanResponse(openRouterService.generateResponse(prompt));
        } catch (Exception e) {
            try {
                return cleanResponse(groqService.generateResponse(prompt));
            } catch (Exception ex) {
                return "⚠️ I'm experiencing high traffic. Please try again in a few moments.";
            }
        }
    }

    // 🔒 STRICT MODE PROMPT (UNCHANGED)
    private String buildStrictPrompt(String context, String question) {
    return """
You are an AI assistant helping users understand a software project.

Your primary source is the provided project context, including the README, repository structure, and important files.

Rules:
- Prioritize information explicitly available in the provided project context.
- If the project mentions a technology, framework, library, tool, API, or concept, you MAY use your general knowledge to explain what it commonly does and why it is generally used.
- Do NOT invent project-specific facts, decisions, architecture details, or implementation details that are not supported by the context.
- Never assume a technology or framework is absent simply because it is not clearly visible in the provided context.
- If something is not explicitly mentioned, clearly say:
  "The provided project context does not clearly specify this detail."
- Be accurate, honest, concise, and helpful.
- Keep answers professional and interview-ready.

Examples:
- If the project mentions YOLOv8, you may explain that YOLOv8 is commonly used for real-time object detection.
- But do NOT assume why the developer specifically chose YOLOv8 unless the context explicitly mentions it.
- If Marked.js appears in the context, you may explain that it is commonly used for rendering Markdown into HTML.

Project Context:
%s

Question:
%s
""".formatted(context, question);
}

    // 🧠 SMART MODE PROMPT (UNCHANGED)
    private String buildSmartPrompt(String context, String question) {
    return """
You are an expert software engineer and AI assistant.

You are helping a user understand a software project and answer technical questions.

You are given project context (README, repository structure, and key files), but you are NOT limited to it.

Your responsibilities include:
- Explain project concepts
- Explain technologies and frameworks
- Suggest improvements
- Compare tools and architectures
- Answer general programming questions
- Provide interview-style explanations

IMPORTANT:
- Use the provided project context whenever relevant.
- If the context is incomplete, use your own technical knowledge to provide helpful explanations.
- Do NOT invent unsupported project-specific implementation details.
- Avoid saying phrases like:
  "According to the README"
  or
  "The context does not mention..."
  unless absolutely necessary.
- Give clear, natural, and technically grounded explanations.

Tone:
- Professional
- Clear
- Concise
- Helpful
- Interview-ready

Project Context:
%s

Question:
%s
""".formatted(context, question);
}
}