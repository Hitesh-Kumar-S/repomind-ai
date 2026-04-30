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
                body.put("max_tokens", 800); // 🔥 prevents token overflow

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

                // 🔥 Retry on network errors
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
You are an experienced software engineer and a friendly technical interviewer.

Analyze the following software project using ONLY the information explicitly available in the README.md.
Please do NOT assume or invent missing details.
If something is not mentioned, gently state that it is not specified in the README.

Explain the project in a **clear, professional, and supportive tone**, as if guiding the project owner during an interview.
Highlight **important concepts, technologies, and conclusions in bold** so they are easy to notice.

Use the following structure:

### **1. Project Overview**
- Explain **what the project does** and **what problem it aims to solve**.
- If the problem statement is unclear, mention this politely.

### **2. Key Features**
- Summarize the **main functionalities or capabilities** described in the README.
- If features are limited, acknowledge them without being harsh.

### **3. Tech Stack Used**
- List the **programming languages, frameworks, tools, or platforms** explicitly mentioned.
- Highlight each technology in **bold**.
- Do not assume technologies that are not stated.

### **4. Architecture / Design Approach**
- Describe the project flow in an **action → action format** (using arrows `→`) if architecture or flow is mentioned.
- Example format:  
  **User Request → Controller → Service Layer → Database → Response**
- Keep the flow **simple, linear, and easy to understand**.
- If architectural details are not mentioned, state in a neutral way:  
  **"The README does not explicitly describe the system architecture or execution flow."**

### **5. Interview Explanation (2 minutes)**
- Explain the project as if the **project owner** is confidently describing it in an interview.
- Keep it **concise, structured, and easy to understand**.
- Emphasize **key technical decisions** in bold.

### **6. README Quality Score**
Evaluate the README based ONLY on available content.

Scoring Criteria (0–10 each):
- Clarity
- Completeness
- Structure
- Setup Instructions
- Examples / Usage

Format (STRICT):

- **Clarity**: X1/10  
- **Completeness**: X2/10  
- **Structure**: X3/10  
- **Setup Instructions**: X4/10  
- **Examples/Usage**: X5/10  

**Final Score**: (X1 + X2 + X3 + X4 + X5)/10

### **7. Missing or Weak Documentation Sections**
- Identify missing sections:
Setup Instructions, Usage, Architecture, Contribution, License

### **8. Possible Improvements**
- Suggest realistic improvements

### **Positive Closing Note**
- Encourage the developer

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