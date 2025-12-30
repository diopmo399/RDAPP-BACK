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
        sb.append("# Flyway Migration Drift Report\n\n");
        sb.append(String.format("**Generated:** %s\n\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        sb.append(String.format("**Base Ref:** `%s`\n\n", baseRef));
        sb.append(String.format("**Target Ref:** `%s`\n\n", targetRef));

        // Summary
        if (!result.hasDrifts()) {
            sb.append("## ✅ No Drifts Detected\n\n");
            sb.append("All Flyway migrations are consistent between branches.\n\n");
            return sb.toString();
        }

        sb.append("## ❌ Drifts Detected\n\n");
        sb.append(String.format("**Total Issues:** %d\n\n", result.getTotalDriftCount()));

        // Duplicates in base
        if (!result.baseDuplicates.isEmpty()) {
            sb.append("### 🔴 Duplicate Migrations in Base Branch\n\n");
            sb.append(String.format("Found %d duplicate(s) in `%s`:\n\n", result.baseDuplicates.size(), baseRef));

            for (DriftDetector.DuplicateMigration dup : result.baseDuplicates) {
                sb.append(String.format("- **%s** - %d file(s):\n", dup.version, dup.migrations.size()));
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
            sb.append("### 🔴 Duplicate Migrations in Target Branch\n\n");
            sb.append(String.format("Found %d duplicate(s) in `%s`:\n\n", result.targetDuplicates.size(), targetRef));

            for (DriftDetector.DuplicateMigration dup : result.targetDuplicates) {
                sb.append(String.format("- **%s** - %d file(s):\n", dup.version, dup.migrations.size()));
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
            sb.append("### 🟠 Behind Migrations\n\n");
            sb.append(String.format("Migrations present in `%s` but missing in `%s`:\n\n", baseRef, targetRef));

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
            sb.append("### 🟡 Diverged Migrations\n\n");
            sb.append("Migrations with same version but different content:\n\n");

            sb.append("| Migration | Base Hash | Target Hash |\n");
            sb.append("|-----------|-----------|-------------|\n");

            for (DriftDetector.DivergedMigration div : result.divergedMigrations) {
                sb.append(String.format("| `%s` | `%s` | `%s` |\n",
                        div.baseMigration.toShortString(),
                        div.baseMigration.getContentHash().substring(0, 8),
                        div.targetMigration.getContentHash().substring(0, 8)));
            }
            sb.append("\n");
        }

        // Recommendations
        sb.append("## 📋 Recommendations\n\n");

        if (!result.baseDuplicates.isEmpty() || !result.targetDuplicates.isEmpty()) {
            sb.append("- **Duplicates:** Remove duplicate migrations. Each version must be unique.\n");
        }

        if (!result.behindMigrations.isEmpty()) {
            sb.append(String.format("- **Behind:** Merge or rebase `%s` with `%s` to get missing migrations.\n", targetRef, baseRef));
        }

        if (!result.divergedMigrations.isEmpty()) {
            sb.append("- **Diverged:** Content mismatch detected. Never modify existing migrations. Create a new migration instead.\n");
        }

        return sb.toString();
    }

    /**
     * Affiche le rapport dans la console (logs Maven).
     */
    public void printToConsole() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FLYWAY MIGRATION DRIFT REPORT");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Base Ref:   " + baseRef);
        System.out.println("Target Ref: " + targetRef);
        System.out.println();

        if (!result.hasDrifts()) {
            System.out.println("✅ No drifts detected. All migrations are consistent.");
            System.out.println("=".repeat(80));
            return;
        }

        System.out.println("❌ DRIFTS DETECTED: " + result.getTotalDriftCount() + " issue(s)");
        System.out.println();

        // Duplicates in base
        if (!result.baseDuplicates.isEmpty()) {
            System.out.println("🔴 DUPLICATE MIGRATIONS IN BASE (" + baseRef + "):");
            for (DriftDetector.DuplicateMigration dup : result.baseDuplicates) {
                System.out.println("  - " + dup.version + " (" + dup.migrations.size() + " files)");
                for (FlywayMigration m : dup.migrations) {
                    System.out.println("    • " + m.getFileName());
                }
            }
            System.out.println();
        }

        // Duplicates in target
        if (!result.targetDuplicates.isEmpty()) {
            System.out.println("🔴 DUPLICATE MIGRATIONS IN TARGET (" + targetRef + "):");
            for (DriftDetector.DuplicateMigration dup : result.targetDuplicates) {
                System.out.println("  - " + dup.version + " (" + dup.migrations.size() + " files)");
                for (FlywayMigration m : dup.migrations) {
                    System.out.println("    • " + m.getFileName());
                }
            }
            System.out.println();
        }

        // Behind
        if (!result.behindMigrations.isEmpty()) {
            System.out.println("🟠 BEHIND MIGRATIONS (present in base, missing in target):");
            for (FlywayMigration m : result.behindMigrations) {
                System.out.println("  - " + m.toShortString() + " (hash: " + m.getContentHash().substring(0, 8) + ")");
            }
            System.out.println();
        }

        // Diverged
        if (!result.divergedMigrations.isEmpty()) {
            System.out.println("🟡 DIVERGED MIGRATIONS (same version, different content):");
            for (DriftDetector.DivergedMigration div : result.divergedMigrations) {
                System.out.println("  - " + div.baseMigration.toShortString());
                System.out.println("    Base:   " + div.baseMigration.getContentHash().substring(0, 16));
                System.out.println("    Target: " + div.targetMigration.getContentHash().substring(0, 16));
            }
            System.out.println();
        }

        System.out.println("=".repeat(80));
    }
}
