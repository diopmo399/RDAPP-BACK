package com.gatling.shadow.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DependencyResolver {

    private final MavenProject project;
    private final Log log;

    public DependencyResolver(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
    }

    /**
     * Résout toutes les dépendances du projet (test + runtime)
     * nécessaires pour Gatling
     */
    public Set<File> resolveGatlingDependencies() {
        Set<File> dependencies = new LinkedHashSet<>();

        // Récupérer tous les artifacts du projet
        Set<Artifact> artifacts = project.getArtifacts();

        if (artifacts == null || artifacts.isEmpty()) {
            log.warn("Aucune dépendance trouvée dans le projet");
            return dependencies;
        }

        log.info("Résolution des dépendances Gatling...");

        for (Artifact artifact : artifacts) {
            // Inclure les dépendances test et runtime
            String scope = artifact.getScope();
            if (isIncludedScope(scope)) {
                File file = artifact.getFile();
                if (file != null && file.exists()) {
                    dependencies.add(file);
                    log.debug("  - " + artifact.getGroupId() + ":" +
                             artifact.getArtifactId() + ":" +
                             artifact.getVersion() + " (" + scope + ")");
                }
            }
        }

        log.info("Total dépendances résolues: " + dependencies.size());
        return dependencies;
    }

    /**
     * Filtre les dépendances Gatling spécifiques
     */
    public Set<File> filterGatlingDependencies(Set<File> allDependencies) {
        Set<File> gatlingDeps = new LinkedHashSet<>();
        Set<Artifact> artifacts = project.getArtifacts();

        for (Artifact artifact : artifacts) {
            if (isGatlingDependency(artifact)) {
                File file = artifact.getFile();
                if (file != null && file.exists()) {
                    gatlingDeps.add(file);
                }
            }
        }

        return gatlingDeps;
    }

    /**
     * Vérifie si un artifact est une dépendance Gatling
     */
    private boolean isGatlingDependency(Artifact artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();

        // Dépendances Gatling
        if (groupId.startsWith("io.gatling")) {
            return true;
        }

        // Dépendances Scala nécessaires
        if (groupId.equals("org.scala-lang") ||
            groupId.equals("org.scala-lang.modules")) {
            return true;
        }

        // Dépendances communes nécessaires
        if (artifactId.contains("netty") ||
            artifactId.contains("akka") ||
            artifactId.contains("jackson") ||
            artifactId.contains("logback") ||
            artifactId.contains("slf4j")) {
            return true;
        }

        return false;
    }

    /**
     * Vérifie si le scope doit être inclus
     */
    private boolean isIncludedScope(String scope) {
        if (scope == null) {
            return true;
        }

        switch (scope.toLowerCase()) {
            case "compile":
            case "runtime":
            case "test":
                return true;
            case "provided":
            case "system":
            default:
                return false;
        }
    }

    /**
     * Récupère la liste des fichiers de dépendances sous forme de String paths
     */
    public List<String> getDependencyPaths() {
        return resolveGatlingDependencies().stream()
            .map(File::getAbsolutePath)
            .collect(Collectors.toList());
    }

    /**
     * Construit le classpath complet pour Gatling
     */
    public String buildClasspath() {
        List<String> paths = getDependencyPaths();

        // Ajouter les classes de test du projet
        File testClasses = new File(project.getBuild().getTestOutputDirectory());
        if (testClasses.exists()) {
            paths.add(0, testClasses.getAbsolutePath());
        }

        // Ajouter les classes principales si nécessaire
        File mainClasses = new File(project.getBuild().getOutputDirectory());
        if (mainClasses.exists()) {
            paths.add(0, mainClasses.getAbsolutePath());
        }

        return String.join(File.pathSeparator, paths);
    }
}
