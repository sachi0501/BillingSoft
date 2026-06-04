package me.astranexis.billingsoft.service;

import java.util.List;
import me.astranexis.billingsoft.dto.ProductRequest;
import me.astranexis.billingsoft.dto.ProductResponse;
import me.astranexis.billingsoft.dto.StockUpdateRequest;
import me.astranexis.billingsoft.exception.BadRequestException;
import me.astranexis.billingsoft.exception.ResourceNotFoundException;
import me.astranexis.billingsoft.model.Product;
import me.astranexis.billingsoft.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normalizeRequired(request.sku(), "sku");
        productRepository.findBySkuIgnoreCase(sku).ifPresent(product -> {
            throw new BadRequestException("Product SKU already exists: " + sku);
        });

        Product product = new Product();
        applyRequest(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(Boolean active, String search) {
        String normalizedSearch = normalizeOptional(search);
        return productRepository.findAll(Sort.by("name").ascending())
                .stream()
                .filter(product -> active == null || product.isActive() == active)
                .filter(product -> matchesProductSearch(product, normalizedSearch))
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        String sku = normalizeRequired(request.sku(), "sku");
        productRepository.findBySkuIgnoreCase(sku)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Product SKU already exists: " + sku);
                });

        applyRequest(product, request);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStock(Long id, StockUpdateRequest request) {
        Product product = findProduct(id);
        product.setStockQuantity(request.stockQuantity());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        product.setActive(false);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setSku(normalizeRequired(request.sku(), "sku"));
        product.setName(normalizeRequired(request.name(), "name"));
        product.setDescription(normalizeOptional(request.description()));
        product.setUnitPrice(request.unitPrice());
        product.setTaxRate(request.taxRate());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(request.active() == null || request.active());
    }

    private boolean matchesProductSearch(Product product, String search) {
        if (search == null) {
            return true;
        }
        String term = search.toLowerCase();
        return product.getName().toLowerCase().contains(term)
                || product.getSku().toLowerCase().contains(term);
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(field + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
