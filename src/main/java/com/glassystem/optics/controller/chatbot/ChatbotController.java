package com.glassystem.optics.controller.chatbot;

import com.glassystem.optics.dto.request.ChatbotChatRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.ChatbotChatResponse;
import com.glassystem.optics.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatbotController {

    ChatbotService chatbotService;

    @PostMapping("/chat")
    public ApiResponse<ChatbotChatResponse> chat(@RequestBody @Valid ChatbotChatRequest request) {
        String reply = chatbotService.chat(request.getMessages());
        return ApiResponse.<ChatbotChatResponse>builder()
                .result(ChatbotChatResponse.builder().reply(reply).build())
                .build();
    }
}
