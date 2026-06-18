package lk.jiat.techmart.ejb.catalog;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import lk.jiat.techmart.api.ProductCatalogLocal;
import lk.jiat.techmart.dto.ProductDTO;
import lk.jiat.techmart.entity.Product;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Local(ProductCatalogLocal.class)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ProductCatalogBean implements ProductCatalogLocal {

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    @Override
    public List<ProductDTO> findAllActive() {
        List<Product> products = em.createNamedQuery("Product.findAllActive", Product.class)
                .getResultList();
        return toDTOList(products);
    }

    @Override
    public List<ProductDTO> findByCategory(String category) {
        List<Product> products = em.createNamedQuery("Product.findByCategory", Product.class)
                .setParameter("category", category)
                .getResultList();
        return toDTOList(products);
    }

    @Override
    public ProductDTO findBySku(String sku) {
        List<Product> results = em.createNamedQuery("Product.findBySku", Product.class)
                .setParameter("sku", sku)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : toDTO(results.get(0));
    }

    @Override
    public List<ProductDTO> searchByName(String keyword) {
        List<Product> products = em.createNamedQuery("Product.searchByName", Product.class)
                .setParameter("keyword", keyword)
                .getResultList();
        return toDTOList(products);
    }

    private List<ProductDTO> toDTOList(List<Product> products) {
        return products.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ProductDTO toDTO(Product p) {
        return new ProductDTO(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getDescription(),
                p.getCategory(),
                p.getPrice(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getUpdatedAt()
        );
    }
}