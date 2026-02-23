package com.rdapp.solutionrdapp.dashboardservice.domain.repository;

// ═══════════════════════════════════════════════════════════════
// NOUVEAU REPOSITORY — ComparaisonRepository
// ═══════════════════════════════════════════════════════════════
//
// La clé de cache d'une comparaison GitHub est :
//   produit + baseVersion + headVersion
//
// Car le diff entre deux tags Git est IMMUABLE :
//   tag "2026.S1.3" → commit A (fixe)
//   tag "2026.S2.1" → commit B (fixe)
//   → le compare A...B donnera toujours le même résultat
//
// ═══════════════════════════════════════════════════════════════

import com.rdapp.solutionrdapp.dashboardservice.domain.entity.ComparaisonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComparaisonRepository extends JpaRepository<ComparaisonEntity, Long> {

    /**
     * Lookup cache : retrouver une comparaison déjà calculée.
     *
     * @return la comparaison si elle existe, Optional.empty() sinon
     */
    Optional<ComparaisonEntity> findByProduitAndBaseVersionAndHeadVersion(
            String produit, String baseVersion, String headVersion);
}
