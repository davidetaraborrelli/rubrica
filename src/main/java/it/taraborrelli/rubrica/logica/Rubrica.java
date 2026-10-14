package it.taraborrelli.rubrica.logica;

import it.taraborrelli.rubrica.modello.Persona;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;
import it.taraborrelli.rubrica.persistenza.PersonaDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Elenco delle persone della rubrica. Ogni modifica viene prima salvata nell'archivio e la
 * lista cambia solo se il salvataggio riesce, così lista e archivio restano allineati.
 */
public class Rubrica {

    private final PersonaDao dao;
    private final List<Persona> persone = new ArrayList<>();

    public Rubrica(PersonaDao dao) {
        this.dao = dao;
    }

    /**
     * Sostituisce il contenuto della rubrica con quello dell'archivio.
     */
    public void carica() throws PersistenzaException {
        List<Persona> caricate = dao.caricaTutte();
        persone.clear();
        persone.addAll(caricate);
    }

    /**
     * Vista in sola lettura della lista, che riflette sempre le ultime modifiche.
     */
    public List<Persona> getPersone() {
        return Collections.unmodifiableList(persone);
    }

    public void aggiungi(Persona persona) throws PersistenzaException {
        dao.inserisci(persona);
        persone.add(persona);
    }

    /**
     * Sostituisce la persona che ha lo stesso id di {@code modificata}.
     */
    public void modifica(Persona modificata) throws PersistenzaException {
        int indice = indiceDi(modificata.getId());
        dao.aggiorna(modificata);
        persone.set(indice, modificata);
    }

    public void elimina(Persona persona) throws PersistenzaException {
        int indice = indiceDi(persona.getId());
        dao.elimina(persona);
        persone.remove(indice);
    }

    private int indiceDi(int id) {
        for (int i = 0; i < persone.size(); i++) {
            if (persone.get(i).getId() == id) {
                return i;
            }
        }
        throw new IllegalArgumentException("Nessuna persona in rubrica con id " + id);
    }
}
