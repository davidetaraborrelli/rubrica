package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcPersonaDaoTest {

    private DatabaseDiProva database;
    private JdbcPersonaDao dao;

    @BeforeEach
    void preparaDao() throws Exception {
        database = new DatabaseDiProva();
        dao = new JdbcPersonaDao(database.configurazione());
    }

    @Test
    void tabellaVuotaDaUnElencoVuoto() throws Exception {
        assertTrue(dao.caricaTutte().isEmpty());
    }

    @Test
    void inserimentoAssegnaIdDiversiELasciaLeggereTuttiICampi() throws Exception {
        Persona steve = new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        Persona bill = new Persona("Bill", "Gates", "via Redmond 10", "06688989", 60);

        dao.inserisci(steve);
        dao.inserisci(bill);

        assertNotEquals(0, steve.getId());
        assertNotEquals(0, bill.getId());
        assertNotEquals(steve.getId(), bill.getId());
        List<Persona> lette = dao.caricaTutte();
        assertEquals(2, lette.size());
        verificaPersona(lette.get(0), steve.getId(), "Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        verificaPersona(lette.get(1), bill.getId(), "Bill", "Gates", "via Redmond 10", "06688989", 60);
    }

    @Test
    void salvaELeggeGliAccenti() throws Exception {
        dao.inserisci(new Persona("Niccolò", "Machiavelli", "via dell'Università 5, Firenze", "055123456", 58));

        Persona letta = dao.caricaTutte().get(0);

        verificaPersona(letta, letta.getId(), "Niccolò", "Machiavelli", "via dell'Università 5, Firenze",
                "055123456", 58);
    }

    @Test
    void modificaCambiaSoloLaPersonaIndicata() throws Exception {
        Persona steve = new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        Persona bill = new Persona("Bill", "Gates", "via Redmond 10", "06688989", 60);
        dao.inserisci(steve);
        dao.inserisci(bill);

        bill.setIndirizzo("via Medina 1");
        dao.aggiorna(bill);

        List<Persona> lette = dao.caricaTutte();
        verificaPersona(lette.get(0), steve.getId(), "Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        verificaPersona(lette.get(1), bill.getId(), "Bill", "Gates", "via Medina 1", "06688989", 60);
    }

    @Test
    void eliminazioneToglieSoloLaPersonaIndicata() throws Exception {
        Persona steve = new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        Persona bill = new Persona("Bill", "Gates", "via Redmond 10", "06688989", 60);
        dao.inserisci(steve);
        dao.inserisci(bill);

        dao.elimina(steve);

        List<Persona> lette = dao.caricaTutte();
        assertEquals(1, lette.size());
        assertEquals(bill.getId(), lette.get(0).getId());
    }

    @Test
    void personeLetteNonSonoCollegateAllArchivio() throws Exception {
        dao.inserisci(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));
        Persona letta = dao.caricaTutte().get(0);

        letta.setNome("Steven");

        assertEquals("Steve", dao.caricaTutte().get(0).getNome());
    }

    @Test
    void unNuovoDaoRileggeQuantoSalvato() throws Exception {
        dao.inserisci(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));

        List<Persona> rilette = new JdbcPersonaDao(database.configurazione()).caricaTutte();

        assertEquals(1, rilette.size());
        assertEquals("Steve", rilette.get(0).getNome());
    }

    @Test
    void modificareOEliminareUnaPersonaCheNonCEDaErrore() {
        Persona inesistente = new Persona("Mario", "Rossi", "via Roma 1", "0612345", 30);
        inesistente.setId(9999);

        assertThrows(PersistenzaException.class, () -> dao.aggiorna(inesistente));
        assertThrows(PersistenzaException.class, () -> dao.elimina(inesistente));
    }

    @Test
    void inserimentoFallitoNonAssegnaLId() throws Exception {
        database.esegui("DROP TABLE persone");
        Persona mario = new Persona("Mario", "Rossi", "via Roma 1", "0612345", 30);

        assertThrows(PersistenzaException.class, () -> dao.inserisci(mario));

        assertEquals(0, mario.getId());
    }

    @Test
    void serverIrraggiungibileDaUnMessaggioComprensibile() {
        // La porta 1 non ha nessun server: il driver MySQL vero fallisce subito la connessione
        ConfigurazioneDatabase senzaServer = new ConfigurazioneDatabase(
                "jdbc:mysql://localhost:1/rubrica?connectTimeout=2000", "root", "");

        PersistenzaException errore = assertThrows(PersistenzaException.class,
                () -> new JdbcPersonaDao(senzaServer).caricaTutte());

        assertTrue(errore.getMessage().contains("Impossibile raggiungere il database"), errore.getMessage());
        assertTrue(errore.getMessage().contains("localhost:1"), errore.getMessage());
    }

    private static void verificaPersona(Persona persona, int id, String nome, String cognome, String indirizzo,
                                        String telefono, int eta) {
        assertEquals(id, persona.getId());
        assertEquals(nome, persona.getNome());
        assertEquals(cognome, persona.getCognome());
        assertEquals(indirizzo, persona.getIndirizzo());
        assertEquals(telefono, persona.getTelefono());
        assertEquals(eta, persona.getEta());
    }
}
