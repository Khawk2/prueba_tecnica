package com.nequi.franchises.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Embedded document within Franchise.
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         VALUE OBJECT: BRANCH                                 ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                ║
 * ║  ROLE: Value Object dentro del Aggregate Franchise                             ║
 * ║        - NO tiene lifecycle propio                                             ║
 * ║        - NO existe fuera del contexto de Franchise                            ║
 * ║        - Su identidad es local al aggregate padre                              ║
 * ║                                                                                ║
 * ║  INVARIANTES DE DOMINIO:                                                       ║
 * ║  ────────────────────────                                                       ║
 * ║  • INV-001: Una Branch NO puede existir sin Franchise                         ║
 * ║             (embebida en documento padre)                                      ║
 * ║  • INV-002: Toda operación sobre Branch requiere cargar Franchise             ║
 * ║             (operaciones atómicas en aggregate root)                          ║
 * ║  • INV-003: Eliminación de Branch es operación sobre Franchise                ║
 * ║                                                                                ║
 * ║  NOTA: Aunque tiene ID, es para referencia interna del aggregate.           ║
 * ║        El ID de Branch NO es único globalmente (solo dentro de franchise).    ║
 * ║                                                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {
    
    private String id;
    
    private String name;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Update the branch name.
     * Controlled mutation for domain consistency.
     * 
     * @param newName the new name to set
     */
    public void updateName(String newName) {
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }
}
