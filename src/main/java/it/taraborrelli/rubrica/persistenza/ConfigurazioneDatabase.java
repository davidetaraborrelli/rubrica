package it.taraborrelli.rubrica.persistenza;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Parametri di connessione al database MySQL, letti da {@code credenziali_database.properties}
 * (chiavi {@code username}, {@code password}, {@code ip-server-mysql} e {@code porta}). Apre le
 * connessioni e traduce gli errori SQL più comuni in messaggi comprensibili.
 */
public class ConfigurazioneDatabase {

    private static final String NOME_DATABASE = "rubrica";
    // Senza un limite, la connessione a un server spento resterebbe in attesa a lungo
    private static final int TIMEOUT_CONNESSIONE_MS = 5000;

    // Codici di errore di MySQL, che MariaDB usa allo stesso modo
    private static final int ERRORE_ACCESSO_NEGATO = 1045;
    private static final int ERRORE_DATABASE_INESISTENTE = 1049;
    private static final int ERRORE_TABELLA_INESISTENTE = 1146;
    private static final int ERRORE_VALORE_TROPPO_LUNGO = 1406;

    private final String url;
    private final String username;
    private final String password;

    public ConfigurazioneDatabase(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * Legge le credenziali dal file e costruisce l'indirizzo del database {@code rubrica}.
     * La password può mancare o essere vuota, gli altri valori sono obbligatori.
     */
    public static ConfigurazioneDatabase leggi(File file) throws PersistenzaException {
        Properties proprieta = new Properties();
        try {
            String testo = new String(Files.readAllBytes(file.toPath()), UTF_8);
            // Alcuni editor aggiungono il BOM UTF-8, che finirebbe nel nome della prima chiave
            if (testo.startsWith("\uFEFF")) {
                testo = testo.substring(1);
            }
            proprieta.load(new StringReader(testo));
        } catch (IOException e) {
            throw new PersistenzaException("Impossibile leggere il file " + file.getAbsolutePath() + ".", e);
        }
        String indirizzo = valoreObbligatorio(proprieta, "ip-server-mysql", file);
        String username = valoreObbligatorio(proprieta, "username", file);
        String porta = valoreObbligatorio(proprieta, "porta", file);
        if (!porta.matches("\\d{1,5}") || Integer.parseInt(porta) < 1 || Integer.parseInt(porta) > 65535) {
            throw new PersistenzaException("Nel file " + file.getAbsolutePath() + " la porta \"" + porta
                    + "\" non è valida: deve essere un numero tra 1 e 65535.");
        }
        String url = "jdbc:mysql://" + indirizzo + ":" + porta + "/" + NOME_DATABASE
                + "?connectTimeout=" + TIMEOUT_CONNESSIONE_MS + "&characterEncoding=UTF-8";
        return new ConfigurazioneDatabase(url, username, proprieta.getProperty("password", ""));
    }

    private static String valoreObbligatorio(Properties proprieta, String chiave, File file)
            throws PersistenzaException {
        String valore = proprieta.getProperty(chiave);
        if (valore == null || valore.trim().isEmpty()) {
            throw new PersistenzaException("Nel file " + file.getAbsolutePath() + " manca il valore di \""
                    + chiave + "\".");
        }
        return valore.trim();
    }

    Connection apriConnessione() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Trasforma un errore SQL in un'eccezione con un messaggio comprensibile, che dice anche
     * cosa fare nei casi più comuni.
     */
    PersistenzaException errore(SQLException e) {
        switch (e.getErrorCode()) {
            case ERRORE_ACCESSO_NEGATO:
                return new PersistenzaException("Accesso al database negato: controllare username e password "
                        + "in credenziali_database.properties.", e);
            case ERRORE_DATABASE_INESISTENTE:
                return new PersistenzaException("Il database \"" + NOME_DATABASE
                        + "\" non esiste: eseguire prima lo script schema_database.sql.", e);
            case ERRORE_TABELLA_INESISTENTE:
                return new PersistenzaException("Le tabelle del database non esistono: eseguire lo script "
                        + "schema_database.sql.", e);
            case ERRORE_VALORE_TROPPO_LUNGO:
                return new PersistenzaException("Uno dei valori è troppo lungo per il database.", e);
            default:
                break;
        }
        // La classe 08 dello stato SQL raggruppa gli errori di connessione: server spento,
        // indirizzo o porta sbagliati, rete non raggiungibile
        if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
            return new PersistenzaException("Impossibile raggiungere il database (" + indirizzoDatabase()
                    + "): controllare che il server sia acceso e che indirizzo e porta in "
                    + "credenziali_database.properties siano giusti.", e);
        }
        return new PersistenzaException("Errore del database: " + e.getMessage(), e);
    }

    // L'indirizzo senza i parametri, che sono dettagli tecnici inutili da mostrare
    private String indirizzoDatabase() {
        int parametri = url.indexOf('?');
        return parametri < 0 ? url : url.substring(0, parametri);
    }
}
