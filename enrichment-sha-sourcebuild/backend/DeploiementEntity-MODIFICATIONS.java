// ══════════════════════════════════════════════════════════
// MODIFICATIONS À FAIRE dans DeploiementEntity.java
// ══════════════════════════════════════════════════════════
//
// Ajouter ce champ dans la classe DeploiementEntity :
//
//     @Column(name = "tag_commit_sha", length = 40)
//     private String tagCommitSha;
//
// Placement : après le champ timestamp (ligne 37)
//
// Résultat final :
//
//     @Entity
//     @IdClass(DeploiementPrimaryKey.class)
//     @Data
//     @AllArgsConstructor
//     @NoArgsConstructor
//     public class DeploiementEntity {
//
//         @Id
//         private String environnement;
//
//         @Id
//         private String organisation;
//
//         @Id
//         private String produit;
//
//         @Id
//         private String version;
//
//         private ZonedDateTime timestamp;
//
//         @Column(name = "tag_commit_sha", length = 40)       // ← AJOUT
//         private String tagCommitSha;                         // ← AJOUT
//
//         @ManyToOne(optional = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
//         @JoinColumn(
//             name = "comparaison_id",
//             foreignKey = @ForeignKey(name = "FK_deploiement__comparaison")
//         )
//         private ComparaisonEntity comparaison;
//     }
