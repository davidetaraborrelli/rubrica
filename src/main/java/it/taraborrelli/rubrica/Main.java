package it.taraborrelli.rubrica;

import it.taraborrelli.rubrica.gui.FinestraLogin;
import it.taraborrelli.rubrica.gui.FinestraPrincipale;
import it.taraborrelli.rubrica.logica.Autenticazione;
import it.taraborrelli.rubrica.logica.Rubrica;
import it.taraborrelli.rubrica.persistenza.FilePersonaDao;
import it.taraborrelli.rubrica.persistenza.FileUtenteDao;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::avvia);
    }

    private static void avvia() {
        impostaAspetto();
        File cartella = cartellaApplicazione();
        Rubrica rubrica = new Rubrica(new FilePersonaDao(new File(cartella, FILE_PERSONE)));
        Autenticazione autenticazione = new Autenticazione(new FileUtenteDao(new File(cartella, FILE_UTENTI)));
        try {
            autenticazione.creaUtenteIniziale();
            rubrica.carica();
        } catch (PersistenzaException e) {
            // Aprire una rubrica vuota farebbe credere che i dati siano andati persi
            JOptionPane.showMessageDialog(null, e.getMessage(), "Rubrica", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        new FinestraLogin(autenticazione, () -> new FinestraPrincipale(rubrica).setVisible(true)).setVisible(true);
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
