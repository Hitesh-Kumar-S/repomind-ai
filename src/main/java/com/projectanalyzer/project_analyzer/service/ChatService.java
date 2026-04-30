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

    // 🔥 LIMIT CONTEXT SIZE (CRITICAL FIX)
    private static final int MAX_CONTEXT_LENGTH = 3000;

    private String trimContext(String text) {
        if (text == null) return "";
        return text.length() > MAX_CONTEXT_LENGTH
                ? text.substring(0, MAX_CONTEXT_LENGTH)
                : text;
    }

    // 🔥 CLEAN OUTPUT (PREVENT GARBAGE TEXT)
    private String cleanResponse(String response) {
        if (response == null) return "";

        // Prevent extremely long / corrupted outputs
        if (response.length() > 5000) {
            response = response.substring(0, 5000);
        }

        return response.trim();
    }

    public String chat(String question, boolean strictMode) {

        if (!contextService.hasContext()) {
            return "❌ Please analyze a repository first.";
        }

        // 🔥 FIX: Trim context before sending to LLM
        String context = trimContext(contextService.buildContext());

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

            // 🔥 fallback to Groq
            try {
                return cleanResponse(groqService.generateResponse(prompt));
            } catch (Exception ex) {
                return "⚠️ I'm experiencing high traffic. Please try again in a few moments.";
            }
        }
    }

    // 🔒 STRICT MODE
    private String buildStrictPrompt(String context, String question) {
        return """
You are an AI assistant.

Answer ONLY using the project README content below.
Do NOT use any external knowledge.
Do NOT guess or assume anything.

If the answer is not present in the README, respond exactly with:
"This is not mentioned in the project README."

README:
%s

Question:
%s
""".formatted(context, question);
    }

    // 🧠 SMART MODE
    private String buildSmartPrompt(String context, String question) {
        return """
You are an expert software engineer and AI assistant.

You are helping a user understand a project and answer technical questions.

You are given project context (README + structure), but you are NOT limited to it.

You can:
- Answer questions about the project
- Explain technologies
- Suggest improvements
- Compare tech stacks
- Answer general programming questions

IMPORTANT:
- Use project context when relevant
- If context is missing, use your own knowledge
- Do NOT say "according to the README"
- Do NOT restrict yourself only to the context
- Give clear, confident, and direct answers

Tone:
- Professional
- Concise
- Interview-ready

Context:
%s

Question:
%s
""".formatted(context, question);
    }
}