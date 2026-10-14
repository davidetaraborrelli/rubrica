package it.taraborrelli.rubrica.logica;

import it.taraborrelli.rubrica.modello.Persona;
import it.taraborrelli.rubrica.persistenza.FilePersonaDao;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * I casi di test del documento, ripetuti sulla logica senza interfaccia grafica.
 * Ogni chiamata a {@link #apriRubrica()} equivale a un nuovo avvio del programma.
 */
class RubricaTest {

    private static final String RIGA_MARIO = "Mario;Rossi;via Roma 1;0612345;30";
    private static final String RIGA_LUIGI = "Luigi;Verdi;via Milano 2;0698765;45";
    private static final String RIGA_ANNA = "Anna;Bianchi;via Napoli 3;0655555;28";

    @TempDir
    Path cartella;

    private Path percorso;

    @BeforeEach
    void preparaPercorso() {
        percorso = cartella.resolve("informazioni.txt");
    }

    @Test
    void personaAggiuntaCompareNellaLista() throws Exception {
        Rubrica rubrica = apriRubrica();

        rubrica.aggiungi(mario());

        assertEquals(1, rubrica.getPersone().size());
        verificaPersona(rubrica.getPersone().get(0), "Mario", "Rossi", "via Roma 1", "0612345", 30);
    }

    @Test
    void eliminaQuellaInMezzoEModificaLUltima() throws Exception {
        Rubrica rubrica = apriRubrica();
        rubrica.aggiungi(mario());
        rubrica.aggiungi(luigi());
        rubrica.aggiungi(anna());

        rubrica.elimina(rubrica.getPersone().get(1));
        Persona annaModificata = new Persona(rubrica.getPersone().get(1));
        annaModificata.setTelefono("3331234567");
        rubrica.modifica(annaModificata);

        verificaMarioEAnnaModificata(rubrica.getPersone());
        verificaMarioEAnnaModificata(apriRubrica().getPersone());
        assertEquals(Arrays.asList(RIGA_MARIO, "Anna;Bianchi;via Napoli 3;3331234567;28"),
                Files.readAllLines(percorso, UTF_8));
    }

    @Test
    void personeMantenuteTraUnAvvioELAltro() throws Exception {
        apriRubrica().aggiungi(mario());
        assertEquals(Arrays.asList(RIGA_MARIO), Files.readAllLines(percorso, UTF_8));

        Rubrica secondoAvvio = apriRubrica();
        assertEquals(1, secondoAvvio.getPersone().size());
        verificaPersona(secondoAvvio.getPersone().get(0), "Mario", "Rossi", "via Roma 1", "0612345", 30);
        secondoAvvio.aggiungi(luigi());
        secondoAvvio.aggiungi(anna());
        assertEquals(Arrays.asList(RIGA_MARIO, RIGA_LUIGI, RIGA_ANNA), Files.readAllLines(percorso, UTF_8));

        List<Persona> terzoAvvio = apriRubrica().getPersone();
        assertEquals(3, terzoAvvio.size());
        verificaPersona(terzoAvvio.get(0), "Mario", "Rossi", "via Roma 1", "0612345", 30);
        verificaPersona(terzoAvvio.get(1), "Luigi", "Verdi", "via Milano 2", "0698765", 45);
        verificaPersona(terzoAvvio.get(2), "Anna", "Bianchi", "via Napoli 3", "0655555", 28);
    }

    @Test
    void salvataggioFallitoLasciaLaListaInvariata() throws Exception {
        Rubrica rubrica = apriRubrica();
        rubrica.aggiungi(mario());
        rubrica.aggiungi(luigi());
        // Una cartella con lo stesso nome rende il file impossibile da scrivere
        Files.delete(percorso);
        Files.createDirectory(percorso);

        Persona marioModificato = new Persona(rubrica.getPersone().get(0));
        marioModificato.setTelefono("000");
        assertThrows(PersistenzaException.class, () -> rubrica.modifica(marioModificato));
        assertThrows(PersistenzaException.class, () -> rubrica.elimina(rubrica.getPersone().get(1)));
        assertThrows(PersistenzaException.class, () -> rubrica.aggiungi(anna()));

        assertEquals(2, rubrica.getPersone().size());
        verificaPersona(rubrica.getPersone().get(0), "Mario", "Rossi", "via Roma 1", "0612345", 30);
        verificaPersona(rubrica.getPersone().get(1), "Luigi", "Verdi", "via Milano 2", "0698765", 45);
    }

    private Rubrica apriRubrica() throws PersistenzaException {
        Rubrica rubrica = new Rubrica(new FilePersonaDao(percorso.toFile()));
        rubrica.carica();
        return rubrica;
    }

    private static Persona mario() {
        return new Persona("Mario", "Rossi", "via Roma 1", "0612345", 30);
    }

    private static Persona luigi() {
        return new Persona("Luigi", "Verdi", "via Milano 2", "0698765", 45);
    }

    private static Persona anna() {
        return new Persona("Anna", "Bianchi", "via Napoli 3", "0655555", 28);
    }

    private static void verificaMarioEAnnaModificata(List<Persona> persone) {
        assertEquals(2, persone.size());
        verificaPersona(persone.get(0), "Mario", "Rossi", "via Roma 1", "0612345", 30);
        verificaPersona(persone.get(1), "Anna", "Bianchi", "via Napoli 3", "3331234567", 28);
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
