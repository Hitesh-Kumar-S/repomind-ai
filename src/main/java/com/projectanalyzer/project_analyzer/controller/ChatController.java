package com.projectanalyzer.project_analyzer.controller;

import com.projectanalyzer.project_analyzer.dto.ChatRequest;
import org.springframework.http.ResponseEntity;
import com.projectanalyzer.project_analyzer.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {

        String response = chatService.chat(
            request.getQuestion(),
            request.isStrictMode()
        );

        return ResponseEntity.ok(response);
    }
}