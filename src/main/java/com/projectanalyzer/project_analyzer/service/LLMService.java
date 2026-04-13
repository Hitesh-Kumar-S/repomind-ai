package com.projectanalyzer.project_analyzer.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LLMService {

    @Value("${groq.api.key}")
    private String apiKey;

    // Safety limit to avoid sending extremely large READMEs
    private static final int MAX_README_LENGTH = 6000;

    public String analyzeProject(String readme) {

        // 🔴 Defensive checks (extra safety)
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

        // ✂️ Trim very large READMEs to safe length
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
-Evaluate the README based ONLY on available content.
-Make sure that the heading are bolded. 
-Scoring Criteria (0–10 each):

-Clarity
-Completeness
-Structure
-Setup Instructions
-Examples / Usage

Format (STRICT):

- **Clarity**: X/10  
- **Completeness**: X/10  
- **Structure**: X/10  
- **Setup Instructions**: X/10  
- **Examples/Usage**: X/10  

**Final Score**: Y/10

-Keep the evaluation fair, realistic, and encouraging.
-If something is missing, reflect it in the score gently.

### **7. Missing or Weak Documentation Sections**
-Identify important sections that are missing or insufficient, such as:
-Setup Instructions
-Usage Examples
-Architecture
-Contribution Guidelines
-License
-Present it in a helpful way:
-"The README could be improved by adding: ..."

### **8. Possible Improvements or Extensions**
- Suggest **realistic and constructive improvements** based on the current scope.
- Avoid speculative or overly advanced features if the README does not suggest them.

### **Positive Closing Note**
- End with a **supportive and encouraging statement** about the project.
- Acknowledge the effort and potential for future growth.

Formatting Guidelines:
- Use **bold text** for important terms, technologies, and key takeaways.
- Use clear section headings.
- Keep the tone **friendly, neutral, and professional**.
- Avoid harsh or judgmental language.

Project README:
""" + readme;


        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            // ---- Message ----
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(message);

            // ---- Request Body ----
            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("messages", messages);
            body.put("temperature", 0.2); // lower = less hallucination

            // ---- Headers ----
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity =
                    new HttpEntity<>(body.toString(), headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            JSONObject json = new JSONObject(response.getBody());

            return json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (Exception e) {
            return "❌ Groq API Error: " + e.getMessage();
        }
    }
}
