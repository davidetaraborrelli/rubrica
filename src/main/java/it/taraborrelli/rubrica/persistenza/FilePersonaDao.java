package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Salva le persone in un file di testo UTF-8, una per riga, nel formato
 * {@code nome;cognome;indirizzo;telefono;eta}.
 * <p>
 * Il file non contiene l'id: viene assegnato alla lettura e vale finché l'applicazione resta
 * aperta. Ogni operazione riscrive l'intero file.
 */
public class FilePersonaDao implements PersonaDao {

    private static final String SEPARATORE = ";";
    private static final String CODIFICA = "UTF-8";
    private static final int NUMERO_CAMPI = 5;

    private final File file;
    // Contenuto del file al momento dell'ultima lettura o scrittura riuscita; null se mai letto
    private List<Persona> persone;
    private int prossimoId;

    public FilePersonaDao(File file) {
        this.file = file;
    }

    @Override
    public List<Persona> caricaTutte() throws PersistenzaException {
        persone = leggiFile();
        prossimoId = persone.size() + 1;
        List<Persona> copie = new ArrayList<>();
        for (Persona persona : persone) {
            copie.add(new Persona(persona));
        }
        return copie;
    }

    @Override
    public void inserisci(Persona persona) throws PersistenzaException {
        List<Persona> nuoveSalvate = new ArrayList<>(personeSalvate());
        Persona copia = new Persona(persona);
        copia.setId(prossimoId);
        nuoveSalvate.add(copia);
        scriviFile(nuoveSalvate);
        persone = nuoveSalvate;
        persona.setId(prossimoId);
        prossimoId++;
    }

    @Override
    public void aggiorna(Persona persona) throws PersistenzaException {
        List<Persona> nuoveSalvate = new ArrayList<>(personeSalvate());
        nuoveSalvate.set(indiceDi(nuoveSalvate, persona.getId()), new Persona(persona));
        scriviFile(nuoveSalvate);
        persone = nuoveSalvate;
    }

    @Override
    public void elimina(Persona persona) throws PersistenzaException {
        List<Persona> nuoveSalvate = new ArrayList<>(personeSalvate());
        nuoveSalvate.remove(indiceDi(nuoveSalvate, persona.getId()));
        scriviFile(nuoveSalvate);
        persone = nuoveSalvate;
    }

    // Scrivere senza aver letto il file lo sovrascriverebbe perdendo le persone già salvate
    private List<Persona> personeSalvate() throws PersistenzaException {
        if (persone == null) {
            caricaTutte();
        }
        return persone;
    }

    private static int indiceDi(List<Persona> elenco, int id) {
        for (int i = 0; i < elenco.size(); i++) {
            if (elenco.get(i).getId() == id) {
                return i;
            }
        }
        throw new IllegalArgumentException("Nessuna persona salvata con id " + id);
    }

    private List<Persona> leggiFile() throws PersistenzaException {
        List<Persona> lette = new ArrayList<>();
        if (!file.exists()) {
            return lette;
        }
        try (Scanner scanner = new Scanner(file, CODIFICA)) {
            int numeroRiga = 0;
            while (scanner.hasNextLine()) {
                String riga = scanner.nextLine();
                numeroRiga++;
                if (riga.trim().isEmpty()) {
                    continue;
                }
                Persona persona = interpretaRiga(riga, numeroRiga);
                persona.setId(lette.size() + 1);
                lette.add(persona);
            }
            // Scanner non lancia IOException: se la lettura si interrompe, ad esempio per un file
            // non in UTF-8, smette di restituire righe e l'errore va chiesto esplicitamente
            if (scanner.ioException() != null) {
                throw new PersistenzaException("Impossibile leggere il file " + file.getAbsolutePath()
                        + ": verificare che sia salvato in UTF-8.", scanner.ioException());
            }
        } catch (FileNotFoundException e) {
            throw new PersistenzaException("Impossibile aprire il file " + file.getAbsolutePath() + ".", e);
        }
        return lette;
    }

    private Persona interpretaRiga(String riga, int numeroRiga) throws PersistenzaException {
        // Con -1 i campi vuoti in fondo non vengono scartati, quindi "a;b;c;d;5;" risulta di 6 campi
        String[] campi = riga.split(SEPARATORE, -1);
        if (campi.length != NUMERO_CAMPI) {
            throw rigaNonValida(numeroRiga, "attesi " + NUMERO_CAMPI + " campi separati da '" + SEPARATORE
                    + "', trovati " + campi.length);
        }
        int eta;
        try {
            eta = Integer.parseInt(campi[4].trim());
        } catch (NumberFormatException e) {
            throw rigaNonValida(numeroRiga, "l'età \"" + campi[4] + "\" non è un numero intero");
        }
        return new Persona(campi[0], campi[1], campi[2], campi[3], eta);
    }

    private PersistenzaException rigaNonValida(int numeroRiga, String motivo) {
        return new PersistenzaException("Il file " + file.getAbsolutePath() + " non è valido alla riga "
                + numeroRiga + ": " + motivo + ".");
    }

    private void scriviFile(List<Persona> daScrivere) throws PersistenzaException {
        // Le righe si preparano prima di aprire il file, che all'apertura viene svuotato
        List<String> righe = new ArrayList<>();
        for (Persona persona : daScrivere) {
            righe.add(formattaRiga(persona));
        }
        try (PrintStream out = new PrintStream(file, CODIFICA)) {
            for (String riga : righe) {
                out.println(riga);
            }
            // PrintStream non lancia eccezioni in scrittura: gli errori si scoprono solo così
            if (out.checkError()) {
                throw new PersistenzaException("Errore durante la scrittura del file " + file.getAbsolutePath() + ".");
            }
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new PersistenzaException("Impossibile scrivere il file " + file.getAbsolutePath() + ".", e);
        }
    }

    private String formattaRiga(Persona persona) throws PersistenzaException {
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
