package com.samistax.application.views.products;

import com.dtsx.astra.sdk.AstraDBCollection;
import com.samistax.application.Application;
import com.samistax.application.components.ColorPicker;
import com.samistax.application.data.astra.json.Product;
import com.samistax.application.services.AstraService;
import com.samistax.application.services.ChatService;
import com.samistax.application.views.MainLayout;
import com.samistax.application.views.checkoutform.CheckoutFormView;
import com.samistax.application.views.feed.Person;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.ThemeVariant;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import com.vaadin.flow.theme.lumo.LumoUtility.ListStyleType;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.MaxWidth;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.BertTokenizer;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.TokenStream;
import io.stargate.sdk.core.domain.Page;
import io.stargate.sdk.json.domain.JsonDocument;
import io.stargate.sdk.json.domain.SelectQuery;
import io.stargate.sdk.json.domain.odm.Result;

import java.net.URL;
import java.util.*;

@PageTitle("Shop")
@Route(value = "shop", layout = MainLayout.class)
public class ProductsView extends SplitLayout implements HasComponents, HasStyle {

    private OrderedList imageContainer;
    private AstraService astraService;
    private ChatService chatService;

    private SelectQuery productQuery;

    private List<String> pageStates = new ArrayList<>();
    private AstraDBCollection collection;

    private int selectedPage = 1;
    private Span currentPageTitle;
    private Product selectedProduct;
    private String selectedModel = "AllMiniLmL6V2";
    private List<Product> similarProducts = new ArrayList<>();

    private TextField promptUser = new TextField("Ask about the product");
    private Span promptResponse = new Span();
    private boolean chatCompletionInProgress = false;
    public ProductsView(AstraService astraService,ChatService chatService) {

        this.astraService = astraService;
        this.chatService = chatService;

        this.collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        this.productQuery = SelectQuery.builder()
                .where("id").exists()
                .includeSimilarity()
                .build();

        // Create UI with personalised welcome text
        this.addToPrimary(constructPrimaryUI());
        // Create UI for Assistant
        this.addToSecondary(constructSecondaryUI());
        this.getSecondaryComponent().setVisible(false);

        // You can map the output as Result<T> using either a Java pojo or mapper
        Page<Result<Product>> page = collection.findPage(productQuery, Product.class);
        if (! page.isEmpty()) {
            // Store page state for retrieving next page.
            page.getPageState().ifPresent(pageState -> pageStates.add(pageState));
            addProductsToContainer(page);
        }
    }
    private FlexLayout createPaginationPanel() {
        FlexLayout layout = new FlexLayout();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Button prevPageBtn = new Button("Prev");
        prevPageBtn.setIcon(VaadinIcon.ARROW_BACKWARD.create());
        Button nextPageBtn = new Button("Next");
        nextPageBtn.setIcon(VaadinIcon.ARROW_FORWARD.create());

        prevPageBtn.setEnabled(false);
        prevPageBtn.addClickListener(e-> {
            // Remove previous state
            int lastItemIdx = pageStates.size()-1;
            pageStates.remove(lastItemIdx);
            Page<Result<Product>> prevPage = null;
            if ( (pageStates.size()-1)  > 0) {
                prevPage = collection.findPage(SelectQuery.builder()
                        .where("id").exists()
                        .includeSimilarity()
                        .withPagingState(pageStates.get(lastItemIdx-2))
                        .build(), Product.class);
            } else{
                // Returned to first page
                prevPage = collection.findPage(SelectQuery.builder()
                        .where("id").exists()
                        .includeSimilarity()
                        .build(), Product.class);
                prevPageBtn.setEnabled(false);
            }
            selectedPage = selectedPage -1;
            currentPageTitle.setText("Page " + selectedPage);
            imageContainer.removeAll();
            addProductsToContainer(prevPage);

        });

        nextPageBtn.addClickListener(e-> {

            Page<Result<Product>> nextPage = collection.findPage(SelectQuery.builder()
                    .where("id").exists()
                    .includeSimilarity()
                    .withPagingState(pageStates.get(pageStates.size()-1))
                    .build(), Product.class);

            nextPage.getPageState().ifPresent(pageState -> {
                pageStates.add(pageState);
                prevPageBtn.setEnabled(true);
            });
            // Enable previous button to return to initial view
             selectedPage = selectedPage + 1;
            currentPageTitle.setText("Page " + selectedPage);

            imageContainer.removeAll();
            addProductsToContainer(nextPage);
        });
        currentPageTitle = new Span("Page " + selectedPage);

        layout.add(prevPageBtn, currentPageTitle, nextPageBtn);
        layout.setWidthFull();
        layout.getStyle().setAlignItems(Style.AlignItems.BASELINE);
        layout.getStyle().setTextAlign(Style.TextAlign.CENTER);

        return layout;
    }

    private void addProductsToContainer(Page<Result<Product>> page){
        // Remove old items
        imageContainer.removeAll();
        similarProducts.clear();
        promptUser.clear();

        page.getResults().stream().forEach(p -> {
            Product prod = p.getData();
            ProductsViewCard card = new ProductsViewCard(prod, p.getSimilarity(), p.getVector());
            card.addAssistClickListener(e-> {
                System.out.println("Assist button clicked for product" + e.getProductId());
                similarProducts.clear();
                this.addToSecondary(constructSecondaryUI());
                this.getSecondaryComponent().setVisible(true);
                this.getSecondaryComponent().scrollIntoView();
            });
            card.addSimilarityClickListener(e-> {System.out.println("ANN search button clicked for vector" + e.getVector());});
            imageContainer.add(card);
        });
    }

    private Component constructPrimaryUI() {
        VerticalLayout layout = new VerticalLayout();

        layout.addClassNames("products-view");

        Button checkOutBtn = new Button("Checkout");
        checkOutBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        checkOutBtn.setEnabled(false);
        checkOutBtn.addClickListener(e-> UI.getCurrent().navigate(CheckoutFormView.class));

        Span badge = new Span();
        badge.getElement().getThemeList().add("badge small contrast");
        Person user = VaadinSession.getCurrent().getAttribute(Person.class);
        if ( user != null  ) {
            Cart shopCart = VaadinSession.getCurrent().getAttribute(Cart.class);
            if ( shopCart != null ) {
                int itemCount = shopCart.getCart(user.getId()).size();
                badge.setText("Items in cart: " + itemCount);
                checkOutBtn.setEnabled(true);
            } else {
                badge.setVisible(false);
                checkOutBtn.setEnabled(false);
            }
        }

        HorizontalLayout headerContainer = new HorizontalLayout();
        H2 header = new H2("Footwear shop");
        header.addClassNames(Margin.Bottom.NONE, Margin.Top.MEDIUM, FontSize.XXXLARGE);

        String prefix = "";
        Person currentUser = VaadinSession.getCurrent().getAttribute(Person.class);
        if ( currentUser != null ) {
            prefix = currentUser.getName() +", ";
        }

        Paragraph description = new Paragraph(prefix + "Welcome to our world of footwear, where style meets comfort in every step. At Footwear Shop, we believe that a great pair of shoes can transform not just your outfit, but your day. Our carefully curated collection combines the latest trends with timeless classics, ensuring there's something for every occasion and personality. ");
        description.addClassNames(Margin.Bottom.SMALL, Margin.Top.NONE, TextColor.SECONDARY);
        headerContainer.add(header,  badge);

        Select<String> sortBy = new Select<>();
        sortBy.setLabel("Sort by");
        sortBy.setItems("Newest first", "Cheapest first", "Most Expensive first");
        sortBy.setValue("Newest first");
        sortBy.getStyle().setAlignSelf(Style.AlignSelf.END);
        sortBy.addValueChangeListener(e -> {
            String value = sortBy.getValue();

            int orderByDirection = 0;
            Map<String, String> options = new HashMap<>();
            if ( value.startsWith("Cheapest") ) {
                orderByDirection = 1;
            } else if (value.startsWith("Most") ) {
                orderByDirection = -1;
            }
            if ( orderByDirection == 0  ) {
                productQuery = SelectQuery.builder()
                        .where("id").exists()
                        .includeSimilarity()
                        .build();
            } else {
                productQuery = SelectQuery.builder()
                        .where("id").exists()
                        .includeSimilarity()
                        .orderBy("wholesale_price", orderByDirection)
                        .build();
            }

            Page<Result<Product>> page = collection.findPage(productQuery, Product.class);
            if (! page.isEmpty()) {
                // Store page state for retrieving next page.
                page.getPageState().ifPresent(pageState -> {
                    // Reset page number and title
                    selectedPage = 1;
                    currentPageTitle.setText("Page " + selectedPage);
                    // Replace pageStates since order by changes next page contents
                    pageStates.clear();
                    pageStates.add(pageState);
                });
            }
            addProductsToContainer(page);
        });

        imageContainer = new OrderedList();
        imageContainer.addClassNames(Gap.MEDIUM, Display.GRID, ListStyleType.NONE, Margin.NONE, Padding.NONE);
         imageContainer.addClickListener(e -> {
            //System.out.println("ClickEvent : " + e.getClass());
             Cart shopCart = VaadinSession.getCurrent().getAttribute(Cart.class);
             if ( shopCart != null ) {
                 int itemCount = shopCart.getCart(user.getId()).size();
                 badge.setText("Items in cart: " + itemCount);
                 badge.setVisible(true);
             } else {
                 badge.setText("");
                 badge.setVisible(false);
             }
        });
        badge.getStyle().setAlignSelf(Style.AlignSelf.STRETCH);
        layout.add(headerContainer, description,  sortBy, createPaginationPanel(), imageContainer);
        return layout;
    }
    private Component constructSecondaryUI() {

        VerticalLayout layout = new VerticalLayout();


        HorizontalLayout header = new HorizontalLayout();
        Button closeBtn = new Button(VaadinIcon.ARROW_FORWARD.create());
        closeBtn.setText("Hide Shop Assistant");
        closeBtn.addClickListener(e-> layout.setVisible(false));
        Button settingsBtn = new Button("", VaadinIcon.COG.create());
        settingsBtn.addClickListener(event -> openSettingsDialog());

        header.add(closeBtn, settingsBtn);
        layout.add(header);

        selectedProduct = VaadinSession.getCurrent().getAttribute(Product.class);
        if ( selectedProduct != null ) {
            layout.addClassNames(LumoUtility.Background.CONTRAST_20, Display.FLEX, LumoUtility.FlexDirection.COLUMN, AlignItems.START, Padding.MEDIUM,
                    LumoUtility.BorderRadius.LARGE);

            Div div = new Div();
            div.addClassNames(/*LumoUtility.Background.CONTRAST, */Display.FLEX, AlignItems.CENTER, JustifyContent.CENTER,
                    Margin.Bottom.MEDIUM, LumoUtility.Overflow.HIDDEN, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Width.FULL);
            div.setHeight("160px");

            Image image = new Image();
            image.setWidth("100%");
            image.setHeight("100%");

            image.setSrc(selectedProduct.getImage_url());
            image.setAlt(selectedProduct.getName());

            div.add(image);

            Span title = new Span();
            title.addClassNames(FontSize.MEDIUM, LumoUtility.FontWeight.SEMIBOLD);
            title.setText(selectedProduct.getCategory());
            layout.add(title, div);
        }

        FlexLayout similarProductsPanel = new FlexLayout();
        similarProducts.forEach(p -> {
            Image img = new Image(p.getImage_url(), p.getId() + " image");
            img.setWidth(60, Unit.PIXELS);
            img.addClickListener(e -> {
                VaadinSession.getCurrent().setAttribute(Product.class, p);
                //selectedProduct = p;
                this.addToSecondary(constructSecondaryUI());
            });
            similarProductsPanel.add(img);
        });

        layout.add(similarProductsPanel);

        Button findBtn = new Button(VaadinIcon.SEARCH.create());
        findBtn.setText("Find Similar Products");
        findBtn.addClickListener(e-> {

            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
            Embedding descEmbedding = embeddingModel.embed(selectedProduct.getDescription()).content();
            List<Result<Product>> annProducts = findSimilarProduct(descEmbedding.vector(), 5);
            if ( annProducts.size() > 0 ) {
                // Update new similar items
                this.similarProducts.clear();
                annProducts.forEach(r -> {
                    if ( r.getSimilarity() < 1) {
                        similarProducts.add(r.getData());
                    }
                });
                this.addToSecondary(constructSecondaryUI());
            }
        });
        layout.add(findBtn);

        Button button = new Button(VaadinIcon.PAPERPLANE.create());
        HorizontalLayout inputLayout = new HorizontalLayout();
        promptUser.setWidth(80, Unit.PERCENTAGE);
        button.setWidth(20, Unit.PERCENTAGE);

        promptUser.addKeyPressListener(Key.ENTER, event -> submitPrompt());
        button.addClickListener(event -> submitPrompt());
        inputLayout.add(promptUser, button);
        inputLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        inputLayout.setWidthFull();

        promptResponse.setWidthFull();
        layout.add(inputLayout, promptResponse);

        layout.setWidth(40,Unit.PERCENTAGE);

        return layout;
    }
    public List<Result<Product>> findSimilarProduct(float[] embedding, int limit) {
        if ( limit > 20 || limit < 0 ) {
            limit = 20;
        }
        AstraDBCollection collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        // Order the results by similarity
        List<Result<Product>> result = collection.find (
                SelectQuery.builder()
                        .orderByAnn(embedding)
                        .includeSimilarity()
                        .withLimit(Integer.valueOf(limit))
                        .build(), Product.class).toList();
        return result;
    }
    private void submitPrompt(){

        UI ui = UI.getCurrent();
        if ( ! chatCompletionInProgress ) {
            chatCompletionInProgress = true;
            promptUser.setPlaceholder(promptUser.getValue());

            TokenStream tokenStream = chatService.chatShopAssistant(promptUser.getValue(), selectedProduct);
            promptResponse.setText("");

            // UI ui = UI.getCurrent();
            tokenStream.onNext(chunk -> {

                ui.access(() -> {
                    promptResponse.setText(promptResponse.getText() + chunk);

                    // Access the underlying vaadin-message-list element
                    ScrollOptions options = new ScrollOptions();
                    options.setBlock(ScrollOptions.Alignment.END);
                    options.setBehavior(ScrollOptions.Behavior.AUTO);
                    promptResponse.scrollIntoView(options);
                });

            })
            .onComplete(c -> {
                chatCompletionInProgress = false;
            })
            .onError(Throwable::printStackTrace)
            .start();

            promptUser.clear();
        }
    }
    private void openSettingsDialog() {
        // Create the dialog
        Dialog settingsDialog = new Dialog();
        settingsDialog.setModal(true); // Set the dialog as modal    // Add the content to the dialog
        settingsDialog.setWidth(50, Unit.PERCENTAGE);
        VerticalLayout dialogContent = new VerticalLayout();

        ComboBox<String> modelSelect = new ComboBox<>("Embedding Model");
        modelSelect.setItems("AllMiniLmL6V2","OpenAi","Onnx","BertTokenizer");
        modelSelect.setValue(selectedModel);

        modelSelect.addValueChangeListener(e -> {
            selectedModel = e.getValue();
            settingsDialog.close();
            Notification n = new Notification(e.getValue() + " embedding model selected", 2000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            n.open();
        });

        Button closeButton = new Button("Close", click -> {
            settingsDialog.close();
        });
        closeButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        //dialogContent.add(toggleButton);
        dialogContent.add(new Span("Change Embedding Model"));
        dialogContent.add(modelSelect);
        dialogContent.add(closeButton);
        settingsDialog.add(dialogContent);
        // Show the dialog
        settingsDialog.open();
    }
}
