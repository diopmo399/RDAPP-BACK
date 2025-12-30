package com.gatling.openapi.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScalaHelperGenerator {

    public void generate(File outputDir, File endpointsDir) throws IOException {
        File scalaFile = new File(outputDir, "GatlingFeeders.scala");

        List<String> csvFiles = findCsvFiles(endpointsDir);

        StringBuilder content = new StringBuilder();
        content.append("package helpers\n\n");
        content.append("import io.gatling.core.Predef._\n");
        content.append("import io.gatling.core.feeder._\n");
        content.append("import io.gatling.core.body.StringBody\n\n");
        content.append("/**\n");
        content.append(" * Helper object généré automatiquement pour charger les feeders Gatling\n");
        content.append(" * depuis les datasets générés à partir du contrat OpenAPI\n");
        content.append(" */\n");
        content.append("object GatlingFeeders {\n\n");

        // Générer les méthodes pour chaque feeder
        for (String csvFile : csvFiles) {
            String feederName = csvFile.replace(".csv", "")
                                      .replaceAll("[^a-zA-Z0-9]", "_");

            content.append("  /**\n");
            content.append("   * Feeder pour ").append(csvFile).append("\n");
            content.append("   * Mode: circular (réutilisation cyclique des données)\n");
            content.append("   */\n");
            content.append("  def ").append(feederName).append(": RecordSeqFeederBuilder[String] = {\n");
            content.append("    csv(\"target/gatling-data/endpoints/").append(csvFile).append("\").circular\n");
            content.append("  }\n\n");
        }

        // Ajouter des méthodes utilitaires
        content.append("  /**\n");
        content.append("   * Crée un StringBody à partir d'une session variable contenant du JSON\n");
        content.append("   * @param columnName nom de la colonne dans le feeder (par défaut: \"body\")\n");
        content.append("   */\n");
        content.append("  def jsonBodyFrom(columnName: String = \"body\"): StringBody = {\n");
        content.append("    StringBody(session => session(columnName).as[String])\n");
        content.append("  }\n\n");

        content.append("  /**\n");
        content.append("   * Charge un feeder custom depuis un chemin spécifique\n");
        content.append("   * @param path chemin vers le fichier CSV\n");
        content.append("   */\n");
        content.append("  def customFeeder(path: String): RecordSeqFeederBuilder[String] = {\n");
        content.append("    csv(path).circular\n");
        content.append("  }\n");

        content.append("}\n");

        try (FileWriter writer = new FileWriter(scalaFile)) {
            writer.write(content.toString());
        }
    }

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
}
