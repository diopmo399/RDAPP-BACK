package com.rdapp.solutionrdapp.dashboardservice.domain.repository;

// ═══════════════════════════════════════════════════════════════
// AJOUT dans DeploiementRepository.java existant
// ═══════════════════════════════════════════════════════════════

import com.rdapp.solutionrdapp.dashboardservice.domain.entity.DeploiementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface DeploiementRepository extends JpaRepository<DeploiementEntity, Long> {

    // ── AJOUTER pour upsert lors de l'ingestion GHA ──
    Optional<DeploiementEntity> findByProduitAndVersionAndEnvironnementAndTimestamp(
            String produit, String version, String environnement, ZonedDateTime timestamp);
}
