package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Salva le persone in un unico file di testo UTF-8, una per riga, nel formato
 * {@code nome;cognome;indirizzo;telefono;eta}: è l'{@code informazioni.txt} della parte base del
 * documento.
 * <p>
 * Il file non contiene l'id: viene assegnato alla lettura e vale finché l'applicazione resta
 * aperta. Ogni operazione riscrive l'intero file.
 */
public class FilePersonaDao implements PersonaDao {

    private final File file;
    // Contenuto del file al momento dell'ultima lettura o scrittura riuscita; null se mai letto
    private List<Persona> persone;
    private int prossimoId;

    public FilePersonaDao(File file) {
        this.file = file;
    }

    @Override
    public List<Persona> caricaTutte() throws PersistenzaException {
        persone = leggiFile();
        prossimoId = persone.size() + 1;
        List<Persona> copie = new ArrayList<>();
        for (Persona persona : persone) {
            copie.add(new Persona(persona));
        }
        return copie;
    }

    @Override
    public void inserisci(Persona persona) throws PersistenzaException {
        List<Persona> nuoveSalvate = new ArrayList<>(personeSalvate());
        Persona copia = new Persona(persona);
        copia.setId(prossimoId);
        nuoveSalvate.add(copia);
        scriviFile(nuoveSalvate);
        persone = nuoveSalvate;
        persona.setId(prossimoId);
        prossimoId++;
    }

    @Override
    public void aggiorna(Persona persona) throws PersistenzaException {
        List<Persona> nuoveSalvate = new ArrayList<>(personeSalvate());
        nuoveSalvate.set(indiceDi(nuoveSalvate, persona.getId()), new Persona(persona));
        scriviFile(nuoveSalvate);
        persone = nuoveSalvate;
    }

    @Override
    public void elimina(Persona persona) throws PersistenzaException {
        List<Persona> nuoveSalvate = new ArrayList<>(personeSalvate());
        nuoveSalvate.remove(indiceDi(nuoveSalvate, persona.getId()));
        scriviFile(nuoveSalvate);
        persone = nuoveSalvate;
    }

    // Scrivere senza aver letto il file lo sovrascriverebbe perdendo le persone già salvate
    private List<Persona> personeSalvate() throws PersistenzaException {
        if (persone == null) {
            caricaTutte();
        }
        return persone;
    }

    private static int indiceDi(List<Persona> elenco, int id) {
        for (int i = 0; i < elenco.size(); i++) {
            if (elenco.get(i).getId() == id) {
                return i;
            }
        }
        throw new IllegalArgumentException("Nessuna persona salvata con id " + id);
    }

    private List<Persona> leggiFile() throws PersistenzaException {
        List<Persona> lette = FileDiTesto.leggi(file,
                (riga, numeroRiga) -> FormatoPersona.interpreta(file, riga, numeroRiga));
        for (int i = 0; i < lette.size(); i++) {
            lette.get(i).setId(i + 1);
        }
        return lette;
    }

    private void scriviFile(List<Persona> daScrivere) throws PersistenzaException {
        // Le righe si preparano prima di aprire il file, che all'apertura viene svuotato
        List<String> righe = new ArrayList<>();
        for (Persona persona : daScrivere) {
            righe.add(FormatoPersona.formatta(persona));
        }
        FileDiTesto.scrivi(file, righe);
    }
}
