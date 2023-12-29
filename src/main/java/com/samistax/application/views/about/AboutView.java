package com.samistax.application.views.about;

import com.samistax.application.views.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
public class AboutView extends VerticalLayout {

    public AboutView() {
        setSpacing(false);

        Image img = new Image("images/datastax-logo-reverse.png", "company logo");
        img.setWidth("200px");
        add(img);

        H2 header = new H2("Real-Time AI for Everyone");
        header.addClassNames(Margin.Top.XLARGE, Margin.Bottom.MEDIUM);
        add(header);
        add(new Paragraph("Build AI-powered applications that make every decision instant, accurate, powerful."));
        add(new Paragraph("Bring machine learning to real-time data to enhance every application with the speed and scale you need."));
        add(header);
        H3 version = new H3("Assistant - v0.1.0");
        add(version);
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");
    }

}
