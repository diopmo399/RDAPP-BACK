package com.example.flyway.drift.report;

import com.example.flyway.drift.detector.DriftDetector;
import com.example.flyway.drift.model.FlywayMigration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Génère un rapport Markdown des drifts détectés.
 */
public class DriftReport {

    private final DriftDetector.DriftResult result;
    private final String baseRef;
    private final String targetRef;

    public DriftReport(DriftDetector.DriftResult result, String baseRef, String targetRef) {
        this.result = result;
        this.baseRef = baseRef;
        this.targetRef = targetRef;
    }

    /**
     * Génère le rapport Markdown et l'écrit dans un fichier.
     *
     * @param outputFile Fichier de sortie
     * @throws IOException En cas d'erreur d'écriture
     */
    public void generateMarkdownReport(File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(generateMarkdown());
        }
    }

    /**
     * Génère le contenu Markdown du rapport.
     *
     * @return Contenu Markdown
     */
    public String generateMarkdown() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("# Rapport de Drift des Migrations Flyway\n\n");
        sb.append(String.format("**Généré le:** %s\n\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        sb.append(String.format("**Branche de base:** `%s`\n\n", baseRef));
        sb.append(String.format("**Branche cible:** `%s`\n\n", targetRef));

        // Summary
        if (!result.hasDrifts()) {
            sb.append("## ✅ Aucun Drift Détecté\n\n");
            sb.append("Toutes les migrations Flyway sont cohérentes entre les branches.\n\n");
            return sb.toString();
        }

        sb.append("## ❌ Drifts Détectés\n\n");
        sb.append(String.format("**Nombre total de problèmes:** %d\n\n", result.getTotalDriftCount()));

        // Duplicates in base
        if (!result.baseDuplicates.isEmpty()) {
            sb.append("### 🔴 Migrations Dupliquées dans la Branche de Base\n\n");
            sb.append(String.format("Trouvé %d doublon(s) dans `%s`:\n\n", result.baseDuplicates.size(), baseRef));

            for (DriftDetector.DuplicateMigration dup : result.baseDuplicates) {
                sb.append(String.format("- **%s** - %d fichier(s):\n", dup.version, dup.migrations.size()));
                for (FlywayMigration migration : dup.migrations) {
                    sb.append(String.format("  - `%s` (hash: `%s`)\n",
                            migration.getFileName(),
                            migration.getContentHash().substring(0, 8)));
                }
                sb.append("\n");
            }
        }

        // Duplicates in target
        if (!result.targetDuplicates.isEmpty()) {
            sb.append("### 🔴 Migrations Dupliquées dans la Branche Cible\n\n");
            sb.append(String.format("Trouvé %d doublon(s) dans `%s`:\n\n", result.targetDuplicates.size(), targetRef));

            for (DriftDetector.DuplicateMigration dup : result.targetDuplicates) {
                sb.append(String.format("- **%s** - %d fichier(s):\n", dup.version, dup.migrations.size()));
                for (FlywayMigration migration : dup.migrations) {
                    sb.append(String.format("  - `%s` (hash: `%s`)\n",
                            migration.getFileName(),
                            migration.getContentHash().substring(0, 8)));
                }
                sb.append("\n");
            }
        }

        // Behind migrations
        if (!result.behindMigrations.isEmpty()) {
            sb.append("### 🟠 Migrations Manquantes (En Retard)\n\n");
            sb.append(String.format("Migrations présentes dans `%s` mais absentes de `%s`:\n\n", baseRef, targetRef));

            sb.append("| Migration | Type | Hash |\n");
            sb.append("|-----------|------|------|\n");

            for (FlywayMigration migration : result.behindMigrations) {
                sb.append(String.format("| `%s` | %s | `%s` |\n",
                        migration.toShortString(),
                        migration.getType(),
                        migration.getContentHash().substring(0, 8)));
            }
            sb.append("\n");
        }

        // Diverged migrations
        if (!result.divergedMigrations.isEmpty()) {
            sb.append("### 🟡 Migrations Divergentes\n\n");
            sb.append("Migrations avec la même version mais un contenu différent:\n\n");

            sb.append("| Migration | Hash Base | Hash Cible |\n");
            sb.append("|-----------|-----------|------------|\n");

            for (DriftDetector.DivergedMigration div : result.divergedMigrations) {
                sb.append(String.format("| `%s` | `%s` | `%s` |\n",
                        div.baseMigration.toShortString(),
                        div.baseMigration.getContentHash().substring(0, 8),
                        div.targetMigration.getContentHash().substring(0, 8)));
            }
            sb.append("\n");
        }

        // Recommendations
        sb.append("## 📋 Recommandations\n\n");

        if (!result.baseDuplicates.isEmpty() || !result.targetDuplicates.isEmpty()) {
            sb.append("- **Doublons:** Supprimez les migrations dupliquées. Chaque version doit être unique.\n");
        }

        if (!result.behindMigrations.isEmpty()) {
            sb.append(String.format("- **En retard:** Fusionnez ou rebasez `%s` avec `%s` pour récupérer les migrations manquantes.\n", targetRef, baseRef));
        }

        if (!result.divergedMigrations.isEmpty()) {
            sb.append("- **Divergentes:** Contenu différent détecté. Ne modifiez jamais une migration existante. Créez plutôt une nouvelle migration.\n");
        }

        return sb.toString();
    }

    /**
     * Affiche le rapport dans la console (logs Maven).
     */
    public void printToConsole() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("RAPPORT DE DRIFT DES MIGRATIONS FLYWAY");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Branche de base:   " + baseRef);
        System.out.println("Branche cible:     " + targetRef);
        System.out.println();

        if (!result.hasDrifts()) {
            System.out.println("✅ Aucun drift détecté. Toutes les migrations sont cohérentes.");
            System.out.println("=".repeat(80));
            return;
        }

        System.out.println("❌ DRIFTS DÉTECTÉS: " + result.getTotalDriftCount() + " problème(s)");
        System.out.println();

        // Duplicates in base
        if (!result.baseDuplicates.isEmpty()) {
            System.out.println("🔴 MIGRATIONS DUPLIQUÉES DANS LA BASE (" + baseRef + "):");
            for (DriftDetector.DuplicateMigration dup : result.baseDuplicates) {
                System.out.println("  - " + dup.version + " (" + dup.migrations.size() + " fichiers)");
                for (FlywayMigration m : dup.migrations) {
                    System.out.println("    • " + m.getFileName());
                }
            }
            System.out.println();
        }

        // Duplicates in target
        if (!result.targetDuplicates.isEmpty()) {
            System.out.println("🔴 MIGRATIONS DUPLIQUÉES DANS LA CIBLE (" + targetRef + "):");
            for (DriftDetector.DuplicateMigration dup : result.targetDuplicates) {
                System.out.println("  - " + dup.version + " (" + dup.migrations.size() + " fichiers)");
                for (FlywayMigration m : dup.migrations) {
                    System.out.println("    • " + m.getFileName());
                }
            }
            System.out.println();
        }

        // Behind
        if (!result.behindMigrations.isEmpty()) {
            System.out.println("🟠 MIGRATIONS MANQUANTES (présentes dans la base, absentes de la cible):");
            for (FlywayMigration m : result.behindMigrations) {
                System.out.println("  - " + m.toShortString() + " (hash: " + m.getContentHash().substring(0, 8) + ")");
            }
            System.out.println();
        }

        // Diverged
        if (!result.divergedMigrations.isEmpty()) {
            System.out.println("🟡 MIGRATIONS DIVERGENTES (même version, contenu différent):");
            for (DriftDetector.DivergedMigration div : result.divergedMigrations) {
                System.out.println("  - " + div.baseMigration.toShortString());
                System.out.println("    Base:  " + div.baseMigration.getContentHash().substring(0, 16));
                System.out.println("    Cible: " + div.targetMigration.getContentHash().substring(0, 16));
            }
            System.out.println();
        }

        System.out.println("=".repeat(80));
    }
}
