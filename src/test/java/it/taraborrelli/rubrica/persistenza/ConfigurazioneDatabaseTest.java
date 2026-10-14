package it.taraborrelli.rubrica.persistenza;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurazioneDatabaseTest {

    @TempDir
    Path cartella;

    private Path file;

    @BeforeEach
    void preparaFile() {
        file = cartella.resolve("credenziali_database.properties");
    }

    @Test
    void costruisceLIndirizzoDelDatabaseDaIndirizzoEPorta() throws Exception {
        scrivi("username=mario", "password=segreta", "ip-server-mysql=192.168.1.10", "porta=3307");

        ConfigurazioneDatabase configurazione = ConfigurazioneDatabase.leggi(file.toFile());

        // L'indirizzo si vede nel messaggio degli errori di connessione, senza i parametri tecnici
        String messaggio = configurazione.errore(new SQLException("rifiutata", "08S01")).getMessage();
        assertTrue(messaggio.contains("jdbc:mysql://192.168.1.10:3307/rubrica"), messaggio);
        assertFalse(messaggio.contains("connectTimeout"), messaggio);
    }

    @Test
    void laPasswordPuoEssereVuotaOMancare() throws Exception {
        scrivi("username=root", "password=", "ip-server-mysql=localhost", "porta=3306");
        ConfigurazioneDatabase.leggi(file.toFile());

        scrivi("username=root", "ip-server-mysql=localhost", "porta=3306");
        ConfigurazioneDatabase.leggi(file.toFile());
    }

    @Test
    void ognunoDegliAltriValoriEObbligatorioEVieneIndicato() throws Exception {
        String[] righe = {"username=root", "ip-server-mysql=localhost", "porta=3306"};
        for (int mancante = 0; mancante < righe.length; mancante++) {
            String[] senzaUna = new String[righe.length - 1];
            for (int i = 0, j = 0; i < righe.length; i++) {
                if (i != mancante) {
                    senzaUna[j++] = righe[i];
                }
            }
            scrivi(senzaUna);

            PersistenzaException errore = assertThrows(PersistenzaException.class,
                    () -> ConfigurazioneDatabase.leggi(file.toFile()));

            String chiave = righe[mancante].substring(0, righe[mancante].indexOf('='));
            assertTrue(errore.getMessage().contains("\"" + chiave + "\""), errore.getMessage());
        }
    }

    @Test
    void valoreVuotoEquivaleAValoreMancante() throws Exception {
        scrivi("username=   ", "ip-server-mysql=localhost", "porta=3306");

        PersistenzaException errore = assertThrows(PersistenzaException.class,
                () -> ConfigurazioneDatabase.leggi(file.toFile()));

        assertTrue(errore.getMessage().contains("\"username\""), errore.getMessage());
    }

    @Test
    void portaNonValidaVieneRifiutata() throws Exception {
        for (String porta : new String[] {"abc", "0", "65536", "-1", "33 06", "3306.5"}) {
            scrivi("username=root", "ip-server-mysql=localhost", "porta=" + porta);

            PersistenzaException errore = assertThrows(PersistenzaException.class,
                    () -> ConfigurazioneDatabase.leggi(file.toFile()), porta);

            assertTrue(errore.getMessage().contains("porta"), errore.getMessage());
        }
    }

    @Test
    void spaziAttornoAiValoriVengonoTolti() throws Exception {
        scrivi("username = root ", "ip-server-mysql =  localhost  ", "porta = 3306 ");

        ConfigurazioneDatabase configurazione = ConfigurazioneDatabase.leggi(file.toFile());

        String messaggio = configurazione.errore(new SQLException("rifiutata", "08S01")).getMessage();
        assertTrue(messaggio.contains("jdbc:mysql://localhost:3306/rubrica)"), messaggio);
    }

    @Test
    void ilBomUtf8InizialeNonRovinaLaPrimaChiave() throws Exception {
        ByteArrayOutputStream contenuto = new ByteArrayOutputStream();
        contenuto.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        contenuto.write("username=root\nip-server-mysql=localhost\nporta=3306\n".getBytes(UTF_8));
        Files.write(file, contenuto.toByteArray());

        ConfigurazioneDatabase.leggi(file.toFile());
    }

    @Test
    void fileAssenteDaErrore() {
        PersistenzaException errore = assertThrows(PersistenzaException.class,
                () -> ConfigurazioneDatabase.leggi(file.toFile()));

        assertTrue(errore.getMessage().contains("credenziali_database.properties"), errore.getMessage());
    }

    @Test
    void erroriFrequentiDiventanoMessaggiComprensibili() {
        ConfigurazioneDatabase configurazione = new ConfigurazioneDatabase(
                "jdbc:mysql://localhost:3306/rubrica?connectTimeout=5000", "root", "");

        assertTrue(messaggio(configurazione, new SQLException("Access denied", "28000", 1045))
                .contains("username e password"));
        assertTrue(messaggio(configurazione, new SQLException("Unknown database", "42000", 1049))
                .contains("schema_database.sql"));
        assertTrue(messaggio(configurazione, new SQLException("Table doesn't exist", "42S02", 1146))
                .contains("schema_database.sql"));
        assertTrue(messaggio(configurazione, new SQLException("Data too long", "22001", 1406))
                .contains("troppo lungo"));
        assertTrue(messaggio(configurazione, new SQLException("Communications link failure", "08S01", 0))
                .contains("Impossibile raggiungere il database (jdbc:mysql://localhost:3306/rubrica)"));
        assertTrue(messaggio(configurazione, new SQLException("qualcosa di strano", "HY000", 9999))
                .startsWith("Errore del database: qualcosa di strano"));
    }

    @Test
    void lErroreOriginaleRestaComeCausa() {
        ConfigurazioneDatabase configurazione = new ConfigurazioneDatabase("jdbc:mysql://localhost:3306/rubrica", "", "");
        SQLException originale = new SQLException("Access denied", "28000", 1045);

        assertSame(originale, configurazione.errore(originale).getCause());
    }

    private static String messaggio(ConfigurazioneDatabase configurazione, SQLException errore) {
        return configurazione.errore(errore).getMessage();
    }

    private void scrivi(String... righe) throws Exception {
        Files.write(file, Arrays.asList(righe), UTF_8);
    }
}
