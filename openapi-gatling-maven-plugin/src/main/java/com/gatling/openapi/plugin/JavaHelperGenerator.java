package com.gatling.openapi.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de helper Java pour Gatling.
 * Crée une classe utilitaire pour charger les feeders depuis les datasets CSV générés.
 */
public class JavaHelperGenerator {

    public void generate(File outputDir, File endpointsDir) throws IOException {
        File javaFile = new File(outputDir, "GatlingFeeders.java");

        List<String> csvFiles = findCsvFiles(endpointsDir);

        StringBuilder content = new StringBuilder();
        content.append("package helpers;\n\n");
        content.append("import io.gatling.javaapi.core.*;\n");
        content.append("import static io.gatling.javaapi.core.CoreDsl.*;\n\n");
        content.append("/**\n");
        content.append(" * Classe helper générée automatiquement pour charger les feeders Gatling\n");
        content.append(" * depuis les datasets générés à partir du contrat OpenAPI.\n");
        content.append(" * \n");
        content.append(" * Cette classe fournit des méthodes statiques pour accéder facilement aux feeders.\n");
        content.append(" */\n");
        content.append("public class GatlingFeeders {\n\n");

        content.append("    private GatlingFeeders() {\n");
        content.append("        // Classe utilitaire, constructeur privé\n");
        content.append("    }\n\n");

        // Générer les méthodes pour chaque feeder
        for (String csvFile : csvFiles) {
            String feederName = csvFile.replace(".csv", "")
                                      .replaceAll("[^a-zA-Z0-9]", "_");
            String methodName = toCamelCase(feederName);

            content.append("    /**\n");
            content.append("     * Feeder pour ").append(csvFile).append("\n");
            content.append("     * Mode: circular (réutilisation cyclique des données)\n");
            content.append("     * \n");
            content.append("     * @return FeederBuilder configuré pour charger ").append(csvFile).append("\n");
            content.append("     */\n");
            content.append("    public static FeederBuilder<String> ").append(methodName).append("() {\n");
            content.append("        return csv(\"target/gatling-data/endpoints/").append(csvFile).append("\").circular();\n");
            content.append("    }\n\n");
        }

        // Ajouter des méthodes utilitaires
        content.append("    /**\n");
        content.append("     * Crée un body à partir d'une session variable contenant du JSON.\n");
        content.append("     * \n");
        content.append("     * @param columnName nom de la colonne dans le feeder (par défaut: \"body\")\n");
        content.append("     * @return Body configuré pour extraire le JSON de la session\n");
        content.append("     */\n");
        content.append("    public static Body.WithString jsonBodyFrom(String columnName) {\n");
        content.append("        return StringBody(session -> session.getString(columnName));\n");
        content.append("    }\n\n");

        content.append("    /**\n");
        content.append("     * Crée un body à partir de la colonne par défaut \"body\".\n");
        content.append("     * \n");
        content.append("     * @return Body configuré pour extraire le JSON de la session\n");
        content.append("     */\n");
        content.append("    public static Body.WithString jsonBody() {\n");
        content.append("        return jsonBodyFrom(\"body\");\n");
        content.append("    }\n\n");

        content.append("    /**\n");
        content.append("     * Charge un feeder custom depuis un chemin spécifique.\n");
        content.append("     * \n");
        content.append("     * @param path chemin vers le fichier CSV\n");
        content.append("     * @return FeederBuilder configuré\n");
        content.append("     */\n");
        content.append("    public static FeederBuilder<String> customFeeder(String path) {\n");
        content.append("        return csv(path).circular();\n");
        content.append("    }\n\n");

        // Ajouter une méthode pour lister tous les feeders disponibles
        content.append("    /**\n");
        content.append("     * Retourne la liste des feeders disponibles.\n");
        content.append("     * Utile pour la documentation ou le debug.\n");
        content.append("     * \n");
        content.append("     * @return tableau des noms de feeders disponibles\n");
        content.append("     */\n");
        content.append("    public static String[] getAvailableFeeders() {\n");
        content.append("        return new String[] {\n");
        for (int i = 0; i < csvFiles.size(); i++) {
            String csvFile = csvFiles.get(i);
            content.append("            \"").append(csvFile.replace(".csv", "")).append("\"");
            if (i < csvFiles.size() - 1) {
                content.append(",\n");
            } else {
                content.append("\n");
            }
        }
        content.append("        };\n");
        content.append("    }\n");

        content.append("}\n");

        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(content.toString());
        }
    }

    /**
     * Trouve tous les fichiers CSV dans le répertoire donné.
     */
    private List<String> findCsvFiles(File directory) {
        List<String> csvFiles = new ArrayList<>();

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    csvFiles.add(file.getName());
                }
            }
        }

        return csvFiles;
    }

    /**
     * Convertit un nom avec underscores en camelCase.
     * Exemple: "get_users_by_id" -> "getUsersById"
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] parts = input.split("_");
        StringBuilder camelCase = new StringBuilder(parts[0].toLowerCase());

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                camelCase.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    camelCase.append(parts[i].substring(1).toLowerCase());
                }
            }
        }

        return camelCase.toString();
    }
}
