package com.samistax.application.views.products;

import com.dtsx.astra.sdk.AstraDBCollection;
import com.samistax.application.Application;
import com.samistax.application.ai.ShopAssistantAgent;
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
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import com.vaadin.flow.theme.lumo.LumoUtility.ListStyleType;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import elemental.json.JsonValue;
import io.stargate.sdk.core.domain.Page;
import io.stargate.sdk.data.domain.odm.DocumentResult;
import io.stargate.sdk.data.domain.query.Filter;
import io.stargate.sdk.data.domain.query.SelectQuery;


import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@PageTitle("Shop")
@Route(value = "shop", layout = MainLayout.class)
public class ProductsView extends SplitLayout implements HasComponents, HasStyle {

    private OrderedList imageContainer;
    private AstraService astraService;
    private ChatService chatService;
    private ShopAssistantAgent agent;

    private SelectQuery productQuery;

    private List<String> pageStates = new ArrayList<>();
    private AstraDBCollection collection;

    private int selectedPage = 1;
    private Span currentPageTitle;
    private Product selectedProduct;
    private String selectedModel = "AllMiniLmL6V2";
    private List<Product> similarProducts = new ArrayList<>();

    private TextField promptInput = new TextField("Ask about the product");
    private Span promptResponse = new Span();
    private boolean chatCompletionInProgress = false;
    private Filter filterProductExists = new Filter().where("pid").exists();


    // File upload fields
    private File file;
    private String originalFileName;
    private String mimeType;
    private Div output = new Div(new Text("(no image file uploaded yet)"));
    private TextArea designInput = new TextArea("Tell us how to make this perfect shoe");
    private OpenAiImageModel imageModel;


    public ProductsView(AstraService astraService, ChatService chatService, ShopAssistantAgent agent, OpenAiImageModel imageModel) {

        this.astraService = astraService;
        this.chatService = chatService;
        this.agent = agent;
        this.imageModel = imageModel;

        this.collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        this.productQuery = SelectQuery.builder()
                .filter(filterProductExists)
                .includeSimilarity()
                .build();

        // Create UI with personalised welcome text
        this.addToPrimary(constructPrimaryUI());
        // Create UI for Assistant
        this.addToSecondary(constructSecondaryUI(false));
        this.getSecondaryComponent().setVisible(false);

        // You can map the output as Result<T> using either a Java pojo or mapper
        Page<DocumentResult<Product>> page = collection.findPage(productQuery, Product.class);
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
            Page<DocumentResult<Product>> prevPage = null;
            if ( (pageStates.size()-1)  > 0) {
                prevPage = collection.findPage(SelectQuery.builder()
                        .filter(filterProductExists)
                        .includeSimilarity()
                        .withPagingState(pageStates.get(lastItemIdx-2))
                        .build(), Product.class);
            } else{
                // Returned to first page
                prevPage = collection.findPage(SelectQuery.builder()
                        .filter(filterProductExists)
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
            Page<DocumentResult<Product>> nextPage = collection.findPage(SelectQuery.builder()
                    .filter(filterProductExists)
                    .withPagingState(pageStates.get(pageStates.size()-1))
                    //.withSkip(selectedPage)
                    .includeSimilarity()
                    .build(), Product.class);
            nextPageBtn.setEnabled(false);
            nextPage.getPageState().ifPresent(pageState -> {
                pageStates.add(pageState);
                nextPageBtn.setEnabled(true);
            });
            // Enable previous button to return to initial view
            prevPageBtn.setEnabled(true);
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

    private void addProductsToContainer(Page<DocumentResult<Product>> page){
        // Remove old items
        imageContainer.removeAll();
        similarProducts.clear();
        promptInput.clear();

        page.getResults().stream().forEach(p -> {

            Product prod = p.getData();
            ProductsViewCard card = new ProductsViewCard(prod, p.getSimilarity(), p.getVector());
            card.addAssistClickListener(e-> {
                System.out.println("Assist button clicked for product" + e.getProductId());
                similarProducts.clear();
                this.addToSecondary(constructSecondaryUI(false));
                this.getSecondaryComponent().setVisible(true);
                this.getSecondaryComponent().scrollIntoView();
            });
            card.addDesignClickListener(e-> {
                System.out.println("Design button clicked for product" + e.getProductId());
                similarProducts.clear();
                this.addToSecondary(constructSecondaryUI(true));
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
                        .filter(filterProductExists)
                        .includeSimilarity()
                        .build();
            } else {
                productQuery = SelectQuery.builder()
                        .filter(filterProductExists)
                        .includeSimilarity()
                        .orderBy("wholesale_price", orderByDirection)
                        .build();
            }

            Page<DocumentResult<Product>> page = collection.findPage(productQuery, Product.class);
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
    private Component constructSecondaryUI(boolean designerMode) {

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
            Image img = new Image(p.getImage_url(), p.getPid() + " image");
            img.setWidth(60, Unit.PIXELS);
            img.addClickListener(e -> {
                VaadinSession.getCurrent().setAttribute(Product.class, p);
                //selectedProduct = p;
                this.addToSecondary(constructSecondaryUI(designerMode));
            });
            similarProductsPanel.add(img);
        });

        layout.add(similarProductsPanel);
        Button findBtn = new Button(VaadinIcon.SEARCH.create());
        findBtn.setText("Find Similar Products");
        findBtn.addClickListener(e-> {

            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
            Embedding descEmbedding = embeddingModel.embed(selectedProduct.getDescription()).content();
            List<DocumentResult<Product>> annProducts = findSimilarProduct(descEmbedding.vector(), 5);
            if ( annProducts.size() > 0 ) {
                // Update new similar items
                this.similarProducts.clear();
                annProducts.forEach(r -> {
                    if ( r.getSimilarity() < 1) {
                        similarProducts.add(r.getData());
                    }
                });
                this.addToSecondary(constructSecondaryUI(designerMode));
            }
        });
        layout.add(findBtn);
        HorizontalLayout inputLayout = new HorizontalLayout();

        if ( designerMode ) {
            designInput.setValueChangeMode(ValueChangeMode.EAGER);
            designInput.setWidthFull();
            designInput.addKeyPressListener(Key.ENTER, event -> submitDesignPrompt());
            inputLayout.add(designInput);
        } else {
            Button button = new Button(VaadinIcon.PAPERPLANE.create());
            //promptInput.setLabel("Asks about the product");
            promptInput.setWidth(80, Unit.PERCENTAGE);
            button.setWidth(20, Unit.PERCENTAGE);
            promptInput.addKeyPressListener(Key.ENTER, event -> submitPrompt());
            button.addClickListener(event -> submitPrompt());
            inputLayout.add(promptInput, button);
        }
        inputLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        inputLayout.setWidthFull();

        promptResponse.setWidthFull();
        layout.add(inputLayout, promptResponse);
        //layout.add(UploadImageToFile());

        layout.setWidth(40,Unit.PERCENTAGE);

        return layout;
    }
    public List<DocumentResult<Product>> findSimilarProduct(float[] embedding, int limit) {
        if ( limit > 20 || limit < 0 ) {
            limit = 20;
        }
        AstraDBCollection collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        // Order the results by similarity
        List<DocumentResult<Product>> result = collection.find (
                SelectQuery.builder()
                        .orderByAnn(embedding)
                        .includeSimilarity()
                        .withLimit(Integer.valueOf(limit))
                        .build(), Product.class).toList();
        return result;
    }
    private void submitPrompt(){

        UI ui = UI.getCurrent();

        try {
            if (!chatCompletionInProgress) {
                chatCompletionInProgress = true;
                promptResponse.setText("");

                promptInput.setPlaceholder(promptInput.getValue());
                TokenStream tokenStream = agent.productChat(promptInput.getValue(), selectedProduct);
                //TokenStream tokenStream = chatService.chatShopAssistant(promptUser.getValue(), selectedProduct);
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
                promptInput.clear();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "submitPrompt threw Exception: " , ex);
        }
    }
    private void submitDesignPrompt(){

        String input = designInput.getValue();
        UI ui = UI.getCurrent();

        try {

            if (!chatCompletionInProgress) {
                chatCompletionInProgress = true;
                promptResponse.setText(input);

                //dev.langchain4j.data.image.Image referenceImage = dev.langchain4j.data.image.Image.builder().url(selectedProduct.getImage_url()).build();
                //Response<dev.langchain4j.data.image.Image> response = imageModel.edit(referenceImage, input);
                Response<dev.langchain4j.data.image.Image> response = imageModel.generate(input);

                // create a buffered image
                Image responseImage = new Image();
                if ( response.content() != null ) {
                    responseImage.setSrc(response.content().url().toString());
                } else {
                    promptResponse.setText("Failed to create an image. Please try to revise your input");
                }
                //this.promptResponse.add(convertToImage(imageBytes));
                this.promptResponse.add(responseImage);

                chatCompletionInProgress = false;
                promptInput.clear();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "submitDesignPrompt threw Exception: " , ex);
            promptResponse.setText("Error: " + ex.getMessage());
            chatCompletionInProgress = false;
        }
    }


    private void openSettingsDialog() {
        // Create the dialog
        Dialog settingsDialog = new Dialog();
        settingsDialog.setModal(true); // Set the dialog as modal    // Add the content to the dialog
        settingsDialog.setWidth(50, Unit.PERCENTAGE);
        VerticalLayout dialogContent = new VerticalLayout();

        ComboBox<String> modelSelect = new ComboBox<>("Text Embedding Model");
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

    // This is called from JavaScript event listener when file is removed from upload component
    @ClientCallable
    public void fileRemove(JsonValue event) {
        /*output.getChildren()
                .filter(component -> event.toJson().contains(
                        (String) ComponentUtil.getData(component, "name")))
                .findFirst().ifPresent(match -> output.remove(match));
         */
        output.removeAll();
    }
    private VerticalLayout UploadImageToFile() {
        VerticalLayout layout = new VerticalLayout();
        Upload upload = new Upload(this::receiveUpload);
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("Search with image"));

        layout.add(output, upload);

        // Configure upload component
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.addSucceededListener(event -> {
            Image searchImage = new Image(new StreamResource(this.originalFileName,this::loadFile),"Uploaded image");
            searchImage.setWidth(200,Unit.PIXELS);
            output.removeAll();
            output.add(searchImage);
        });
        upload.addFileRejectedListener(event -> {
            Notification.show(event.getErrorMessage());

        });
        upload.getElement().executeJs(
                "this.addEventListener('file-remove', (e) => $0.$server.fileRemove(e.detail.file.name));",
                getElement());

        upload.addFailedListener(event -> {
            output.removeAll();
            output.add(new Text("Upload failed: " + event.getReason()));
        });
        return layout;
    }

    /** Load a file from local filesystem.
     *
     */
    public InputStream loadFile() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to create InputStream for: '" + this.file.getAbsolutePath(), e);
        }
        return null;
    }

    /** Receive a uploaded file to a file.
     */
    public OutputStream receiveUpload(String originalFileName, String MIMEType) {
        this.originalFileName = originalFileName;
        this.mimeType = MIMEType;
        try {
            // Create a temporary file for example, you can provide your file here.
            this.file = File.createTempFile("prefix-", "-suffix");
            file.deleteOnExit();
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to create InputStream for: '" + this.file.getAbsolutePath(), e);
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to create InputStream for: '" + this.file.getAbsolutePath() + "'", e);
        }

        return null;
    }

}
