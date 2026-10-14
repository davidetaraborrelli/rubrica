package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Utente;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Salva gli utenti in un file di testo UTF-8, uno per riga, nel formato {@code username;hash}.
 * Il file viene riletto a ogni operazione e riscritto per intero a ogni inserimento.
 */
public class FileUtenteDao implements UtenteDao {

    private static final String SEPARATORE = ";";
    private static final String CODIFICA = "UTF-8";
    private static final int NUMERO_CAMPI = 2;

    private final File file;

    public FileUtenteDao(File file) {
        this.file = file;
    }

    @Override
    public Optional<Utente> trova(String username) throws PersistenzaException {
        return leggiFile().stream().filter(utente -> utente.getUsername().equals(username)).findFirst();
    }

    @Override
    public boolean esistonoUtenti() throws PersistenzaException {
        return !leggiFile().isEmpty();
    }

    @Override
    public void inserisci(Utente utente) throws PersistenzaException {
        List<Utente> utenti = leggiFile();
        for (Utente esistente : utenti) {
            if (esistente.getUsername().equals(utente.getUsername())) {
                throw new PersistenzaException("Esiste già un utente \"" + utente.getUsername() + "\".");
            }
        }
        utenti.add(utente);
        scriviFile(utenti);
    }

    private List<Utente> leggiFile() throws PersistenzaException {
        List<Utente> letti = new ArrayList<>();
        if (!file.exists()) {
            return letti;
        }
        try (Scanner scanner = new Scanner(file, CODIFICA)) {
            int numeroRiga = 0;
            while (scanner.hasNextLine()) {
                String riga = scanner.nextLine();
                numeroRiga++;
                if (riga.trim().isEmpty()) {
                    continue;
                }
                letti.add(interpretaRiga(riga, numeroRiga));
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
        return letti;
    }

    private Utente interpretaRiga(String riga, int numeroRiga) throws PersistenzaException {
        // Con -1 i campi vuoti in fondo non vengono scartati, quindi "a;b;" risulta di 3 campi
        String[] campi = riga.split(SEPARATORE, -1);
        if (campi.length != NUMERO_CAMPI) {
            throw rigaNonValida(numeroRiga, "attesi " + NUMERO_CAMPI + " campi separati da '" + SEPARATORE
                    + "', trovati " + campi.length);
        }
        if (campi[0].isEmpty() || campi[1].isEmpty()) {
            throw rigaNonValida(numeroRiga, "username e hash non possono essere vuoti");
        }
        return new Utente(campi[0], campi[1]);
    }

    private PersistenzaException rigaNonValida(int numeroRiga, String motivo) {
        return new PersistenzaException("Il file " + file.getAbsolutePath() + " non è valido alla riga "
                + numeroRiga + ": " + motivo + ".");
    }

    private void scriviFile(List<Utente> daScrivere) throws PersistenzaException {
        // Le righe si preparano prima di aprire il file, che all'apertura viene svuotato
        List<String> righe = new ArrayList<>();
        for (Utente utente : daScrivere) {
            righe.add(campo(utente.getUsername()) + SEPARATORE + campo(utente.getPasswordHash()));
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

    // Un valore vuoto o con ';' renderebbe il file illeggibile alla lettura successiva
    private static String campo(String valore) throws PersistenzaException {
        if (valore.isEmpty() || valore.contains(SEPARATORE) || valore.contains("\n") || valore.contains("\r")) {
            throw new PersistenzaException("Username e hash non possono essere vuoti né contenere '" + SEPARATORE
                    + "' o ritorni a capo: \"" + valore + "\".");
        }
        return valore;
    }
}
