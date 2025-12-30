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
import java.util.Set;

@Mojo(name = "shadow-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST,
      requiresDependencyResolution = ResolutionScope.TEST)
public class ShadowAndRunGatlingMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    // Paramètres pour la création du shadow JAR
    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/gatling-runner")
    private File outputDir;

    @Parameter(property = "shadowJarName")
    private String shadowJarName;

    // Paramètres pour l'exécution de Gatling
    @Parameter(property = "simulationClass")
    private String simulationClass;

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
            getLog().info("Shadow-test ignoré (skip=true)");
            return;
        }

        getLog().info("=== Gatling Shadow-Test ===");
        getLog().info("1. Création du shadow JAR");
        getLog().info("2. Exécution de Gatling");
        getLog().info("");

        File shadowJar = null;

        try {
            // ÉTAPE 1: Créer le shadow JAR
            shadowJar = createShadowJar();

            // ÉTAPE 2: Exécuter Gatling
            runGatling(shadowJar);

            getLog().info("");
            getLog().info("=== Shadow-Test terminé avec succès ===");

        } catch (Exception e) {
            throw new MojoExecutionException("Erreur lors du shadow-test", e);
        }
    }

    /**
     * Crée le shadow JAR
     */
    private File createShadowJar() throws MojoExecutionException {
        getLog().info("--- Création du shadow JAR ---");

        try {
            outputDir.mkdirs();

            String jarName = shadowJarName;
            if (jarName == null || jarName.isEmpty()) {
                jarName = project.getArtifactId() + "-" + project.getVersion() + "-gatling-all.jar";
            }

            File shadowJar = new File(outputDir, jarName);

            DependencyResolver dependencyResolver = new DependencyResolver(project, getLog());
            Set<File> dependencies = dependencyResolver.resolveGatlingDependencies();

            if (dependencies.isEmpty()) {
                getLog().warn("Aucune dépendance Gatling trouvée !");
            }

            ShadowJarBuilder builder = new ShadowJarBuilder(project, getLog());
            File generatedJar = builder.buildShadowJar(shadowJar, dependencies);

            long sizeInMB = generatedJar.length() / (1024 * 1024);
            getLog().info("Shadow JAR créé: " + generatedJar.getName() + " (" + sizeInMB + " MB)");
            getLog().info("");

            return generatedJar;

        } catch (Exception e) {
            throw new MojoExecutionException("Erreur lors de la création du shadow JAR", e);
        }
    }

    /**
     * Exécute Gatling avec le shadow JAR
     */
    private void runGatling(File shadowJar) throws MojoExecutionException {
        getLog().info("--- Exécution de Gatling ---");

        try {
            resultsDir.mkdirs();
            reportsDir.mkdirs();

            List<String> simulationsToRun = determineSimulationsToRun();

            if (simulationsToRun.isEmpty()) {
                throw new MojoExecutionException("Aucune simulation Gatling trouvée ou spécifiée");
            }

            ProcessRunner runner = new ProcessRunner(getLog(), failOnError);

            for (String simulation : simulationsToRun) {
                getLog().info("");
                getLog().info("Simulation: " + simulation);

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
                    break;
                }
            }

            getLog().info("");
            getLog().info("Rapports: " + reportsDir.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Erreur lors de l'exécution de Gatling", e);
        }
    }

    /**
     * Détermine quelles simulations exécuter
     */
    private List<String> determineSimulationsToRun() throws IOException {
        List<String> simulations = new java.util.ArrayList<>();

        if (simulationClass != null && !simulationClass.isEmpty()) {
            simulations.add(simulationClass);
            getLog().info("Simulation spécifiée: " + simulationClass);
        } else {
            File testClasses = new File(project.getBuild().getTestOutputDirectory());
            SimulationScanner scanner = new SimulationScanner(testClasses, getLog(), includes, excludes);
            simulations = scanner.scanSimulations();

            if (!runAll && !simulations.isEmpty()) {
                String first = simulations.get(0);
                getLog().info("Exécution de: " + first);
                if (simulations.size() > 1) {
                    getLog().info("(" + (simulations.size() - 1) + " autre(s) simulation(s) disponible(s), utilisez -DrunAll=true)");
                }
                simulations = java.util.Collections.singletonList(first);
            }
        }

        return simulations;
    }
}
