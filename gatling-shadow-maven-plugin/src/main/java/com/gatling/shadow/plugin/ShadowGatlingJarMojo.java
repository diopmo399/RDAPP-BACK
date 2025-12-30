package com.gatling.shadow.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Set;

@Mojo(name = "shadow", defaultPhase = LifecyclePhase.PACKAGE,
      requiresDependencyResolution = ResolutionScope.TEST)
public class ShadowGatlingJarMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/gatling-runner")
    private File outputDir;

    @Parameter(property = "shadowJarName")
    private String shadowJarName;

    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Génération du shadow JAR ignorée (skip=true)");
            return;
        }

        getLog().info("=== Gatling Shadow JAR Maven Plugin ===");
        getLog().info("Projet: " + project.getArtifactId());
        getLog().info("Version: " + project.getVersion());

        try {
            // Créer le répertoire de sortie
            outputDir.mkdirs();

            // Déterminer le nom du shadow JAR
            String jarName = shadowJarName;
            if (jarName == null || jarName.isEmpty()) {
                jarName = project.getArtifactId() + "-" + project.getVersion() + "-gatling-all.jar";
            }

            File shadowJar = new File(outputDir, jarName);

            // Résoudre les dépendances
            getLog().info("Résolution des dépendances...");
            DependencyResolver dependencyResolver = new DependencyResolver(project, getLog());
            Set<File> dependencies = dependencyResolver.resolveGatlingDependencies();

            if (dependencies.isEmpty()) {
                getLog().warn("Aucune dépendance Gatling trouvée !");
                getLog().warn("Assurez-vous que votre projet contient les dépendances Gatling nécessaires.");
            }

            // Construire le shadow JAR
            ShadowJarBuilder builder = new ShadowJarBuilder(project, getLog());
            File generatedJar = builder.buildShadowJar(shadowJar, dependencies);

            // Afficher le résultat
            long sizeInMB = generatedJar.length() / (1024 * 1024);
            getLog().info("");
            getLog().info("=== Shadow JAR généré avec succès ===");
            getLog().info("Fichier: " + generatedJar.getAbsolutePath());
            getLog().info("Taille: " + sizeInMB + " MB");
            getLog().info("");
            getLog().info("Pour exécuter Gatling:");
            getLog().info("  java -jar " + generatedJar.getAbsolutePath() +
                         " -s <YourSimulationClass>");
            getLog().info("");

            // Stocker le chemin du JAR dans les propriétés du projet
            // pour pouvoir l'utiliser dans le goal "test"
            project.getProperties().setProperty("gatling.shadowJar", generatedJar.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Erreur lors de la génération du shadow JAR", e);
        }
    }
}
