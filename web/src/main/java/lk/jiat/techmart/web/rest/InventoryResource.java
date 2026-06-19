package lk.jiat.techmart.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.techmart.api.InventoryManagerLocal;
import lk.jiat.techmart.dto.InventoryStatusDTO;

@Path("/inventory")
public class InventoryResource {

    @EJB
    private InventoryManagerLocal inventoryManager;

    @GET
    @Path("/{sku}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStock(@PathParam("sku") String sku) {
        InventoryStatusDTO status = inventoryManager.checkStock(sku);
        if (status == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(status).build();
    }
}