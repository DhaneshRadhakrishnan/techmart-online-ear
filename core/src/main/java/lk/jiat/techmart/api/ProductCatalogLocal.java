package lk.jiat.techmart.api;

import lk.jiat.techmart.dto.ProductDTO;
import java.util.List;

public interface ProductCatalogLocal {

    List<ProductDTO> findAllActive();

    List<ProductDTO> findByCategory(String category);

    ProductDTO findBySku(String sku);

    List<ProductDTO> searchByName(String keyword);
}