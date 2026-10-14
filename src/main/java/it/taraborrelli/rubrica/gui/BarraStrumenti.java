package it.taraborrelli.rubrica.gui;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * Barra degli strumenti fissa in cima a una finestra. I pulsanti hanno icona e testo; le icone
 * sono in {@code src/main/resources/icone}.
 */
class BarraStrumenti extends JToolBar {

    BarraStrumenti() {
        setFloatable(false);
        setRollover(true);
    }

    /**
     * Aggiunge un pulsante alla barra e lo restituisce, per chi deve usarlo anche altrove
     * (ad esempio come pulsante predefinito della finestra).
     */
    JButton aggiungiPulsante(String testo, String icona, ActionListener azione) {
        JButton pulsante = new JButton(testo, caricaIcona(icona));
        pulsante.addActionListener(azione);
        add(pulsante);
        return pulsante;
    }

    private static ImageIcon caricaIcona(String nomeFile) {
        URL url = BarraStrumenti.class.getResource("/icone/" + nomeFile);
        if (url == null) {
            throw new IllegalStateException("Icona non trovata: " + nomeFile);
        }
        return new ImageIcon(url);
    }
}
