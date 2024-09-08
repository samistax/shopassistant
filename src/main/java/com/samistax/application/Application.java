package com.samistax.application;

import com.dtsx.astra.sdk.AstraDBCollection;
import com.fasterxml.jackson.databind.ObjectReader;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.samistax.application.data.SamplePersonRepository;
import com.samistax.application.data.astra.json.Product;
import com.samistax.application.services.AstraService;
import com.samistax.application.views.products.Cart;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import javax.sql.DataSource;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.stargate.sdk.data.domain.SimilarityMetric;
import io.stargate.sdk.data.domain.odm.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.context.annotation.Bean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@NpmPackage(value = "@fontsource/walter-turncoat", version = "4.5.0")
@Theme(value = "my-assistant", variant = Lumo.DARK)

public class Application implements AppShellConfigurator {

    private static Cart cart = new Cart();
    public final static String ASTRA_PRODUCT_TABLE = "products";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    SqlDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource,
            SqlInitializationProperties properties, SamplePersonRepository repository) {
        // This bean ensures the database is only initialized when empty
        return new SqlDataSourceScriptDatabaseInitializer(dataSource, properties) {
            @Override
            public boolean initializeDatabase() {
                if (repository.count() == 0L) {
                    return super.initializeDatabase();
                }
                return false;
            }
        };
    }

    @Bean
    public Cart getCart() {
        return cart;
    }

    @Bean
    public String loadSampleProductData(AstraService astraService, EmbeddingStore astraDbEmbeddingStore, EmbeddingModel embeddingModel) {

        // Get handle to Astra Vector store
        AstraDBCollection collection = astraService.createCollection(ASTRA_PRODUCT_TABLE, 384, SimilarityMetric.cosine);

        if (collection != null ) {
            // In case policy file not embedded yet then vectorize and persist the content.
            if (collection.countDocuments() == 0 ) {
                try {
                    String resourcePath = "META-INF/resources/footwear_sampledata.csv";
                    // Open the resource file using the class loader
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

                    // Check if the resource file was found
                    if (inputStream != null) {
                        // Read the contents of the resource file
                        // You can use BufferedReader or any other class depending on your requirements
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                        String line;
                        int id = 0;

                        CsvMapper mapper=new CsvMapper();
                        CsvSchema schema = mapper.schemaFor(Product.class).withColumnSeparator(';');
                        ObjectReader r=mapper.readerFor(Product.class).with(schema);

                        // Read header
                        reader.readLine();

                        List<TextSegment> segments = new ArrayList<>();
                        List<Embedding> embeddings = new ArrayList<>();
                        List<Document<Product>> docs = new ArrayList<>();

                        while ((line = reader.readLine()) != null ) {
                            id++;
                            Product p = r.readValue(line);

                            if ( p.getDescription().trim().length() > 0 ) {
                                Map<String, String> metadata = Map.of(
                                        "pid", p.getPid(),
                                        "name", p.getName(),
                                        "color", p.getColor(),
                                        "description", p.getDescription(),
                                        "category", p.getCategory(),
                                        "brand", p.getBrand(),
                                        "wholesale_price", p.getWholesale_price(),
                                        "image_url", p.getImage_url()
                                );

                                TextSegment s = TextSegment.from(p.getDescription(), new Metadata(metadata));
                                System.out.println("segment: " + s);
                                segments.add(s);

                                Embedding textEmbedding = embeddingModel.embed(s).content();
                                embeddings.add(textEmbedding);

                                //astraDbEmbeddingStore.add(textEmbedding,s);
/*
                                // Store embedding to vector store, individually
                                docs.add(new Document(p.getId(),p, textEmbedding.vector()));

                                // Flush docs at 20 item interval to operate within Astra Json API guardrail
                                if ( docs.size() == 20 ) {

                                    //List<DocumentMutationResult<Product>> results = collection.insertMany(docs);
                                    collection.insertManyASync(docs).thenAccept(resultsAsynch -> {
                                        if (resultsAsynch !=null) {
                                            resultsAsynch.forEach(res -> {
                                                if (  res.getStatus() == DocumentMutationStatus.CREATED) {
                                                    System.out.println(p.getId() + "stored in vector db");
                                                } else {
                                                    System.out.println(p.getId() + "vector store failed. " + res.getStatus().toString());
                                                }
                                            });
                                        }
                                    });
                                    docs.clear();
                                    System.out.println("Embedded 20 items:  #" + id );
                                }
 */
                            }
                        }
                        // Close the reader
                        reader.close();

                        // Store remaining documents
                        System.out.println("Embedded " + docs.size() + " Count:  #" + id );
                       // collection.insertMany(docs);

                        // Store embedding to vector store, using collections
                        astraDbEmbeddingStore.addAll(embeddings, segments);
                    } else {
                        // Resource file not found
                        System.out.println("Resource file not found: " + resourcePath);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return "loaded";
        }

        return "loading failed";
    }
}
