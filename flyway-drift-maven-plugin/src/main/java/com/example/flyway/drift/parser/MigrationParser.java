package com.example.flyway.drift.parser;

import com.example.flyway.drift.model.FlywayMigration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parse les fichiers de migration Flyway.
 */
public class MigrationParser {

    private final String migrationsPath;

    public MigrationParser(String migrationsPath) {
        this.migrationsPath = migrationsPath;
    }

    /**
     * Parse une liste de fichiers et retourne des objets FlywayMigration.
     *
     * @param filesWithHash Map<fileName, contentHash>
     * @return Liste de FlywayMigration
     */
    public List<FlywayMigration> parseMigrations(Map<String, String> filesWithHash) {
        List<FlywayMigration> migrations = new ArrayList<>();

        for (Map.Entry<String, String> entry : filesWithHash.entrySet()) {
            String fileName = entry.getKey();
            String contentHash = entry.getValue();

            // Vérifier si c'est un fichier de migration valide
            if (!isValidMigrationFile(fileName)) {
                continue;
            }

            try {
                String filePath = migrationsPath + "/" + fileName;
                FlywayMigration migration = new FlywayMigration(fileName, filePath, contentHash);
                migrations.add(migration);
            } catch (IllegalArgumentException e) {
                // Fichier ignoré (nom invalide)
                System.err.println("Warning: Invalid migration filename ignored: " + fileName);
            }
        }

        // Trier les migrations par version
        migrations.sort(FlywayMigration::compareTo);

        return migrations;
    }

    /**
     * Vérifie si un fichier est un fichier de migration Flyway valide.
     *
     * @param fileName Nom du fichier
     * @return true si valide
     */
    private boolean isValidMigrationFile(String fileName) {
        if (!fileName.endsWith(".sql")) {
            return false;
        }

        // Versioned: V<version>__<description>.sql
        if (fileName.matches("^V\\d+(?:[._]\\d+)*__.+\\.sql$")) {
            return true;
        }

        // Repeatable: R__<description>.sql
        if (fileName.matches("^R__.+\\.sql$")) {
            return true;
        }

        return false;
    }
}
