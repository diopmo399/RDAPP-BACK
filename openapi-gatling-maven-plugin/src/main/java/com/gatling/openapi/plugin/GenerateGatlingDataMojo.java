package com.gatling.openapi.plugin;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.maven.model.Plugin;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Set;

@Mojo(name = "generate-gatling-data", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES)
public class GenerateGatlingDataMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "inputSpec", required = true)
    private String inputSpec;

    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/gatling-data")
    private File outputDir;

    @Parameter(property = "rows", defaultValue = "500")
    private int rows;

    @Parameter(property = "format", defaultValue = "CSV")
    private OutputFormat format;

    @Parameter(property = "seed", defaultValue = "12345")
    private long seed;

    @Parameter(property = "includeSchemas")
    private List<String> includeSchemas;

    @Parameter(property = "includePaths")
    private List<String> includePaths;

    @Parameter(property = "arraysMaxSize", defaultValue = "3")
    private int arraysMaxSize;

    @Parameter(property = "overwrite", defaultValue = "true")
    private boolean overwrite;

    @Parameter(property = "jsonColumnName", defaultValue = "body")
    private String jsonColumnName;

    @Parameter(property = "addCorrelationColumns", defaultValue = "true")
    private boolean addCorrelationColumns;

    @Parameter(property = "generateScalaHelper", defaultValue = "true")
    private boolean generateScalaHelper;

    /**
     * Langage cible pour le helper Gatling: 'scala' ou 'java'.
     * Si non spécifié, détection automatique basée sur les dépendances et plugins du projet.
     */
    @Parameter(property = "language")
    private String language;

    public enum OutputFormat {
        CSV, JSON, BOTH
    }

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("=== OpenAPI Gatling Data Generator ===");
        getLog().info("Input Spec: " + inputSpec);
        getLog().info("Output Dir: " + outputDir.getAbsolutePath());
        getLog().info("Rows: " + rows);
        getLog().info("Format: " + format);
        getLog().info("Seed: " + seed);

        try {
            // Créer les répertoires de sortie
            File schemasDir = new File(outputDir, "schemas");
            File endpointsDir = new File(outputDir, "endpoints");
            schemasDir.mkdirs();
            endpointsDir.mkdirs();

            // Charger le fichier OpenAPI
            getLog().info("Chargement du fichier OpenAPI...");
            OpenApiLoader loader = new OpenApiLoader();
            OpenAPI openAPI = loader.load(new File(inputSpec));

            if (openAPI == null) {
                throw new MojoExecutionException("Impossible de charger le fichier OpenAPI: " + inputSpec);
            }

            getLog().info("OpenAPI chargé: " + openAPI.getInfo().getTitle() + " v" + openAPI.getInfo().getVersion());

            // Créer les générateurs
            RefResolver refResolver = new RefResolver(openAPI);
            SchemaExampleGenerator exampleGenerator = new SchemaExampleGenerator(refResolver, seed, arraysMaxSize);
            EndpointDatasetGenerator endpointGenerator = new EndpointDatasetGenerator(
                openAPI, refResolver, exampleGenerator, rows, jsonColumnName, addCorrelationColumns
            );

            // Générer les datasets pour les schemas
            if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
                getLog().info("Génération des datasets pour les schemas...");
                openAPI.getComponents().getSchemas().forEach((schemaName, schema) -> {
                    if (includeSchemas == null || includeSchemas.isEmpty() || includeSchemas.contains(schemaName)) {
                        try {
                            generateSchemaDataset(schemasDir, schemaName, schema, exampleGenerator);
                        } catch (Exception e) {
                            getLog().error("Erreur lors de la génération du schema " + schemaName, e);
                        }
                    }
                });
            }

            // Générer les datasets pour les endpoints
            if (openAPI.getPaths() != null) {
                getLog().info("Génération des datasets pour les endpoints...");
                endpointGenerator.generateEndpointDatasets(endpointsDir, includePaths, format);
            }

            // Générer le helper (Scala ou Java)
            if (generateScalaHelper) {
                String targetLanguage = resolveLanguage();
                getLog().info("Langage cible détecté: " + targetLanguage);
                generateHelper(targetLanguage, outputDir, endpointsDir);
            }

            getLog().info("=== Génération terminée avec succès ===");
            getLog().info("Fichiers générés dans: " + outputDir.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Erreur lors de la génération des données Gatling", e);
        }
    }

    private void generateSchemaDataset(File schemasDir, String schemaName,
                                      io.swagger.v3.oas.models.media.Schema<?> schema,
                                      SchemaExampleGenerator exampleGenerator) throws Exception {
        getLog().debug("Génération du dataset pour le schema: " + schemaName);

        if (format == OutputFormat.CSV || format == OutputFormat.BOTH) {
            File csvFile = new File(schemasDir, schemaName + ".csv");
            CsvWriter csvWriter = new CsvWriter(csvFile);
            csvWriter.writeSchemaData(schema, rows, exampleGenerator);
            getLog().info("  - " + csvFile.getName() + " (" + rows + " lignes)");
        }

        if (format == OutputFormat.JSON || format == OutputFormat.BOTH) {
            File jsonFile = new File(schemasDir, schemaName + ".json");
            JsonWriter jsonWriter = new JsonWriter(jsonFile);
            jsonWriter.writeSchemaData(schema, rows, exampleGenerator);
            getLog().info("  - " + jsonFile.getName() + " (" + rows + " lignes)");
        }
    }

    /**
     * Résout le langage à utiliser (configuration explicite ou détection automatique).
     */
    private String resolveLanguage() throws MojoExecutionException {
        // 1. Si spécifié explicitement dans la configuration
        if (language != null && !language.trim().isEmpty()) {
            String lang = language.trim().toLowerCase();
            if ("scala".equals(lang) || "java".equals(lang)) {
                getLog().info("→ Langage spécifié explicitement: " + lang);
                return lang;
            } else {
                throw new MojoExecutionException(
                    "Langage invalide: '" + language + "'. Utilisez 'scala' ou 'java'.");
            }
        }

        // 2. Détection automatique
        getLog().info("→ Détection automatique du langage...");

        // Méthode 1: Vérifier les dépendances
        String langFromDeps = detectLanguageFromDependencies();
        if (langFromDeps != null) {
            return langFromDeps;
        }

        // Méthode 2: Vérifier les plugins Maven
        String langFromPlugins = detectLanguageFromPlugins();
        if (langFromPlugins != null) {
            return langFromPlugins;
        }

        // Méthode 3: Vérifier les répertoires sources
        String langFromDirs = detectLanguageFromSourceDirs();
        if (langFromDirs != null) {
            return langFromDirs;
        }

        // Par défaut: Scala (pour la rétrocompatibilité)
        getLog().info("→ Aucun indicateur trouvé, utilisation de Scala par défaut");
        return "scala";
    }

    /**
     * Détecte le langage via les dépendances Maven.
     */
    private String detectLanguageFromDependencies() {
        Set<Artifact> artifacts = project.getArtifacts();

        for (Artifact artifact : artifacts) {
            String groupId = artifact.getGroupId();
            String artifactId = artifact.getArtifactId();

            // Vérifier scala-library
            if ("org.scala-lang".equals(groupId) && "scala-library".equals(artifactId)) {
                getLog().info("  ✓ Scala détecté via dépendance: scala-library");
                return "scala";
            }

            // Vérifier Gatling Scala
            if ("io.gatling".equals(groupId) && artifactId.contains("scala")) {
                getLog().info("  ✓ Scala détecté via dépendance Gatling: " + artifactId);
                return "scala";
            }

            // Vérifier Gatling Java
            if ("io.gatling".equals(groupId) && (artifactId.contains("java") || artifactId.equals("gatling-javaapi"))) {
                getLog().info("  ✓ Java détecté via dépendance Gatling Java: " + artifactId);
                return "java";
            }
        }

        return null;
    }

    /**
     * Détecte le langage via les plugins Maven.
     */
    private String detectLanguageFromPlugins() {
        List<Plugin> plugins = project.getBuildPlugins();

        for (Plugin plugin : plugins) {
            String key = plugin.getGroupId() + ":" + plugin.getArtifactId();

            // Plugins Scala
            if (key.equals("net.alchim31.maven:scala-maven-plugin") ||
                key.equals("org.scala-tools:maven-scala-plugin")) {
                getLog().info("  ✓ Scala détecté via plugin: " + key);
                return "scala";
            }

            // Plugin Gatling avec exécution Scala
            if (key.equals("io.gatling:gatling-maven-plugin")) {
                // Vérifier la configuration du plugin pour détecter le langage
                // Par défaut, Gatling Maven Plugin utilise Scala
                getLog().info("  ✓ Plugin Gatling détecté (Scala par défaut)");
                return "scala";
            }
        }

        return null;
    }

    /**
     * Détecte le langage via les répertoires sources.
     */
    private String detectLanguageFromSourceDirs() {
        File scalaDir = new File(project.getBasedir(), "src/test/scala");
        File javaDir = new File(project.getBasedir(), "src/test/java");

        boolean hasScala = scalaDir.exists() && hasScalaFiles(scalaDir);
        boolean hasJava = javaDir.exists() && hasJavaFiles(javaDir);

        if (hasScala) {
            getLog().info("  ✓ Scala détecté via src/test/scala");
            return "scala";
        }

        if (hasJava) {
            getLog().info("  ✓ Java détecté via src/test/java");
            return "java";
        }

        return null;
    }

    /**
     * Vérifie si un répertoire contient des fichiers .scala.
     */
    private boolean hasScalaFiles(File dir) {
        if (!dir.isDirectory()) return false;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".scala"));
        if (files != null && files.length > 0) return true;

        // Vérifier récursivement
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                if (hasScalaFiles(subdir)) return true;
            }
        }

        return false;
    }

    /**
     * Vérifie si un répertoire contient des fichiers .java.
     */
    private boolean hasJavaFiles(File dir) {
        if (!dir.isDirectory()) return false;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".java"));
        if (files != null && files.length > 0) return true;

        // Vérifier récursivement
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                if (hasJavaFiles(subdir)) return true;
            }
        }

        return false;
    }

    /**
     * Génère le helper dans le langage cible.
     */
    private void generateHelper(String targetLanguage, File outputDir, File endpointsDir) throws Exception {
        if ("scala".equals(targetLanguage)) {
            getLog().info("Génération du helper Scala...");
            ScalaHelperGenerator scalaGenerator = new ScalaHelperGenerator();
            scalaGenerator.generate(outputDir, endpointsDir);
            getLog().info("  ✓ Fichier généré: GatlingFeeders.scala");
        } else if ("java".equals(targetLanguage)) {
            getLog().info("Génération du helper Java...");
            JavaHelperGenerator javaGenerator = new JavaHelperGenerator();
            javaGenerator.generate(outputDir, endpointsDir);
            getLog().info("  ✓ Fichier généré: GatlingFeeders.java");
        } else {
            throw new MojoExecutionException("Langage non supporté: " + targetLanguage);
        }
    }
}
