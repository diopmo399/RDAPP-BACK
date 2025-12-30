package com.gatling.shadow.plugin;

import org.apache.maven.plugin.logging.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SimulationScanner {

    private final File testClassesDir;
    private final Log log;
    private final List<String> includes;
    private final List<String> excludes;

    public SimulationScanner(File testClassesDir, Log log, List<String> includes, List<String> excludes) {
        this.testClassesDir = testClassesDir;
        this.log = log;
        this.includes = includes != null ? includes : new ArrayList<>();
        this.excludes = excludes != null ? excludes : new ArrayList<>();
    }

    /**
     * Scanne le répertoire des classes de test pour trouver les simulations Gatling
     */
    public List<String> scanSimulations() throws IOException {
        List<String> simulations = new ArrayList<>();

        if (!testClassesDir.exists() || !testClassesDir.isDirectory()) {
            log.warn("Répertoire de classes de test introuvable: " + testClassesDir.getAbsolutePath());
            return simulations;
        }

        log.info("Scan des simulations dans: " + testClassesDir.getAbsolutePath());

        try (Stream<Path> paths = Files.walk(testClassesDir.toPath())) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".class"))
                 .forEach(path -> {
                     try {
                         String className = getClassName(testClassesDir.toPath(), path);
                         if (isSimulation(path.toFile()) && matchesFilters(className)) {
                             simulations.add(className);
                             log.info("  - Simulation trouvée: " + className);
                         }
                     } catch (IOException e) {
                         log.debug("Erreur lors de l'analyse de " + path + ": " + e.getMessage());
                     }
                 });
        }

        if (simulations.isEmpty()) {
            log.warn("Aucune simulation Gatling trouvée !");
        } else {
            log.info("Total simulations trouvées: " + simulations.size());
        }

        return simulations;
    }

    /**
     * Vérifie si une classe est une simulation Gatling
     */
    private boolean isSimulation(File classFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(classFile)) {
            ClassReader reader = new ClassReader(fis);
            SimulationClassVisitor visitor = new SimulationClassVisitor();
            reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.isSimulation();
        }
    }

    /**
     * Extrait le nom de classe complet à partir du chemin
     */
    private String getClassName(Path baseDir, Path classFile) {
        Path relativePath = baseDir.relativize(classFile);
        String className = relativePath.toString()
                                      .replace(File.separatorChar, '.')
                                      .replace('/', '.')
                                      .replace(".class", "");
        return className;
    }

    /**
     * Vérifie si la classe correspond aux filtres includes/excludes
     */
    private boolean matchesFilters(String className) {
        // Si excludes matche, on exclut
        for (String exclude : excludes) {
            if (className.matches(exclude.replace("*", ".*"))) {
                return false;
            }
        }

        // Si includes est vide, on inclut tout
        if (includes.isEmpty()) {
            return true;
        }

        // Sinon, vérifier si includes matche
        for (String include : includes) {
            if (className.matches(include.replace("*", ".*"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * ClassVisitor pour détecter si une classe est une simulation Gatling
     */
    private static class SimulationClassVisitor extends ClassVisitor {
        private boolean isSimulation = false;

        public SimulationClassVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                         String superName, String[] interfaces) {
            // Vérifier si la classe étend io.gatling.core.scenario.Simulation
            if (superName != null && superName.equals("io/gatling/core/scenario/Simulation")) {
                isSimulation = true;
            }

            // Vérifier aussi les noms de classe se terminant par "Simulation"
            if (name != null && name.endsWith("Simulation")) {
                // Double vérification avec le superName
                if (superName != null &&
                    (superName.contains("Simulation") || superName.contains("gatling"))) {
                    isSimulation = true;
                }
            }

            super.visit(version, access, name, signature, superName, interfaces);
        }

        public boolean isSimulation() {
            return isSimulation;
        }
    }
}
