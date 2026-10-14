package it.taraborrelli.rubrica.modello;

/**
 * Un utente che può accedere all'applicazione. La password non è mai conservata in chiaro:
 * l'utente contiene solo il suo hash.
 */
public class Utente {

    private final String username;
    private final String passwordHash;

    public Utente(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
