package it.taraborrelli.rubrica.persistenza;

/**
 * Errore nel leggere o salvare i dati. Il messaggio è pensato per essere mostrato all'utente.
 */
public class PersistenzaException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenzaException(String messaggio) {
        super(messaggio);
    }

    public PersistenzaException(String messaggio, Throwable causa) {
        super(messaggio, causa);
    }
}
