package com.samistax.application.data.astra.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.StringTokenizer;

@JsonPropertyOrder({ "pid", "name", "color", "description", "category", "brand", "wholesale_price", "image_url" })
public class Product {
    @JsonProperty("pid") private String pid;
    @JsonProperty("name") private String name;
    @JsonProperty("color") private String color;
    @JsonProperty("description") private String description;
    @JsonProperty("category") private String category;
    @JsonProperty("brand") private String brand;
    @JsonProperty("wholesale_price") private String wholesale_price;
    @JsonProperty("image_url") private String image_url;

    public Product() {}

    public void initFromCSV(String csv_row)  {
        StringTokenizer st = new StringTokenizer(",");
        if ( csv_row != null && st != null && st.countTokens() > 7 ) {
            this.pid = st.nextToken();
            this.name = st.nextToken();
            this.color = st.nextToken();
            this.description = st.nextToken();
            this.category = st.nextToken();
            this.brand = st.nextToken();
            this.wholesale_price = st.nextToken();
            this.image_url = st.nextToken();
        }
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + pid + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", brand='" + brand + '\'' +
                ", wholesale_price='" + wholesale_price + '\'' +
                ", image_url='" + image_url + '\'' +
                '}';
    }

    // Getter and setters for working with POJOs

    public String getPid() {
        return pid;
    }

    public void setPid(String id) {
        this.pid = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getWholesale_price() {
        return wholesale_price;
    }

    public void setWholesale_price(String wholesale_price) {
        this.wholesale_price = wholesale_price;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }
}
