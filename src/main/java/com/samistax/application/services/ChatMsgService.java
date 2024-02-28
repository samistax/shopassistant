package com.samistax.application.services;

import com.dtsx.astra.sdk.AstraDB;
import com.dtsx.astra.sdk.AstraDBCollection;
import com.samistax.application.data.astra.ChatMsg;
import io.stargate.sdk.data.domain.JsonDocument;

import io.stargate.sdk.data.domain.JsonDocumentMutationResult;
import io.stargate.sdk.data.domain.JsonDocumentResult;
import io.stargate.sdk.data.domain.query.Filter;
import io.stargate.sdk.data.domain.query.SelectQuery;
import io.stargate.sdk.data.domain.query.DeleteQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatMsgService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value( "${astra.api.endpoint}" )
    private String ASTRA_API_ENDPOINT;
    @Value( "${astra.api.application-token}" )
    private String ASTRA_TOKEN;

    private AstraDB astraDB;
    private AstraDBCollection collection;

    public static final String COLLECTION_NAME = "chat_msgs";

    public ChatMsgService() {}

    @PostConstruct
    public void init(){
        // Initialize the client
        astraDB = new AstraDB(ASTRA_TOKEN, ASTRA_API_ENDPOINT);
        // Create a collection
        collection = astraDB.createCollection(COLLECTION_NAME);
    }

    private void printRequestDuration(String methodName, long startTime) {
        logger.info(methodName+" request (ms): " + (System.currentTimeMillis() - startTime));
    }

    public int countAllMessages() {
        return collection.countDocuments().intValue();
    }

    private ChatMsg jsonResultToChatMsg(JsonDocumentResult r) {
        ChatMsg msg = new ChatMsg();
        if ( r != null ) {
            Map<String, Object> data = r.getData();
            msg.setTopic("" + data.get("topic"));
            msg.setTime(Instant.parse(data.get("time").toString()));

            msg.setUserId("" + data.get("userId"));
            msg.setUserName("" + data.get("userName"));
            msg.setUserImage("" + data.get("userImage"));
            if (data.get("userColorIndex") != null) {
                msg.setUserColorIndex((int)(data.get("userColorIndex")));
            }
            if (data.get("userAbbreviation") != null) {
                msg.setUserAbbreviation(data.get("userAbbreviation").toString());
            }
            msg.setText("" + data.get("text"));
        }
        return msg;
    }
    public List<ChatMsg> findAllMessages() {

        List<ChatMsg> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        List<JsonDocumentResult> results = collection.findAll().collect(Collectors.toList());
        printRequestDuration("findAll: ",startTime);
        results.forEach(r -> messages.add(jsonResultToChatMsg(r)));
        return messages;
    }

    public List<ChatMsg> findAllMessages(String topic) {
        logger.info("findAllMessages. Topic: " + topic );
        List<ChatMsg> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        Filter filter = new Filter()
                .where("topic")
                .isEqualsTo(topic);


        List<JsonDocumentResult> results = collection.find(
                SelectQuery.builder().filter(filter).build()
        ).toList();

        /*List<JsonDocumentResult> results = collection.find(SelectQuery.builder()
                .where("topic")
                .isEqualsTo(topic)
                .build()).toList();
         */
        printRequestDuration("findAllMessages("+topic+"): ",startTime);
        results.forEach(r -> messages.add(jsonResultToChatMsg(r)));
        return messages;
    }

    public List<ChatMsg> findAllMessagesSince(String topic, Instant timestamp) {


        logger.info("findAllMessagesSince. Timestamp: " + timestamp + " topic: " + topic);
        List<ChatMsg> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // For now retrieve all messages and sort in client
        messages = findAllMessages(topic).stream().filter(m-> (!m.getTime().isBefore(timestamp)) ).toList();

        // TODO: Revert to this when .and() method is supported
        /*List<JsonDocumentResult> results = collection.find(SelectQuery.builder()
                .where("topic")
                .isEqualsTo(topic).and()
                .where("time").isGreaterOrEqualsThan(timestamp)
                .build()).toList();



        // TODO: Revert to this when .and() operator is supported with JsonFilter
        String jsonFilter = "{\n" +
        "   \"$and\":[\n" +
                "       {\"$eq\":{\"topic\":\""+topic+"\"}},\n" +
                "       {\"$gt\":[{\"time\":\""+timestamp.toString()+"\"}]} ]}}";

        System.out.println(jsonFilter);
        List<JsonDocumentResult> results = collection.find(SelectQuery.builder().withJsonFilter(jsonFilter).build()).toList();
        results.forEach(r -> messages.add(jsonResultToChatMsg(r)));

*/

        printRequestDuration("findAllMessagesSince("+topic+"): ", startTime);

        logger.debug("findAllMessagesSince returned  " +messages.size() + " messages!!!" );
        return messages;
    }

    // Start of Producer methods
    public JsonDocument saveMessage(ChatMsg msg) {
        //String result = "";
        JsonDocumentMutationResult result = null;
        long startTime = System.currentTimeMillis();
        if ( collection != null ) {

            //result = collection.insertOne(new Document<ChatMsg>().data(msg));

            result = collection.insertOne(
                new JsonDocument()
                        //.id("doc1") // generated if not set
                        .put("topic", msg.getTopic())
                        .put("time", msg.getTime().toString())
                        .put("userId", msg.getUserId())
                        .put("userName", msg.getUserName())
                        .put("text", msg.getText())
                        .put("userAbbreviation", msg.getUserAbbreviation())
                        .put("userColorIndex", msg.getUserColorIndex())
                        .put("userImage", msg.getUserImage())
                        .put("text", msg.getText())
                        .put("text", msg.getText())
            );
            printRequestDuration("saveTopicMessage: ",startTime);
            return result.asJsonDocumentMutationResult().getDocument();
        } else {
            return null;
        }
    }

    public int deleteAllByTopic(String topic) {
        if ( collection != null ) {
            // Delete rows based on a query
            Filter filter = new Filter()
                            .where("topic")
                            .isEqualsTo(topic);

            return collection.deleteMany(
                    DeleteQuery.builder()
                            .filter(filter)
                            .build()).getDeletedCount();
        }
        return 0;
    }


}
