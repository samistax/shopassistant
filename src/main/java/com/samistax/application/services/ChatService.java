package com.samistax.application.services;

import com.samistax.application.ai.ShopAssistantAgent;
import com.samistax.application.ai.tools.ShopAssistantTools;
import com.samistax.application.data.astra.json.Product;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.store.memory.chat.cassandra.AstraDbChatMemoryStore;


import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.cassandra.CassandraChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ChatService {
    @Value("${openai.apikey}")
    private String OPENAI_API_KEY;
    @Value( "${astra.api.application-token}" )
    private String ASTRA_TOKEN;
    @Value( "${astra.api.database-id}" )
    private String ASTRA_DB_ID;
    @Value( "${astra.api.database-region}" )
    private String ASTRA_DB_REGION;
    @Value( "${astra.api.default-keyspace:default_keyspace}" )
    public  String ASTRA_DEFAULT_KEYSPACE;

    private ShopAssistantAgent streamingShopAssistant;
    private ShopAssistantTools shopAssistantTools;
    private EmbeddingModel embeddingModel;
    private CassandraChatMemoryStore memoryStore;


    public ChatService(ShopAssistantTools shopAssistantTools, CassandraChatMemoryStore memoryStore) {
        this.shopAssistantTools = shopAssistantTools;
        this.memoryStore = memoryStore;
    }

    @PostConstruct
    public void init() {

        if (OPENAI_API_KEY == null) {
            System.err.println("ERROR: OPENAI_API_KEY environment variable is not set. Please set it to your OpenAI API key.");
        }
        this.embeddingModel =  new AllMiniLmL6V2EmbeddingModel();

        //var memoryStore = new AstraDbChatMemoryStore( ASTRA_TOKEN,  ASTRA_DB_ID,  ASTRA_DB_REGION,  ASTRA_DEFAULT_KEYSPACE);
        var memory = TokenWindowChatMemory.builder()
                .maxTokens(2000, new OpenAiTokenizer("gpt-3.5-turbo"))
                .chatMemoryStore(memoryStore)
                .build();

        //var memoryWindow = MessageWindowChatMemory.withMaxMessages(10);
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(memoryStore)
                .build();

        // TODO: Remove table creation once AstraDbChatMemoryStore is generating the message_store table as part of the constructor
        //CqlSession cqlSession = AstraClient.builder().withToken(ASTRA_TOKEN).withCqlKeyspace(ASTRA_DEFAULT_KEYSPACE).withDatabaseId(ASTRA_DB_ID).withDatabaseRegion(ASTRA_DB_REGION).enableCql().enableDownloadSecureConnectBundle().build().cqlSession();
        //cqlSession.execute("CREATE TABLE IF NOT EXISTS " + ASTRA_DEFAULT_KEYSPACE + "." + "message_store" + " (partition_id text, row_id timeuuid, body_blob text, PRIMARY KEY ((partition_id), row_id)) WITH CLUSTERING ORDER BY (row_id DESC)");
        // TODO: Remove table creation once AstraDbChatMemoryStore is generating the message_store table as part of the constructor

        streamingShopAssistant = AiServices.builder(ShopAssistantAgent.class)
                .streamingChatLanguageModel(OpenAiStreamingChatModel.withApiKey(OPENAI_API_KEY))
                .chatMemory(memory)
                .tools(shopAssistantTools)
                .build();
    }
    public TokenStream chatShopAssistant(String message, Product product) {
        return streamingShopAssistant.productChat( message,product);
    }
}