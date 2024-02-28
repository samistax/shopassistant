package com.samistax.application.data.astra;

import com.vaadin.collaborationengine.CollaborationMessage;
import com.vaadin.collaborationengine.CollaborationMessagePersister;
//import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

public class ChatMsg {

    private String topic;
    private Instant time;
    // CollaborationEngine Massage params
    private String text;
    // De normalizing UserInfo fiedls
    // Collaboration Engine User Info  parameter
    private String userId;
    private String userName;
    private String userAbbreviation;
    private String userImage;
    private int userColorIndex;

    public ChatMsg() {}

    public ChatMsg(String topic, Instant time) {
        this.topic = topic;
        this.time = time;
    }

    public ChatMsg(CollaborationMessagePersister.PersistRequest request) {
        if ( request != null && request.getMessage() != null ) {
            CollaborationMessage message = request.getMessage();
            // Initialize Key

            setTopic(request.getTopicId());
            setTime(message.getTime());
            // Initialize other properties

            // CollaborationEngine Massage params
            this.text = message.getText();
            this.userId = message.getUser().getId();
            this.userName = message.getUser().getName();
            this.userAbbreviation = message.getUser().getAbbreviation();
            this.userImage = message.getUser().getImage();
            this.userColorIndex = message.getUser().getColorIndex();
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAbbreviation() {
        return userAbbreviation;
    }

    public void setUserAbbreviation(String userAbbreviation) {
        this.userAbbreviation = userAbbreviation;
    }

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public int getUserColorIndex() {
        return userColorIndex;
    }

    public void setUserColorIndex(int userColorIndex) {
        this.userColorIndex = userColorIndex;
    }
}
