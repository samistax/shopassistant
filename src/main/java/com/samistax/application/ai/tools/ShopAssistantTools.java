package com.samistax.application.ai.tools;

import com.dtsx.astra.sdk.AstraDB;
import com.dtsx.astra.sdk.AstraDBCollection;
import com.samistax.application.Application;
import com.samistax.application.data.astra.json.Product;
import com.samistax.application.services.AstraService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.stargate.sdk.data.domain.odm.DocumentResult;
import io.stargate.sdk.data.domain.query.Filter;
import io.stargate.sdk.data.domain.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ShopAssistantTools {

    @Autowired
    private AstraService astraService;
    private AstraDB astraDB;

    public ShopAssistantTools(AstraService astraService) {
        this.astraService = astraService;
        this.astraDB = astraService.getAstraDB();
    }
    @Tool("Get the total amount of products available in the web shop.")
    public int getProductCount() {
        System.out.println("Agent called: getProductCount");
        AstraDBCollection collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        return collection.countDocuments();
    }

    @Tool("Get product information by product id. Return empty Product in case id not found.")
    public Product getProductById(String pid) {
        System.out.println("Agent called: getProductById. ID: " + pid);
        AstraDBCollection collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        // Retrieve a products from its id
        //Optional<DocumentResult<Product>> result = collection.findById(id, Product.class);
        Filter filter = new Filter()
                .where("pid")
                .isEqualsTo(pid);

        Optional<DocumentResult<Product>> result = collection.findOne (
                SelectQuery.builder().filter(filter)
                        .build(), Product.class);
        if ( result.isPresent() ) {
            return result.get().getData();
        }
        return new Product();
    }

    @Tool("Search for products in a shop. Searching by product description")
    public List<Product> searchProducts(String description) {
        System.out.println("Agent called: searchProducts");

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        Embedding descEmbedding = embeddingModel.embed(description).content();

        List<Product> products = new ArrayList<>();
        AstraDBCollection collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        // Order the results by similarity
        List<DocumentResult<Product>> result = collection.find (
                SelectQuery.builder()
                        .orderByAnn(descEmbedding.vector())
                        .includeSimilarity()
                        .withLimit(Integer.valueOf(5))
                        .build(), Product.class).toList();
        // Return list of products
        result.forEach(r -> products.add(r.getData()));
        return products;
    }
    /*
    @Tool("Search for products in a shop. Searching by vector embedding")
    public List<Product> searchProductsByEmbedding(float[] embedding) {
        System.out.println("Agent called: searchProductsByEmbedding");

        List<Product> products = new ArrayList<>();
        AstraDBCollection collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        // Order the results by similarity
        List<Result<Product>> result = collection.find (
                SelectQuery.builder()
                        .orderByAnn(embedding)
                        .includeSimilarity()
                        .withLimit(Integer.valueOf(5))
                        .build(), Product.class).toList();
        // Return list of products
        result.forEach(r -> products.add(r.getData()));
        return products;
    }

     */
}
