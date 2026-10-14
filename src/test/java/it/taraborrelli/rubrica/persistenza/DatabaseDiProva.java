package it.taraborrelli.rubrica.persistenza;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Database H2 in memoria, in modalità MySQL, creato con lo stesso script SQL che si consegna.
 * H2 non conosce CREATE DATABASE e USE: quelle due istruzioni si saltano, il resto si esegue com'è.
 */
final class DatabaseDiProva {

    private static final String SCRIPT = "dist/schema_database.sql";

    private final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";

    DatabaseDiProva() throws IOException, SQLException {
        eseguiScript();
    }

    ConfigurazioneDatabase configurazione() {
        return new ConfigurazioneDatabase(url, "sa", "");
    }

    /** Lancia di nuovo lo script, come fa chi lo esegue più volte. */
    void eseguiScript() throws IOException, SQLException {
        try (Connection connessione = DriverManager.getConnection(url, "sa", "");
             Statement istruzione = connessione.createStatement()) {
            for (String comando : comandiDelloScript()) {
                istruzione.execute(comando);
            }
        }
    }

    void esegui(String sql) throws SQLException {
        try (Connection connessione = DriverManager.getConnection(url, "sa", "");
             Statement istruzione = connessione.createStatement()) {
            istruzione.execute(sql);
        }
    }

    int contaRighe(String tabella) throws SQLException {
        try (Connection connessione = DriverManager.getConnection(url, "sa", "");
             Statement istruzione = connessione.createStatement();
             ResultSet risultati = istruzione.executeQuery("SELECT COUNT(*) FROM " + tabella)) {
            risultati.next();
            return risultati.getInt(1);
        }
    }

    private static List<String> comandiDelloScript() throws IOException {
        StringBuilder senzaCommenti = new StringBuilder();
        for (String riga : Files.readAllLines(Paths.get(SCRIPT), UTF_8)) {
            if (!riga.trim().startsWith("--")) {
                senzaCommenti.append(riga).append('\n');
            }
        }
        List<String> comandi = new ArrayList<>();
        for (String comando : senzaCommenti.toString().split(";")) {
            String pulito = comando.trim();
            if (!pulito.isEmpty() && !pulito.startsWith("CREATE DATABASE") && !pulito.startsWith("USE ")) {
                comandi.add(pulito);
            }
        }
        return comandi;
    }
}
