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
            getLog().info("Vérification des drifts Flyway ignorée.");
            return;
        }

        getLog().info("========================================");
        getLog().info("Vérification des Drifts de Migrations Flyway");
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

            getLog().info("Branche de base:       " + resolvedBaseRef);
            getLog().info("Branche cible:         " + resolvedTargetRef);
            getLog().info("Chemin des migrations: " + migrationsPath);
            getLog().info("");

            // Vérifier que les refs existent
            validateRefs(gitReader, resolvedBaseRef, resolvedTargetRef);

            // Lire les fichiers de migration depuis Git
            getLog().info("Lecture des migrations depuis la branche de base...");
            Map<String, String> baseFiles = gitReader.listMigrationFiles(resolvedBaseRef, migrationsPath);
            getLog().info("Trouvé " + baseFiles.size() + " fichier(s) de migration dans la base.");

            getLog().info("Lecture des migrations depuis la branche cible...");
            Map<String, String> targetFiles = gitReader.listMigrationFiles(resolvedTargetRef, migrationsPath);
            getLog().info("Trouvé " + targetFiles.size() + " fichier(s) de migration dans la cible.");
            getLog().info("");

            // Parser les migrations
            MigrationParser parser = new MigrationParser(migrationsPath);
            List<FlywayMigration> baseMigrations = parser.parseMigrations(baseFiles);
            List<FlywayMigration> targetMigrations = parser.parseMigrations(targetFiles);

            getLog().info("Analysé " + baseMigrations.size() + " migration(s) depuis la base.");
            getLog().info("Analysé " + targetMigrations.size() + " migration(s) depuis la cible.");
            getLog().info("");

            // Détecter les drifts
            getLog().info("Analyse des drifts...");
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
                getLog().info("Rapport généré: " + reportFile.getAbsolutePath());
            }

            // Fail le build si nécessaire
            if (result.hasDrifts()) {
                handleDrifts(result);
            } else {
                getLog().info("");
                getLog().info("✅ Aucun drift détecté. Le build peut continuer.");
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Git error: " + e.getMessage(), e);
        }
    }

    /**
     * Effectue un git fetch pour mettre à jour les branches distantes.
     */
    private void performGitFetch(GitFileReader gitReader) {
        getLog().info("Récupération depuis le dépôt distant...");

        boolean success = gitReader.fetchFromRemoteSafe();

        if (success) {
            getLog().info("✓ Derniers changements récupérés avec succès depuis origin.");
        } else {
            getLog().warn("⚠ Impossible de récupérer depuis le dépôt distant (mode hors ligne ou pas de remote configuré).");
            getLog().warn("  Continuation avec l'état local du dépôt...");
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
            getLog().info("Branche de base auto-détectée: " + detected);
            return detected;
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Impossible de détecter automatiquement la branche de base. Veuillez spécifier <baseRef> dans la configuration du plugin.", e);
        }
    }

    /**
     * Valide que les refs existent dans le repository.
     */
    private void validateRefs(GitFileReader gitReader, String base, String target) throws MojoFailureException {
        if (!gitReader.refExists(base)) {
            throw new MojoFailureException("La référence de base n'existe pas: " + base +
                    "\n\nAstuce: Si vous êtes en CI, assurez-vous que fetch-depth est défini à 0 dans GitHub Actions checkout.");
        }

        if (!gitReader.refExists(target)) {
            throw new MojoFailureException("La référence cible n'existe pas: " + target);
        }
    }

    /**
     * Gère les drifts détectés et fail le build si nécessaire.
     */
    private void handleDrifts(DriftDetector.DriftResult result) throws MojoFailureException {
        boolean shouldFail = false;
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("\n❌ DRIFT DE MIGRATION FLYWAY DÉTECTÉ\n\n");

        if (failOnDuplicates && (!result.baseDuplicates.isEmpty() || !result.targetDuplicates.isEmpty())) {
            shouldFail = true;
            errorMessage.append("🔴 Migrations dupliquées trouvées.\n");
        }

        if (failIfBehind && !result.behindMigrations.isEmpty()) {
            shouldFail = true;
            errorMessage.append("🟠 Migrations manquantes détectées (absentes dans la branche cible).\n");
        }

        if (failIfDiverged && !result.divergedMigrations.isEmpty()) {
            shouldFail = true;
            errorMessage.append("🟡 Migrations divergentes détectées (même version, contenu différent).\n");
        }

        if (shouldFail) {
            errorMessage.append("\nConsultez le rapport ci-dessus pour plus de détails.\n");
            errorMessage.append("\nPour corriger:\n");
            errorMessage.append("  - Doublons: Supprimez les fichiers de migration dupliqués.\n");
            errorMessage.append("  - En retard: Fusionnez ou rebasez avec la branche de base.\n");
            errorMessage.append("  - Divergentes: Ne modifiez jamais les migrations existantes. Créez plutôt une nouvelle migration.\n");

            throw new MojoFailureException(errorMessage.toString());
        }
    }
}
