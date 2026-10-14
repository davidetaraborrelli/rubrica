package it.taraborrelli.rubrica;

import it.taraborrelli.rubrica.gui.FinestraPrincipale;
import it.taraborrelli.rubrica.logica.Rubrica;
import it.taraborrelli.rubrica.persistenza.FilePersonaDao;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.File;

public class Main {

    private static final String FILE_PERSONE = "informazioni.txt";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::avvia);
    }

    private static void avvia() {
        impostaAspetto();
        Rubrica rubrica = new Rubrica(new FilePersonaDao(new File(FILE_PERSONE)));
        try {
            rubrica.carica();
            new FinestraPrincipale(rubrica).setVisible(true);
        } catch (PersistenzaException e) {
            // Aprire una rubrica vuota farebbe credere che i dati siano andati persi
            JOptionPane.showMessageDialog(null, e.getMessage(), "Rubrica", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
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
