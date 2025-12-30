package com.gatling.shadow.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "test", defaultPhase = LifecyclePhase.INTEGRATION_TEST,
      requiresDependencyResolution = ResolutionScope.TEST)
public class RunGatlingMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "simulationClass")
    private String simulationClass;

    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/gatling")
    private File outputDir;

    @Parameter(property = "resultsDir", defaultValue = "${project.build.directory}/gatling/results")
    private File resultsDir;

    @Parameter(property = "reportsDir", defaultValue = "${project.build.directory}/gatling/reports")
    private File reportsDir;

    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError;

    @Parameter(property = "fork", defaultValue = "true")
    private boolean fork;

    @Parameter(property = "jvmArgs")
    private List<String> jvmArgs;

    @Parameter(property = "systemProps")
    private Map<String, String> systemProps;

    @Parameter(property = "env")
    private Map<String, String> env;

    @Parameter(property = "runDescription")
    private String runDescription;

    @Parameter(property = "nonInteractive", defaultValue = "true")
    private boolean nonInteractive;

    @Parameter(property = "runAll", defaultValue = "false")
    private boolean runAll;

    @Parameter(property = "includes")
    private List<String> includes;

    @Parameter(property = "excludes")
    private List<String> excludes;

    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Exécution de Gatling ignorée (skip=true)");
            return;
        }

        getLog().info("=== Exécution de Gatling ===");

        try {
            // Créer les répertoires de sortie
            resultsDir.mkdirs();
            reportsDir.mkdirs();

            // Récupérer le shadow JAR (généré par le goal "shadow")
            File shadowJar = getShadowJar();

            if (!shadowJar.exists()) {
                throw new MojoExecutionException(
                    "Shadow JAR introuvable: " + shadowJar.getAbsolutePath() + "\n" +
                    "Exécutez d'abord le goal 'shadow' ou utilisez 'shadow-test'");
            }

            getLog().info("Shadow JAR: " + shadowJar.getAbsolutePath());

            // Déterminer quelle(s) simulation(s) exécuter
            List<String> simulationsToRun = determineSimulationsToRun();

            if (simulationsToRun.isEmpty()) {
                throw new MojoExecutionException("Aucune simulation Gatling trouvée ou spécifiée");
            }

            // Exécuter les simulations
            ProcessRunner runner = new ProcessRunner(getLog(), failOnError);

            for (String simulation : simulationsToRun) {
                getLog().info("");
                getLog().info("=== Exécution de la simulation: " + simulation + " ===");
                getLog().info("");

                int exitCode = runner.runGatling(
                    shadowJar,
                    simulation,
                    resultsDir,
                    reportsDir,
                    runDescription,
                    jvmArgs,
                    systemProps != null ? systemProps : new HashMap<>(),
                    env != null ? env : new HashMap<>(),
                    nonInteractive
                );

                if (exitCode != 0 && !runAll) {
                    // Arrêter après la première erreur si runAll=false
                    break;
                }
            }

            getLog().info("");
            getLog().info("=== Résultats ===");
            getLog().info("Rapports: " + reportsDir.getAbsolutePath());
            getLog().info("");

        } catch (Exception e) {
            throw new MojoExecutionException("Erreur lors de l'exécution de Gatling", e);
        }
    }

    /**
     * Récupère le shadow JAR (depuis les propriétés ou le chemin par défaut)
     */
    private File getShadowJar() {
        // Vérifier si le chemin est stocké dans les propriétés du projet
        String shadowJarPath = project.getProperties().getProperty("gatling.shadowJar");
        if (shadowJarPath != null) {
            return new File(shadowJarPath);
        }

        // Sinon, utiliser le chemin par défaut
        File runnerDir = new File(project.getBuild().getDirectory(), "gatling-runner");
        String jarName = project.getArtifactId() + "-" + project.getVersion() + "-gatling-all.jar";
        return new File(runnerDir, jarName);
    }

    /**
     * Détermine quelles simulations exécuter
     */
    private List<String> determineSimulationsToRun() throws IOException {
        List<String> simulations = new java.util.ArrayList<>();

        if (simulationClass != null && !simulationClass.isEmpty()) {
            // Simulation spécifique fournie
            simulations.add(simulationClass);
            getLog().info("Simulation spécifiée: " + simulationClass);
        } else {
            // Scanner les simulations disponibles
            File testClasses = new File(project.getBuild().getTestOutputDirectory());
            SimulationScanner scanner = new SimulationScanner(testClasses, getLog(), includes, excludes);
            simulations = scanner.scanSimulations();

            if (!runAll && !simulations.isEmpty()) {
                // Exécuter seulement la première simulation si runAll=false
                String first = simulations.get(0);
                getLog().info("Plusieurs simulations trouvées, exécution de la première: " + first);
                getLog().info("Utilisez -DrunAll=true pour exécuter toutes les simulations");
                simulations = java.util.Collections.singletonList(first);
            }
        }

        return simulations;
    }
}
