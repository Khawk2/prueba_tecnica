package com.nequi.franchises.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Domain model for Product.
 * MongoDB Document: products collection.
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                      REFERENCED ENTITY: PRODUCT                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                ║
 * ║  ROLE: Entity referenciada (NO parte del aggregate Franchise)                  ║
 * ║        - Tiene lifecycle propio                                                ║
 * ║        - Referencia a Branch por ID (NO contenida)                          ║
 * ║        - Colección separada por alta cardinalidad                             ║
 * ║                                                                                ║
 * ║  INVARIANTES DE DOMINIO:                                                       ║
 * ║  ────────────────────────                                                       ║
 * ║  • INV-001: Un Product REQUIERE referenciar una Branch válida en creación     ║
 * ║             (validación: branch debe existir en franchise)                  ║
 * ║  • INV-002: Product NO es parte del aggregate Franchise                      ║
 * ║             (referencia débil, eventual consistency)                          ║
 * ║  • INV-003: Modificación de stock/nombre NO requiere validar franchise      ║
 * ║             (operación directa por ID)                                         ║
 * ║                                                                                ║
 * ║  DISEÑO: Colección separada porque:                                            ║
 * ║  ─────────────────────────────────────                                          ║
 * ║  • Alta cardinalidad: miles-millones de products                              ║
 * ║  • Queries independientes: find by stock, find by name                         ║
 * ║  • Movimiento entre branches: cambiar branchId                                 ║
 * ║  • Evitar documento Franchise gigante con productos embebidos                   ║
 * ║                                                                                ║
 * ║  INDEX: branch_id (fast lookups), stock (top products queries)                 ║
 * ║                                                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {
    
    @Id
    private String id;
    
    @Field("branch_id")
    @Indexed  // Index for fast lookups by branch
    private String branchId;
    
    private String name;
    
    @Indexed  // Index for sorting by stock (top products)
    private Integer stock;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Update the product name.
     * 
     * @param newName the new name to set
     */
    public void updateName(String newName) {
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Update the product stock.
     * 
     * @param newStock the new stock quantity
     */
    public void updateStock(Integer newStock) {
        this.stock = newStock;
        this.updatedAt = LocalDateTime.now();
    }
}
