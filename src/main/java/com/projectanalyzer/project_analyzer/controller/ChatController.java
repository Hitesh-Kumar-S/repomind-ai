package com.projectanalyzer.project_analyzer.controller;

import jakarta.servlet.http.HttpSession;
import com.projectanalyzer.project_analyzer.model.ContextData;
import com.projectanalyzer.project_analyzer.dto.ChatRequest;
import org.springframework.http.ResponseEntity;
import com.projectanalyzer.project_analyzer.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.projectanalyzer.project_analyzer.service.ContextService;

@RestController
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;

    private final ContextService contextService;

    public ChatController(
        ChatService chatService,
        ContextService contextService
) {
    this.chatService = chatService;
    this.contextService = contextService;
}

    @PostMapping("/chat")
public ResponseEntity<String> chat(
        @RequestBody ChatRequest request,
        HttpSession session
) {

    ContextData context =
        (ContextData) session.getAttribute("repoContext");

if (context == null) {

    return ResponseEntity.ok(
        "Please analyze a repository first before using the chatbot."
    );
}

    String response = chatService.chat(
    request.getQuestion(),
    request.isStrictMode(),
    context.buildContext()
);

    return ResponseEntity.ok(response);
}

}