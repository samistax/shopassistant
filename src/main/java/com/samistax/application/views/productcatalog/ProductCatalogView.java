package com.samistax.application.views.productcatalog;

import com.dtsx.astra.sdk.AstraDBCollection;
import com.dtsx.astra.sdk.utils.JsonUtils;
import com.samistax.application.Application;
import com.samistax.application.data.astra.json.Product;
import com.samistax.application.services.AstraService;
import com.samistax.application.services.SamplePersonService;
import com.samistax.application.views.MainLayout;
import com.samistax.application.views.products.ProductDataProvider;
import com.samistax.application.views.products.ProductFilter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.stargate.sdk.core.domain.Page;
import io.stargate.sdk.data.domain.odm.DocumentResult;
import io.stargate.sdk.data.domain.query.Filter;
import io.stargate.sdk.data.domain.query.SelectQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;

@PageTitle("Product Catalog")
@Route(value = "product-catalog", layout = MainLayout.class)
@Uses(Icon.class)
public class ProductCatalogView extends Div {

    //private Grid<SamplePerson> grid;
    private Grid<Product> grid;
    private AstraDBCollection collection;
    private SelectQuery query;

    private Filters filters;
    private static Map<String, Object> filterMap = new HashMap<>();

    private final SamplePersonService samplePersonService;

    public ProductCatalogView(SamplePersonService SamplePersonService, AstraService astraService) {
        this.samplePersonService = SamplePersonService;
        if ( astraService != null ) {
            this.collection = astraService.getCollection(Application.ASTRA_PRODUCT_TABLE);
        }
        setSizeFull();
        addClassNames("product-catalog-view");

        filters = new Filters(() -> refreshGrid());
        VerticalLayout layout = new VerticalLayout(createMobileFilters(), /*filters,*/ createGrid());
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);
    }

    private HorizontalLayout createMobileFilters() {
        // Mobile version
        HorizontalLayout mobileFilters = new HorizontalLayout();
        mobileFilters.setWidthFull();
        mobileFilters.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BoxSizing.BORDER,
                LumoUtility.AlignItems.CENTER);
        mobileFilters.addClassName("mobile-filters");

        Icon mobileIcon = new Icon("lumo", "plus");
        Span filtersHeading = new Span("Filters");
        mobileFilters.add(mobileIcon, filtersHeading);
        mobileFilters.setFlexGrow(1, filtersHeading);
        mobileFilters.addClickListener(e -> {
            if (filters.getClassNames().contains("visible")) {
                filters.removeClassName("visible");
                mobileIcon.getElement().setAttribute("icon", "lumo:plus");
            } else {
                filters.addClassName("visible");
                mobileIcon.getElement().setAttribute("icon", "lumo:minus");
            }
        });
        return mobileFilters;
    }

    public static class Filters extends Div implements Specification<Product> {

        private final TextField name = new TextField("Name");
        private final TextField phone = new TextField("Phone");
        private final DatePicker startDate = new DatePicker("Date of Birth");
        private final DatePicker endDate = new DatePicker();
        private final MultiSelectComboBox<String> occupations = new MultiSelectComboBox<>("Occupation");
        private final CheckboxGroup<String> roles = new CheckboxGroup<>("Role");
        private final HashMap<String, TextField> filters = new HashMap<>();

        public Filters(Runnable onSearch) {

            List<Field> names = Arrays.stream(Product.class.getDeclaredFields()).toList();
            names.forEach(field -> {
                String fname = field.getName();
                TextField tf = new TextField(fname);
                if ( filterMap.containsKey(fname) ) {
                    tf.setValue(filterMap.get(fname).toString());
                }
                tf.setId(fname);
                filters.put(fname, tf);
                add(tf);
            });

            setWidthFull();
            addClassName("filter-layout");
            addClassNames(LumoUtility.Padding.Horizontal.LARGE, LumoUtility.Padding.Vertical.MEDIUM,
                    LumoUtility.BoxSizing.BORDER);
            name.setPlaceholder("First or last name");

            occupations.setItems("Insurance Clerk", "Mortarman", "Beer Coil Cleaner", "Scale Attendant");

            roles.setItems("Worker", "Supervisor", "Manager", "External");
            roles.addClassName("double-width");

            // Action buttons
            Button resetBtn = new Button("Reset");
            resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            resetBtn.addClickListener(e -> {
                name.clear();
                phone.clear();
                startDate.clear();
                endDate.clear();
                occupations.clear();
                roles.clear();
                filterMap.clear();
                onSearch.run();
            });
            Button searchBtn = new Button("Search");
            searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            searchBtn.addClickListener(e -> onSearch.run());

            Div actions = new Div(resetBtn, searchBtn);
            actions.addClassName(LumoUtility.Gap.SMALL);
            actions.addClassName("actions");

            //add(name, phone, createDateRangeFilter(), occupations, roles, actions);
            add(actions);
        }

        private Component createDateRangeFilter() {
            startDate.setPlaceholder("From");

            endDate.setPlaceholder("To");

            // For screen readers
            startDate.setAriaLabel("From date");
            endDate.setAriaLabel("To date");

            FlexLayout dateRangeComponent = new FlexLayout(startDate, new Text(" – "), endDate);
            dateRangeComponent.setAlignItems(FlexComponent.Alignment.BASELINE);
            dateRangeComponent.addClassName(LumoUtility.Gap.XSMALL);

            return dateRangeComponent;
        }

        @Override
        public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            List<Predicate> predicates = new ArrayList<>();

            if (!name.isEmpty()) {
                String lowerCaseFilter = name.getValue().toLowerCase();
                Predicate firstNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")),
                        lowerCaseFilter + "%");
                Predicate lastNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")),
                        lowerCaseFilter + "%");
                predicates.add(criteriaBuilder.or(firstNameMatch, lastNameMatch));
            }
            if (!phone.isEmpty()) {
                String databaseColumn = "phone";
                String ignore = "- ()";

                String lowerCaseFilter = ignoreCharacters(ignore, phone.getValue().toLowerCase());
                Predicate phoneMatch = criteriaBuilder.like(
                        ignoreCharacters(ignore, criteriaBuilder, criteriaBuilder.lower(root.get(databaseColumn))),
                        "%" + lowerCaseFilter + "%");
                predicates.add(phoneMatch);

            }
            if (startDate.getValue() != null) {
                String databaseColumn = "dateOfBirth";
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(databaseColumn),
                        criteriaBuilder.literal(startDate.getValue())));
            }
            if (endDate.getValue() != null) {
                String databaseColumn = "dateOfBirth";
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.literal(endDate.getValue()),
                        root.get(databaseColumn)));
            }
            if (!occupations.isEmpty()) {
                String databaseColumn = "occupation";
                List<Predicate> occupationPredicates = new ArrayList<>();
                for (String occupation : occupations.getValue()) {
                    occupationPredicates
                            .add(criteriaBuilder.equal(criteriaBuilder.literal(occupation), root.get(databaseColumn)));
                }
                predicates.add(criteriaBuilder.or(occupationPredicates.toArray(Predicate[]::new)));
            }
            if (!roles.isEmpty()) {
                String databaseColumn = "role";
                List<Predicate> rolePredicates = new ArrayList<>();
                for (String role : roles.getValue()) {
                    rolePredicates.add(criteriaBuilder.equal(criteriaBuilder.literal(role), root.get(databaseColumn)));
                }
                predicates.add(criteriaBuilder.or(rolePredicates.toArray(Predicate[]::new)));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }

        private String ignoreCharacters(String characters, String in) {
            String result = in;
            for (int i = 0; i < characters.length(); i++) {
                result = result.replace("" + characters.charAt(i), "");
            }
            return result;
        }

        private Expression<String> ignoreCharacters(String characters, CriteriaBuilder criteriaBuilder,
                Expression<String> inExpression) {
            Expression<String> expression = inExpression;
            for (int i = 0; i < characters.length(); i++) {
                expression = criteriaBuilder.function("replace", String.class, expression,
                        criteriaBuilder.literal(characters.charAt(i)), criteriaBuilder.literal(""));
            }
            return expression;
        }

    }

    private Component createGrid() {


        ProductDataProvider dataProvider = new ProductDataProvider(collection);
        grid = new Grid<Product>();
        grid.setPageSize(20);
        grid.setDataProvider(dataProvider);
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);


        Grid.Column idCol = grid.addColumn(r -> r.getPid() == null ? "" : r.getPid())
                .setHeader("pid").setKey("pid").setAutoWidth(true).setSortable(true);
        Grid.Column descriptionCol = grid.addColumn(r -> r.getDescription() == null ? "" : r.getDescription())
                .setHeader("Description").setKey("description").setWidth("40%").setSortable(true);
        Grid.Column nameCol = grid.addColumn(r -> r.getName() == null ? "" : r.getName())
                .setHeader("Name").setKey("name").setAutoWidth(true).setSortable(true);
        Grid.Column brandCol = grid.addColumn(r -> r.getBrand() == null ? "" : r.getBrand())
                .setHeader("Brand").setKey("brand").setAutoWidth(true).setSortable(true);
        Grid.Column categoryCol = grid.addColumn(r -> r.getCategory() == null ? "" : r.getCategory())
                .setHeader("Category").setKey("category").setAutoWidth(true).setSortable(true);
        Grid.Column colorCol = grid.addColumn(r -> r.getColor() == null ? "" : r.getColor())
                .setHeader("Color").setKey("color").setAutoWidth(true).setSortable(true);
        Grid.Column priceCol = grid.addColumn(r -> r.getWholesale_price() == null ? "" : r.getWholesale_price())
                .setHeader("Price").setKey("wholesale_price").setAutoWidth(true).setSortable(true);
        Grid.Column imageCol = grid.addColumn(r -> r.getImage_url() == null ? "" : r.getImage_url())
                .setHeader("Image").setKey("image_url").setAutoWidth(true).setSortable(false);

          /*
        Grid.Column idCol = grid.addColumn(r -> r.getData().getId() == null ? "" : r.getData().getId())
                .setHeader("pid").setAutoWidth(true).setSortable(true);
        idCol.getHeaderComponent().add
        grid.addColumn(r -> r.getData().getDescription() == null ? "" : r.getData().getDescription())
                .setHeader("Description").setAutoWidth(true).setSortable(true);
        grid.addColumn(r -> r.getData().getName() == null ? "" : r.getData().getName())
                .setHeader("Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(r -> r.getData().getBrand() == null ? "" : r.getData().getBrand())
                .setHeader("Brand").setAutoWidth(true).setSortable(true);
        grid.addColumn(r -> r.getData().getCategory() == null ? "" : r.getData().getCategory())
                .setHeader("Category").setAutoWidth(true).setSortable(true);
        grid.addColumn(r -> r.getData().getColor() == null ? "" : r.getData().getColor())
                .setHeader("Color").setAutoWidth(true).setSortable(true);
        grid.addColumn(r -> r.getData().getWholesale_price() == null ? "" : r.getData().getWholesale_price())
                .setHeader("Price").setAutoWidth(true).setSortable(true);
        grid.addColumn(r -> r.getData().getImage_url() == null ? "" : r.getData().getImage_url())
                .setHeader("Image").setAutoWidth(true).setSortable(false);



        grid.setItems(query -> samplePersonService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)),
                filters).stream());

         */
        HeaderRow headerRow = grid.appendHeaderRow();
        ProductFilter productFilter = new ProductFilter(grid.getLazyDataView());

        ConfigurableFilterDataProvider<Product, Void, ProductFilter> filterDataProvider = dataProvider
                .withConfigurableFilter();
        filterDataProvider.setFilter(productFilter);

        //Product prodSchema = new Product();
        //Field[] fields = prodSchema.getClass().getDeclaredFields();

        grid.getColumns().forEach(col-> {
            headerRow.getCell(col).setComponent(createFilterHeader(col.getKey(),productFilter, productFilter::setPid));
        } );
/*
        headerRow.getCell(idCol).setComponent(createFilterHeader("pid",productFilter, productFilter::setId));
        headerRow.getCell(nameCol).setComponent(createFilterHeader("Name",productFilter, productFilter::setName));
        headerRow.getCell(colorCol).setComponent(createFilterHeader("Color",productFilter, productFilter::setColor));
        headerRow.getCell(descriptionCol).setComponent(createFilterHeader("Description",productFilter, productFilter::setDescription));
        headerRow.getCell(categoryCol).setComponent(createFilterHeader("Category",productFilter, productFilter::setCategory));
        headerRow.getCell(brandCol).setComponent(createFilterHeader("Brand",productFilter, productFilter::setBrand));
        headerRow.getCell(priceCol).setComponent(createFilterHeader("Price",productFilter, productFilter::setWholesale_price));
 */
        grid.setItems(filterDataProvider);


        //refreshGrid();

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);

        return grid;
    }

    private static Component createFilterHeader(String labelText,
                                                ProductFilter productFilter,
                                                Consumer<String> filterChangeConsumer) {
        NativeLabel label = new NativeLabel(labelText);
        label.getStyle().set("padding-top", "var(--lumo-space-m)")
                .set("font-size", "var(--lumo-font-size-xs)");
        TextField textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setClearButtonVisible(true);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setWidthFull();

        textField.getStyle().set("max-width", "100%");
        textField.addValueChangeListener(e -> {
                //filterChangeConsumer.accept(e.getValue());
                if ( e.getValue().isBlank() ) {
                    productFilter.remove(labelText);
                } else {
                    productFilter.put(labelText, e.getValue());
                }
                productFilter.refreshDataView();

                //productFilter.setSearchTerm(e.getValue());
                //filterDataProvider.setFilter(productFilter);
        });

        //VerticalLayout layout = new VerticalLayout(label, textField);
        VerticalLayout layout = new VerticalLayout(textField);
        layout.getThemeList().clear();
        layout.getThemeList().add("spacing-xs");

        return layout;
    }
    private List<Product> queryProducts(Map<String, Object> filters) {
        Filter filterProductExists = new Filter().where("pid").exists();
        // TODO: Convert UI filters to Data API query filters.
        SelectQuery query = SelectQuery.builder()
                .filter(filterProductExists)
                .build();
        Page<DocumentResult<Product>> page = collection.findPage(query, Product.class);
        List<DocumentResult<Product>> results = page.getResults();
        List<Product> prods = new ArrayList<>();
        results.forEach(r-> prods.add(r.getData()));
        return prods;
    }

    private void refreshGrid() {
        //grid.setItems(queryProducts(filterMap));
        grid.getDataProvider().refreshAll();
    }

}
