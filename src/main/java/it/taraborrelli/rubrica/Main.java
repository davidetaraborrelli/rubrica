package it.taraborrelli.rubrica;

import it.taraborrelli.rubrica.gui.FinestraLogin;
import it.taraborrelli.rubrica.gui.FinestraPrincipale;
import it.taraborrelli.rubrica.logica.Autenticazione;
import it.taraborrelli.rubrica.logica.Rubrica;
import it.taraborrelli.rubrica.persistenza.CartellaPersonaDao;
import it.taraborrelli.rubrica.persistenza.ConfigurazioneDatabase;
import it.taraborrelli.rubrica.persistenza.FilePersonaDao;
import it.taraborrelli.rubrica.persistenza.FileUtenteDao;
import it.taraborrelli.rubrica.persistenza.JdbcPersonaDao;
import it.taraborrelli.rubrica.persistenza.JdbcUtenteDao;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.File;
import java.net.URISyntaxException;

public class Main {

    private static final String FILE_PERSONE = "informazioni.txt";
    private static final String CARTELLA_PERSONE = "informazioni";
    private static final String FILE_UTENTI = "utenti.txt";
    private static final String FILE_CREDENZIALI = "credenziali_database.properties";
    private static final String ARGOMENTO_CARTELLA = "cartella";
    private static final String ARGOMENTO_FILE = "file";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> avvia(args));
    }

    private static void avvia(String[] args) {
        impostaAspetto();
        if (!argomentiValidi(args)) {
            mostraErrore("Argomenti non validi: " + String.join(" ", args)
                    + "\nUso: java -jar Rubrica.jar [" + ARGOMENTO_CARTELLA + "|" + ARGOMENTO_FILE + "]");
            System.exit(1);
        }
        boolean unSoloFile = args.length == 1 && args[0].equals(ARGOMENTO_FILE);
        File cartella = cartellaApplicazione();
        File credenziali = new File(cartella, FILE_CREDENZIALI);
        try {
            Rubrica rubrica;
            Autenticazione autenticazione;
            if (args.length == 0 && credenziali.isFile()) {
                // Il file delle credenziali accanto al jar è ciò che attiva il salvataggio su database.
                // Un argomento esplicito vale di più, perché nel pacchetto da consegnare il file c'è sempre
                ConfigurazioneDatabase database = ConfigurazioneDatabase.leggi(credenziali);
                rubrica = new Rubrica(new JdbcPersonaDao(database));
                autenticazione = new Autenticazione(new JdbcUtenteDao(database));
            } else if (unSoloFile) {
                rubrica = new Rubrica(new FilePersonaDao(new File(cartella, FILE_PERSONE)));
                autenticazione = new Autenticazione(new FileUtenteDao(new File(cartella, FILE_UTENTI)));
            } else {
                // Salvataggio predefinito: un file per persona nella cartella informazioni
                rubrica = new Rubrica(new CartellaPersonaDao(new File(cartella, CARTELLA_PERSONE)));
                autenticazione = new Autenticazione(new FileUtenteDao(new File(cartella, FILE_UTENTI)));
            }
            autenticazione.creaUtenteIniziale();
            rubrica.carica();
            new FinestraLogin(autenticazione, () -> new FinestraPrincipale(rubrica).setVisible(true)).setVisible(true);
        } catch (PersistenzaException e) {
            // Una rubrica vuota, o i file al posto del database, farebbero credere che i dati siano andati persi
            mostraErrore(e.getMessage());
            System.exit(1);
        }
    }

    private static boolean argomentiValidi(String[] args) {
        return args.length == 0
                || (args.length == 1 && (args[0].equals(ARGOMENTO_CARTELLA) || args[0].equals(ARGOMENTO_FILE)));
    }

    private static void mostraErrore(String messaggio) {
        JOptionPane.showMessageDialog(null, messaggio, "Rubrica", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Cartella che contiene Rubrica.jar. Col doppio click su Mac e Linux la cartella corrente può
     * essere un'altra, quindi i dati stanno accanto al jar. Avviando dall'IDE le classi non sono in
     * un jar e si usa la cartella corrente, cioè la radice del progetto.
     */
    private static File cartellaApplicazione() {
        try {
            File origine = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (origine.isFile()) {
                return origine.getParentFile();
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            // Posizione del jar non ricavabile: si ripiega sulla cartella corrente
        }
        return new File(System.getProperty("user.dir"));
    }

    private static void impostaAspetto() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Si resta sull'aspetto predefinito di Swing, disponibile ovunque
        }
        // Pulsanti dei dialoghi in italiano anche su un sistema in un'altra lingua
        UIManager.put("OptionPane.yesButtonText", "Sì");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Annulla");
    }
}
