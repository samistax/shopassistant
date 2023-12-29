package com.samistax.application.services;

import com.datastax.astra.sdk.AstraClient;
import com.datastax.astra.sdk.config.AstraClientConfig;
import com.dtsx.astra.sdk.AstraDB;
import com.dtsx.astra.sdk.AstraDBCollection;
import io.stargate.sdk.json.domain.CollectionDefinition;
import io.stargate.sdk.json.domain.SimilarityMetric;
import io.stargate.sdk.json.exception.CollectionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class AstraService {
    // DataStax Java SDK for Astra (for CQL use case)
    private AstraClient astraClient;

    // DataStax JSON API Client
    @Value( "${astra.api.endpoint}" )
    private String ASTRA_API_ENDPOINT;
    @Value( "${astra.api.application-token}" )
    private String ASTRA_TOKEN;
    private AstraDB astraDB;


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AstraService(AstraClient astraClient) {
        // Initialize the CQL client
        this.astraClient = astraClient;
    }
    public AstraDB getAstraDB() {
        return this.astraDB;
    }
    public AstraDBCollection getCollection(String id){
        return astraDB.collection(id);
    }

    public AstraDBCollection createCollection(String id, int dimension, SimilarityMetric metric){
        AstraDBCollection col = null;
        try {
            col = astraDB.collection(id);
        } catch (CollectionNotFoundException cnfe ) {
            // Collection did not exist, create one and return it to client
            CollectionDefinition colDefinition = CollectionDefinition.builder()
                    .name(id)
                    .vector(dimension, metric)
                    .build();
            col = astraDB.createCollection(colDefinition);

        }
        return col;
    }

    @PostConstruct
    public void init() {
        if ( this.astraClient == null ){
            // TODO: Report a bug. Needed to manually rename downloaded SCB. eu-west-1 to eu-west1
            AstraClientConfig conf = AstraClient.builder().withToken(ASTRA_TOKEN);
            //this.astraClient = new AstraClient(conf);
            this.astraClient = new AstraClient(ASTRA_TOKEN);
        }

        // Initialize the JSON client
        astraDB = new AstraDB(ASTRA_TOKEN, ASTRA_API_ENDPOINT);

    }

}
