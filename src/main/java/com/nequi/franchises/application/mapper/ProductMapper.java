package com.nequi.franchises.application.mapper;

import com.nequi.franchises.application.dto.CreateProductRequest;
import com.nequi.franchises.application.dto.ProductDto;
import com.nequi.franchises.application.dto.TopProductDto;
import com.nequi.franchises.domain.model.Product;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Component
public class ProductMapper {

    /**
     * Convert create request to entity.
     * 
     * @param request the product creation request (name, stock)
     * @param branchId the branch ID from URL path
     * @return the product entity
     */
    public Product toEntity(CreateProductRequest request, String branchId) {
        return Product.builder()
                .branchId(branchId)
                .name(request.getName())
                .stock(request.getStock())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public ProductDto toDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .branchId(product.getBranchId())
                .name(product.getName())
                .stock(product.getStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public Flux<ProductDto> toDtoFlux(Flux<Product> products) {
        return products.map(this::toDto);
    }

    public TopProductDto toTopProductDto(Product product, String branchName) {
        return TopProductDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .stock(product.getStock())
                .branchId(product.getBranchId())
                .branchName(branchName)
                .build();
    }
}
