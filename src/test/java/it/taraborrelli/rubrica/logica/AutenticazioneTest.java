package it.taraborrelli.rubrica.logica;

import it.taraborrelli.rubrica.modello.Utente;
import it.taraborrelli.rubrica.persistenza.UtenteDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticazioneTest {

    private static final String PASSWORD = "Segreta-1";

    // Calcolarlo costa un po' (è voluto): lo si fa una volta sola per i test che ne hanno bisogno
    private static String hashDellaPassword;

    private UtentiInMemoria dao;
    private Autenticazione autenticazione;

    @BeforeAll
    static void calcolaHashDiProva() {
        hashDellaPassword = Autenticazione.calcolaHash(PASSWORD.toCharArray());
    }

    @BeforeEach
    void preparaAutenticazione() {
        dao = new UtentiInMemoria();
        autenticazione = new Autenticazione(dao);
    }

    @Test
    void hashHaIlFormatoAtteso() {
        assertTrue(hashDellaPassword.matches("pbkdf2_sha256\\$600000\\$[A-Za-z0-9+/]+=*\\$[A-Za-z0-9+/]+=*"),
                hashDellaPassword);
        assertFalse(hashDellaPassword.contains(PASSWORD));
    }

    @Test
    void stessaPasswordDaHashDiversiPerIlSaltCasuale() {
        String altroHash = Autenticazione.calcolaHash(PASSWORD.toCharArray());

        assertNotEquals(hashDellaPassword, altroHash);
        assertTrue(Autenticazione.controllaPassword(PASSWORD.toCharArray(), hashDellaPassword));
        assertTrue(Autenticazione.controllaPassword(PASSWORD.toCharArray(), altroHash));
    }

    @Test
    void controllaPasswordAccettaSoloQuellaGiusta() {
        assertTrue(Autenticazione.controllaPassword(PASSWORD.toCharArray(), hashDellaPassword));
        assertFalse(Autenticazione.controllaPassword("segreta-1".toCharArray(), hashDellaPassword));
        assertFalse(Autenticazione.controllaPassword("Segreta-".toCharArray(), hashDellaPassword));
        assertFalse(Autenticazione.controllaPassword("Segreta-1 ".toCharArray(), hashDellaPassword));
        assertFalse(Autenticazione.controllaPassword(new char[0], hashDellaPassword));
    }

    @Test
    void hashIllegibileNonSoddisfaNessunaPassword() {
        char[] password = PASSWORD.toCharArray();

        assertFalse(Autenticazione.controllaPassword(password, ""));
        assertFalse(Autenticazione.controllaPassword(password, PASSWORD));
        assertFalse(Autenticazione.controllaPassword(password, "md5$600000$AAAA$AAAA"));
        assertFalse(Autenticazione.controllaPassword(password, "pbkdf2_sha256$molte$AAAA$AAAA"));
        assertFalse(Autenticazione.controllaPassword(password, "pbkdf2_sha256$600000$non base64!$AAAA"));
        assertFalse(Autenticazione.controllaPassword(password, "pbkdf2_sha256$600000$$"));
    }

    @Test
    void credenzialiCorretteVengonoAccettate() throws Exception {
        dao.inserisci(new Utente("mario", hashDellaPassword));

        assertTrue(autenticazione.verificaCredenziali("mario", PASSWORD.toCharArray()));
    }

    @Test
    void utenteInesistenteVieneRifiutato() throws Exception {
        dao.inserisci(new Utente("mario", hashDellaPassword));

        assertFalse(autenticazione.verificaCredenziali("luigi", PASSWORD.toCharArray()));
        assertFalse(autenticazione.verificaCredenziali("", PASSWORD.toCharArray()));
    }

    @Test
    void passwordErrataVieneRifiutata() throws Exception {
        dao.inserisci(new Utente("mario", hashDellaPassword));

        assertFalse(autenticazione.verificaCredenziali("mario", "sbagliata".toCharArray()));
        assertFalse(autenticazione.verificaCredenziali("mario", new char[0]));
    }

    @Test
    void verificaNonModificaLaPasswordRicevuta() throws Exception {
        dao.inserisci(new Utente("mario", hashDellaPassword));
        char[] password = PASSWORD.toCharArray();

        autenticazione.verificaCredenziali("mario", password);

        assertEquals(PASSWORD, new String(password));
    }

    @Test
    void creaUtenteInizialeCreaAdminQuandoNonCiSonoUtenti() throws Exception {
        autenticazione.creaUtenteIniziale();

        assertEquals(1, dao.utenti.size());
        assertTrue(autenticazione.verificaCredenziali("admin", "admin".toCharArray()));
        assertFalse(autenticazione.verificaCredenziali("admin", "Admin".toCharArray()));
        assertNotEquals("admin", dao.utenti.get(0).getPasswordHash());
    }

    @Test
    void creaUtenteInizialeNonLoDuplicaEnonLoRicalcola() throws Exception {
        autenticazione.creaUtenteIniziale();
        String hashIniziale = dao.utenti.get(0).getPasswordHash();

        autenticazione.creaUtenteIniziale();

        assertEquals(1, dao.utenti.size());
        assertEquals(hashIniziale, dao.utenti.get(0).getPasswordHash());
    }

    @Test
    void creaUtenteInizialeNonFaNienteSeCiSonoGiaUtenti() throws Exception {
        dao.inserisci(new Utente("mario", hashDellaPassword));

        autenticazione.creaUtenteIniziale();

        assertEquals(1, dao.utenti.size());
        assertFalse(autenticazione.verificaCredenziali("admin", "admin".toCharArray()));
    }

    private static class UtentiInMemoria implements UtenteDao {

        final List<Utente> utenti = new ArrayList<>();

        @Override
        public Optional<Utente> trova(String username) {
            return utenti.stream().filter(utente -> utente.getUsername().equals(username)).findFirst();
        }

        @Override
        public boolean esistonoUtenti() {
            return !utenti.isEmpty();
        }

        @Override
        public void inserisci(Utente utente) {
            utenti.add(utente);
        }
    }
}
