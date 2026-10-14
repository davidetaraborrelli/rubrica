package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePersonaDaoTest {

    private static final List<String> ESEMPIO_DOCUMENTO = Arrays.asList(
            "Steve;Jobs;via Cupertino 13;0612344;56",
            "Bill;Gates;via Redmond 10;06688989;60",
            "Babbo;Natale;via del Polo Nord;00000111;99");

    @TempDir
    Path cartella;

    private Path percorso;
    private FilePersonaDao dao;

    @BeforeEach
    void preparaDao() {
        percorso = cartella.resolve("informazioni.txt");
        dao = new FilePersonaDao(percorso.toFile());
    }

    @Test
    void fileAssenteDaUnElencoVuotoSenzaCrearlo() throws Exception {
        assertTrue(dao.caricaTutte().isEmpty());
        assertFalse(Files.exists(percorso));
    }

    @Test
    void leggeIlFormatoDelDocumento() throws Exception {
        Files.write(percorso, ESEMPIO_DOCUMENTO, UTF_8);

        List<Persona> persone = dao.caricaTutte();

        assertEquals(3, persone.size());
        verificaPersona(persone.get(0), "Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        verificaPersona(persone.get(1), "Bill", "Gates", "via Redmond 10", "06688989", 60);
        verificaPersona(persone.get(2), "Babbo", "Natale", "via del Polo Nord", "00000111", 99);
    }

    @Test
    void scriveIlFormatoDelDocumento() throws Exception {
        dao.inserisci(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));
        dao.inserisci(new Persona("Bill", "Gates", "via Redmond 10", "06688989", 60));
        dao.inserisci(new Persona("Babbo", "Natale", "via del Polo Nord", "00000111", 99));

        assertEquals(ESEMPIO_DOCUMENTO, Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void inserimentoAssegnaIdDiversi() throws Exception {
        Persona steve = new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56);
        Persona bill = new Persona("Bill", "Gates", "via Redmond 10", "06688989", 60);

        dao.inserisci(steve);
        dao.inserisci(bill);

        assertNotEquals(0, steve.getId());
        assertNotEquals(0, bill.getId());
        assertNotEquals(steve.getId(), bill.getId());
    }

    @Test
    void modificaRiscriveSoloLaPersonaIndicata() throws Exception {
        Files.write(percorso, ESEMPIO_DOCUMENTO, UTF_8);
        Persona bill = dao.caricaTutte().get(1);

        bill.setIndirizzo("via Medina 1");
        dao.aggiorna(bill);

        assertEquals(Arrays.asList(
                ESEMPIO_DOCUMENTO.get(0),
                "Bill;Gates;via Medina 1;06688989;60",
                ESEMPIO_DOCUMENTO.get(2)), Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void eliminazioneTogliePersonaDalFile() throws Exception {
        Files.write(percorso, ESEMPIO_DOCUMENTO, UTF_8);
        Persona bill = dao.caricaTutte().get(1);

        dao.elimina(bill);

        assertEquals(Arrays.asList(ESEMPIO_DOCUMENTO.get(0), ESEMPIO_DOCUMENTO.get(2)),
                Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void personeLetteNonSonoCollegateAllArchivio() throws Exception {
        Files.write(percorso, ESEMPIO_DOCUMENTO, UTF_8);
        List<Persona> persone = dao.caricaTutte();

        persone.get(0).setNome("Steven");
        dao.elimina(persone.get(2));

        assertEquals(ESEMPIO_DOCUMENTO.get(0), Files.readAllLines(percorso, UTF_8).get(0));
    }

    @Test
    void unNuovoDaoRileggeQuantoSalvato() throws Exception {
        dao.inserisci(new Persona("Niccolò", "Machiavelli", "via Guicciardini 16, Firenze", "055123456", 58));

        List<Persona> rilette = new FilePersonaDao(percorso.toFile()).caricaTutte();

        assertEquals(1, rilette.size());
        verificaPersona(rilette.get(0), "Niccolò", "Machiavelli", "via Guicciardini 16, Firenze", "055123456", 58);
    }

    @Test
    void scriveInUtf8() throws Exception {
        dao.inserisci(new Persona("Niccolò", "Machiavelli", "via Guicciardini 16", "055123456", 58));

        String contenuto = new String(Files.readAllBytes(percorso), UTF_8);

        assertEquals("Niccolò;Machiavelli;via Guicciardini 16;055123456;58", contenuto.trim());
    }

    @Test
    void ignoraLeRigheVuote() throws Exception {
        Files.write(percorso, Arrays.asList("", ESEMPIO_DOCUMENTO.get(0), "   ", ESEMPIO_DOCUMENTO.get(1), ""), UTF_8);

        assertEquals(2, dao.caricaTutte().size());
    }

    @Test
    void rigaConCampiMancantiIndicaIlNumeroDiRiga() throws Exception {
        Files.write(percorso, Arrays.asList(ESEMPIO_DOCUMENTO.get(0), "Mario;Rossi;via Roma 1;0612345"), UTF_8);

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("riga 2"), errore.getMessage());
    }

    @Test
    void rigaConCampiInPiuNonEValida() throws Exception {
        Files.write(percorso, Arrays.asList("Mario;Rossi;via Roma 1;0612345;30;"), UTF_8);

        assertThrows(PersistenzaException.class, () -> dao.caricaTutte());
    }

    @Test
    void etaNonNumericaIndicaIlNumeroDiRiga() throws Exception {
        Files.write(percorso, Arrays.asList("Mario;Rossi;via Roma 1;0612345;trenta"), UTF_8);

        PersistenzaException errore = assertThrows(PersistenzaException.class, () -> dao.caricaTutte());

        assertTrue(errore.getMessage().contains("riga 1"), errore.getMessage());
    }

    @Test
    void fileNonValidoNonVieneSovrascritto() throws Exception {
        Files.write(percorso, Arrays.asList(ESEMPIO_DOCUMENTO.get(0), "riga rovinata"), UTF_8);
        byte[] originale = Files.readAllBytes(percorso);

        assertThrows(PersistenzaException.class,
                () -> dao.inserisci(new Persona("Mario", "Rossi", "via Roma 1", "0612345", 30)));

        assertArrayEquals(originale, Files.readAllBytes(percorso));
    }

    @Test
    void fileNonInUtf8DaErroreInveceDiEssereLettoAMeta() throws Exception {
        Files.write(percorso, Arrays.asList(ESEMPIO_DOCUMENTO.get(0), "Niccolò;Machiavelli;via Firenze 1;055123;58"),
                ISO_8859_1);

        assertThrows(PersistenzaException.class, () -> dao.caricaTutte());
    }

    @Test
    void separatoreNeiDatiVieneRifiutato() throws Exception {
        dao.inserisci(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));
        Persona mario = new Persona("Mario", "Rossi", "via Roma; 1", "0612345", 30);

        assertThrows(PersistenzaException.class, () -> dao.inserisci(mario));

        assertEquals(0, mario.getId());
        assertEquals(Arrays.asList(ESEMPIO_DOCUMENTO.get(0)), Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void scritturaFallitaNonLasciaTracce() throws Exception {
        dao.caricaTutte();
        // Una cartella con lo stesso nome rende il file impossibile da scrivere
        Files.createDirectory(percorso);
        Persona mario = new Persona("Mario", "Rossi", "via Roma 1", "0612345", 30);

        assertThrows(PersistenzaException.class, () -> dao.inserisci(mario));
        assertEquals(0, mario.getId());

        Files.delete(percorso);
        dao.inserisci(new Persona("Steve", "Jobs", "via Cupertino 13", "0612344", 56));
        assertEquals(Arrays.asList(ESEMPIO_DOCUMENTO.get(0)), Files.readAllLines(percorso, UTF_8));
    }

    private static void verificaPersona(Persona persona, String nome, String cognome, String indirizzo,
                                        String telefono, int eta) {
        assertEquals(nome, persona.getNome());
        assertEquals(cognome, persona.getCognome());
        assertEquals(indirizzo, persona.getIndirizzo());
        assertEquals(telefono, persona.getTelefono());
        assertEquals(eta, persona.getEta());
    }
}
