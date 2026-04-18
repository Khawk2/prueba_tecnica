package com.nequi.franchises.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model for Franchise.
 * MongoDB Document: franchises collection.
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         AGGREGATE ROOT: FRANCHISE                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                ║
 * ║  ROLE: Aggregate Root - Única entidad con lifecycle propio y repositorio      ║
 * ║                                                                                ║
 * ║  INVARIANTES DE DOMINIO:                                                       ║
 * ║  ────────────────────────                                                       ║
 * ║  • INV-001: Una Branch NO puede existir fuera de un Franchise                 ║
 * ║             (Branch es Value Object embebido)                                   ║
 * ║  • INV-002: Franchise garantiza consistencia de todas sus Branches            ║
 * ║  • INV-003: Eliminación de Franchise implica eliminación de Branches          ║
 * ║             (cascade en documento embebido)                                   ║
 * ║                                                                                ║
 * ║  DISEÑO: Branches embebidas porque:                                            ║
 * ║  ─────────────────────────────────                                              ║
 * ║  • Baja cardinalidad (< 1000 por franchise)                                    ║
 * ║  • Acceso siempre dentro de contexto de franchise                              ║
 * ║  • Operaciones atómicas franchise+branches                                      ║
 * ║  • Mejor performance de lectura (sin joins)                                     ║
 * ║                                                                                ║
 * ║  CLEAN ARCHITECTURE: @Getter (no @Data) para inmutabilidad por defecto       ║
 * ║                      Mutaciones solo vía métodos de dominio explícitos        ║
 * ║                                                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "franchises")
public class Franchise {
    
    @Id
    private String id;
    
    private String name;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Builder.Default
    @Field("branches")
    private List<Branch> branches = new ArrayList<>();
    
    /**
     * Version for optimistic locking.
     * MongoDB uses this to detect concurrent modifications.
     */
    @Version
    private Long version;
    
    /**
     * Update the franchise name.
     * Controlled mutation method for domain integrity.
     * 
     * @param newName the new name to set
     */
    public void updateName(String newName) {
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Add a branch to this franchise.
     * Controlled mutation for domain consistency.
     * 
     * @param branch the branch to add
     */
    public void addBranch(Branch branch) {
        if (this.branches == null) {
            this.branches = new ArrayList<>();
        }
        this.branches.add(branch);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Remove a branch from this franchise by ID.
     * 
     * @param branchId the ID of branch to remove
     * @return true if removed, false if not found
     */
    public boolean removeBranch(String branchId) {
        if (this.branches == null) {
            return false;
        }
        boolean removed = this.branches.removeIf(b -> branchId.equals(b.getId()));
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }
}
