package com.samistax.application.views.products;

import com.samistax.application.data.astra.json.Product;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;

import java.util.HashMap;
import java.util.Map;

public class ProductFilter {
    private final GridLazyDataView<Product> dataView;

    private String pid;
    private String name;
    private String color;
    private String description;
    private String category;
    private String brand;
    private String wholesale_price;
    private String image_url;

    private Map<String, Object> filters = new HashMap<>();

    public ProductFilter(GridLazyDataView<Product> dataView) {
        this.dataView = dataView;
        //this.dataView.addFilter(this::test);
    }
    //public ProductFilter() {}
    public void refreshDataView() {
        this.dataView.refreshAll();
    }
    public Map<String, Object> getFilters() {
        return filters;
    }
    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
    public Object put(String key, Object value) {
        Object prevItem = this.filters.put(key,value);
        return prevItem;
    }
    public Object remove(String key) {
        return this.filters.remove(key);
    }

    public void setPid(String pid) {
        this.pid = pid;
       // this.dataView.refreshAll();
    }

    public void setName(String name) {
        this.name = name;
       // this.dataView.refreshAll();
    }

    public void setColor(String color) {
        this.color = color;
       // this.dataView.refreshAll();
    }

    public void setDescription(String description) {
        this.description = description;
        //this.dataView.refreshAll();
    }

    public void setCategory(String category) {
        this.category = category;
        //this.dataView.refreshAll();
    }

    public void setBrand(String brand) {
        this.brand = brand;
        //this.dataView.refreshAll();
    }

    public void setWholesale_price(String wholesale_price) {
        this.wholesale_price = wholesale_price;
       // this.dataView.refreshAll();
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
        //this.dataView.refreshAll();
    }

    public Map<String, Object> getFilterMap() {
        HashMap<String, Object> filters = new HashMap<>();
        if ( pid != null) {filters.replace("pid", this.pid); }
        if ( name != null) {filters.replace("name", this.name); }
        if ( color != null) {filters.replace("color", this.color); }
        if ( description != null) {filters.replace("description", this.description); }
        if ( category != null) {filters.replace("category", this.category); }
        if ( brand != null) {filters.replace("brand", this.brand); }
        if ( wholesale_price != null) {filters.replace("wholesale_price", this.wholesale_price); }
        if ( image_url != null) {filters.replace("image_url", this.image_url); }
        return filters;
    }
    public boolean test(Product person) {
        boolean matchesPid = matches(person.getPid(), pid);
        boolean matchesName = matches(person.getName(), name);
        boolean matchesColor = matches(person.getColor(), color);
        boolean matchesDescription = matches(person.getDescription(), description);
        boolean matchesCategory = matches(person.getCategory(), category);
        boolean matchesBrand = matches(person.getBrand(), brand);
        boolean matchesWholesale_price = matches(person.getWholesale_price(), wholesale_price);
        boolean matchesImage_url = matches(person.getImage_url(), image_url);

        return matchesPid || matchesName || matchesColor || matchesDescription || matchesCategory || matchesBrand ||
                matchesWholesale_price || matchesImage_url;
    }

    private boolean matches(String value, String searchTerm) {
        return searchTerm == null || searchTerm.isEmpty()
                || value.toLowerCase().contains(searchTerm.toLowerCase());
    }
}
