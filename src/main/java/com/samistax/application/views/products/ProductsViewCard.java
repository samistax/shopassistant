package com.samistax.application.views.products;

import com.samistax.application.data.astra.json.Product;
import com.samistax.application.views.feed.Person;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vaadin.lineawesome.LineAwesomeIcon;


public class ProductsViewCard extends ListItem {

    public static String EVENT_ASSIST = "ai";
    public static String EVENT_ANN = "ann";
    public static Button annSearch = new Button("Similar");
    public static Button aiHelp = new Button(VaadinIcon.INFO_CIRCLE.create());

    public static class SimilarityClickEvent extends ComponentEvent<ProductsViewCard> {
        private float[] vector = new float[384];
        public SimilarityClickEvent(ProductsViewCard source) {
            super(source, false);
        }
        public float[] getVector(){return this.vector;}
        public void setVector(float[] vector){this.vector = vector;}
    }
    public static class AssistClickEvent extends ComponentEvent<ProductsViewCard> {
        private String productId = new String("");
        public AssistClickEvent(ProductsViewCard source) {
            super(source, false);
        }
        public String getProductId(){return this.productId;}
        public void setProductId(String productId){this.productId = productId;}
    }
    public static class DesignClickEvent extends ComponentEvent<ProductsViewCard> {
        private String productId = new String("");
        public DesignClickEvent(ProductsViewCard source) {
            super(source, false);
        }
        public String getProductId(){return this.productId;}
        public void setProductId(String productId){this.productId = productId;}
    }

    @Autowired
    private Cart cart;

    public ProductsViewCard() {
        this.cart = cart;
    }


    public ProductsViewCard(Product p, Float similarity, float[] vector) {
        addClassNames(Background.CONTRAST_5, Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Padding.MEDIUM,
                BorderRadius.LARGE);


        Div div = new Div();
        div.addClassNames(Background.CONTRAST, Display.FLEX, AlignItems.CENTER, JustifyContent.CENTER,
                Margin.Bottom.MEDIUM, Overflow.HIDDEN, BorderRadius.MEDIUM, Width.FULL);
        div.setHeight("160px");

        Image image = new Image();
        image.setHeight(100, Unit.PERCENTAGE);
        image.setSrc(p.getImage_url());
        image.setAlt(p.getName());

        div.add(image);

        Span header = new Span();
        header.addClassNames(FontSize.LARGE, FontWeight.SEMIBOLD);
        header.setText(p.getCategory());

        Span subtitle = new Span();
        subtitle.addClassNames(FontSize.SMALL, TextColor.SECONDARY);
        subtitle.setText(p.getBrand());

        Paragraph description = new Paragraph(p.getDescription());
        description.addClassName(Margin.Vertical.MEDIUM);
        description.add(subtitle);

        Span badge = new Span();
        badge.getElement().setAttribute("theme", "badge");
        badge.setText(p.getName());

        Span price = new Span();
        price.getElement().setAttribute("theme", "price");
        price.addClassNames(FontSize.SMALL, TextColor.PRIMARY_CONTRAST);
        try {
            double wholesale_price = Double.valueOf(p.getWholesale_price());
            price.setText(String.format("$%,.2f", wholesale_price));
        } catch ( Exception ex) {
            price.setText(ex.getMessage());
        }

        // Add call to actions
        Button annSearch = new Button(VaadinIcon.SEARCH.create());
        annSearch.addClickListener(event -> {
            SimilarityClickEvent pce = new SimilarityClickEvent(this);
            pce.setVector(vector);
            fireEvent(pce);
        });


        Button buyBtn = new Button(VaadinIcon.CART.create());
        buyBtn.addClickListener(event -> {

            Person user = VaadinSession.getCurrent().getAttribute(Person.class);
            if ( user != null  ) {
                Cart shopCart = VaadinSession.getCurrent().getAttribute(Cart.class);
                if (shopCart == null) {
                    shopCart = new Cart();
                }
                shopCart.addItem(user.getId(), p);
                // Set the cart to session to be shared with other views./
                VaadinSession.getCurrent().setAttribute(Cart.class, shopCart);
            }
        });

        Button assistBtn = new Button(VaadinIcon.INFO_CIRCLE.create());
        assistBtn.setTooltipText("Click for AI assistant");
        assistBtn.addClickListener(event -> {
            VaadinSession.getCurrent().setAttribute(Product.class, p);
            AssistClickEvent ace = new AssistClickEvent(this);
            ace.setProductId(p.getPid());
            fireEvent(ace);
        });
        Button designBtn = new Button(LineAwesomeIcon.SHOE_PRINTS_SOLID.create());
        designBtn.setTooltipText("Click to Design your onw shoe");
        designBtn.addClickListener(event -> {
            VaadinSession.getCurrent().setAttribute(Product.class, p);
            DesignClickEvent dce = new DesignClickEvent(this);
            dce.setProductId(p.getPid());
            fireEvent(dce);
        });

        HorizontalLayout footer = new HorizontalLayout();
        footer.setAlignItems(FlexComponent.Alignment.BASELINE);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.add(badge, price, buyBtn, assistBtn, designBtn );

        add( div, footer,subtitle,  header,description );
    }
    public Registration addAssistClickListener(
            ComponentEventListener<AssistClickEvent> listener) {
        return addListener(AssistClickEvent.class, listener);
    }
    public Registration addDesignClickListener(
            ComponentEventListener<DesignClickEvent> listener) {
        return addListener(DesignClickEvent.class, listener);
    }
    public Registration addSimilarityClickListener(
            ComponentEventListener<SimilarityClickEvent> listener) {
        return addListener(SimilarityClickEvent.class, listener);
    }
}
