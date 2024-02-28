package com.samistax.application.views.products;

import com.dtsx.astra.sdk.AstraDBCollection;
import com.dtsx.astra.sdk.utils.JsonUtils;
import com.samistax.application.data.astra.json.Product;
import com.vaadin.flow.data.provider.*;
import io.stargate.sdk.core.domain.Page;
import io.stargate.sdk.data.domain.odm.DocumentResult;
import io.stargate.sdk.data.domain.query.Filter;
import io.stargate.sdk.data.domain.query.SelectQuery;

import java.util.*;
import java.util.stream.Stream;

public class ProductDataProvider extends AbstractBackEndDataProvider<Product, ProductFilter> {
    AstraDBCollection collection;
   // final List<Product> DATABASE = new ArrayList<>(DataService.getPeople());

    public ProductDataProvider(AstraDBCollection collection) {
        this.collection = collection;
    }

    private List<DocumentResult<Product>> queryResultProducts(Map<String, Object> filters, int skip) {
        Filter filterProductExists = new Filter().where("pid").exists().and();


        SelectQuery query = SelectQuery.builder()
                .withoutVector()
                .filter(filterProductExists)
                //.where("pid").exists().withFilter(new Filter(JsonUtils.mapAsJson(filters)))
                //.withSkip(skip) // Use only with sort
                //.sort(filters)
                .build();
        Page<DocumentResult<Product>> page = collection.findPage(query, Product.class);
        return page.getResults();
    }

    @Override
    protected Stream<Product> fetchFromBackEnd(Query<Product, ProductFilter> query) {
        // A real app should use a real database or a service
        // to fetch, filter and sort data.

        //Stream<Product> stream = DATABASE.stream();
        List<Product> products = new ArrayList<>();
        if ( query.getFilter().isPresent() ) {
            Map filters = query.getFilter().stream().findFirst().get().getFilters();
            List<DocumentResult<Product>> results = queryResultProducts(filters, query.getPage());
            results.forEach(r-> products.add(r.getData()));
        }
        return products.stream();

       /*
        // Filtering
        if (query.getFilter().isPresent()) {
            stream = stream.filter(person -> query.getFilter().get().test(person));
        }

        // Sorting
        if (query.getSortOrders().size() > 0) {
            stream = stream.sorted(sortComparator(query.getSortOrders()));
        }

        // Pagination
        return stream.skip(query.getOffset()).limit(query.getLimit());

        */
    }

    @Override
    protected int sizeInBackEnd(Query<Product, ProductFilter> query) {
        return (int) fetchFromBackEnd(query).count();
    }

    private static Comparator<Product> sortComparator(List<QuerySortOrder> sortOrders) {
        return sortOrders.stream().map(sortOrder -> {
            Comparator<Product> comparator = productFieldComparator(sortOrder.getSorted());

            if (sortOrder.getDirection() == SortDirection.DESCENDING) {
                comparator = comparator.reversed();
            }

            return comparator;
        }).reduce(Comparator::thenComparing).orElse((p1, p2) -> 0);
    }

    private static Comparator<Product> productFieldComparator(String sorted) {

        if (sorted.equals("pid")) {
            return Comparator.comparing(product -> product.getPid());
        } else if (sorted.equals("name")) {
            return Comparator.comparing(product -> product.getName());
        } else if (sorted.equals("color")) {
            return Comparator.comparing(product -> product.getColor());
        } else if (sorted.equals("description")) {
            return Comparator.comparing(product -> product.getDescription());
        } else if (sorted.equals("category")) {
            return Comparator.comparing(product -> product.getCategory());
        } else if (sorted.equals("brand")) {
            return Comparator.comparing(product -> product.getBrand());
        } else if (sorted.equals("price")) {
            return Comparator.comparing(product -> product.getWholesale_price());
        }
        return (p1, p2) -> 0;
    }
}
