package com.samistax.application.ai;
import com.samistax.application.data.astra.json.Product;
import dev.langchain4j.service.*;

public interface ShopAssistantAgent {

    @SystemMessage({
            "You are a customer support agent of a footwear shop company.",
            "Before providing information about specific details about product in the shop, you MUST always check:",
            "id, name and description.",
            "You can use tools to find more information of the {{product}} and search for products in the store.",
    })
    TokenStream productChat(@UserMessage String message, @V("product") Product product);

    TokenStream chat(@MemoryId String userId, @UserMessage String message);

}
