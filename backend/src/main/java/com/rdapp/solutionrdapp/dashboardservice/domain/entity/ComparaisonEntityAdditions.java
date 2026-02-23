package com.rdapp.solutionrdapp.dashboardservice.domain.entity;

// ═══════════════════════════════════════════════════════════════
// MODIFICATIONS À FAIRE dans ComparaisonEntity.java
// ═══════════════════════════════════════════════════════════════
//
// Ajouter ces 3 colonnes pour supporter le cache des comparaisons :
//
//     @Column(name = "produit", length = 200)
//     private String produit;
//
//     @Column(name = "base_version", length = 100)
//     private String baseVersion;
//
//     @Column(name = "head_version", length = 100)
//     private String headVersion;
//
// + un index unique pour le lookup rapide :
//
//     @Table(name = "comparaison", uniqueConstraints = {
//         @UniqueConstraint(
//             name = "uk_comparaison_cache",
//             columnNames = {"produit", "base_version", "head_version"}
//         )
//     })
//
// ═══════════════════════════════════════════════════════════════
//
// Changeset Liquibase à ajouter :
//
//   - changeSet:
//       id: X-add-comparaison-cache-columns
//       author: dylan
//       changes:
//         - addColumn:
//             tableName: comparaison
//             columns:
//               - column: {name: produit, type: VARCHAR(200)}
//               - column: {name: base_version, type: VARCHAR(100)}
//               - column: {name: head_version, type: VARCHAR(100)}
//         - createIndex:
//             tableName: comparaison
//             indexName: idx_comparaison_cache
//             unique: true
//             columns:
//               - {name: produit}
//               - {name: base_version}
//               - {name: head_version}
//
// ═══════════════════════════════════════════════════════════════

import javax.persistence.*;

/**
 * Champs existants (garder) :
 *   - id, status, aheadBy, behindBy, totalCommits,
 *     filesChanged, additions, deletions, commits
 *
 * Champs AJOUTÉS pour le cache :
 *   - produit      → nom du repo GitHub
 *   - baseVersion  → tag de base (ancien)
 *   - headVersion  → tag de head (nouveau)
 *
 * La combinaison (produit, baseVersion, headVersion) est UNIQUE
 * car le diff entre deux tags Git est immuable.
 */
public class ComparaisonEntityAdditions {
    // Ce fichier est un guide — les modifications sont à faire
    // directement dans ton ComparaisonEntity.java existant.
}
