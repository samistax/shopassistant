package com.samistax.application.views.assistant;

import com.samistax.application.ai.ShopAssistantAgent;
import com.samistax.application.data.astra.ChatMsg;
import com.samistax.application.services.ChatMessagePersister;
import com.samistax.application.services.ChatMsgService;
import com.samistax.application.views.MainLayout;
import com.samistax.application.views.feed.Person;
import com.vaadin.collaborationengine.*;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ScrollOptions;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.Tabs.Orientation;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxSizing;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Flex;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import dev.langchain4j.service.TokenStream;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Assistant")
@Route(value = "assistant", layout = MainLayout.class)
public class AssistantView extends HorizontalLayout {

    public static class ChatTab extends Tab {
        private final ChatInfo chatInfo;

        public ChatTab(ChatInfo chatInfo) {
            this.chatInfo = chatInfo;
        }

        public ChatInfo getChatInfo() {
            return chatInfo;
        }
    }

    public static class ChatInfo {
        private String name;
        private int unread;
        private Span unreadBadge;
        private MessageManager mm;

        private ChatInfo(String name, int unread) {
            this.name = name;
            this.unread = unread;
        }

        public void resetUnread() {
            unread = 0;
            updateBadge();
        }

        public void incrementUnread() {
            unread++;
            updateBadge();
        }

        private void updateBadge() {
            unreadBadge.setText(unread + "");
            unreadBadge.setVisible(unread != 0);
        }

        public void setUnreadBadge(Span unreadBadge) {
            this.unreadBadge = unreadBadge;
            updateBadge();
        }

        public MessageManager getMm() {
            return mm;
        }

        public void setMm(MessageManager mm) {
            this.mm = mm;
        }

        public String getCollaborationTopic() {
            return "chat/" + name;
        }
    }

    private ChatInfo[] chats = new ChatInfo[]{new ChatInfo("general", 0), new ChatInfo("support", 0),
            new ChatInfo("casual", 0)};
    private ChatInfo currentChat = chats[0];
    private Tabs tabs;
    private CollaborationMessageList messageList;
    //private ChatService chatService;
    private ShopAssistantAgent assistant;
    private ChatMsgService chatMsgService;
    private MessageManager messageManager;
    private UserInfo AI_USER = new UserInfo("0", "AI Assistant");

    private boolean callCompletion = false;

    public AssistantView(ShopAssistantAgent assistant, ChatMsgService chatMsgService, ChatMessagePersister messagePersister ) {
        addClassNames("assistant-view", Width.FULL, Display.FLEX, Flex.AUTO);
        setSpacing(false);

        this.assistant = assistant;
        this.chatMsgService = chatMsgService;

        // UserInfo is used by Collaboration Engine and is used to share details
        // of users to each other to able collaboration. Replace this with
        // information about the actual user that is logged, providing a user
        // identifier, and the user's real name. You can also provide the users
        // avatar by passing an url to the image as a third parameter, or by
        // configuring an `ImageProvider` to `avatarGroup`.

        UserInfo userInfo = AI_USER;
        userInfo.setColorIndex(-1);
        // Create user info from currently selected user
        Person userListPerson = VaadinSession.getCurrent().getAttribute(Person.class);
        if ( userListPerson != null ) {
            userInfo = new UserInfo(userListPerson.getId().toString(), userListPerson.getName());
            userInfo.setColorIndex(-1); //value -1, indicates that the user color can be automatically assigned by Collaboration Engine.
            userInfo.setImage(userListPerson.getImage());
        }
        final String currentUserId = userInfo.getId();
        tabs = new Tabs();

        for (ChatInfo chatInfo : chats) {
            // Listen for new messages in each chat so we can update the
            // "unread" count
            MessageManager mm = new MessageManager(this, userInfo, chatInfo.getCollaborationTopic(), messagePersister);
            mm.setMessageHandler(context -> {
                if (currentChat != chatInfo) {
                    chatInfo.incrementUnread();
                }
                // Call chat completion for new user messages from this view.
                if ( callCompletion ) {
                    Thread workerThread = new Thread(() -> {
                        this.getUI().ifPresent(ui -> ui.access(() -> {
                            ui.accessSynchronously(() -> callAsynchChatCompletion(messagePersister, currentChat.getCollaborationTopic(), currentUserId, context.getMessage().getText()));
                        }));
                    });
                    workerThread.start();
                }
            });
            chatInfo.setMm(mm);
            tabs.add(createTab(chatInfo));
        }

        tabs.setOrientation(Orientation.VERTICAL);
        tabs.addClassNames(Flex.GROW, Flex.SHRINK, Overflow.HIDDEN);

        // CollaborationMessageList displays messages that are in a
        // Collaboration Engine topic. You should give in the user details of
        // the current user using the component, and a topic Id. Topic id can be
        // any freeform string. In this template, we have used the format
        // "chat/#general".
        messageList = new CollaborationMessageList(
                userInfo,
                currentChat.getCollaborationTopic(),
                messagePersister);

        // Change default scrollign alignment to scroll to the end of view
        ScrollOptions options = new ScrollOptions();
        options.setBlock(ScrollOptions.Alignment.END);
        options.setBehavior(ScrollOptions.Behavior.AUTO);
        messageList.scrollIntoView(options);
        messageList.setSizeFull();

        messageList.setMessageConfigurator((message, user) -> {
            if (user.getId().equals("0")) {
                message.addThemeNames("ai-user");
            } else if (user.getId().equals(currentUserId)) {
                message.addThemeNames("current-user");
            } else {
                message.addThemeNames("other-user");
            }
        });

        // `CollaborationMessageInput is a textfield and button, to be able to
        // submit new messages. To avoid having to set the same info into both
        // the message list and message input, the input takes in the list as an
        // constructor argument to get the information from there.
        CollaborationMessageInput input = new CollaborationMessageInput(messageList);
        input.getContent().addSubmitListener((submitEvent -> {
            String message = submitEvent.getValue();

            // Handle the submitted message
            // Add your custom logic here
            callCompletion = true;
        }));
        input.setWidthFull();

        // Layouting

        VerticalLayout chatContainer = new VerticalLayout();
        chatContainer.addClassNames(Flex.AUTO, Overflow.HIDDEN);

        Aside side = new Aside();
        side.addClassNames(Display.FLEX, FlexDirection.COLUMN, Flex.GROW_NONE, Flex.SHRINK_NONE, Background.CONTRAST_5);
        side.setWidth("18rem");
        Header header = new Header();
        header.addClassNames(Display.FLEX, FlexDirection.ROW, Width.FULL, AlignItems.CENTER, Padding.MEDIUM,
                BoxSizing.BORDER);
        H3 channels = new H3("Channels");
        channels.addClassNames(Flex.GROW, Margin.NONE);
        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(userInfo, "chat");
        avatarGroup.setMaxItemsVisible(4);
        avatarGroup.addClassNames(Width.AUTO);

        header.add(channels, avatarGroup);

        tabs.setSizeFull();
        Button clearChatBtn = new Button("Clear Chat History");
        clearChatBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        clearChatBtn.addClickListener(e-> {
            // Remove persisted messages
            chatMsgService.deleteAllByTopic(currentChat.getCollaborationTopic());
            messageList.getContent().setItems(new ArrayList<MessageListItem>());
            messageManager.close();

            // Open a connection to the desired topic
            TopicConnectionRegistration reg = CollaborationEngine.getInstance().openTopicConnection(this, currentChat.getCollaborationTopic(), messageManager.getLocalUser(), connection -> {
                try {
                     CollaborationList list = connection.getNamedList("com.vaadin.collaborationengine.MessageManager");
                     list.getKeys().forEach(key -> {
                             System.out.println("Key: " + key);
                             //list.remove(key);
                             ListOperationResult<Boolean> result = list.apply(ListOperation.delete(key));
                     });
                     List<CollaborationMessage> messages = list.getItems(CollaborationMessage.class);
                   System.out.println("messages: " + messages);
                    CollaborationList test = connection.getNamedList("com.vaadin.collaborationengine.CollaborationMessage");
                    messages = test.getItems(CollaborationMessage.class);
                    System.out.println("messages: " + messages);
                } catch ( Exception ex) {
                    System.out.println("Exception: " + ex);
                }
                return null;
            });
            // Remove topic registration
            reg.remove();

            // Start with new manager with cleared topic message history
            messageManager = new MessageManager(this, messageManager.getLocalUser(), currentChat.getCollaborationTopic(), messagePersister);

        });

        side.add(header, tabs, clearChatBtn);

        chatContainer.add(messageList, input);
        add(chatContainer, side);
        setSizeFull();
        expand(messageList);

        // Select first tab as default
        messageManager = ((ChatTab) tabs.getTabAt(0)).getChatInfo().getMm();

        // Change the topic id of the chat when a new tab is selected
        tabs.addSelectedChangeListener(event -> {
            currentChat = ((ChatTab) event.getSelectedTab()).getChatInfo();
            currentChat.resetUnread();
            messageList.setTopic(currentChat.getCollaborationTopic());
            messageManager = currentChat.getMm();
        });

    }

    private ChatTab createTab(ChatInfo chat) {
        ChatTab tab = new ChatTab(chat);
        tab.addClassNames(JustifyContent.BETWEEN);

        Span badge = new Span();
        chat.setUnreadBadge(badge);
        badge.getElement().getThemeList().add("badge small contrast");
        tab.add(new Span("#" + chat.name), badge);
        return tab;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Page page = attachEvent.getUI().getPage();
        page.retrieveExtendedClientDetails(details -> {
            setMobile(details.getWindowInnerWidth() < 740);
        });
        page.addBrowserWindowResizeListener(e -> {
            setMobile(e.getWidth() < 740);
        });
    }

    private void setMobile(boolean mobile) {
        tabs.setOrientation(mobile ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }
    private void callAsynchChatCompletion(ChatMessagePersister persister , String topicId, String userId, String msg) {

        // Use message list in UI component instead for better performance.
        CollaborationMessage botMsg = new CollaborationMessage(AI_USER, "", Instant.now());
        UI ui = UI.getCurrent();
        ui.access(() -> {
            List<MessageListItem> itemsWithResponse = new ArrayList<MessageListItem>();
            itemsWithResponse.addAll(messageList.getContent().getItems());
            // Add placeholder list item for writing response tokens
            MessageListItem chatResponse = new MessageListItem(botMsg.getText(), botMsg.getTime(), botMsg.getUser().getName());
            chatResponse.addThemeNames("ai-user");
            itemsWithResponse.add(chatResponse);
            messageList.getContent().setItems(itemsWithResponse);
        });

        //TokenStream tokenStream = chatService.chatTokenStream(userId, msg);
        TokenStream tokenStream = assistant.chat(userId, msg);

        // UI ui = UI.getCurrent();
        tokenStream.onNext(chunk -> {
            botMsg.setText(botMsg.getText() + chunk);

            List<MessageListItem> refreshedItems = new ArrayList<MessageListItem>();
            refreshedItems.addAll(messageList.getContent().getItems());
            int lastItemIndex = refreshedItems.size()-1;
            MessageListItem updatedItem = new MessageListItem(botMsg.getText(),
                    botMsg.getTime(), botMsg.getUser().getName());
            updatedItem.addThemeNames("ai-user");
            refreshedItems.set(lastItemIndex, updatedItem);

            ui.access(() -> {
                messageList.getContent().setItems(refreshedItems);
                messageList.setSizeFull();

                // Access the underlying vaadin-message-list element
                ScrollOptions options = new ScrollOptions();
                options.setBlock(ScrollOptions.Alignment.END);
                options.setBehavior(ScrollOptions.Behavior.AUTO);
                messageList.scrollIntoView(options);
            });

        })
        .onComplete( c -> {
            // Upsert message once completed
            System.out.println("User : " + userId);
            System.out.println("BotText: " + botMsg.getText());
            System.out.println("c.content(): " + c.content().text());
            botMsg.setText(c.content().text());
            // Message complete submit to message list and persist
            //botMessageManager.submit(botMsg);
            messageManager.submit(botMsg);
            //persistChatResponse(topicId, botMsg);
            callCompletion = false;
        })
        .onError(Throwable::printStackTrace)
        .start();

    }
    private void persistChatResponse(String topicId, CollaborationMessage message) {

        // Persist user message into Astra DB Collection
        ChatMsg msg = new ChatMsg();
        msg.setTopic(topicId);
        msg.setTime(message.getTime());
        msg.setText(message.getText());
        msg.setUserId(message.getUser().getId());
        msg.setUserName(message.getUser().getName());
        msg.setUserImage(message.getUser().getImage());
        msg.setUserAbbreviation(message.getUser().getAbbreviation());
        msg.setUserColorIndex(message.getUser().getColorIndex());
        if( chatMsgService != null ) {
            chatMsgService.saveMessage(msg);
        }
    }
}
