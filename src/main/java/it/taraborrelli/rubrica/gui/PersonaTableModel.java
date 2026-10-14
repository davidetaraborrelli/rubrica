package it.taraborrelli.rubrica.gui;

import it.taraborrelli.rubrica.modello.Persona;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Mostra nella tabella nome, cognome e telefono delle persone.
 * <p>
 * Legge direttamente la lista della rubrica, senza copiarla: dopo ogni modifica alla lista
 * va chiamato il metodo {@code fireTableRows...} corrispondente.
 */
public class PersonaTableModel extends AbstractTableModel {

    private static final String[] COLONNE = {"Nome", "Cognome", "Telefono"};

    private final List<Persona> persone;

    public PersonaTableModel(List<Persona> persone) {
        this.persone = persone;
    }

    public Persona getPersona(int riga) {
        return persone.get(riga);
    }

    @Override
    public int getRowCount() {
        return persone.size();
    }

    @Override
    public int getColumnCount() {
        return COLONNE.length;
    }

    @Override
    public String getColumnName(int colonna) {
        return COLONNE[colonna];
    }

    @Override
    public Object getValueAt(int riga, int colonna) {
        Persona persona = persone.get(riga);
        switch (colonna) {
            case 0:
                return persona.getNome();
            case 1:
                return persona.getCognome();
            case 2:
                return persona.getTelefono();
            default:
                throw new IndexOutOfBoundsException("Colonna inesistente: " + colonna);
        }
    }
}
