package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Utente;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Salva gli utenti in un file di testo UTF-8, uno per riga, nel formato {@code username;hash}.
 * Il file viene riletto a ogni operazione e riscritto per intero a ogni inserimento.
 */
public class FileUtenteDao implements UtenteDao {

    private static final String SEPARATORE = ";";
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
        return FileDiTesto.leggi(file, this::interpretaRiga);
    }

    private Utente interpretaRiga(String riga, int numeroRiga) throws PersistenzaException {
        // Con -1 i campi vuoti in fondo non vengono scartati, quindi "a;b;" risulta di 3 campi
        String[] campi = riga.split(SEPARATORE, -1);
        if (campi.length != NUMERO_CAMPI) {
            throw FileDiTesto.rigaNonValida(file, numeroRiga, "attesi " + NUMERO_CAMPI
                    + " campi separati da '" + SEPARATORE + "', trovati " + campi.length);
        }
        if (campi[0].isEmpty() || campi[1].isEmpty()) {
            throw FileDiTesto.rigaNonValida(file, numeroRiga, "username e hash non possono essere vuoti");
        }
        return new Utente(campi[0], campi[1]);
    }

    private void scriviFile(List<Utente> daScrivere) throws PersistenzaException {
        // Le righe si preparano prima di aprire il file, che all'apertura viene svuotato
        List<String> righe = new ArrayList<>();
        for (Utente utente : daScrivere) {
            righe.add(campo(utente.getUsername()) + SEPARATORE + campo(utente.getPasswordHash()));
        }
        FileDiTesto.scrivi(file, righe);
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
