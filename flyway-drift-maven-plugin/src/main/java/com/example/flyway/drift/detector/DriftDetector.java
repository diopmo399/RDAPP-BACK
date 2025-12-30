package com.example.flyway.drift.detector;

import com.example.flyway.drift.model.FlywayMigration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Détecte les drifts entre deux ensembles de migrations Flyway.
 */
public class DriftDetector {

    private final List<FlywayMigration> baseMigrations;
    private final List<FlywayMigration> targetMigrations;

    public DriftDetector(List<FlywayMigration> baseMigrations, List<FlywayMigration> targetMigrations) {
        this.baseMigrations = baseMigrations;
        this.targetMigrations = targetMigrations;
    }

    /**
     * Détecte tous les types de drifts.
     *
     * @return Résultat de la détection
     */
    public DriftResult detectDrifts() {
        DriftResult result = new DriftResult();

        // 1. Détecter les duplicates dans chaque branche
        result.baseDuplicates = detectDuplicates(baseMigrations);
        result.targetDuplicates = detectDuplicates(targetMigrations);

        // 2. Détecter les migrations manquantes (behind)
        result.behindMigrations = detectBehind();

        // 3. Détecter les divergences (même version, hash différent)
        result.divergedMigrations = detectDiverged();

        return result;
    }

    /**
     * Détecte les migrations en double (même version) dans une liste.
     *
     * @param migrations Liste de migrations
     * @return Liste des migrations dupliquées
     */
    private List<DuplicateMigration> detectDuplicates(List<FlywayMigration> migrations) {
        List<DuplicateMigration> duplicates = new ArrayList<>();

        // Grouper par version (pour les versioned) ou par fileName (pour les repeatable)
        Map<String, List<FlywayMigration>> grouped = migrations.stream()
                .collect(Collectors.groupingBy(m -> {
                    if (m.isVersioned()) {
                        return "V" + m.getVersion();
                    } else {
                        return "R__" + m.getDescription();
                    }
                }));

        // Vérifier les doublons
        for (Map.Entry<String, List<FlywayMigration>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.add(new DuplicateMigration(entry.getKey(), entry.getValue()));
            }
        }

        return duplicates;
    }

    /**
     * Détecte les migrations présentes dans base mais absentes de target (behind).
     *
     * @return Liste des migrations manquantes
     */
    private List<FlywayMigration> detectBehind() {
        List<FlywayMigration> behind = new ArrayList<>();

        // Créer une map des migrations target par version/nom
        Map<String, FlywayMigration> targetMap = createMigrationMap(targetMigrations);

        for (FlywayMigration baseMigration : baseMigrations) {
            String key = getMigrationKey(baseMigration);

            if (!targetMap.containsKey(key)) {
                behind.add(baseMigration);
            }
        }

        return behind;
    }

    /**
     * Détecte les migrations avec même version mais contenu différent (diverged).
     *
     * @return Liste des migrations divergentes
     */
    private List<DivergedMigration> detectDiverged() {
        List<DivergedMigration> diverged = new ArrayList<>();

        Map<String, FlywayMigration> baseMap = createMigrationMap(baseMigrations);
        Map<String, FlywayMigration> targetMap = createMigrationMap(targetMigrations);

        for (Map.Entry<String, FlywayMigration> entry : baseMap.entrySet()) {
            String key = entry.getKey();
            FlywayMigration baseMigration = entry.getValue();

            if (targetMap.containsKey(key)) {
                FlywayMigration targetMigration = targetMap.get(key);

                // Comparer les hash
                if (!baseMigration.getContentHash().equals(targetMigration.getContentHash())) {
                    diverged.add(new DivergedMigration(baseMigration, targetMigration));
                }
            }
        }

        return diverged;
    }

    /**
     * Crée une map des migrations par clé unique.
     *
     * @param migrations Liste de migrations
     * @return Map<key, migration>
     */
    private Map<String, FlywayMigration> createMigrationMap(List<FlywayMigration> migrations) {
        Map<String, FlywayMigration> map = new HashMap<>();

        for (FlywayMigration migration : migrations) {
            String key = getMigrationKey(migration);
            map.put(key, migration);
        }

        return map;
    }

    /**
     * Retourne une clé unique pour une migration.
     *
     * @param migration Migration
     * @return Clé (version pour versioned, fileName pour repeatable)
     */
    private String getMigrationKey(FlywayMigration migration) {
        if (migration.isVersioned()) {
            return "V" + migration.getVersion();
        } else {
            return migration.getFileName();
        }
    }

    /**
     * Résultat de la détection de drifts.
     */
    public static class DriftResult {
        public List<DuplicateMigration> baseDuplicates = new ArrayList<>();
        public List<DuplicateMigration> targetDuplicates = new ArrayList<>();
        public List<FlywayMigration> behindMigrations = new ArrayList<>();
        public List<DivergedMigration> divergedMigrations = new ArrayList<>();

        public boolean hasDrifts() {
            return !baseDuplicates.isEmpty() ||
                   !targetDuplicates.isEmpty() ||
                   !behindMigrations.isEmpty() ||
                   !divergedMigrations.isEmpty();
        }

        public int getTotalDriftCount() {
            return baseDuplicates.size() +
                   targetDuplicates.size() +
                   behindMigrations.size() +
                   divergedMigrations.size();
        }
    }

    /**
     * Migration dupliquée.
     */
    public static class DuplicateMigration {
        public final String version;
        public final List<FlywayMigration> migrations;

        public DuplicateMigration(String version, List<FlywayMigration> migrations) {
            this.version = version;
            this.migrations = migrations;
        }
    }

    /**
     * Migration divergente (même version, hash différent).
     */
    public static class DivergedMigration {
        public final FlywayMigration baseMigration;
        public final FlywayMigration targetMigration;

        public DivergedMigration(FlywayMigration baseMigration, FlywayMigration targetMigration) {
            this.baseMigration = baseMigration;
            this.targetMigration = targetMigration;
        }
    }
}
