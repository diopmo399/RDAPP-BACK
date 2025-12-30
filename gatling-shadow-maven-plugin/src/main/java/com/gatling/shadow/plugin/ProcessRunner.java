package com.gatling.shadow.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessRunner {

    private final Log log;
    private final boolean failOnError;

    public ProcessRunner(Log log, boolean failOnError) {
        this.log = log;
        this.failOnError = failOnError;
    }

    /**
     * Execute Gatling via ProcessBuilder avec le classpath du shadow JAR
     */
    public int runGatling(File shadowJar, String simulationClass, File resultsDir,
                          File reportsDir, String runDescription, List<String> jvmArgs,
                          Map<String, String> systemProps, Map<String, String> env,
                          boolean nonInteractive) throws MojoExecutionException {

        List<String> command = buildGatlingCommand(shadowJar, simulationClass, resultsDir,
                                                   reportsDir, runDescription, jvmArgs, systemProps, nonInteractive);

        log.info("=== Exécution de Gatling ===");
        log.info("Commande: " + String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));

            // Ajouter les variables d'environnement
            if (env != null && !env.isEmpty()) {
                pb.environment().putAll(env);
            }

            // Rediriger la sortie standard et d'erreur
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Lire et logger la sortie
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMsg = "Gatling a échoué avec le code de sortie: " + exitCode;
                log.error(errorMsg);
                if (failOnError) {
                    throw new MojoExecutionException(errorMsg);
                }
            } else {
                log.info("Gatling exécuté avec succès");
            }

            return exitCode;

        } catch (Exception e) {
            String errorMsg = "Erreur lors de l'exécution de Gatling: " + e.getMessage();
            log.error(errorMsg, e);
            if (failOnError) {
                throw new MojoExecutionException(errorMsg, e);
            }
            return 1;
        }
    }

    /**
     * Construit la commande complète pour exécuter Gatling
     */
    private List<String> buildGatlingCommand(File shadowJar, String simulationClass,
                                             File resultsDir, File reportsDir,
                                             String runDescription, List<String> jvmArgs,
                                             Map<String, String> systemProps, boolean nonInteractive) {

        List<String> command = new ArrayList<>();

        // 1. Java executable
        String javaHome = System.getProperty("java.home");
        String javaCmd = javaHome + File.separator + "bin" + File.separator + "java";
        command.add(javaCmd);

        // 2. JVM arguments
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            command.addAll(jvmArgs);
        }

        // 3. System properties
        if (systemProps != null && !systemProps.isEmpty()) {
            for (Map.Entry<String, String> entry : systemProps.entrySet()) {
                command.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        // 4. Classpath
        command.add("-cp");
        command.add(shadowJar.getAbsolutePath());

        // 5. Main class Gatling
        command.add("io.gatling.app.Gatling");

        // 6. Arguments Gatling
        if (simulationClass != null && !simulationClass.isEmpty()) {
            command.add("-s");
            command.add(simulationClass);
        }

        if (resultsDir != null) {
            command.add("-rf");
            command.add(resultsDir.getAbsolutePath());
        }

        if (reportsDir != null) {
            command.add("-rsf");
            command.add(reportsDir.getAbsolutePath());
        }

        if (runDescription != null && !runDescription.isEmpty()) {
            command.add("-rd");
            command.add(runDescription);
        }

        if (nonInteractive) {
            command.add("-nr");
        }

        return command;
    }

    /**
     * Exécute une commande générique
     */
    public int runCommand(List<String> command, File workingDir, Map<String, String> env)
            throws MojoExecutionException {

        log.debug("Exécution de la commande: " + String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);

            if (workingDir != null) {
                pb.directory(workingDir);
            }

            if (env != null && !env.isEmpty()) {
                pb.environment().putAll(env);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0 && failOnError) {
                throw new MojoExecutionException("La commande a échoué avec le code: " + exitCode);
            }

            return exitCode;

        } catch (Exception e) {
            if (failOnError) {
                throw new MojoExecutionException("Erreur lors de l'exécution de la commande", e);
            }
            return 1;
        }
    }
}
