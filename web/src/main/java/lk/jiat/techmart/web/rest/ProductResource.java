package lk.jiat.techmart.web.rest;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.techmart.api.ProductCatalogLocal;
import lk.jiat.techmart.dto.ProductDTO;

import java.util.List;

@RequestScoped
@Path("/products")
public class ProductResource {

    @EJB
    private ProductCatalogLocal productCatalog;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProducts(@QueryParam("category") String category,
                                 @QueryParam("search") String search) {
        List<ProductDTO> results;
        if (search != null && !search.isBlank()) {
            results = productCatalog.searchByName(search);
        } else if (category != null && !category.isBlank()) {
            results = productCatalog.findByCategory(category);
        } else {
            results = productCatalog.findAllActive();
        }
        return Response.ok(results).build();
    }
}