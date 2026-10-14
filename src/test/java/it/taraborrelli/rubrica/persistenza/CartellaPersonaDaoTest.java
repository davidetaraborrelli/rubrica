package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartellaPersonaDaoTest {

    private static final String RIGA_STEVE = "Steve;Jobs;via Cupertino 13;0612344;56";
    private static final String RIGA_BILL = "Bill;Gates;via Redmond 10;06688989;60";
    private static final String RIGA_BABBO = "Babbo;Natale;via del Polo Nord;00000111;99";

    @TempDir
    Path radice;

    private Path cartella;
    private CartellaPersonaDao dao;

    @BeforeEach
    void preparaDao() {
        // La cartella non esiste: la crea il DAO alla prima lettura
        cartella = radice.resolve("informazioni");
        dao = new CartellaPersonaDao(cartella.toFile());
    }

    @Test
    void cartellaAssenteVieneCreataEDaUnElencoVuoto() throws Exception {
        assertTrue(dao.caricaTutte().isEmpty());
        assertTrue(Files.isDirectory(cartella));
    }

    @Test
    void inserimentoCreaLaCartellaEUnFilePerPersona() throws Exception {
        Persona steve = steve();
        Persona bill = bill();

        dao.inserisci(steve);
        dao.inserisci(bill);

        assertEquals(1, steve.getId());
        assertEquals(2, bill.getId());
        assertEquals(Arrays.asList(RIGA_STEVE), righe("Persona1.txt"));
        assertEquals(Arrays.asList(RIGA_BILL), righe("Persona2.txt"));
    }

    @Test
    void leggeLePersoneInOrdineDiIdNonInOrdineAlfabetico() throws Exception {
        scrivi("Persona10.txt", RIGA_BABBO);
        scrivi("Persona2.txt", RIGA_BILL);
        scrivi("Persona1.txt", RIGA_STEVE);

        List<Persona> persone = dao.caricaTutte();

        assertEquals(3, persone.size());
        verificaPersona(persone.get(0), 1, "Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        verificaPersona(persone.get(1), 2, "Bill", "Gates", "via Redmond 10", "06688989", 60);
        verificaPersona(persone.get(2), 10, "Babbo", "Natale", "via del Polo Nord", "00000111", 99);
    }

    @Test
    void ilNuovoIdParteDalMassimoEsistentePiuUno() throws Exception {
        scrivi("Persona2.txt", RIGA_BILL);
        scrivi("Persona7.txt", RIGA_BABBO);
        Persona steve = steve();

        // Il DAO non ha ancora letto la cartella: lo fa da sé prima di scegliere l'id
        dao.inserisci(steve);

        assertEquals(8, steve.getId());
        assertEquals(Arrays.asList(RIGA_STEVE), righe("Persona8.txt"));
        assertEquals(Arrays.asList(RIGA_BILL), righe("Persona2.txt"));
        assertEquals(Arrays.asList(RIGA_BABBO), righe("Persona7.txt"));
    }

    @Test
    void gliIdRestanoStabiliTraUnAvvioELAltro() throws Exception {
        dao.inserisci(steve());
        dao.inserisci(bill());
        dao.inserisci(babbo());
        dao.elimina(dao.caricaTutte().get(0));

        List<Persona> rilette = new CartellaPersonaDao(cartella.toFile()).caricaTutte();

        assertEquals(2, rilette.size());
        verificaPersona(rilette.get(0), 2, "Bill", "Gates", "via Redmond 10", "06688989", 60);
        verificaPersona(rilette.get(1), 3, "Babbo", "Natale", "via del Polo Nord", "00000111", 99);
    }

    @Test
    void modificaRiscriveSoloIlFileDellaPersona() throws Exception {
        scrivi("Persona1.txt", RIGA_STEVE);
        scrivi("Persona2.txt", RIGA_BILL);
        scrivi("Persona3.txt", RIGA_BABBO);
        Persona bill = dao.caricaTutte().get(1);

        bill.setIndirizzo("via Medina 1");
        dao.aggiorna(bill);

        assertEquals(Arrays.asList("Bill;Gates;via Medina 1;06688989;60"), righe("Persona2.txt"));
        assertEquals(Arrays.asList(RIGA_STEVE), righe("Persona1.txt"));
        assertEquals(Arrays.asList(RIGA_BABBO), righe("Persona3.txt"));
    }

    @Test
    void eliminazioneCancellaIlFileDellaPersona() throws Exception {
        scrivi("Persona1.txt", RIGA_STEVE);
        scrivi("Persona2.txt", RIGA_BILL);
        scrivi("Persona3.txt", RIGA_BABBO);
        Persona bill = dao.caricaTutte().get(1);

        dao.elimina(bill);

        assertFalse(Files.exists(cartella.resolve("Persona2.txt")));
        assertTrue(Files.exists(cartella.resolve("Persona1.txt")));
        assertTrue(Files.exists(cartella.resolve("Persona3.txt")));
        assertEquals(2, new CartellaPersonaDao(cartella.toFile()).caricaTutte().size());
    }

    @Test
    void personeConLoStessoNomeStannoInFileDiversi() throws Exception {
        Persona primo = new Persona("Mario", "Rossi", "via Roma 1", "0611111", 30);
        Persona secondo = new Persona("Mario", "Rossi", "via Milano 2", "0622222", 40);
        dao.inserisci(primo);
        dao.inserisci(secondo);

        secondo.setTelefono("0633333");
        dao.aggiorna(secondo);
        List<Persona> rilette = new CartellaPersonaDao(cartella.toFile()).caricaTutte();

        assertEquals(2, rilette.size());
        verificaPersona(rilette.get(0), 1, "Mario", "Rossi", "via Roma 1", "0611111", 30);
        verificaPersona(rilette.get(1), 2, "Mario", "Rossi", "via Milano 2", "0633333", 40);
    }

    @Test
    void scriveELeggeInUtf8() throws Exception {
        dao.inserisci(new Persona("Niccolò", "Machiavelli", "via Guicciardini 16, Firenze", "055123456", 58));

        String contenuto = new String(Files.readAllBytes(cartella.resolve("Persona1.txt")), UTF_8);
        List<Persona> rilette = new CartellaPersonaDao(cartella.toFile()).caricaTutte();

        assertEquals("Niccolò;Machiavelli;via Guicciardini 16, Firenze;055123456;58", contenuto.trim());
        verificaPersona(rilette.get(0), 1, "Niccolò", "Machiavelli", "via Guicciardini 16, Firenze", "055123456", 58);
    }

    @Test
    void ignoraIFileCheNonHannoIlNomeDiUnaPersona() throws Exception {
        scrivi("Persona1.txt", RIGA_STEVE);
        for (String nome : Arrays.asList("note.txt", "Persona.txt", "Persona0.txt", "Persona01.txt",
                "Persona2.txt.bak", "persona3.txt", "Persona4.dat")) {
            scrivi(nome, "contenuto che non è una persona");
        }
        Files.createDirectory(cartella.resolve("Persona5.txt"));

        List<Persona> persone = dao.caricaTutte();

        assertEquals(1, persone.size());
        assertEquals("Steve", persone.get(0).getNome());
    }

    @Test
    void ignoraLeRigheVuote() throws Exception {
        scrivi("Persona1.txt", "", RIGA_STEVE, "   ", "");

        List<Persona> persone = dao.caricaTutte();

        assertEquals(1, persone.size());
        assertEquals("Steve", persone.get(0).getNome());
    }

    @Test
    void fileConCampiMancantiIndicaIlFileELaRiga() throws Exception {
        scrivi("Persona1.txt", RIGA_STEVE);
        scrivi("Persona2.txt", "", "Mario;Rossi;via Roma 1;0612345");

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("Persona2.txt"), errore.getMessage());
        assertTrue(errore.getMessage().contains("riga 2"), errore.getMessage());
    }

    @Test
    void etaNonNumericaIndicaIlFileELaRiga() throws Exception {
        scrivi("Persona1.txt", "Mario;Rossi;via Roma 1;0612345;trenta");

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("Persona1.txt"), errore.getMessage());
        assertTrue(errore.getMessage().contains("riga 1"), errore.getMessage());
    }

    @Test
    void fileVuotoNonEValido() throws Exception {
        scrivi("Persona1.txt", "", "  ");

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("Persona1.txt"), errore.getMessage());
    }

    @Test
    void fileConPiuPersoneNonEValido() throws Exception {
        scrivi("Persona1.txt", RIGA_STEVE, RIGA_BILL);

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("Persona1.txt"), errore.getMessage());
    }

    @Test
    void fileNonInUtf8DaErroreInveceDiEssereLettoAMeta() throws Exception {
        Files.createDirectories(cartella);
        Files.write(cartella.resolve("Persona1.txt"),
                Arrays.asList("Niccolò;Machiavelli;via Firenze 1;055123;58"), ISO_8859_1);

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("UTF-8"), errore.getMessage());
    }

    @Test
    void fileNonValidoNonVieneSovrascrittoDaUnInserimento() throws Exception {
        scrivi("Persona1.txt", "riga rovinata");
        byte[] originale = Files.readAllBytes(cartella.resolve("Persona1.txt"));

        assertThrows(PersistenzaException.class, () -> dao.inserisci(steve()));

        assertArrayEquals(originale, Files.readAllBytes(cartella.resolve("Persona1.txt")));
        assertFalse(Files.exists(cartella.resolve("Persona2.txt")));
    }

    @Test
    void separatoreNeiDatiVieneRifiutatoSenzaCreareFile() throws Exception {
        Persona mario = new Persona("Mario", "Rossi", "via Roma; 1", "0612345", 30);

        assertThrows(PersistenzaException.class, () -> dao.inserisci(mario));

        assertEquals(0, mario.getId());
        assertFalse(Files.exists(cartella.resolve("Persona1.txt")));
    }

    @Test
    void modificaConSeparatoreNeiDatiLasciaIlFileComeEra() throws Exception {
        Persona steve = steve();
        dao.inserisci(steve);

        steve.setNome("Ste;ve");

        assertThrows(PersistenzaException.class, () -> dao.aggiorna(steve));
        assertEquals(Arrays.asList(RIGA_STEVE), righe("Persona1.txt"));
    }

    @Test
    void sePercorsoEUnFileNonEUnaCartella() throws Exception {
        Files.write(cartella, Arrays.asList("non sono una cartella"), UTF_8);

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("cartella"), errore.getMessage());
    }

    @Test
    void inserimentoRicreaLaCartellaCancellataMentreIlProgrammaEAperto() throws Exception {
        dao.caricaTutte();
        Files.delete(cartella);
        Persona steve = steve();

        dao.inserisci(steve);

        assertEquals(1, steve.getId());
        assertEquals(Arrays.asList(RIGA_STEVE), righe("Persona1.txt"));
    }

    @Test
    void inserimentoNonSovrascriveUnFileComparsoNelFrattempo() throws Exception {
        dao.caricaTutte();
        scrivi("Persona1.txt", RIGA_BILL);
        Persona steve = steve();

        assertThrows(PersistenzaException.class, () -> dao.inserisci(steve));

        assertEquals(0, steve.getId());
        assertEquals(Arrays.asList(RIGA_BILL), righe("Persona1.txt"));
    }

    @Test
    void personaEliminataAltroveVieneSegnalata() throws Exception {
        Persona steve = steve();
        dao.inserisci(steve);
        Files.delete(cartella.resolve("Persona1.txt"));

        assertThrows(PersistenzaException.class, () -> dao.aggiorna(steve));
        assertThrows(PersistenzaException.class, () -> dao.elimina(steve));

        assertFalse(Files.exists(cartella.resolve("Persona1.txt")));
    }

    @Test
    void scritturaFallitaNonLasciaTracce() throws Exception {
        dao.caricaTutte();
        // Una cartella con lo stesso nome rende il file impossibile da creare
        Files.createDirectories(cartella.resolve("Persona1.txt"));
        Persona mario = new Persona("Mario", "Rossi", "via Roma 1", "0612345", 30);

        assertThrows(PersistenzaException.class, () -> dao.inserisci(mario));
        assertEquals(0, mario.getId());

        Files.delete(cartella.resolve("Persona1.txt"));
        Persona steve = steve();
        dao.inserisci(steve);
        assertEquals(1, steve.getId());
        assertEquals(Arrays.asList(RIGA_STEVE), righe("Persona1.txt"));
    }

    private void scrivi(String nomeFile, String... contenuto) throws IOException {
        Files.createDirectories(cartella);
        Files.write(cartella.resolve(nomeFile), Arrays.asList(contenuto), UTF_8);
    }

    private List<String> righe(String nomeFile) throws IOException {
        return Files.readAllLines(cartella.resolve(nomeFile), UTF_8);
    }

    private static Persona steve() {
        return new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56);
    }

    private static Persona bill() {
        return new Persona("Bill", "Gates", "via Redmond 10", "06688989", 60);
    }

    private static Persona babbo() {
        return new Persona("Babbo", "Natale", "via del Polo Nord", "00000111", 99);
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
