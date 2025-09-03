package com.vtnet.netat.web.ai;

import com.vtnet.netat.driver.ConfigReader;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;

public class AiModelFactory {

    private static final Logger log = LoggerFactory.getLogger(AiModelFactory.class);

    public static ChatModel createModel() {
        String provider = ConfigReader.getProperty("ai.provider");
        if (provider == null || provider.trim().isEmpty()) {
            log.warn("Nhà cung cấp AI (ai.provider) không được cấu hình. Bỏ qua.");
            return null;
        }

        long timeout = Long.parseLong(ConfigReader.getProperty("ai.timeout.seconds", "60"));
        log.info("Khởi tạo mô hình AI từ nhà cung cấp: {}", provider.toUpperCase());

        switch (provider.toLowerCase()) {
            case "gemini":
                return GoogleAiGeminiChatModel.builder()
                        .apiKey(ConfigReader.getProperty("ai.gemini.apiKey"))
                        .modelName(ConfigReader.getProperty("ai.gemini.modelName", "gemini-pro"))
                        .timeout(Duration.ofSeconds(timeout))
                        .build();
//            case "huggingface":
//                return HuggingFaceChatModel.builder()
//                        .accessToken(ConfigReader.getProperty("ai.huggingface.token"))
//                        .modelId(ConfigReader.getProperty("ai.huggingface.modelId"))
//                        .waitForModel(true)
//                        .timeout(Duration.ofSeconds(timeout))
//                        .build();
            case "custom":
                return OpenAiChatModel.builder()
                        .baseUrl(ConfigReader.getProperty("ai.custom.baseUrl"))
                        .apiKey(ConfigReader.getProperty("ai.custom.apiKey"))
                        .modelName(ConfigReader.getProperty("ai.custom.modelName"))
                        .timeout(Duration.ofSeconds(timeout))
                        .build();
            default:
                log.error("Nhà cung cấp AI '{}' không được hỗ trợ.", provider);
                return null;
        }
    }
}