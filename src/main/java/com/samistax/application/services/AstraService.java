package com.samistax.application.services;

import com.datastax.astra.sdk.AstraClient;
import com.dtsx.astra.sdk.AstraDB;
import com.dtsx.astra.sdk.AstraDBCollection;
import io.stargate.sdk.data.domain.SimilarityMetric;
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
    public AstraClient getAstraClient() {
        return this.astraClient;
    }

    public AstraDBCollection getCollection(String name){
        return astraDB.getCollection(name);
    }
    public AstraDBCollection createCollection(String name, int dimension, SimilarityMetric metric){
        // Create new collection ( retrieving the collection if already exists)
        return astraDB.createCollection(name, dimension, metric);
    }

    @PostConstruct
    public void init() {

        // Initialize the JSON client
        astraDB = new AstraDB(ASTRA_TOKEN, ASTRA_API_ENDPOINT);
    }

}
