package com.samistax.application.services;

import com.samistax.application.ai.ShopAssistantAgent;
import com.samistax.application.ai.tools.ShopAssistantTools;
import com.samistax.application.data.astra.json.Product;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ChatService {
    @Value("${openai.apikey}")
    private String OPENAI_API_KEY;
    private Assistant assistant;
    private StreamingAssistant streamingAssistant;
    private ShopAssistantAgent streamingShopAssistant;
    private ShopAssistantTools shopAssistantTools;
    private EmbeddingModel embeddingModel;

    interface Assistant {
        String chat(String message);
    }

    interface StreamingAssistant {
        TokenStream chat(@UserMessage String message);
    }

    public ChatService(ShopAssistantTools shopAssistantTools) {
        this.shopAssistantTools = shopAssistantTools;
    }

    @PostConstruct
    public void init() {

        if (OPENAI_API_KEY == null) {
            System.err.println("ERROR: OPENAI_API_KEY environment variable is not set. Please set it to your OpenAI API key.");
        }
        this.embeddingModel =  new AllMiniLmL6V2EmbeddingModel();

        var memory = TokenWindowChatMemory.withMaxTokens(2000, new OpenAiTokenizer("gpt-3.5-turbo"));

        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(OpenAiChatModel.withApiKey(OPENAI_API_KEY))
                .chatMemory(memory)
                .build();

        streamingAssistant = AiServices.builder(StreamingAssistant.class)
                .streamingChatLanguageModel(OpenAiStreamingChatModel.withApiKey(OPENAI_API_KEY))
                .chatMemory(memory)
                .build();

        streamingShopAssistant = AiServices.builder(ShopAssistantAgent.class)
                .streamingChatLanguageModel(OpenAiStreamingChatModel.withApiKey(OPENAI_API_KEY))
                .chatMemory(memory)
                .tools(shopAssistantTools)
                .build();

    }

    public String chat(String message) {
        return assistant.chat(message);
    }

    public TokenStream chatTokenStream(String message) {
        return streamingAssistant.chat(message);
    }
    public TokenStream chatShopAssistant(String message, Product product) {
        //return streamingAssistant.chat(message);
        return streamingShopAssistant.chat( message,product);
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return embeddingModel;
    }
    public void setEmbeddingModel(EmbeddingModel model){
        this.embeddingModel = model;
    }

}