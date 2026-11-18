package com.bright.superaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String message  = "你好，我是程序员郭明亮";
        String answer = loveApp.doChat(message, chatId);
        //第二轮
        message  = "我想让我的另一半（伍卓仪），更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        //第三轮
        message  = "我的另一半叫什么来着，我刚刚跟你说过";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String message  = "你好，我是程序员郭明亮，我想让另一半（伍卓仪）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }
}