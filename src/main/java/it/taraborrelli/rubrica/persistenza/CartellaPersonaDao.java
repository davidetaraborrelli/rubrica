package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Salva ogni persona in un file a sé, {@code Persona<id>.txt}, dentro una cartella che alla prima
 * lettura viene creata se manca. Il file ha una sola riga, nello stesso formato di
 * {@code informazioni.txt}: {@code nome;cognome;indirizzo;telefono;eta}.
 * <p>
 * L'id è il numero nel nome del file: resta lo stesso a ogni avvio e due persone con gli stessi dati
 * non si sovrascrivono. I file con un altro nome sono ignorati.
 */
public class CartellaPersonaDao implements PersonaDao {

    // Senza zeri iniziali, così a ogni id corrisponde un solo nome di file
    private static final Pattern NOME_FILE = Pattern.compile("Persona([1-9][0-9]{0,8})\\.txt");

    private final File cartella;
    // Id della prossima persona inserita; 0 finché la cartella non è stata letta
    private int prossimoId;

    public CartellaPersonaDao(File cartella) {
        this.cartella = cartella;
    }

    @Override
    public List<Persona> caricaTutte() throws PersistenzaException {
        creaCartella();
        List<Persona> lette = new ArrayList<>();
        for (Map.Entry<Integer, File> voce : fileDellePersone().entrySet()) {
            lette.add(leggiFile(voce.getValue(), voce.getKey()));
        }
        prossimoId = lette.isEmpty() ? 1 : lette.get(lette.size() - 1).getId() + 1;
        return lette;
    }

    @Override
    public void inserisci(Persona persona) throws PersistenzaException {
        // Senza aver letto la cartella non si sa quale id sia libero
        if (prossimoId == 0) {
            caricaTutte();
        }
        String riga = FormatoPersona.formatta(persona);
        creaCartella();
        // scriviNuovo rifiuta un file già presente: non si sovrascrive mai la persona di un altro file
        FileDiTesto.scriviNuovo(fileDi(prossimoId), Collections.singletonList(riga));
        persona.setId(prossimoId);
        prossimoId++;
    }

    @Override
    public void aggiorna(Persona persona) throws PersistenzaException {
        String riga = FormatoPersona.formatta(persona);
        FileDiTesto.scrivi(fileEsistente(persona.getId()), Collections.singletonList(riga));
    }

    @Override
    public void elimina(Persona persona) throws PersistenzaException {
        File file = fileEsistente(persona.getId());
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            throw new PersistenzaException("Impossibile eliminare il file " + file.getAbsolutePath() + ".", e);
        }
    }

    private File fileDi(int id) {
        return new File(cartella, "Persona" + id + ".txt");
    }

    // I file stanno in una cartella visibile: qualcuno può averli cancellati mentre il programma è aperto
    private File fileEsistente(int id) throws PersistenzaException {
        File file = fileDi(id);
        if (!file.isFile()) {
            throw new PersistenzaException("La persona non è più presente nella cartella "
                    + cartella.getAbsolutePath() + ".");
        }
        return file;
    }

    // La cartella è il posto fisso dei dati: si crea già all'avvio, e di nuovo al salvataggio se
    // qualcuno l'ha cancellata mentre il programma era aperto
    private void creaCartella() throws PersistenzaException {
        if (cartella.isDirectory()) {
            return;
        }
        if (cartella.exists()) {
            throw new PersistenzaException("Impossibile usare " + cartella.getAbsolutePath()
                    + " come cartella dei dati: esiste già un file con questo nome.");
        }
        try {
            Files.createDirectories(cartella.toPath());
        } catch (IOException e) {
            throw new PersistenzaException("Impossibile creare la cartella " + cartella.getAbsolutePath() + ".", e);
        }
    }

    // Restituisce i file delle persone ordinati per id, cioè nell'ordine di inserimento
    private SortedMap<Integer, File> fileDellePersone() throws PersistenzaException {
        SortedMap<Integer, File> trovati = new TreeMap<>();
        File[] contenuto = cartella.listFiles();
        if (contenuto == null) {
            throw new PersistenzaException("Impossibile leggere la cartella " + cartella.getAbsolutePath() + ".");
        }
        for (File file : contenuto) {
            Matcher nome = NOME_FILE.matcher(file.getName());
            if (nome.matches() && file.isFile()) {
                trovati.put(Integer.valueOf(nome.group(1)), file);
            }
        }
        return trovati;
    }

    private Persona leggiFile(File file, int id) throws PersistenzaException {
        List<Persona> contenuto = FileDiTesto.leggi(file,
                (riga, numeroRiga) -> FormatoPersona.interpreta(file, riga, numeroRiga));
        if (contenuto.isEmpty()) {
            throw new PersistenzaException("Il file " + file.getAbsolutePath()
                    + " è vuoto: deve contenere una persona.");
        }
        if (contenuto.size() > 1) {
            throw new PersistenzaException("Il file " + file.getAbsolutePath()
                    + " contiene più di una persona: ne deve contenere una sola.");
        }
        Persona persona = contenuto.get(0);
        persona.setId(id);
        return persona;
    }
}
