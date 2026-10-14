package it.taraborrelli.rubrica.gui;

import it.taraborrelli.rubrica.logica.Rubrica;
import it.taraborrelli.rubrica.modello.Persona;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Finestra modale per inserire una persona nuova o modificarne una esistente.
 * Il salvataggio avviene qui: se non riesce, la finestra resta aperta con i dati inseriti.
 */
public class EditorPersona extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int ETA_MASSIMA = 150;

    private final Rubrica rubrica;
    // null quando si sta creando una persona nuova
    private final Persona originale;

    private final JTextField campoNome = new JTextField(25);
    private final JTextField campoCognome = new JTextField(25);
    private final JTextField campoIndirizzo = new JTextField(25);
    private final JTextField campoTelefono = new JTextField(25);
    private final JTextField campoEta = new JTextField(25);

    private boolean salvata;

    /**
     * @param originale la persona da modificare, oppure null per crearne una nuova
     */
    public EditorPersona(Window owner, Rubrica rubrica, Persona originale) {
        super(owner, originale == null ? "Nuova persona" : "Modifica persona", ModalityType.APPLICATION_MODAL);
        this.rubrica = rubrica;
        this.originale = originale;

        if (originale != null) {
            campoNome.setText(originale.getNome());
            campoCognome.setText(originale.getCognome());
            campoIndirizzo.setText(originale.getIndirizzo());
            campoTelefono.setText(originale.getTelefono());
            campoEta.setText(String.valueOf(originale.getEta()));
        }

        add(creaBarraStrumenti(), BorderLayout.NORTH);
        add(creaModulo(), BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Dopo la chiusura della finestra, indica se la persona è stata salvata.
     */
    public boolean isSalvata() {
        return salvata;
    }

    private JPanel creaModulo() {
        JPanel modulo = new JPanel(new GridBagLayout());
        modulo.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        aggiungiRiga(modulo, 0, "Nome", campoNome);
        aggiungiRiga(modulo, 1, "Cognome", campoCognome);
        aggiungiRiga(modulo, 2, "Indirizzo", campoIndirizzo);
        aggiungiRiga(modulo, 3, "Telefono", campoTelefono);
        aggiungiRiga(modulo, 4, "Età", campoEta);
        return modulo;
    }

    private static void aggiungiRiga(JPanel modulo, int riga, String testo, JTextField campo) {
        JLabel etichetta = new JLabel(testo);
        etichetta.setLabelFor(campo);

        GridBagConstraints vincoli = new GridBagConstraints();
        vincoli.gridy = riga;
        vincoli.insets = new Insets(4, 4, 4, 4);
        vincoli.anchor = GridBagConstraints.LINE_START;
        vincoli.gridx = 0;
        modulo.add(etichetta, vincoli);

        vincoli.gridx = 1;
        vincoli.fill = GridBagConstraints.HORIZONTAL;
        vincoli.weightx = 1;
        modulo.add(campo, vincoli);
    }

    private BarraStrumenti creaBarraStrumenti() {
        BarraStrumenti barra = new BarraStrumenti();
        JButton salva = barra.aggiungiPulsante("Salva", "salva.png", e -> salva());
        barra.aggiungiPulsante("Annulla", "annulla.png", e -> dispose());

        getRootPane().setDefaultButton(salva);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return barra;
    }

    private void salva() {
        if (!datiValidi()) {
            return;
        }
        Persona persona = new Persona(testo(campoNome), testo(campoCognome), testo(campoIndirizzo),
                testo(campoTelefono), Integer.parseInt(testo(campoEta)));
        try {
            if (originale == null) {
                rubrica.aggiungi(persona);
            } else {
                persona.setId(originale.getId());
                rubrica.modifica(persona);
            }
        } catch (PersistenzaException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Salvataggio non riuscito", JOptionPane.ERROR_MESSAGE);
            return;
        }
        salvata = true;
        dispose();
    }

    private boolean datiValidi() {
        List<JTextField> campi = Arrays.asList(campoNome, campoCognome, campoIndirizzo, campoTelefono, campoEta);
        for (JTextField campo : campi) {
            // Il ; separa i campi nel file dei dati
            if (campo.getText().contains(";")) {
                return segnalaErrore(campo, "Il carattere ';' non è ammesso.");
            }
        }
        if (testo(campoNome).isEmpty()) {
            return segnalaErrore(campoNome, "Il nome è obbligatorio.");
        }
        if (testo(campoCognome).isEmpty()) {
            return segnalaErrore(campoCognome, "Il cognome è obbligatorio.");
        }
        if (testo(campoTelefono).isEmpty()) {
            return segnalaErrore(campoTelefono, "Il telefono è obbligatorio.");
        }
        String eta = testo(campoEta);
        if (!eta.matches("\\d{1,3}") || Integer.parseInt(eta) > ETA_MASSIMA) {
            return segnalaErrore(campoEta, "L'età deve essere un numero intero tra 0 e " + ETA_MASSIMA + ".");
        }
        return true;
    }

    private boolean segnalaErrore(JTextField campo, String messaggio) {
        JOptionPane.showMessageDialog(this, messaggio, "Dati non validi", JOptionPane.WARNING_MESSAGE);
        campo.requestFocusInWindow();
        return false;
    }

    private static String testo(JTextField campo) {
        return campo.getText().trim();
    }
}
