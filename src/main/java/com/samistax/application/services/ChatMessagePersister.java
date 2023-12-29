package com.samistax.application.services;


import com.samistax.application.data.astra.ChatMsg;
import com.vaadin.collaborationengine.CollaborationMessage;
import com.vaadin.collaborationengine.CollaborationMessagePersister;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SpringComponent
public class ChatMessagePersister implements CollaborationMessagePersister {

    private final ChatMsgService chatService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ChatMessagePersister(ChatMsgService chatService) {
        this.chatService = chatService;
    }

    @Override
    public Stream<CollaborationMessage> fetchMessages(FetchQuery query) {
        ArrayList<CollaborationMessage> messages = new ArrayList<>();
        if ( this.chatService != null ) {
            List<ChatMsg> chatMsgs = chatService.findAllMessagesSince(query.getTopicId(), query.getSince());
            if ( chatMsgs != null ) {
                chatMsgs.stream().forEach(e -> {
                    messages.add(new CollaborationMessage(
                            new UserInfo(e.getUserId(),e.getUserName(),e.getUserImage()),
                            e.getText(),
                            e.getTime()));
                });
            }
        }
        return messages.stream();
    }

    @Override
    public void persistMessage(PersistRequest request) {

        if( chatService != null ) {

            CollaborationMessage message = request.getMessage();
            String topicId = request.getTopicId();
            UserInfo user = message.getUser();

            // Persist user message into Astra DB
            ChatMsg msg = new ChatMsg(topicId, message.getTime());
            msg.setText(message.getText());
            msg.setUserId(message.getUser().getId());
            msg.setUserName(message.getUser().getName());
            msg.setUserImage(message.getUser().getImage());
            msg.setUserAbbreviation(message.getUser().getAbbreviation());
            msg.setUserColorIndex(message.getUser().getColorIndex());

            chatService.saveMessage(msg);
        }
    }
}


