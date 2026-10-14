package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.logica.Autenticazione;
import it.taraborrelli.rubrica.modello.Persona;
import it.taraborrelli.rubrica.modello.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUtenteDaoTest {

    private static final String HASH_MARIO = "pbkdf2_sha256$600000$YWx0cm8=$aGFzaDI=";

    private DatabaseDiProva database;
    private JdbcUtenteDao dao;

    @BeforeEach
    void preparaDao() throws Exception {
        database = new DatabaseDiProva();
        dao = new JdbcUtenteDao(database.configurazione());
    }

    @Test
    void loScriptCreaLUtenteAdmin() throws Exception {
        assertTrue(dao.esistonoUtenti());
        Optional<Utente> admin = dao.trova("admin");
        assertTrue(admin.isPresent());
        assertTrue(admin.get().getPasswordHash().startsWith("pbkdf2_sha256$"));
    }

    @Test
    void laPasswordDiAdminNelloScriptEAdmin() throws Exception {
        Autenticazione autenticazione = new Autenticazione(dao);

        assertTrue(autenticazione.verificaCredenziali("admin", "admin".toCharArray()));
        assertFalse(autenticazione.verificaCredenziali("admin", "Admin".toCharArray()));
        assertFalse(autenticazione.verificaCredenziali("admin", "sbagliata".toCharArray()));
    }

    @Test
    void rilanciareLoScriptNonDuplicaNeCambiaAdminNeCancellaIDati() throws Exception {
        String hashPrima = dao.trova("admin").get().getPasswordHash();
        new JdbcPersonaDao(database.configurazione())
                .inserisci(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));

        database.eseguiScript();
        database.eseguiScript();

        assertEquals(1, database.contaRighe("utenti"));
        assertEquals(hashPrima, dao.trova("admin").get().getPasswordHash());
        assertEquals(1, database.contaRighe("persone"));
    }

    @Test
    void inserisceERitrovaUnUtente() throws Exception {
        dao.inserisci(new Utente("mario", HASH_MARIO));

        Optional<Utente> mario = dao.trova("mario");

        assertTrue(mario.isPresent());
        assertEquals("mario", mario.get().getUsername());
        assertEquals(HASH_MARIO, mario.get().getPasswordHash());
        assertEquals(2, database.contaRighe("utenti"));
    }

    @Test
    void usernameGiaPresenteVieneRifiutato() {
        PersistenzaException errore = assertThrows(PersistenzaException.class,
                () -> dao.inserisci(new Utente("admin", HASH_MARIO)));

        assertTrue(errore.getMessage().contains("Esiste già"), errore.getMessage());
    }

    @Test
    void trovaDistingueMaiuscoleEUtentiInesistenti() throws Exception {
        assertFalse(dao.trova("Admin").isPresent());
        assertFalse(dao.trova("nessuno").isPresent());
        assertFalse(dao.trova("").isPresent());
    }

    @Test
    void senzaUtentiLUtenteInizialeVieneRicreato() throws Exception {
        database.esegui("DELETE FROM utenti");
        Autenticazione autenticazione = new Autenticazione(dao);
        assertFalse(dao.esistonoUtenti());

        autenticazione.creaUtenteIniziale();

        assertTrue(autenticazione.verificaCredenziali("admin", "admin".toCharArray()));
    }

    @Test
    void conLoScriptEseguitoLUtenteInizialeNonVieneToccato() throws Exception {
        String hashPrima = dao.trova("admin").get().getPasswordHash();

        new Autenticazione(dao).creaUtenteIniziale();

        assertEquals(1, database.contaRighe("utenti"));
        assertEquals(hashPrima, dao.trova("admin").get().getPasswordHash());
    }
}
