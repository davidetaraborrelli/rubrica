package it.taraborrelli.rubrica.gui;

import it.taraborrelli.rubrica.logica.Autenticazione;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

/**
 * Finestra di accesso, la sola mostrata all'avvio, con il pulsante LOGIN nella barra degli
 * strumenti. Con credenziali corrette si chiude ed esegue l'azione ricevuta, che apre la finestra
 * principale; con credenziali errate segnala l'errore e resta aperta.
 */
public class FinestraLogin extends JFrame {

    private static final long serialVersionUID = 1L;

    private final Autenticazione autenticazione;
    private final Runnable dopoAccesso;

    private final JTextField campoUtente = new JTextField(15);
    private final JPasswordField campoPassword = new JPasswordField(15);

    /**
     * @param dopoAccesso azione da eseguire quando le credenziali sono corrette
     */
    public FinestraLogin(Autenticazione autenticazione, Runnable dopoAccesso) {
        super("Login");
        this.autenticazione = autenticazione;
        this.dopoAccesso = dopoAccesso;

        add(creaBarraStrumenti(), BorderLayout.NORTH);
        add(creaModulo(), BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel creaModulo() {
        JPanel modulo = new JPanel(new GridBagLayout());
        modulo.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        aggiungiRiga(modulo, 0, "Utente", campoUtente);
        aggiungiRiga(modulo, 1, "Password", campoPassword);
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
        JButton login = barra.aggiungiPulsante("LOGIN", "login.png", e -> accedi());
        getRootPane().setDefaultButton(login);
        return barra;
    }

    private void accedi() {
        char[] password = campoPassword.getPassword();
        try {
            if (autenticazione.verificaCredenziali(campoUtente.getText().trim(), password)) {
                // Prima si apre la finestra principale e poi si chiude questa, così non resta mai
                // un momento senza finestre
                dopoAccesso.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Login errato", "Login", JOptionPane.ERROR_MESSAGE);
                campoPassword.setText("");
                campoPassword.requestFocusInWindow();
            }
        } catch (PersistenzaException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Login non riuscito", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
