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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import javax.sql.DataSource;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.cassandra.AstraDbEmbeddingConfiguration;
import dev.langchain4j.store.embedding.cassandra.AstraDbEmbeddingStore;
import io.stargate.sdk.json.domain.JsonDocument;
import io.stargate.sdk.json.domain.SimilarityMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@Push
public class Application implements AppShellConfigurator {

    @Autowired
    private AstraService astraService;

    // DataStax JSON API Client
    @Value( "${astra.api.endpoint}" )
    private String ASTRA_API_ENDPOINT;
    @Value( "${astra.api.application-token}" )
    private String ASTRA_TOKEN;
    @Value( "${astra.api.database-id}" )
    private String ASTRA_DB_ID;
    @Value( "${astra.api.database-region}" )
    private String ASTRA_DB_REGION;
    //@Value( "${spring.cassandra.keyspace-name}" )
    private String ASTRA_DB_KEYSPACE = "default_keyspace";


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
    public String loadSampleProductData() {

        // Get handle to Astra Vector store
        AstraDBCollection collection = astraService.createCollection(ASTRA_PRODUCT_TABLE, 384, SimilarityMetric.cosine);

        // Setup Embedding engine
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // Create the Store with the builder
        AstraDbEmbeddingStore astraDbEmbeddingStore = new AstraDbEmbeddingStore(AstraDbEmbeddingConfiguration
                .builder()
                .token(ASTRA_TOKEN)
                .databaseId(ASTRA_DB_ID)
                .databaseRegion(ASTRA_DB_REGION)
                .keyspace(ASTRA_DB_KEYSPACE)
                .table(ASTRA_PRODUCT_TABLE)
                .dimension(384) // Used with MiniLM-L6-v2 model
                .build());

        if ( collection != null) {
            // In case policy file not embedded yet then vectorize and persist the content.
            if ( collection.countDocuments() == 0 ) {
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
                        int id = 1;

                        CsvMapper mapper=new CsvMapper();
                        CsvSchema schema = mapper.schemaFor(Product.class).withColumnSeparator(';');
                        ObjectReader r=mapper.readerFor(Product.class).with(schema);


                        // Read header
                        reader.readLine();

                        // TODO: Check why the Langchain wrapper throws exception due to missing row_id column in product
                        List<TextSegment> segments = new ArrayList<>();
                        List<Embedding> embeddings = new ArrayList<>();

                        List<JsonDocument> docs = new ArrayList<>();


                        while ((line = reader.readLine()) != null) {

                            Product p = r.readValue(line);
                            if ( p.getDescription().trim().length() > 0 ) {
                                Map<String, String> metadata = Map.of(
                                        "id", p.getId(),
                                        "name", p.getName()
                                );
                                TextSegment s = TextSegment.from(p.getDescription(), new Metadata(metadata));
                                System.out.println("segment: " + s);
                                segments.add(s);

                                Embedding textEmbedding = embeddingModel.embed(s).content();
                                embeddings.add(textEmbedding);

                                // Store embedding to vector store, individually
                                //astraDbEmbeddingStore.add(textEmbedding,s);
                                JsonDocument doc = new JsonDocument().data(p)
                                        .vector(textEmbedding.vector());

                                //doc.setId(""+id);

                                String result = collection.insertOne(doc);
                                System.out.println("Result: " + result);

                                id++;
                            }

                            System.out.println("Embedded items: " + id );
                        }
                        // Close the reader
                        reader.close();

                        System.out.println("embeddings size : " + embeddings.size());
                        System.out.println("segments size : " + segments.size());
                        // Store embedding to vector store, using collections
                        //astraDbEmbeddingStore.addAll(embeddings, segments);
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
