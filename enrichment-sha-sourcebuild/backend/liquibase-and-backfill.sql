-- ══════════════════════════════════════════════════════════
-- Changeset Liquibase à ajouter dans db.changelog-master.yaml
-- ══════════════════════════════════════════════════════════
--
-- - changeSet:
--     id: 9-add-tag-commit-sha
--     author: dylan
--     changes:
--       - addColumn:
--           tableName: deploiement
--           columns:
--             - column:
--                 name: tag_commit_sha
--                 type: VARCHAR(40)
--       - createIndex:
--           tableName: deploiement
--           indexName: idx_deploiement_tag_sha
--           columns:
--             - column:
--                 name: tag_commit_sha
--
-- ══════════════════════════════════════════════════════════
-- Backfill SQL — remplir head_commit_date / base_commit_date
-- depuis les commits existants
-- ══════════════════════════════════════════════════════════

-- 1. Backfill head_commit_date et base_commit_date
UPDATE comparaison c
SET head_commit_date = (
    SELECT CAST(MAX(co.date) AS VARCHAR)
    FROM commit co
    WHERE co.comparaison_id = c.id
),
base_commit_date = (
    SELECT CAST(MIN(co.date) AS VARCHAR)
    FROM commit co
    WHERE co.comparaison_id = c.id
)
WHERE c.head_commit_date IS NULL
  AND EXISTS (SELECT 1 FROM commit co WHERE co.comparaison_id = c.id);

-- 2. Vérification
SELECT
    c.id,
    c.produit,
    c.base_version,
    c.head_version,
    c.head_commit_date,
    c.base_commit_date,
    (SELECT COUNT(*) FROM commit co WHERE co.comparaison_id = c.id) as nb_commits
FROM comparaison c
WHERE c.head_commit_date IS NOT NULL
ORDER BY c.id DESC;
