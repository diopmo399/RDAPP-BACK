package com.gatling.shadow.plugin;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClasspathBuilder {

    private final MavenProject project;
    private final List<File> additionalClasspathElements;

    public ClasspathBuilder(MavenProject project) {
        this.project = project;
        this.additionalClasspathElements = new ArrayList<>();
    }

    /**
     * Ajoute un élément au classpath
     */
    public void addClasspathElement(File file) {
        if (file != null && file.exists()) {
            additionalClasspathElements.add(file);
        }
    }

    /**
     * Construit le classpath complet avec les dépendances et les classes compilées
     */
    public String buildFullClasspath(Set<File> dependencies) {
        List<String> classpathElements = new ArrayList<>();

        // 1. Classes de test compilées
        File testClasses = new File(project.getBuild().getTestOutputDirectory());
        if (testClasses.exists()) {
            classpathElements.add(testClasses.getAbsolutePath());
        }

        // 2. Classes principales compilées
        File mainClasses = new File(project.getBuild().getOutputDirectory());
        if (mainClasses.exists()) {
            classpathElements.add(mainClasses.getAbsolutePath());
        }

        // 3. Resources de test
        for (Object resource : project.getBuild().getTestResources()) {
            if (resource instanceof org.apache.maven.model.Resource) {
                File resourceDir = new File(((org.apache.maven.model.Resource) resource).getDirectory());
                if (resourceDir.exists()) {
                    classpathElements.add(resourceDir.getAbsolutePath());
                }
            }
        }

        // 4. Dépendances
        for (File dependency : dependencies) {
            classpathElements.add(dependency.getAbsolutePath());
        }

        // 5. Éléments additionnels
        for (File element : additionalClasspathElements) {
            classpathElements.add(element.getAbsolutePath());
        }

        return String.join(File.pathSeparator, classpathElements);
    }

    /**
     * Construit le classpath pour le shadow jar (juste le jar)
     */
    public String buildShadowJarClasspath(File shadowJar) {
        return shadowJar.getAbsolutePath();
    }

    /**
     * Récupère tous les éléments du classpath sous forme de liste
     */
    public List<File> getClasspathElements(Set<File> dependencies) {
        List<File> elements = new ArrayList<>();

        File testClasses = new File(project.getBuild().getTestOutputDirectory());
        if (testClasses.exists()) {
            elements.add(testClasses);
        }

        File mainClasses = new File(project.getBuild().getOutputDirectory());
        if (mainClasses.exists()) {
            elements.add(mainClasses);
        }

        elements.addAll(dependencies);
        elements.addAll(additionalClasspathElements);

        return elements;
    }
}
