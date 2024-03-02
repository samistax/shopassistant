package com.samistax.application.views.design;

import com.samistax.application.data.astra.json.Product;
import com.samistax.application.views.MainLayout;
import com.samistax.application.views.feed.Person;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@PageTitle("Design")
@Route(value = "design", layout = MainLayout.class)
public class DesignView extends HorizontalLayout {

    private Product selectedProduct;
    private ProgressBar spinner = new ProgressBar();
    private Component inputPanel;
    private Image generatedImage = new Image();

    private boolean chatCompletionInProgress = false;

    private TextArea designInput = new TextArea("Describe your perfect shoe");
    private OpenAiImageModel imageModel;

    public DesignView(OpenAiImageModel imageModel) {
        this.imageModel = imageModel;
        add(constructUI());
    }

    private Component constructUI() {
        VerticalLayout layout = new VerticalLayout();

        layout.addClassNames("products-view");

        HorizontalLayout headerContainer = new HorizontalLayout();
        H2 header = new H2("Footwear - Design your own shoe");
        header.addClassNames(Margin.Bottom.NONE, Margin.Top.MEDIUM, FontSize.XXXLARGE);
        headerContainer.add(header);

        Paragraph description = new Paragraph("Did you not find what you were looking for. Use generative AI assistant to build the shoe that you always dreamt of. ");
        description.addClassNames(Margin.Bottom.SMALL, Margin.Top.NONE, TextColor.SECONDARY);

        Paragraph info = new Paragraph("(This demo uses Open AI DALL-E to generate images. Visit https://dorik.com/blog/how-to-write-dall-e-prompts to learn how to write DALL-E prompts effectively to generate outstanding visuals.)");
        description.addClassNames(Margin.Bottom.SMALL, Margin.Top.NONE, TextColor.SECONDARY);


        spinner.setIndeterminate(true);
        spinner.setVisible(false);

        inputPanel = constructDesignPanel();

        layout.add(headerContainer, description, inputPanel, spinner, generatedImage, info);
        layout.setSizeFull();
        return layout;
    }
    private Component constructDesignPanel() {

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(Alignment.CENTER);
        selectedProduct = VaadinSession.getCurrent().getAttribute(Product.class);
        /*
        if ( selectedProduct != null ) {
            layout.addClassNames(LumoUtility.Background.CONTRAST_20, Display.FLEX, LumoUtility.FlexDirection.COLUMN, AlignItems.START, Padding.MEDIUM,
                    LumoUtility.BorderRadius.LARGE);

            Div div = new Div();
            div.addClassNames(Display.FLEX, AlignItems.CENTER, JustifyContent.CENTER,
                    Margin.Bottom.MEDIUM, LumoUtility.Overflow.HIDDEN, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Width.FULL);
            div.setHeight("160px");

            Image image = new Image();
            image.setSrc(selectedProduct.getImage_url());
            image.setAlt(selectedProduct.getName());
            div.add(image);

            Span title = new Span();
            title.addClassNames(FontSize.MEDIUM, LumoUtility.FontWeight.SEMIBOLD);
            title.setText(selectedProduct.getCategory());
            layout.add(title, div);
        }
        */
        Button button = new Button(VaadinIcon.PAPERPLANE.create());
        button.setText("Generate Shoe Design");
        button.addClickListener(event -> submitDesignPrompt());

        designInput.setValueChangeMode(ValueChangeMode.EAGER);
        designInput.setWidthFull();
        //designInput.addKeyPressListener(Key.ENTER, event -> submitDesignPrompt());

        layout.add(designInput, button);

        layout.setWidthFull();
        return layout;
    }

    private void submitDesignPrompt(){
        // Give some basic instructions for the LLM to generate similar type of images.
        String promptTemplate = "Create a photo realistic image of pair of shoes. Solid white color background. ";
        String input = designInput.getValue();
        UI ui = UI.getCurrent();
        spinner.setVisible(true);
        inputPanel.setVisible(false);
        Notification n = new Notification("Your shoe design is being created. Hang on tight this might take up to a minute", 4000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.open();

        // Start background task
        CompletableFuture.runAsync(() -> {
            // Do some long running task
            try {
                Response<dev.langchain4j.data.image.Image> response = imageModel.generate(promptTemplate+input);
                // Need to use access() when running from background thread
                ui.access(() -> {
                    // create a buffered image
                    generatedImage.setWidthFull();
                    if ( response.content() != null ) {
                        generatedImage.setSrc(response.content().url().toString());
                    } else {
                        generatedImage.setText("Failed to create an image. Please try to revise your input");
                    }
                    // Stop polling and hide spinner
                    ui.setPollInterval(-1);
                    spinner.setVisible(false);
                    inputPanel.setVisible(true);
                });
            } catch (Exception e) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "submitDesignPrompt threw Exception: " , e);
                ui.access(() -> {
                    spinner.setVisible(false);
                    Notification error = new Notification(e.getMessage(), 3000, Notification.Position.MIDDLE);
                    error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    error.open();
                });
            }
        });
    }
}
