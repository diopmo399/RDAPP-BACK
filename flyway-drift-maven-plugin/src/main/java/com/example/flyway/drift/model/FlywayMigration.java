package com.example.flyway.drift.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Représente une migration Flyway avec son type, version, description et hash.
 */
public class FlywayMigration implements Comparable<FlywayMigration> {

    private static final Pattern VERSIONED_PATTERN = Pattern.compile("^V(\\d+(?:[._]\\d+)*)__(.+)\\.sql$");
    private static final Pattern REPEATABLE_PATTERN = Pattern.compile("^R__(.+)\\.sql$");

    private final String fileName;
    private final MigrationType type;
    private final String version;  // null for repeatable
    private final String description;
    private final String contentHash;
    private final String filePath;

    public enum MigrationType {
        VERSIONED,
        REPEATABLE
    }

    public FlywayMigration(String fileName, String filePath, String contentHash) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.contentHash = contentHash;

        // Parse filename to extract type, version, description
        Matcher versionedMatcher = VERSIONED_PATTERN.matcher(fileName);
        Matcher repeatableMatcher = REPEATABLE_PATTERN.matcher(fileName);

        if (versionedMatcher.matches()) {
            this.type = MigrationType.VERSIONED;
            this.version = normalizeVersion(versionedMatcher.group(1));
            this.description = versionedMatcher.group(2);
        } else if (repeatableMatcher.matches()) {
            this.type = MigrationType.REPEATABLE;
            this.version = null;
            this.description = repeatableMatcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid Flyway migration filename: " + fileName);
        }
    }

    /**
     * Normalise la version (remplace _ et . par un séparateur unique).
     * Ex: V1_2_3 → 1.2.3, V1.2.3 → 1.2.3
     */
    private String normalizeVersion(String rawVersion) {
        return rawVersion.replace('_', '.');
    }

    public String getFileName() {
        return fileName;
    }

    public MigrationType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isVersioned() {
        return type == MigrationType.VERSIONED;
    }

    public boolean isRepeatable() {
        return type == MigrationType.REPEATABLE;
    }

    /**
     * Compare deux migrations par version (pour les versioned).
     * Les repeatable ne sont pas comparables entre elles.
     */
    @Override
    public int compareTo(FlywayMigration other) {
        if (this.type != other.type) {
            // Versioned avant Repeatable
            return this.type == MigrationType.VERSIONED ? -1 : 1;
        }

        if (this.isVersioned()) {
            return compareVersions(this.version, other.version);
        }

        // Pour les repeatable, trier par description
        return this.description.compareTo(other.description);
    }

    /**
     * Compare deux versions sémantiques.
     * Ex: 1.2.3 < 1.10.0
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlywayMigration that = (FlywayMigration) o;
        return Objects.equals(fileName, that.fileName) &&
               type == that.type &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, type, version);
    }

    @Override
    public String toString() {
        if (isVersioned()) {
            return String.format("V%s__%s (hash: %s)", version, description, contentHash.substring(0, 8));
        } else {
            return String.format("R__%s (hash: %s)", description, contentHash.substring(0, 8));
        }
    }

    /**
     * Retourne une représentation courte pour les logs.
     */
    public String toShortString() {
        if (isVersioned()) {
            return String.format("V%s__%s", version, description);
        } else {
            return String.format("R__%s", description);
        }
    }
}
