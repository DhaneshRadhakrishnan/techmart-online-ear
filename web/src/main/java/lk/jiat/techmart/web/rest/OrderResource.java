package lk.jiat.techmart.web.rest;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.techmart.api.InsufficientStockException;
import lk.jiat.techmart.api.OrderProcessingLocal;
import lk.jiat.techmart.dto.OrderRequestDTO;
import lk.jiat.techmart.dto.OrderResultDTO;

@RequestScoped
@Path("/orders")
public class OrderResource {

    @EJB
    private OrderProcessingLocal orderProcessing;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response placeOrder(OrderRequestDTO request) {
        try {
            OrderResultDTO result = orderProcessing.placeOrder(request);
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (EJBTransactionRolledbackException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InsufficientStockException) {
                InsufficientStockException ise = (InsufficientStockException) cause;
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorPayload(ise.getMessage())).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorPayload("Transaction failed: " + e.getMessage())).build();
        } catch (InsufficientStockException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorPayload(e.getMessage())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorPayload(e.getMessage())).build();
        }
    }

    public static class ErrorPayload {
        public String error;
        public ErrorPayload(String error) {
            this.error = error;
        }
    }
}