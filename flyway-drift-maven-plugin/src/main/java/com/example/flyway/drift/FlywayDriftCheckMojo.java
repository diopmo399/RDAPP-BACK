package com.example.flyway.drift;

import com.example.flyway.drift.detector.DriftDetector;
import com.example.flyway.drift.git.GitFileReader;
import com.example.flyway.drift.model.FlywayMigration;
import com.example.flyway.drift.parser.MigrationParser;
import com.example.flyway.drift.report.DriftReport;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Maven Mojo pour détecter les drifts de migrations Flyway entre branches Git.
 *
 * @author Flyway Drift Plugin Team
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class FlywayDriftCheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Branche de base pour la comparaison (ex: origin/main, origin/master).
     * Si vide, détection automatique de origin/main ou origin/master.
     */
    @Parameter(property = "flyway.drift.baseRef", defaultValue = "")
    private String baseRef;

    /**
     * Branche cible pour la comparaison (par défaut: HEAD).
     */
    @Parameter(property = "flyway.drift.targetRef", defaultValue = "HEAD")
    private String targetRef;

    /**
     * Chemin vers le répertoire des migrations Flyway.
     */
    @Parameter(property = "flyway.drift.migrationsPath", defaultValue = "src/main/resources/db/migration")
    private String migrationsPath;

    /**
     * Fail le build si des migrations sont manquantes (behind).
     */
    @Parameter(property = "flyway.drift.failIfBehind", defaultValue = "true")
    private boolean failIfBehind;

    /**
     * Fail le build si des migrations sont divergentes (même version, contenu différent).
     */
    @Parameter(property = "flyway.drift.failIfDiverged", defaultValue = "true")
    private boolean failIfDiverged;

    /**
     * Fail le build si des migrations sont dupliquées.
     */
    @Parameter(property = "flyway.drift.failOnDuplicates", defaultValue = "true")
    private boolean failOnDuplicates;

    /**
     * Générer un rapport Markdown dans target/.
     */
    @Parameter(property = "flyway.drift.generateReport", defaultValue = "true")
    private boolean generateReport;

    /**
     * Nom du fichier de rapport.
     */
    @Parameter(property = "flyway.drift.reportFileName", defaultValue = "flyway-drift-report.md")
    private String reportFileName;

    /**
     * Skip l'exécution du plugin.
     */
    @Parameter(property = "flyway.drift.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Effectue un git fetch avant la vérification pour s'assurer que les branches distantes sont à jour.
     * Recommandé pour éviter les faux positifs.
     */
    @Parameter(property = "flyway.drift.fetchBeforeCheck", defaultValue = "true")
    private boolean fetchBeforeCheck;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Flyway drift check skipped.");
            return;
        }

        getLog().info("========================================");
        getLog().info("Flyway Migration Drift Check");
        getLog().info("========================================");

        File baseDir = project.getBasedir();

        try (GitFileReader gitReader = new GitFileReader(baseDir)) {

            // Effectuer un git fetch si activé
            if (fetchBeforeCheck) {
                performGitFetch(gitReader);
            }

            // Déterminer la branche de base si non spécifiée
            String resolvedBaseRef = resolveBaseRef(gitReader);
            String resolvedTargetRef = targetRef;

            getLog().info("Base Ref:       " + resolvedBaseRef);
            getLog().info("Target Ref:     " + resolvedTargetRef);
            getLog().info("Migrations Path: " + migrationsPath);
            getLog().info("");

            // Vérifier que les refs existent
            validateRefs(gitReader, resolvedBaseRef, resolvedTargetRef);

            // Lire les fichiers de migration depuis Git
            getLog().info("Reading migrations from base ref...");
            Map<String, String> baseFiles = gitReader.listMigrationFiles(resolvedBaseRef, migrationsPath);
            getLog().info("Found " + baseFiles.size() + " migration file(s) in base.");

            getLog().info("Reading migrations from target ref...");
            Map<String, String> targetFiles = gitReader.listMigrationFiles(resolvedTargetRef, migrationsPath);
            getLog().info("Found " + targetFiles.size() + " migration file(s) in target.");
            getLog().info("");

            // Parser les migrations
            MigrationParser parser = new MigrationParser(migrationsPath);
            List<FlywayMigration> baseMigrations = parser.parseMigrations(baseFiles);
            List<FlywayMigration> targetMigrations = parser.parseMigrations(targetFiles);

            getLog().info("Parsed " + baseMigrations.size() + " migration(s) from base.");
            getLog().info("Parsed " + targetMigrations.size() + " migration(s) from target.");
            getLog().info("");

            // Détecter les drifts
            getLog().info("Analyzing drifts...");
            DriftDetector detector = new DriftDetector(baseMigrations, targetMigrations);
            DriftDetector.DriftResult result = detector.detectDrifts();

            // Afficher le rapport dans la console
            DriftReport report = new DriftReport(result, resolvedBaseRef, resolvedTargetRef);
            report.printToConsole();

            // Générer le rapport Markdown
            if (generateReport) {
                File reportFile = new File(project.getBuild().getDirectory(), reportFileName);
                reportFile.getParentFile().mkdirs();
                report.generateMarkdownReport(reportFile);
                getLog().info("");
                getLog().info("Report generated: " + reportFile.getAbsolutePath());
            }

            // Fail le build si nécessaire
            if (result.hasDrifts()) {
                handleDrifts(result);
            } else {
                getLog().info("");
                getLog().info("✅ No drifts detected. Build can proceed.");
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Git error: " + e.getMessage(), e);
        }
    }

    /**
     * Effectue un git fetch pour mettre à jour les branches distantes.
     */
    private void performGitFetch(GitFileReader gitReader) {
        getLog().info("Fetching from remote repository...");

        boolean success = gitReader.fetchFromRemoteSafe();

        if (success) {
            getLog().info("✓ Successfully fetched latest changes from origin.");
        } else {
            getLog().warn("⚠ Could not fetch from remote (offline mode or no remote configured).");
            getLog().warn("  Continuing with local repository state...");
        }
        getLog().info("");
    }

    /**
     * Résout la branche de base (détection automatique si non spécifiée).
     */
    private String resolveBaseRef(GitFileReader gitReader) throws MojoExecutionException {
        if (baseRef != null && !baseRef.isEmpty()) {
            return baseRef;
        }

        // Détection automatique
        try {
            String detected = gitReader.detectMainBranch();
            getLog().info("Auto-detected base branch: " + detected);
            return detected;
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Cannot auto-detect base branch. Please specify <baseRef> in plugin configuration.", e);
        }
    }

    /**
     * Valide que les refs existent dans le repository.
     */
    private void validateRefs(GitFileReader gitReader, String base, String target) throws MojoFailureException {
        if (!gitReader.refExists(base)) {
            throw new MojoFailureException("Base ref does not exist: " + base +
                    "\n\nHint: If running in CI, ensure fetch-depth is set to 0 in GitHub Actions checkout.");
        }

        if (!gitReader.refExists(target)) {
            throw new MojoFailureException("Target ref does not exist: " + target);
        }
    }

    /**
     * Gère les drifts détectés et fail le build si nécessaire.
     */
    private void handleDrifts(DriftDetector.DriftResult result) throws MojoFailureException {
        boolean shouldFail = false;
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("\n❌ FLYWAY MIGRATION DRIFT DETECTED\n\n");

        if (failOnDuplicates && (!result.baseDuplicates.isEmpty() || !result.targetDuplicates.isEmpty())) {
            shouldFail = true;
            errorMessage.append("🔴 Duplicate migrations found.\n");
        }

        if (failIfBehind && !result.behindMigrations.isEmpty()) {
            shouldFail = true;
            errorMessage.append("🟠 Behind migrations detected (missing in target branch).\n");
        }

        if (failIfDiverged && !result.divergedMigrations.isEmpty()) {
            shouldFail = true;
            errorMessage.append("🟡 Diverged migrations detected (same version, different content).\n");
        }

        if (shouldFail) {
            errorMessage.append("\nSee report above for details.\n");
            errorMessage.append("\nTo fix:\n");
            errorMessage.append("  - Duplicates: Remove duplicate migration files.\n");
            errorMessage.append("  - Behind: Merge or rebase with base branch.\n");
            errorMessage.append("  - Diverged: Never modify existing migrations. Create a new migration instead.\n");

            throw new MojoFailureException(errorMessage.toString());
        }
    }
}
