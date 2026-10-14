package it.taraborrelli.rubrica;

import it.taraborrelli.rubrica.gui.FinestraLogin;
import it.taraborrelli.rubrica.gui.FinestraPrincipale;
import it.taraborrelli.rubrica.logica.Autenticazione;
import it.taraborrelli.rubrica.logica.Rubrica;
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
    private static final String FILE_UTENTI = "utenti.txt";
    private static final String FILE_CREDENZIALI = "credenziali_database.properties";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::avvia);
    }

    private static void avvia() {
        impostaAspetto();
        File cartella = cartellaApplicazione();
        File credenziali = new File(cartella, FILE_CREDENZIALI);
        try {
            Rubrica rubrica;
            Autenticazione autenticazione;
            // Il file delle credenziali accanto al jar è ciò che attiva il salvataggio su database
            if (credenziali.isFile()) {
                ConfigurazioneDatabase database = ConfigurazioneDatabase.leggi(credenziali);
                rubrica = new Rubrica(new JdbcPersonaDao(database));
                autenticazione = new Autenticazione(new JdbcUtenteDao(database));
            } else {
                rubrica = new Rubrica(new FilePersonaDao(new File(cartella, FILE_PERSONE)));
                autenticazione = new Autenticazione(new FileUtenteDao(new File(cartella, FILE_UTENTI)));
            }
            autenticazione.creaUtenteIniziale();
            rubrica.carica();
            new FinestraLogin(autenticazione, () -> new FinestraPrincipale(rubrica).setVisible(true)).setVisible(true);
        } catch (PersistenzaException e) {
            // Una rubrica vuota, o i file al posto del database, farebbero credere che i dati siano andati persi
            JOptionPane.showMessageDialog(null, e.getMessage(), "Rubrica", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
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
