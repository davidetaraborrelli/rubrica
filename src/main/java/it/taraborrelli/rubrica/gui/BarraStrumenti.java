package it.taraborrelli.rubrica.gui;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Barra degli strumenti fissa in cima a una finestra. I pulsanti hanno icona e testo; le icone
 * sono in {@code src/main/resources/icone}.
 */
class BarraStrumenti extends JToolBar {

    // Lato delle icone sullo schermo; i file sono a risoluzione doppia, 36 px
    private static final int LATO_ICONE = 18;

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

    private static Icon caricaIcona(String nomeFile) {
        URL url = BarraStrumenti.class.getResource("/icone/" + nomeFile);
        if (url == null) {
            throw new IllegalStateException("Icona non trovata: " + nomeFile);
        }
        try {
            return new IconaNitida(ImageIO.read(url));
        } catch (IOException e) {
            throw new IllegalStateException("Icona non leggibile: " + nomeFile, e);
        }
    }

    /**
     * Disegna un'immagine a risoluzione doppia nel lato delle icone. Con la scala dello schermo al
     * 125% o 150% Swing ingrandirebbe un'immagine da 18 px copiando i pixel e la sgranerebbe;
     * ridisegnando quella più grande con l'interpolazione l'icona resta nitida.
     */
    private static class IconaNitida implements Icon {

        private final BufferedImage immagine;

        IconaNitida(BufferedImage immagine) {
            this.immagine = immagine;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(immagine, x, y, LATO_ICONE, LATO_ICONE, null);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return LATO_ICONE;
        }

        @Override
        public int getIconHeight() {
            return LATO_ICONE;
        }
    }
}
