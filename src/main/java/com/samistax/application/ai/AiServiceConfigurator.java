package com.samistax.application.ai;

import com.dtsx.astra.sdk.AstraDBCollection;
import com.samistax.application.Application;
import com.samistax.application.ai.tools.ShopAssistantTools;
import com.samistax.application.services.AstraService;
import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore;
import dev.langchain4j.store.memory.chat.cassandra.CassandraChatMemoryStore;

import dev.langchain4j.store.embedding.EmbeddingStore;

import io.stargate.sdk.data.domain.SimilarityMetric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static dev.ai4j.openai4j.image.ImageModel.DALL_E_QUALITY_HD;


@Service
public class AiServiceConfigurator {
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

    private ShopAssistantAgent shopAssistant;
    private ShopAssistantTools shopAssistantTools;

    // Configurable settings from UI
    private OpenAiImageModel imageModel;
    private StreamingChatLanguageModel chatModel;
    private EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    private int retrieverMaxResults = 3;
    private int retrieverVectorDimension = 384;
    private double retrieverMinScore = 0.6;
    private int memoryMaxItems = 10;

    public AiServiceConfigurator() {}

    @Bean
    OpenAiImageModel imageEmbeddingModel() {

        imageModel = OpenAiImageModel.builder()
                .apiKey(OPENAI_API_KEY)
                .quality(DALL_E_QUALITY_HD)
                .logRequests(true)
                .logResponses(true)
                .withPersisting(false)
                .build();

        return imageModel;
    }

    @Bean
    CassandraChatMemoryStore chatMemoryStore(AstraService astraService) {
        // TODO: The below Throws Caused by: java.lang.ClassNotFoundException: com.dtsx.astra.sdk.cassio.ClusteredTable
        return CassandraChatMemoryStore.builderAstra()
                .token(ASTRA_TOKEN)
                .databaseId(UUID.fromString(ASTRA_DB_ID))
                .databaseRegion(ASTRA_DB_REGION)
                //.keyspace(ASTRA_DEFAULT_KEYSPACE)
                .build();
        //return new CassandraChatMemoryStore(astraService.getAstraClient().cqlSession());
    }
    @Bean
    ChatMemoryProvider chatMemoryProvider(CassandraChatMemoryStore memoryStore) {

        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(memoryMaxItems)
                .chatMemoryStore(memoryStore)
                .build();
        return chatMemoryProvider;
    }

    @Bean
    ShopAssistantAgent shopAssistant(StreamingChatLanguageModel model, CassandraChatMemoryStore memoryStore, ChatMemoryProvider chatMemoryProvider, ContentRetriever retriever, ShopAssistantTools tool) {
        var memory = TokenWindowChatMemory.builder()
                .maxTokens(2000, new OpenAiTokenizer("gpt-3.5-turbo"))
                .chatMemoryStore(memoryStore)
                .build();

        shopAssistant = AiServices.builder(ShopAssistantAgent.class)
                .streamingChatLanguageModel(model)
                .chatMemoryProvider(chatMemoryProvider)
                .chatMemory(memory)
                .contentRetriever(retriever)
                //.retriever(retriever)
                .tools(tool)
                .build();

        return shopAssistant;
    }
    @Bean
    StreamingChatLanguageModel chatModel() {
        chatModel =  OpenAiStreamingChatModel.withApiKey(OPENAI_API_KEY);
        return chatModel;
    }
    @Bean
    EmbeddingModel embeddingModel() {
        return embeddingModel;
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(AstraService astraService) {
        // Create new  Astra DB Embedding store instance
        /*AstraDbEmbeddingStore embeddingStore = new AstraDbEmbeddingStore(AstraDbEmbeddingConfiguration
                .builder()
                .token(ASTRA_TOKEN)
                .databaseId(ASTRA_DB_ID)
                .databaseRegion(ASTRA_DB_REGION)
                .keyspace(ASTRA_DEFAULT_KEYSPACE)
                .table(Application.ASTRA_PRODUCT_TABLE)
                .dimension(retrieverVectorDimension) // Used with MiniLM-L6-v2 model
                .build());
         */
        // Create Astra DB Embedding store from existing collection
        AstraDBCollection collection = astraService.createCollection(Application.ASTRA_PRODUCT_TABLE, retrieverVectorDimension, SimilarityMetric.cosine);
        AstraDbEmbeddingStore embeddingStore = new AstraDbEmbeddingStore(collection);
        return embeddingStore;
    }
    @Bean
    ContentRetriever retriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(this.retrieverMaxResults)
                .minScore(this.retrieverMinScore)
                .build();
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
    public void setEmbeddingModel(EmbeddingModel model){
        this.embeddingModel = model;
    }

    public StreamingChatLanguageModel getChatModel() {
        return chatModel;
    }

    public void setChatModel(StreamingChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    public int getRetrieverMaxResults() {
        return retrieverMaxResults;
    }

    public void setRetrieverMaxResults(int retrieverMaxResults) {
        this.retrieverMaxResults = retrieverMaxResults;
    }

    public int getRetrieverVectorDimension() {
        return retrieverVectorDimension;
    }

    public void setRetrieverVectorDimension(int retrieverVectorDimension) {
        this.retrieverVectorDimension = retrieverVectorDimension;
    }

    public double getRetrieverMinScore() {
        return retrieverMinScore;
    }

    public void setRetrieverMinScore(double retrieverMinScore) {
        this.retrieverMinScore = retrieverMinScore;
    }

    public int getMemoryMaxItems() {
        return memoryMaxItems;
    }

    public void setMemoryMaxItems(int memoryMaxItems) {
        this.memoryMaxItems = memoryMaxItems;
    }
}



