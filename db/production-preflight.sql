-- PostgreSQL. Revisar e executar uma vez no banco existente ANTES de publicar esta versão.
-- Não é executado automaticamente pela aplicação. Flyway continua desabilitado.
-- Se existirem e-mails duplicados, a transação falha sem modificar os dados.
BEGIN;
LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email IS NULL OR btrim(email) = '') THEN
        RAISE EXCEPTION 'Corrija usuários sem e-mail antes da migração';
    END IF;
    IF EXISTS (SELECT lower(btrim(email)) FROM users GROUP BY lower(btrim(email)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Resolva e-mails duplicados antes da migração';
    END IF;
END $$;
UPDATE users SET email = lower(btrim(email));
ALTER TABLE users ALTER COLUMN email SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_normalized ON users (lower(btrim(email)));
-- Perfis antigos de cargo comercial passam a USER; somente ADMIN explícito é privilegiado.
UPDATE users SET role = CASE WHEN role = 'ADMIN' THEN 'ADMIN' ELSE 'USER' END;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'USER';
ALTER TABLE users ALTER COLUMN role SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_transactions_user_date_type ON transactions (id_user, date, id_type);
CREATE INDEX IF NOT EXISTS ix_categories_user_type ON categories (id_user, id_type);
COMMIT;
