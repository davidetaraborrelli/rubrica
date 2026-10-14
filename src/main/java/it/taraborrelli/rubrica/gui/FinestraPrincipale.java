package it.taraborrelli.rubrica.gui;

import it.taraborrelli.rubrica.logica.Rubrica;
import it.taraborrelli.rubrica.modello.Persona;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Finestra principale: la tabella delle persone e, in alto, la barra con i comandi Nuovo, Modifica,
 * Elimina.
 */
public class FinestraPrincipale extends JFrame {

    private final Rubrica rubrica;
    private final PersonaTableModel modello;
    private final JTable tabella;

    public FinestraPrincipale(Rubrica rubrica) {
        super("Rubrica");
        this.rubrica = rubrica;
        modello = new PersonaTableModel(rubrica.getPersone());
        tabella = new JTable(modello);
        tabella.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabella.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabella.rowAtPoint(e.getPoint()) >= 0) {
                    modifica();
                }
            }
        });

        add(creaBarraStrumenti(), BorderLayout.NORTH);
        add(new JScrollPane(tabella), BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
    }

    private BarraStrumenti creaBarraStrumenti() {
        BarraStrumenti barra = new BarraStrumenti();
        barra.aggiungiPulsante("Nuovo", "nuovo.png", e -> nuovo());
        barra.aggiungiPulsante("Modifica", "modifica.png", e -> modifica());
        barra.aggiungiPulsante("Elimina", "elimina.png", e -> elimina());
        return barra;
    }

    private void nuovo() {
        EditorPersona editor = new EditorPersona(this, rubrica, null);
        editor.setVisible(true);
        if (editor.isSalvata()) {
            int riga = modello.getRowCount() - 1;
            modello.fireTableRowsInserted(riga, riga);
            tabella.setRowSelectionInterval(riga, riga);
            tabella.scrollRectToVisible(tabella.getCellRect(riga, 0, true));
        }
    }

    private void modifica() {
        int riga = tabella.getSelectedRow();
        if (riga < 0) {
            mostraErrore("Per modificare è necessario prima selezionare una persona.");
            return;
        }
        EditorPersona editor = new EditorPersona(this, rubrica, modello.getPersona(riga));
        editor.setVisible(true);
        if (editor.isSalvata()) {
            modello.fireTableRowsUpdated(riga, riga);
        }
    }

    private void elimina() {
        int riga = tabella.getSelectedRow();
        if (riga < 0) {
            mostraErrore("Per eliminare è necessario prima selezionare una persona.");
            return;
        }
        Persona persona = modello.getPersona(riga);
        int risposta = JOptionPane.showConfirmDialog(this,
                "Eliminare la persona " + persona.getNome() + " " + persona.getCognome() + "?",
                "Conferma eliminazione", JOptionPane.YES_NO_OPTION);
        if (risposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            rubrica.elimina(persona);
        } catch (PersistenzaException e) {
            mostraErrore(e.getMessage());
            return;
        }
        modello.fireTableRowsDeleted(riga, riga);
    }

    private void mostraErrore(String messaggio) {
        JOptionPane.showMessageDialog(this, messaggio, "Errore", JOptionPane.ERROR_MESSAGE);
    }
}
