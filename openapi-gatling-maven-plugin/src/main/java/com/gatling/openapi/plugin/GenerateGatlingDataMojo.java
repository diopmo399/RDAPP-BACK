package com.gatling.openapi.plugin;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

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

            // Générer le helper Scala
            if (generateScalaHelper) {
                getLog().info("Génération du helper Scala...");
                ScalaHelperGenerator scalaGenerator = new ScalaHelperGenerator();
                scalaGenerator.generate(outputDir, endpointsDir);
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
}
