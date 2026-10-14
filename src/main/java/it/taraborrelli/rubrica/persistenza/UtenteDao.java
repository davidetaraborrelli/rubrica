package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Utente;

import java.util.Optional;

/**
 * Archivio degli utenti.
 */
public interface UtenteDao {

    /**
     * Cerca l'utente con lo username indicato, distinguendo maiuscole e minuscole.
     */
    Optional<Utente> trova(String username) throws PersistenzaException;

    /**
     * Dice se nell'archivio c'è almeno un utente.
     */
    boolean esistonoUtenti() throws PersistenzaException;

    /**
     * Salva un nuovo utente. Non riesce se esiste già un utente con lo stesso username.
     */
    void inserisci(Utente utente) throws PersistenzaException;
}
