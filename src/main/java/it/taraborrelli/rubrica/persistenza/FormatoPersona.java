package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;

import java.io.File;

/**
 * Una persona su una riga di testo, nel formato del documento:
 * {@code nome;cognome;indirizzo;telefono;eta}. È il formato di {@code informazioni.txt} e anche
 * quello di ogni file della cartella {@code informazioni}.
 */
final class FormatoPersona {

    private static final String SEPARATORE = ";";
    private static final int NUMERO_CAMPI = 5;

    private FormatoPersona() {
    }

    /**
     * @param file il file da cui viene la riga, citato nei messaggi d'errore
     */
    static Persona interpreta(File file, String riga, int numeroRiga) throws PersistenzaException {
        // Con -1 i campi vuoti in fondo non vengono scartati, quindi "a;b;c;d;5;" risulta di 6 campi
        String[] campi = riga.split(SEPARATORE, -1);
        if (campi.length != NUMERO_CAMPI) {
            throw FileDiTesto.rigaNonValida(file, numeroRiga, "attesi " + NUMERO_CAMPI
                    + " campi separati da '" + SEPARATORE + "', trovati " + campi.length);
        }
        int eta;
        try {
            eta = Integer.parseInt(campi[4].trim());
        } catch (NumberFormatException e) {
            throw FileDiTesto.rigaNonValida(file, numeroRiga, "l'età \"" + campi[4] + "\" non è un numero intero");
        }
        return new Persona(campi[0], campi[1], campi[2], campi[3], eta);
    }

    static String formatta(Persona persona) throws PersistenzaException {
        return campo(persona.getNome()) + SEPARATORE
                + campo(persona.getCognome()) + SEPARATORE
                + campo(persona.getIndirizzo()) + SEPARATORE
                + campo(persona.getTelefono()) + SEPARATORE
                + persona.getEta();
    }

    private static String campo(String valore) throws PersistenzaException {
        if (valore.contains(SEPARATORE) || valore.contains("\n") || valore.contains("\r")) {
            throw new PersistenzaException("I dati non possono contenere '" + SEPARATORE
                    + "' o ritorni a capo: \"" + valore + "\".");
        }
        return valore;
    }
}
