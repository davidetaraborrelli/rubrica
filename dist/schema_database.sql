-- Rubrica: database e tabelle per il salvataggio su MySQL 8 o su MariaDB.
-- Lo script si puo lanciare piu volte: quello che esiste gia non viene toccato.
--
-- Dal prompt dei comandi, nella cartella dello script:
--     mysql -u <username> -p < schema_database.sql
-- oppure aprendolo ed eseguendolo da MySQL Workbench o da un altro client.

CREATE DATABASE IF NOT EXISTS rubrica
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rubrica;

CREATE TABLE IF NOT EXISTS persone (
    id INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    indirizzo VARCHAR(255) NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    eta INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- La collation utf8mb4_bin fa distinguere maiuscole e minuscole nello username,
-- come avviene nel salvataggio su file
CREATE TABLE IF NOT EXISTS utenti (
    username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Utente iniziale: admin con password admin.
-- La password e salvata come hash PBKDF2 con salt casuale, mai in chiaro.
INSERT IGNORE INTO utenti (username, password_hash)
VALUES ('admin', 'pbkdf2_sha256$600000$s3wielu8Z82uAjlw7OmHXA==$OQ6CDx/QuxE/AF/X16cxZDpF5zFBPQ9OoZof+JM5GS8=');
