package com.gatling.shadow.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ShadowJarBuilder {

    private final MavenProject project;
    private final Log log;
    private final Set<String> addedEntries;
    private final Map<String, List<String>> serviceEntries;

    public ShadowJarBuilder(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
        this.addedEntries = new HashSet<>();
        this.serviceEntries = new HashMap<>();
    }

    /**
     * Construit le shadow JAR avec toutes les dépendances
     */
    public File buildShadowJar(File outputJar, Set<File> dependencies) throws MojoExecutionException {
        log.info("=== Construction du Shadow JAR ===");
        log.info("Fichier de sortie: " + outputJar.getAbsolutePath());

        try {
            // Créer le répertoire parent si nécessaire
            outputJar.getParentFile().mkdirs();

            try (JarOutputStream jos = new JarOutputStream(
                    new BufferedOutputStream(new FileOutputStream(outputJar)),
                    createManifest())) {

                // 1. Ajouter les classes de test compilées
                File testClasses = new File(project.getBuild().getTestOutputDirectory());
                if (testClasses.exists()) {
                    log.info("Ajout des classes de test...");
                    addDirectoryToJar(jos, testClasses, "");
                }

                // 2. Ajouter les classes principales compilées
                File mainClasses = new File(project.getBuild().getOutputDirectory());
                if (mainClasses.exists()) {
                    log.info("Ajout des classes principales...");
                    addDirectoryToJar(jos, mainClasses, "");
                }

                // 3. Ajouter les resources de test
                for (Object resource : project.getBuild().getTestResources()) {
                    if (resource instanceof org.apache.maven.model.Resource) {
                        File resourceDir = new File(((org.apache.maven.model.Resource) resource).getDirectory());
                        if (resourceDir.exists()) {
                            log.info("Ajout des resources de test: " + resourceDir.getName());
                            addDirectoryToJar(jos, resourceDir, "");
                        }
                    }
                }

                // 4. Ajouter toutes les dépendances
                log.info("Ajout de " + dependencies.size() + " dépendances...");
                for (File dependency : dependencies) {
                    addJarToJar(jos, dependency);
                }

                // 5. Écrire les META-INF/services fusionnés
                writeMergedServices(jos);

                log.info("Shadow JAR créé avec succès: " + outputJar.getName());
            }

            return outputJar;

        } catch (Exception e) {
            throw new MojoExecutionException("Erreur lors de la création du shadow JAR", e);
        }
    }

    /**
     * Crée le manifest pour le JAR
     */
    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();

        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(Attributes.Name.MAIN_CLASS, "io.gatling.app.Gatling");
        mainAttributes.putValue("Created-By", "Gatling Shadow Maven Plugin");
        mainAttributes.putValue("Built-By", System.getProperty("user.name"));
        mainAttributes.putValue("Build-Date", new Date().toString());

        return manifest;
    }

    /**
     * Ajoute un répertoire au JAR récursivement
     */
    private void addDirectoryToJar(JarOutputStream jos, File directory, String prefix) throws IOException {
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     try {
                         File file = path.toFile();
                         String relativePath = directory.toPath().relativize(path).toString()
                                                       .replace(File.separatorChar, '/');

                         if (prefix != null && !prefix.isEmpty()) {
                             relativePath = prefix + "/" + relativePath;
                         }

                         addFileToJar(jos, file, relativePath);
                     } catch (IOException e) {
                         log.warn("Erreur lors de l'ajout de " + path + ": " + e.getMessage());
                     }
                 });
        }
    }

    /**
     * Ajoute un fichier JAR au shadow JAR (décompresse et ajoute les entrées)
     */
    private void addJarToJar(JarOutputStream jos, File jarFile) throws IOException {
        log.debug("  - " + jarFile.getName());

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Exclure les signatures et manifests
                if (shouldExcludeEntry(entryName)) {
                    continue;
                }

                // Gérer les META-INF/services spécialement (fusion)
                if (entryName.startsWith("META-INF/services/")) {
                    collectServiceEntry(entryName, zis);
                    continue;
                }

                // Ajouter l'entrée si pas déjà présente
                if (!addedEntries.contains(entryName)) {
                    if (entry.isDirectory()) {
                        addDirectoryEntry(jos, entryName);
                    } else {
                        addStreamEntry(jos, entryName, zis);
                    }
                }
            }
        }
    }

    /**
     * Ajoute un fichier au JAR
     */
    private void addFileToJar(JarOutputStream jos, File file, String entryName) throws IOException {
        if (addedEntries.contains(entryName)) {
            return;
        }

        JarEntry jarEntry = new JarEntry(entryName);
        jarEntry.setTime(file.lastModified());

        jos.putNextEntry(jarEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                jos.write(buffer, 0, bytesRead);
            }
        }

        jos.closeEntry();
        addedEntries.add(entryName);
    }

    /**
     * Ajoute une entrée depuis un stream
     */
    private void addStreamEntry(JarOutputStream jos, String entryName, InputStream is) throws IOException {
        JarEntry jarEntry = new JarEntry(entryName);
        jos.putNextEntry(jarEntry);

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            jos.write(buffer, 0, bytesRead);
        }

        jos.closeEntry();
        addedEntries.add(entryName);
    }

    /**
     * Ajoute une entrée de répertoire
     */
    private void addDirectoryEntry(JarOutputStream jos, String entryName) throws IOException {
        if (!addedEntries.contains(entryName)) {
            JarEntry jarEntry = new JarEntry(entryName);
            jos.putNextEntry(jarEntry);
            jos.closeEntry();
            addedEntries.add(entryName);
        }
    }

    /**
     * Collecte les entrées de service pour les fusionner
     */
    private void collectServiceEntry(String entryName, InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        List<String> implementations = serviceEntries.computeIfAbsent(entryName, k -> new ArrayList<>());

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (!implementations.contains(line)) {
                    implementations.add(line);
                }
            }
        }
    }

    /**
     * Écrit les fichiers META-INF/services fusionnés
     */
    private void writeMergedServices(JarOutputStream jos) throws IOException {
        for (Map.Entry<String, List<String>> entry : serviceEntries.entrySet()) {
            String serviceName = entry.getKey();
            List<String> implementations = entry.getValue();

            if (!addedEntries.contains(serviceName)) {
                JarEntry jarEntry = new JarEntry(serviceName);
                jos.putNextEntry(jarEntry);

                PrintWriter writer = new PrintWriter(new OutputStreamWriter(jos));
                for (String impl : implementations) {
                    writer.println(impl);
                }
                writer.flush();

                jos.closeEntry();
                addedEntries.add(serviceName);
            }
        }
    }

    /**
     * Vérifie si une entrée doit être exclue
     */
    private boolean shouldExcludeEntry(String entryName) {
        // Exclure les signatures
        if (entryName.startsWith("META-INF/") &&
            (entryName.endsWith(".SF") || entryName.endsWith(".DSA") ||
             entryName.endsWith(".RSA") || entryName.endsWith(".EC"))) {
            return true;
        }

        // Exclure le manifest original (on utilise le nôtre)
        if (entryName.equals("META-INF/MANIFEST.MF")) {
            return true;
        }

        // Exclure les fichiers de module Java 9+
        if (entryName.equals("module-info.class") || entryName.startsWith("META-INF/versions/")) {
            return true;
        }

        return false;
    }
}
