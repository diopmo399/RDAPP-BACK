package com.example.flyway.drift.git;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.transport.FetchResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire pour lire les fichiers depuis un repository Git via JGit.
 */
public class GitFileReader implements AutoCloseable {

    private final Repository repository;

    public GitFileReader(File projectBaseDir) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        // Essayer de trouver le .git dans le répertoire actuel
        File gitDir = new File(projectBaseDir, ".git");

        if (gitDir.exists()) {
            // .git existe dans le répertoire du projet
            this.repository = builder
                    .setGitDir(gitDir)
                    .readEnvironment()
                    .build();
        } else {
            // .git n'existe pas, chercher dans les répertoires parents
            this.repository = builder
                    .setWorkTree(projectBaseDir)
                    .readEnvironment()
                    .findGitDir()
                    .build();
        }
    }

    /**
     * Résout une ref Git (ex: origin/main, HEAD, commit SHA).
     *
     * @param ref Référence Git
     * @return ObjectId du commit
     * @throws IOException Si la ref n'existe pas
     */
    public ObjectId resolveRef(String ref) throws IOException {
        ObjectId objectId = repository.resolve(ref);
        if (objectId == null) {
            throw new IOException("Cannot resolve Git ref: " + ref);
        }
        return objectId;
    }

    /**
     * Liste tous les fichiers .sql dans un chemin donné pour une ref Git.
     *
     * @param ref           Référence Git (ex: origin/main)
     * @param migrationsPath Chemin relatif (ex: src/main/resources/db/migration)
     * @return Map<fileName, contentHash>
     * @throws IOException En cas d'erreur Git
     */
    public Map<String, String> listMigrationFiles(String ref, String migrationsPath) throws IOException {
        ObjectId commitId = resolveRef(ref);
        Map<String, String> files = new HashMap<>();

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();

            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(migrationsPath));

                while (treeWalk.next()) {
                    String path = treeWalk.getPathString();

                    // Ne prendre que les fichiers .sql
                    if (!path.endsWith(".sql")) {
                        continue;
                    }

                    // Extraire le nom du fichier
                    String fileName = path.substring(path.lastIndexOf('/') + 1);

                    // Lire le contenu et calculer le hash
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    byte[] bytes = loader.getBytes();
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    String hash = DigestUtils.sha256Hex(content);

                    files.put(fileName, hash);
                }
            }
        }

        return files;
    }

    /**
     * Lit le contenu d'un fichier spécifique depuis une ref Git.
     *
     * @param ref      Référence Git
     * @param filePath Chemin complet du fichier
     * @return Contenu du fichier
     * @throws IOException Si le fichier n'existe pas
     */
    public String readFileContent(String ref, String filePath) throws IOException {
        ObjectId commitId = resolveRef(ref);

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();

            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));

                if (!treeWalk.next()) {
                    throw new IOException("File not found: " + filePath + " in ref: " + ref);
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                byte[] bytes = loader.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * Vérifie si une ref existe.
     *
     * @param ref Référence Git
     * @return true si la ref existe
     */
    public boolean refExists(String ref) {
        try {
            return repository.resolve(ref) != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Détecte automatiquement la branche principale (main ou master).
     *
     * @return origin/main ou origin/master
     * @throws IOException Si aucune des deux n'existe
     */
    public String detectMainBranch() throws IOException {
        if (refExists("origin/main")) {
            return "origin/main";
        } else if (refExists("origin/master")) {
            return "origin/master";
        } else if (refExists("refs/remotes/origin/main")) {
            return "refs/remotes/origin/main";
        } else if (refExists("refs/remotes/origin/master")) {
            return "refs/remotes/origin/master";
        } else {
            throw new IOException("Cannot detect main branch. Neither origin/main nor origin/master exists.");
        }
    }

    /**
     * Retourne le nom court du commit pour une ref.
     *
     * @param ref Référence Git
     * @return SHA court (7 caractères)
     * @throws IOException En cas d'erreur
     */
    public String getShortCommitId(String ref) throws IOException {
        ObjectId objectId = resolveRef(ref);
        return objectId.getName().substring(0, 7);
    }

    /**
     * Effectue un git fetch pour mettre à jour les branches distantes.
     * Récupère toutes les branches du remote "origin".
     *
     * @return FetchResult avec les détails du fetch
     * @throws IOException En cas d'erreur I/O
     * @throws GitAPIException En cas d'erreur Git (pas de remote, pas de connexion, etc.)
     */
    public FetchResult fetchFromRemote() throws IOException, GitAPIException {
        try (Git git = new Git(repository)) {
            FetchCommand fetchCommand = git.fetch()
                    .setRemote("origin")
                    .setRefSpecs("+refs/heads/*:refs/remotes/origin/*");

            return fetchCommand.call();
        }
    }

    /**
     * Effectue un git fetch silencieux (sans lever d'exception si échec).
     * Utile pour ne pas bloquer le build si pas de connexion réseau.
     *
     * @return true si le fetch a réussi, false sinon
     */
    public boolean fetchFromRemoteSafe() {
        try {
            fetchFromRemote();
            return true;
        } catch (IOException | GitAPIException e) {
            // Échec silencieux
            return false;
        }
    }

    public void close() {
        repository.close();
    }
}
