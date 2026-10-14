package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
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
    private static final String SEPARATORE = ";";
    private static final int NUMERO_CAMPI = 5;

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
        String riga = formattaRiga(persona);
        creaCartella();
        // CREATE_NEW rifiuta un file già presente: non si sovrascrive mai la persona di un altro file
        scriviFile(fileDi(prossimoId), riga, StandardOpenOption.CREATE_NEW);
        persona.setId(prossimoId);
        prossimoId++;
    }

    @Override
    public void aggiorna(Persona persona) throws PersistenzaException {
        String riga = formattaRiga(persona);
        scriviFile(fileEsistente(persona.getId()), riga);
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
        List<String> righe;
        try {
            righe = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            throw new PersistenzaException("Impossibile leggere il file " + file.getAbsolutePath()
                    + ": verificare che sia salvato in UTF-8.", e);
        } catch (IOException e) {
            throw new PersistenzaException("Impossibile leggere il file " + file.getAbsolutePath() + ".", e);
        }
        Persona persona = null;
        for (int i = 0; i < righe.size(); i++) {
            if (righe.get(i).trim().isEmpty()) {
                continue;
            }
            if (persona != null) {
                throw new PersistenzaException("Il file " + file.getAbsolutePath()
                        + " contiene più di una persona: ne deve contenere una sola.");
            }
            persona = interpretaRiga(file, righe.get(i), i + 1);
        }
        if (persona == null) {
            throw new PersistenzaException("Il file " + file.getAbsolutePath()
                    + " è vuoto: deve contenere una persona.");
        }
        persona.setId(id);
        return persona;
    }

    private Persona interpretaRiga(File file, String riga, int numeroRiga) throws PersistenzaException {
        // Con -1 i campi vuoti in fondo non vengono scartati, quindi "a;b;c;d;5;" risulta di 6 campi
        String[] campi = riga.split(SEPARATORE, -1);
        if (campi.length != NUMERO_CAMPI) {
            throw rigaNonValida(file, numeroRiga, "attesi " + NUMERO_CAMPI + " campi separati da '" + SEPARATORE
                    + "', trovati " + campi.length);
        }
        int eta;
        try {
            eta = Integer.parseInt(campi[4].trim());
        } catch (NumberFormatException e) {
            throw rigaNonValida(file, numeroRiga, "l'età \"" + campi[4] + "\" non è un numero intero");
        }
        return new Persona(campi[0], campi[1], campi[2], campi[3], eta);
    }

    private static PersistenzaException rigaNonValida(File file, int numeroRiga, String motivo) {
        return new PersistenzaException("Il file " + file.getAbsolutePath() + " non è valido alla riga "
                + numeroRiga + ": " + motivo + ".");
    }

    private static void scriviFile(File file, String riga, OpenOption... opzioni) throws PersistenzaException {
        try {
            Files.write(file.toPath(), Collections.singletonList(riga), StandardCharsets.UTF_8, opzioni);
        } catch (IOException e) {
            throw new PersistenzaException("Impossibile scrivere il file " + file.getAbsolutePath() + ".", e);
        }
    }

    private static String formattaRiga(Persona persona) throws PersistenzaException {
        return campo(persona.getNome()) + SEPARATORE
                + campo(persona.getCognome()) + SEPARATORE
                + campo(persona.getIndirizzo()) + SEPARATORE
                + campo(persona.getTelefono()) + SEPARATORE
                + persona.getEta();
    }

    private static String campo(String valore) throws PersistenzaException {
        if (valore.contains(SEPARATORE) || valore.contains("\n") || valore.contains("\r")) {
            throw new PersistenzaException("I dati non possono contenere '" + SEPARATORE
                    + "' o ritorni a capo: \"" + valore + "\".");
        }
        return valore;
    }
}
