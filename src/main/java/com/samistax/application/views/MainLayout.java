package com.samistax.application.views;

import com.samistax.application.components.ColorPicker;
import com.samistax.application.views.about.AboutView;
import com.samistax.application.views.assistant.AssistantView;
import com.samistax.application.views.checkoutform.CheckoutFormView;
import com.samistax.application.views.design.DesignView;
import com.samistax.application.views.feed.FeedView;
import com.samistax.application.views.productcatalog.ProductCatalogView;
import com.samistax.application.views.products.ProductsView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.model.style.Color;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.net.URL;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    private H2 viewTitle;
    private Image logo = new Image();

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        H1 appName = new H1("Shop Assistant");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Button settingsBtn = new Button("", VaadinIcon.COG.create());
        settingsBtn.addClickListener(event -> openSettingsDialog());
        Header header = new Header(appName,  settingsBtn);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("(Choose Demo user)", FeedView.class, LineAwesomeIcon.LIST_SOLID.create()));
        nav.addItem(new SideNavItem("Assistant", AssistantView.class, LineAwesomeIcon.COMMENTS.create()));
        nav.addItem(new SideNavItem("Shop", ProductsView.class, LineAwesomeIcon.SHOPPING_BAG_SOLID.create()));
        nav.addItem(new SideNavItem("Design your shoe", DesignView.class, LineAwesomeIcon.SHOE_PRINTS_SOLID.create()));
        nav.addItem(new SideNavItem("Checkout", CheckoutFormView.class, LineAwesomeIcon.CREDIT_CARD.create()));
        //nav.addItem(new SideNavItem("Product Catalog", ProductCatalogView.class, LineAwesomeIcon.FILTER_SOLID.create()));
        nav.addItem(new SideNavItem("About", AboutView.class, LineAwesomeIcon.FILE.create()));
        return nav;
    }

    private Footer createFooter() {
        logo.setWidth("225px");
        logo.setSrc("images/datastax-logo-reverse.png");
        Footer layout = new Footer(logo);
        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
    private void openSettingsDialog() {
        // Create the dialog
        Dialog settingsDialog = new Dialog();
        settingsDialog.setModal(true); // Set the dialog as modal    // Add the content to the dialog
        settingsDialog.setWidth(50, Unit.PERCENTAGE);
        VerticalLayout dialogContent = new VerticalLayout();
        ColorPicker colorPicker = new ColorPicker();

        colorPicker.addValueChangeListener(e -> {
            //this.getStyle().setBackground(colorPicker.getValue());
            String newColor = colorPicker.getValue();
            Integer color_r =  Integer.valueOf( newColor.substring( 1, 3 ), 16 );
            Integer color_g =  Integer.valueOf( newColor.substring( 3, 5 ), 16 );
            Integer color_b =  Integer.valueOf( newColor.substring( 5, 7 ), 16 );

            String baseColor = "rgba("+color_r+","+color_g+","+color_b;

            // Parse the hex color to an integer
            int color = Integer.parseInt(newColor.substring(1), 16);

            // Invert the color
            int invertedColor = 0xFFFFFF - color;

            // Make the color web-safe
            int red = (invertedColor >> 16) & 0xFF;
            int green = (invertedColor >> 8) & 0xFF;
            int blue = invertedColor & 0xFF;

            String contrastColor = "rgba("+red+","+green+","+blue;
            //String contrastColor = "rgba("+contrast_r+","+contrast_g+","+contrast_b;

            UI.getCurrent().getElement().getStyle().set("--lumo-base-color", baseColor+")");
            UI.getCurrent().getElement().getStyle().set("--lumo-body-text-color", contrastColor+", 0.5)");
            UI.getCurrent().getElement().getStyle().set("--lumo-primary-text-color",  contrastColor+")");
            UI.getCurrent().getElement().getStyle().set("--lumo-secondary-text-color", contrastColor+", 0.66)");
            UI.getCurrent().getElement().getStyle().set("--lumo-tertiary-text-color", contrastColor+", 0.8)");
            UI.getCurrent().getElement().getStyle().set("--lumo-primary-contrast-color", contrastColor+")");
            UI.getCurrent().getElement().getStyle().set("--lumo-primary-color-50pct", baseColor+", 0.5)");
            UI.getCurrent().getElement().getStyle().set("--lumo-text-primary-contrast", contrastColor+")");
            UI.getCurrent().getElement().getStyle().set("--lumo-header-text-color", contrastColor+", 0.9)");
            UI.getCurrent().getElement().getStyle().set("--lumo-contrast-60pct", contrastColor+", 0.4)");

            //UI.getCurrent().getElement().getStyle().set("--lumo-primary-color", contrastColor+",0.5)");


            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            if (! themeList.contains(Lumo.DARK)) {
                themeList.add(Lumo.DARK);
            }

            //UI.getCurrent().getElement().getStyle().set("--lumo-primary-color-75pct", baseColorVariant+", 0.75)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-primary-color-50pct", baseColorVariant+", 0.5)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-primary-color-10pct", baseColorVariant+", 0.1)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-contrast-50pct", baseColorVariant+", 0.5)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-contrast-20pct", baseColorVariant+", 0.2)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-contrast-10pct", baseColorVariant+", 0.1)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-contrast-5pct", baseColor+", 0.2)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-contrast", baseColorVariant+", 0.0)");
            //UI.getCurrent().getElement().getStyle().set("--lumo-contrast", "white");
        });

        TextField logoSource = new TextField("Company Logo");
        logoSource.setValue(logo.getSrc());
        logoSource.setWidthFull();
        logoSource.addValueChangeListener(e -> {
            try {
                //Image image = new Image(e.getValue(), "Company Logo");
                URL testConenction = new URL(e.getValue());
                if ( testConenction.getContent() != null ) {
                    logo.setSrc(e.getValue());
                }
            } catch (Exception ex) {
                Notification.show("Not a valid image URL: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button toggleButton = new Button("Toggle Theme", click -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            if (themeList.contains(Lumo.DARK)) {
                themeList.remove(Lumo.DARK);
            } else {
                themeList.add(Lumo.DARK);
            }
        });
        toggleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button closeButton = new Button("Close", click -> {
            settingsDialog.close();
        });
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        //dialogContent.add(toggleButton);
        dialogContent.add(new Span("Theme Base Color"));
        dialogContent.add(colorPicker);
        dialogContent.add(logoSource);
        dialogContent.add(closeButton);
        settingsDialog.add(dialogContent);

        // Show the dialog
        settingsDialog.open();
    }
}
