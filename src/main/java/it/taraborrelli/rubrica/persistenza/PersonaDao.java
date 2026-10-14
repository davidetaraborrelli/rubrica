package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;

import java.util.List;

/**
 * Archivio delle persone. Le implementazioni restituiscono sempre oggetti nuovi e salvano
 * una copia di quelli ricevuti: modificare una persona non cambia l'archivio finché non si
 * chiama {@link #aggiorna(Persona)}.
 */
public interface PersonaDao {

    /**
     * Legge tutte le persone salvate, nell'ordine in cui sono state inserite.
     */
    List<Persona> caricaTutte() throws PersistenzaException;

    /**
     * Salva una nuova persona e, se il salvataggio riesce, le assegna l'id.
     */
    void inserisci(Persona persona) throws PersistenzaException;

    /**
     * Sostituisce i dati salvati della persona con lo stesso id.
     */
    void aggiorna(Persona persona) throws PersistenzaException;

    /**
     * Elimina la persona con lo stesso id.
     */
    void elimina(Persona persona) throws PersistenzaException;
}
