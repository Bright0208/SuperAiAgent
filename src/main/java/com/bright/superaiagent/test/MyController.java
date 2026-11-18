package com.bright.superaiagent.test;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI聊天接口")
@RestController
public class MyController {

    private final ChatClient chatClient;

    private final ChatClient customChatClient;
    @Resource
    private DashScopeChatModel chatModel;

    public MyController(ChatClient.Builder builder, DashScopeChatModel chatModel) {
        // 方式一：使用 Spring 注入的 Builder
        this.chatClient = builder
                .defaultSystem("你是恋爱大师")
                .build();

        // 方式二：使用指定的 DashScopeChatModel
        this.customChatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful assistant.")
                .build();
    }

    @Operation(summary = "调用千问大模型")
    @GetMapping("/ai")
    public String generation(
            @Parameter(description = "用户输入")
            @RequestParam String userInput) {

        return chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }
    @Operation(summary = "调用DashScope大模型")
    @GetMapping("/DashScopeChatModel")
    public String testDashScope(
            @Parameter(description = "用户输入")
            @RequestParam String userInput){
        return customChatClient.prompt().user(userInput).call().content();
    }
}