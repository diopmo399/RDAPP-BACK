-- Repeatable migration: Refresh materialized views

-- This migration runs every time its checksum changes
-- Useful for views, stored procedures, functions

CREATE OR REPLACE VIEW user_stats AS
SELECT
    COUNT(*) as total_users,
    COUNT(CASE WHEN created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as users_last_30_days
FROM users;
