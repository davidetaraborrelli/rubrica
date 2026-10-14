package it.taraborrelli.rubrica.logica;

import it.taraborrelli.rubrica.modello.Utente;
import it.taraborrelli.rubrica.persistenza.PersistenzaException;
import it.taraborrelli.rubrica.persistenza.UtenteDao;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Controllo delle credenziali. La password non viene mai salvata in chiaro: si conserva il suo hash
 * PBKDF2, calcolato con un salt casuale diverso per ogni utente, nel formato
 * {@code pbkdf2_sha256$iterazioni$salt$hash} (salt e hash in Base64). Il numero di iterazioni è
 * scritto nell'hash, quindi si può aumentare senza invalidare gli utenti già salvati.
 */
public class Autenticazione {

    private static final String UTENTE_INIZIALE = "admin";
    private static final String PASSWORD_INIZIALE = "admin";

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFISSO = "pbkdf2_sha256";
    private static final String SEPARATORE = "$";
    private static final int ITERAZIONI = 600_000;
    private static final int BYTE_SALT = 16;
    private static final int BIT_HASH = 256;
    private static final SecureRandom CASUALE = new SecureRandom();

    private final UtenteDao dao;

    public Autenticazione(UtenteDao dao) {
        this.dao = dao;
    }

    /**
     * Se non c'è ancora nessun utente crea quello iniziale, admin con password admin: senza di lui
     * nessuno potrebbe accedere.
     */
    public void creaUtenteIniziale() throws PersistenzaException {
        if (!dao.esistonoUtenti()) {
            dao.inserisci(new Utente(UTENTE_INIZIALE, calcolaHash(PASSWORD_INIZIALE.toCharArray())));
        }
    }

    /**
     * Dice se username e password corrispondono a un utente salvato. La password ricevuta non viene
     * modificata: azzerarla dopo l'uso spetta a chi l'ha letta.
     */
    public boolean verificaCredenziali(String username, char[] password) throws PersistenzaException {
        return dao.trova(username)
                .map(utente -> controllaPassword(password, utente.getPasswordHash()))
                .orElse(false);
    }

    static String calcolaHash(char[] password) {
        byte[] salt = new byte[BYTE_SALT];
        CASUALE.nextBytes(salt);
        byte[] hash = derivaChiave(password, salt, ITERAZIONI, BIT_HASH);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFISSO + SEPARATORE + ITERAZIONI + SEPARATORE
                + base64.encodeToString(salt) + SEPARATORE + base64.encodeToString(hash);
    }

    static boolean controllaPassword(char[] password, String hashSalvato) {
        String[] parti = hashSalvato.split(Pattern.quote(SEPARATORE), -1);
        if (parti.length != 4 || !PREFISSO.equals(parti[0])) {
            return false;
        }
        try {
            int iterazioni = Integer.parseInt(parti[1]);
            byte[] salt = Base64.getDecoder().decode(parti[2]);
            byte[] atteso = Base64.getDecoder().decode(parti[3]);
            byte[] calcolato = derivaChiave(password, salt, iterazioni, atteso.length * 8);
            // Confronto a tempo costante: non rivela da quale byte i due hash differiscono
            return MessageDigest.isEqual(atteso, calcolato);
        } catch (IllegalArgumentException e) {
            // Hash illeggibile (iterazioni o Base64 non validi) o password vuota: nessuna password lo soddisfa
            return false;
        }
    }

    private static byte[] derivaChiave(char[] password, byte[] salt, int iterazioni, int bit) {
        PBEKeySpec parametri = new PBEKeySpec(password, salt, iterazioni, bit);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(parametri).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Calcolo dell'hash della password non riuscito.", e);
        } finally {
            parametri.clearPassword();
        }
    }
}
