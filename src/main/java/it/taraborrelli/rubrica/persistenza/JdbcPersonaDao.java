package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Persona;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Salva le persone nella tabella {@code persone} del database. Ogni operazione apre e chiude la
 * propria connessione, così una connessione scaduta non può rovinare quelle successive. L'id lo
 * genera il database.
 */
public class JdbcPersonaDao implements PersonaDao {

    private final ConfigurazioneDatabase configurazione;

    public JdbcPersonaDao(ConfigurazioneDatabase configurazione) {
        this.configurazione = configurazione;
    }

    @Override
    public List<Persona> caricaTutte() throws PersistenzaException {
        String sql = "SELECT id, nome, cognome, indirizzo, telefono, eta FROM persone ORDER BY id";
        try (Connection connessione = configurazione.apriConnessione();
             PreparedStatement istruzione = connessione.prepareStatement(sql);
             ResultSet risultati = istruzione.executeQuery()) {
            List<Persona> persone = new ArrayList<>();
            while (risultati.next()) {
                Persona persona = new Persona(risultati.getString("nome"), risultati.getString("cognome"),
                        risultati.getString("indirizzo"), risultati.getString("telefono"), risultati.getInt("eta"));
                persona.setId(risultati.getInt("id"));
                persone.add(persona);
            }
            return persone;
        } catch (SQLException e) {
            throw configurazione.errore(e);
        }
    }

    @Override
    public void inserisci(Persona persona) throws PersistenzaException {
        String sql = "INSERT INTO persone (nome, cognome, indirizzo, telefono, eta) VALUES (?, ?, ?, ?, ?)";
        try (Connection connessione = configurazione.apriConnessione();
             PreparedStatement istruzione = connessione.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            impostaCampi(istruzione, persona);
            istruzione.executeUpdate();
            try (ResultSet chiavi = istruzione.getGeneratedKeys()) {
                if (!chiavi.next()) {
                    throw new PersistenzaException("Il database non ha restituito l'id della persona salvata.");
                }
                // L'id si assegna solo a salvataggio riuscito
                persona.setId(chiavi.getInt(1));
            }
        } catch (SQLException e) {
            throw configurazione.errore(e);
        }
    }

    @Override
    public void aggiorna(Persona persona) throws PersistenzaException {
        String sql = "UPDATE persone SET nome = ?, cognome = ?, indirizzo = ?, telefono = ?, eta = ? WHERE id = ?";
        try (Connection connessione = configurazione.apriConnessione();
             PreparedStatement istruzione = connessione.prepareStatement(sql)) {
            impostaCampi(istruzione, persona);
            istruzione.setInt(6, persona.getId());
            verificaTrovata(istruzione.executeUpdate());
        } catch (SQLException e) {
            throw configurazione.errore(e);
        }
    }

    @Override
    public void elimina(Persona persona) throws PersistenzaException {
        String sql = "DELETE FROM persone WHERE id = ?";
        try (Connection connessione = configurazione.apriConnessione();
             PreparedStatement istruzione = connessione.prepareStatement(sql)) {
            istruzione.setInt(1, persona.getId());
            verificaTrovata(istruzione.executeUpdate());
        } catch (SQLException e) {
            throw configurazione.errore(e);
        }
    }

    private static void impostaCampi(PreparedStatement istruzione, Persona persona) throws SQLException {
        istruzione.setString(1, persona.getNome());
        istruzione.setString(2, persona.getCognome());
        istruzione.setString(3, persona.getIndirizzo());
        istruzione.setString(4, persona.getTelefono());
        istruzione.setInt(5, persona.getEta());
    }

    // Con un database condiviso la persona potrebbe essere stata eliminata da qualcun altro
    private static void verificaTrovata(int righeInteressate) throws PersistenzaException {
        if (righeInteressate == 0) {
            throw new PersistenzaException("La persona non è più presente nel database.");
        }
    }
}
