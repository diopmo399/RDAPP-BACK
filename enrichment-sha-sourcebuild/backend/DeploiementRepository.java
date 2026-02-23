package com.rdapp.solutionrdapp.dashboardservice.data.repository;

import com.rdapp.solutionrdapp.dashboardservice.data.entity.deploiement.DeploiementEntity;
import com.rdapp.solutionrdapp.dashboardservice.data.entity.deploiement.DeploiementPrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface DeploiementRepository extends JpaRepository<DeploiementEntity, DeploiementPrimaryKey> {

    // ── Existant (upsert) ──
    Optional<DeploiementEntity> findByProduitAndVersionAndEnvironnementAndTimestamp(
            String produit, String version, String environnement, ZonedDateTime timestamp);

    // ── AJOUT : par produit (pour le mapper) ──
    List<DeploiementEntity> findByProduit(String produit);

    // ── AJOUT : par produit + organisation ──
    List<DeploiementEntity> findByProduitAndOrganisation(String produit, String organisation);

    // ── AJOUT : trouver par SHA (retrouver le build d'un rc/release) ──
    List<DeploiementEntity> findByProduitAndTagCommitSha(String produit, String tagCommitSha);

    // ── AJOUT : dernier déploiement par env (pour les envs actuels) ──
    Optional<DeploiementEntity> findTopByProduitAndEnvironnementOrderByTimestampDesc(
            String produit, String environnement);

    // ── AJOUT : par produit + version ──
    Optional<DeploiementEntity> findTopByProduitAndVersion(String produit, String version);
}
