# Scénarios de Test - Flyway Drift Plugin

Ce document explique comment tester le plugin avec différents cas de figure.

## Configuration Importante

Dans `pom.xml`, assurez-vous que le `migrationsPath` pointe vers le bon chemin **relatif à la racine du repository Git** :

```xml
<migrationsPath>flyway-drift-maven-plugin/example-project/src/main/resources/db/migration</migrationsPath>
<baseRef>main</baseRef>
```

## Branches de Test

- **main** : Branche de référence avec les migrations de base
- **test-drift-scenarios** : Branche de test avec des drifts intentionnels

## Scénario 1 : DIVERGED (Modification d'une migration existante) ✅

### Situation
Un fichier de migration **existant** a été modifié (son contenu a changé).

### Fichier concerné
`V2__add_products_table.sql`

### Différence
- **main** : Table `products` sans le champ `category`
- **test-drift-scenarios** : Table `products` AVEC le champ `category`

### Test
```bash
git checkout test-drift-scenarios
cd flyway-drift-maven-plugin/example-project
mvn flyway-drift:check
```

### Résultat attendu
```
🟡 DIVERGED MIGRATIONS (same version, different content):
  - V2__add_products_table
    Base:   163a93c0fc752b9c
    Target: 82e4b06c4ccd1c8b
```

### Pourquoi c'est un problème ?
Modifier une migration existante est **une erreur grave** en Flyway. Une fois qu'une migration est déployée en production, elle ne doit jamais être modifiée.

### Solution
Annuler les modifications et créer une **nouvelle migration** (V5, V6, etc.) pour ajouter le champ manquant.

---

## Scénario 2 : BEHIND (Fichier manquant dans la branche courante) ✅

### Situation
Un fichier de migration existe dans `main` mais est **absent** de la branche courante.

### Fichier concerné
`V4__add_categories_table.sql`

### Différence
- **main** : Contient V4__add_categories_table.sql
- **test-drift-scenarios** : Ne contient PAS V4

### Test
```bash
git checkout test-drift-scenarios
cd flyway-drift-maven-plugin/example-project
mvn flyway-drift:check
```

### Résultat attendu
```
🟠 BEHIND MIGRATIONS (present in base, missing in target):
  - V4__add_categories_table (hash: d3afe5e4)
```

### Pourquoi c'est un problème ?
Votre branche est **en retard** par rapport à main. Si vous déployez, il manquera des migrations.

### Solution
Faire un `merge` ou `rebase` avec main pour récupérer les migrations manquantes.

---

## Scénario 3 : AHEAD (Nouveau fichier dans la branche courante) ℹ️

### Situation
Un fichier de migration existe dans la branche courante mais est **absent** de `main`.

### Fichier concerné
`V3__add_orders_table.sql`

### Différence
- **main** : Ne contient PAS V3
- **test-drift-scenarios** : Contient V3__add_orders_table.sql

### Test
```bash
git checkout test-drift-scenarios
cd flyway-drift-maven-plugin/example-project
mvn flyway-drift:check
```

### Résultat
**V3 n'est PAS signalé comme un drift.**

### Pourquoi ?
C'est du **développement normal**. Ajouter de nouvelles migrations dans une branche de feature est attendu.

### Solution
Rien à faire - c'est normal. Une fois mergé dans main, V3 sera disponible pour tous.

---

## Scénario 4 : DUPLICATE (Fichiers en double)

### Situation
Deux fichiers ont la **même version** Flyway.

### Comment créer ce scénario
```bash
git checkout test-drift-scenarios
cd flyway-drift-maven-plugin/example-project/src/main/resources/db/migration

# Créer un doublon de V2
cp V2__add_products_table.sql V2__another_migration.sql
```

### Test
```bash
mvn flyway-drift:check
```

### Résultat attendu
```
🔴 DUPLICATE MIGRATIONS detected:
  - V2 appears in 2 files
```

### Pourquoi c'est un problème ?
Flyway ne peut pas avoir deux migrations avec la même version. Cela causera une erreur au démarrage.

### Solution
Renommer l'une des migrations avec une version unique (V5, V6, etc.).

---

## Résumé des Branches

### Branch: main
- V1__init.sql
- V2__add_products_table.sql (version originale)
- R__refresh_views.sql
- V4__add_categories_table.sql

### Branch: test-drift-scenarios
- V1__init.sql
- V2__add_products_table.sql (**modifié** - DIVERGED)
- R__refresh_views.sql
- V3__add_orders_table.sql (**nouveau** - AHEAD)
- (manque V4 - BEHIND)

---

## Commandes Utiles

### Tester avec fetch désactivé
```bash
mvn flyway-drift:check -Dflyway.drift.fetchBeforeCheck=false
```

### Tester avec une autre branche de base
```bash
mvn flyway-drift:check -Dflyway.drift.baseRef=origin/main
```

### Voir le rapport généré
```bash
cat target/flyway-drift-report.md
```

### Comparer les hashs manuellement
```bash
# Hash de V2 dans main
git show main:flyway-drift-maven-plugin/example-project/src/main/resources/db/migration/V2__add_products_table.sql | sha256sum

# Hash de V2 dans la branche courante
cat src/main/resources/db/migration/V2__add_products_table.sql | sha256sum
```

---

## Debugging

### Le plugin trouve 0 fichiers
Vérifiez que le `migrationsPath` est correct et **relatif à la racine du repository Git** :
```xml
<!-- CORRECT -->
<migrationsPath>flyway-drift-maven-plugin/example-project/src/main/resources/db/migration</migrationsPath>

<!-- INCORRECT si le .git est à la racine de RDAPP_BACK -->
<migrationsPath>src/main/resources/db/migration</migrationsPath>
```

### Le fetch ne fonctionne pas
Le plugin essaie de faire un `git fetch origin` mais en mode silencieux. Si ça échoue (pas de connexion réseau), il continue sans bloquer.

---

## Configuration Recommandée pour CI/CD

```xml
<configuration>
    <baseRef>origin/main</baseRef>
    <targetRef>HEAD</targetRef>
    <migrationsPath>flyway-drift-maven-plugin/example-project/src/main/resources/db/migration</migrationsPath>
    <fetchBeforeCheck>true</fetchBeforeCheck>
    <failIfBehind>true</failIfBehind>
    <failIfDiverged>true</failIfDiverged>
    <failOnDuplicates>true</failOnDuplicates>
</configuration>
```

Dans GitHub Actions :
```yaml
- name: Checkout
  uses: actions/checkout@v4
  with:
    fetch-depth: 0  # Important pour avoir toutes les branches
```
