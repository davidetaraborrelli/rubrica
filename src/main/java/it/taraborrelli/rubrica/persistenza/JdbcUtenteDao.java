package it.taraborrelli.rubrica.persistenza;

import it.taraborrelli.rubrica.modello.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Salva gli utenti nella tabella {@code utenti} del database. Lo username distingue maiuscole e
 * minuscole perché la colonna usa la collation {@code utf8mb4_bin}, come nello script SQL.
 */
public class JdbcUtenteDao implements UtenteDao {

    private final ConfigurazioneDatabase configurazione;

    public JdbcUtenteDao(ConfigurazioneDatabase configurazione) {
        this.configurazione = configurazione;
    }

    @Override
    public Optional<Utente> trova(String username) throws PersistenzaException {
        try (Connection connessione = configurazione.apriConnessione()) {
            return trova(connessione, username);
        } catch (SQLException e) {
            throw configurazione.errore(e);
        }
    }

    @Override
    public boolean esistonoUtenti() throws PersistenzaException {
        try (Connection connessione = configurazione.apriConnessione();
             PreparedStatement istruzione = connessione.prepareStatement("SELECT COUNT(*) FROM utenti");
             ResultSet risultati = istruzione.executeQuery()) {
            return risultati.next() && risultati.getInt(1) > 0;
        } catch (SQLException e) {
            throw configurazione.errore(e);
        }
    }

    @Override
    public void inserisci(Utente utente) throws PersistenzaException {
        String sql = "INSERT INTO utenti (username, password_hash) VALUES (?, ?)";
        try (Connection connessione = configurazione.apriConnessione()) {
            if (trova(connessione, utente.getUsername()).isPresent()) {
                throw new PersistenzaException("Esiste già un utente \"" + utente.getUsername() + "\".");
            }
            try (PreparedStatement istruzione = connessione.prepareStatement(sql)) {
                istruzione.setString(1, utente.getUsername());
                istruzione.setString(2, utente.getPasswordHash());
                istruzione.executeUpdate();
            }
        } catch (SQLException e) {
            throw configurazione.errore(e);
        }
    }

    private static Optional<Utente> trova(Connection connessione, String username) throws SQLException {
        String sql = "SELECT username, password_hash FROM utenti WHERE username = ?";
        try (PreparedStatement istruzione = connessione.prepareStatement(sql)) {
            istruzione.setString(1, username);
            try (ResultSet risultati = istruzione.executeQuery()) {
                if (risultati.next()) {
                    return Optional.of(new Utente(risultati.getString("username"), risultati.getString("password_hash")));
                }
                return Optional.empty();
            }
        }
    }
}
