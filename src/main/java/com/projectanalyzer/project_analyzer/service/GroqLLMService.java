package com.projectanalyzer.project_analyzer.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class GroqLLMService implements LLMService {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final int MAX_README_LENGTH = 4000; // 🔥 Reduced for stability

    private static final String GROQ_API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();

    // 🔥 =========================
    // 🔹 GENERIC LLM CALL (UPDATED)
    // 🔥 =========================
    public String callLLM(String prompt) {

        int retries = 0;

        while (retries < 3) {
            try {
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);

                JSONArray messages = new JSONArray();
                messages.put(message);

                JSONObject body = new JSONObject();
                body.put("model", "llama-3.1-8b-instant");
                body.put("messages", messages);
                body.put("temperature", 0.2);
                body.put("max_tokens", 1000); // 🔥 prevents token overflow

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity =
                        new HttpEntity<>(body.toString(), headers);

                ResponseEntity<String> response =
                        restTemplate.postForEntity(GROQ_API_URL, entity, String.class);

                return extractContent(response.getBody());

            } catch (HttpClientErrorException e) {

                int status = e.getStatusCode().value();

                // 🔥 RATE LIMIT HANDLING
                if (status == 429) {
                    try {
                        Thread.sleep(2000); // wait 2 sec
                    } catch (InterruptedException ignored) {}
                    retries++;
                    continue;
                }

                // 🔥 AUTH ERROR
                if (status == 401) {
                    return "❌ Invalid Groq API Key. Please check configuration.";
                }

                return "❌ Groq API Error: " + e.getMessage();

            } catch (Exception e) {

    System.out.println("Groq Error: " + e.getMessage());

    String error = e.getMessage().toLowerCase();

    // 🔥 Internet / connection issues
    if (error.contains("connection")
            || error.contains("timeout")
            || error.contains("host")
            || error.contains("network")) {

        return "❌ Unable to connect to AI service. Please check your internet connection.";
    }

    // 🔥 Retry on temporary failures
    try {
        Thread.sleep(1000);
    } catch (InterruptedException ignored) {}

    retries++;
}
        }

        return "⚠️ Groq service is busy. Please try again in a few seconds.";
    }

    // 🔥 =========================
    // 🔹 PROJECT ANALYSIS (UNCHANGED PROMPT)
    // 🔥 =========================
    public String analyzeProject(String readme) {

        if (readme == null || "WEAK_README".equals(readme)) {
            return """
❌ Analysis Skipped

The README.md provided is missing or insufficient.

⚠️ To generate an accurate project analysis, please ensure the README contains:
- Clear project overview
- Tech stack
- Architecture or flow
- Features and improvements
""";
        }

        // 🔥 Reduced size to avoid rate limit
        if (readme.length() > MAX_README_LENGTH) {
            readme = readme.substring(0, MAX_README_LENGTH);
        }

        String prompt = """
You are an experienced software engineer and technical reviewer helping developers better understand and present their projects.

Analyze the following software project using ONLY the information explicitly available in the provided project context (README, repository structure, and important files).

Keep explanations concise and well-structured.
Avoid excessively long paragraphs.
Focus on important insights only.

Do not invent unsupported project-specific details. If information is unclear or unavailable, mention it neutrally.
If something is not mentioned, gently state that it is not specified in the README.

Explain the project in a **clear, professional, and supportive tone**, as if guiding the project owner during an interview.
Keep responses concise, information-dense, and avoid unnecessary repetition or overly long explanations.
Highlight **important concepts, technologies, and conclusions in bold** so they are easy to notice.

Use the following structure:


### **1. Project Overview**
- Explain what the project does and the problem it aims to solve.
- Clearly summarize the project purpose in a concise and understandable way.
- If the problem statement is unclear or missing, mention it politely and neutrally.


### **2. Key Features**
- Summarize the main functionalities or capabilities described in the project.
- Focus on practical features and user-facing functionality.
- If the project has limited functionality, mention it constructively without sounding harsh.


### **3. Tech Stack Used**
- List the programming languages, frameworks, libraries, tools, APIs, or platforms explicitly mentioned in the project.
- For each technology, briefly mention its purpose or role in the project when it is reasonably clear.
  Example:
  - **Spring Boot** → Backend framework for REST APIs
  - **MongoDB** → Database for storing application data
  - **HTML/CSS/JavaScript** → Frontend user interface
- Highlight each technology in **bold**.
- Do not assume technologies that are not clearly supported by the context.


### **4. Architecture / Design Approach**
- Focus primarily on explaining the application's request lifecycle or execution flow.
- Describe how data or requests move through the system using a clear action → action format whenever possible.
- Prefer workflow explanations over generic architectural descriptions.

Example:
**User Request → Controller → Service Layer → Database → Response**

IMPORTANT:
- Always include at least one execution flow or request lifecycle if enough context is available.
- Mention the key components involved in the workflow.
- Avoid only listing services, layers, or architecture qualities without describing how they interact.
- Keep the explanation concise, technical, and easy to understand.
- Mention the architecture style briefly only if it is clearly supported by the context.

If architectural details are unavailable, respond neutrally:

**"The project does not explicitly describe the system architecture or execution flow."**

### **5. Interview Explanation (Short Summary)**
- Explain the project as if the developer is confidently presenting it in an interview.
- Keep the explanation concise, structured, and technically clear.
- Emphasize important implementation decisions, technologies, and project goals.
- Avoid sounding overly promotional or generic.


### **6. README Quality Score**
Evaluate the README based ONLY on the available project context.

Scoring Criteria (0–10 each):
- Clarity
- Completeness
- Structure
- Setup Instructions
- Examples / Usage

IMPORTANT:
- Each score MUST appear on a separate line.
- Use proper markdown bullet formatting.
- Do NOT combine multiple scores into a paragraph.
- Brief explanations for each score should remain concise.

Format (STRICT):

- **Clarity**: X1/10 — brief reason
- **Completeness**: X2/10 — brief reason
- **Structure**: X3/10 — brief reason
- **Setup Instructions**: X4/10 — brief reason
- **Examples/Usage**: X5/10 — brief reason

**Final Score**: (Average of all category scores)/10


### **7. Missing or Weak Documentation Sections**
- Identify missing, incomplete, or weak documentation areas.
- Mention only sections that are genuinely absent or underdeveloped.
- Common examples include:
  Setup Instructions, Usage Examples, Architecture, API Documentation, Contribution Guidelines, Deployment, and License.


### **8. Suggested Features & Enhancements**
- Suggest practical features, technical enhancements, integrations, or usability improvements that could strengthen the project.
- Recommendations should align with the current project scope, architecture, and tech stack.
- Mention missing but potentially valuable capabilities when relevant.
- Examples may include:
  Authentication, Search, Notifications, Caching, API Documentation, Analytics, Role-Based Access, Docker Support, Deployment Improvements, Performance Optimization, or UI/UX Enhancements.
- Avoid unrealistic or overly ambitious suggestions.
- Keep recommendations practical, concise, and technically meaningful.


### **Final Thoughts**
- Provide a concise professional conclusion about the project's overall quality, technical direction, maintainability, and documentation maturity.
- Mention practical improvements if relevant.
- Keep the tone professional, constructive, and interview-ready.

IMPORTANT:
- Keep the entire analysis concise and readable.
- Prefer short paragraphs and focused explanations.
- Avoid repeating the same idea across multiple sections.


Project README:
""" + readme;

        return callLLM(prompt);
    }

    // 🔥 =========================
    // 🔹 SAFE RESPONSE PARSER (UPDATED)
    // 🔥 =========================
    private String extractContent(String responseBody) {

        try {
            JSONObject json = new JSONObject(responseBody);

            // 🔥 Handle error response safely
            if (json.has("error")) {
                return "❌ Groq Error: " + json.getJSONObject("error").getString("message");
            }

            return json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (Exception e) {
            return "❌ Failed to parse Groq response.";
        }
    }

    // 🔥 =========================
    // 🔹 CHAT RESPONSE
    // 🔥 =========================
    public String chatResponse(String prompt) {
        return callLLM(prompt);
    }

    @Override
    public String generateResponse(String prompt) {
        return callLLM(prompt);
    }
}