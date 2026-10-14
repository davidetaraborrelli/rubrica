package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtenteDaoTest {

    // Per il DAO l'hash è una stringa qualsiasi senza ';'
    private static final String HASH_ADMIN = "pbkdf2_sha256$600000$c2FsZQ==$aGFzaA==";
    private static final String HASH_MARIO = "pbkdf2_sha256$600000$YWx0cm8=$aGFzaDI=";

    @TempDir
    Path cartella;

    private Path percorso;
    private FileUtenteDao dao;

    @BeforeEach
    void preparaDao() {
        percorso = cartella.resolve("utenti.txt");
        dao = new FileUtenteDao(percorso.toFile());
    }

    @Test
    void fileAssenteNonDaUtentiSenzaCrearlo() throws Exception {
        assertFalse(dao.esistonoUtenti());
        assertFalse(dao.trova("admin").isPresent());
        assertFalse(Files.exists(percorso));
    }

    @Test
    void scriveUsernameEHashSeparatiDaPuntoEVirgola() throws Exception {
        dao.inserisci(new Utente("admin", HASH_ADMIN));
        dao.inserisci(new Utente("mario", HASH_MARIO));

        assertEquals(Arrays.asList("admin;" + HASH_ADMIN, "mario;" + HASH_MARIO),
                Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void leggeIlFormatoUsernameEHash() throws Exception {
        Files.write(percorso, Arrays.asList("admin;" + HASH_ADMIN, "mario;" + HASH_MARIO), UTF_8);

        Optional<Utente> mario = dao.trova("mario");

        assertTrue(dao.esistonoUtenti());
        assertTrue(mario.isPresent());
        assertEquals("mario", mario.get().getUsername());
        assertEquals(HASH_MARIO, mario.get().getPasswordHash());
    }

    @Test
    void trovaDistingueMaiuscoleEUtentiInesistenti() throws Exception {
        dao.inserisci(new Utente("admin", HASH_ADMIN));

        assertFalse(dao.trova("Admin").isPresent());
        assertFalse(dao.trova("nessuno").isPresent());
        assertFalse(dao.trova("").isPresent());
    }

    @Test
    void unNuovoDaoRileggeQuantoSalvato() throws Exception {
        dao.inserisci(new Utente("admin", HASH_ADMIN));

        Optional<Utente> riletto = new FileUtenteDao(percorso.toFile()).trova("admin");

        assertTrue(riletto.isPresent());
        assertEquals(HASH_ADMIN, riletto.get().getPasswordHash());
    }

    @Test
    void inserireUnSecondoUtenteMantieneIlPrimo() throws Exception {
        dao.inserisci(new Utente("admin", HASH_ADMIN));
        dao.inserisci(new Utente("mario", HASH_MARIO));

        assertEquals(HASH_ADMIN, dao.trova("admin").get().getPasswordHash());
        assertEquals(HASH_MARIO, dao.trova("mario").get().getPasswordHash());
    }

    @Test
    void usernameGiaPresenteVieneRifiutato() throws Exception {
        dao.inserisci(new Utente("admin", HASH_ADMIN));

        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("admin", HASH_MARIO)));

        assertEquals(Arrays.asList("admin;" + HASH_ADMIN), Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void scriveInUtf8() throws Exception {
        dao.inserisci(new Utente("niccolò", HASH_ADMIN));

        String contenuto = new String(Files.readAllBytes(percorso), UTF_8);

        assertEquals("niccolò;" + HASH_ADMIN, contenuto.trim());
        assertTrue(new FileUtenteDao(percorso.toFile()).trova("niccolò").isPresent());
    }

    @Test
    void ignoraLeRigheVuote() throws Exception {
        Files.write(percorso, Arrays.asList("", "admin;" + HASH_ADMIN, "   ", "mario;" + HASH_MARIO, ""), UTF_8);

        assertTrue(dao.trova("admin").isPresent());
        assertTrue(dao.trova("mario").isPresent());
    }

    @Test
    void rigaConCampiMancantiIndicaIlNumeroDiRiga() throws Exception {
        Files.write(percorso, Arrays.asList("admin;" + HASH_ADMIN, "mario"), UTF_8);

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.esistonoUtenti());

        assertTrue(errore.getMessage().contains("riga 2"), errore.getMessage());
    }

    @Test
    void rigaConCampiInPiuNonEValida() throws Exception {
        Files.write(percorso, Arrays.asList("admin;" + HASH_ADMIN + ";altro"), UTF_8);

        assertThrows(PersistenzaException.class, () -> dao.trova("admin"));
    }

    @Test
    void usernameOHashVuotiNonSonoValidi() throws Exception {
        Files.write(percorso, Arrays.asList(";" + HASH_ADMIN), UTF_8);
        PersistenzaException senzaUsername = assertThrows(PersistenzaException.class, () -> dao.trova("admin"));
        assertTrue(senzaUsername.getMessage().contains("riga 1"), senzaUsername.getMessage());

        Files.write(percorso, Arrays.asList("admin;"), UTF_8);
        assertThrows(PersistenzaException.class, () -> dao.trova("admin"));
    }

    @Test
    void fileNonValidoNonVieneSovrascritto() throws Exception {
        Files.write(percorso, Arrays.asList("admin;" + HASH_ADMIN, "riga rovinata"), UTF_8);
        byte[] originale = Files.readAllBytes(percorso);

        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("mario", HASH_MARIO)));

        assertArrayEquals(originale, Files.readAllBytes(percorso));
    }

    @Test
    void fileNonInUtf8DaErroreInveceDiEssereLettoAMeta() throws Exception {
        Files.write(percorso, Arrays.asList("admin;" + HASH_ADMIN, "niccolò;" + HASH_MARIO), ISO_8859_1);

        assertThrows(PersistenzaException.class, () -> dao.trova("admin"));
    }

    @Test
    void datiConSeparatoreRitorniACapoOVuotiVengonoRifiutati() throws Exception {
        dao.inserisci(new Utente("admin", HASH_ADMIN));

        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("ma;rio", HASH_MARIO)));
        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("mario", "ha;sh")));
        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("ma\nrio", HASH_MARIO)));
        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("", HASH_MARIO)));
        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("mario", "")));

        assertEquals(Arrays.asList("admin;" + HASH_ADMIN), Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void scritturaFallitaNonLasciaTracce() throws Exception {
        // Una cartella con lo stesso nome rende il file impossibile da scrivere
        Files.createDirectory(percorso);

        assertThrows(PersistenzaException.class, () -> dao.inserisci(new Utente("admin", HASH_ADMIN)));

        Files.delete(percorso);
        assertFalse(dao.esistonoUtenti());
        dao.inserisci(new Utente("admin", HASH_ADMIN));
        assertEquals(Arrays.asList("admin;" + HASH_ADMIN), Files.readAllLines(percorso, UTF_8));
    }
}
