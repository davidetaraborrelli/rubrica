package it.taraborrelli.rubrica.persistenza;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Lettura e scrittura dei file di testo UTF-8 dei DAO su file, come chiede il documento: la classe
 * Scanner per leggere, la classe PrintStream per scrivere e la classe File per identificare il file.
 */
final class FileDiTesto {

    private static final String CODIFICA = "UTF-8";

    /**
     * Trasforma una riga non vuota del file nell'oggetto che rappresenta.
     */
    interface Interprete<T> {
        T interpreta(String riga, int numeroRiga) throws PersistenzaException;
    }

    private FileDiTesto() {
    }

    /**
     * Legge il file e interpreta le righe non vuote, nell'ordine in cui si trovano. Se il file non
     * esiste restituisce un elenco vuoto.
     */
    static <T> List<T> leggi(File file, Interprete<T> interprete) throws PersistenzaException {
        List<T> lette = new ArrayList<>();
        if (!file.exists()) {
            return lette;
        }
        try (Scanner scanner = new Scanner(file, CODIFICA)) {
            int numeroRiga = 0;
            while (scanner.hasNextLine()) {
                String riga = scanner.nextLine();
                numeroRiga++;
                if (riga.trim().isEmpty()) {
                    continue;
                }
                lette.add(interprete.interpreta(riga, numeroRiga));
            }
            // Scanner non lancia IOException: se la lettura si interrompe, ad esempio per un file
            // non in UTF-8, smette di restituire righe e l'errore va chiesto esplicitamente
            if (scanner.ioException() != null) {
                throw new PersistenzaException("Impossibile leggere il file " + file.getAbsolutePath()
                        + ": verificare che sia salvato in UTF-8.", scanner.ioException());
            }
        } catch (FileNotFoundException e) {
            throw new PersistenzaException("Impossibile aprire il file " + file.getAbsolutePath() + ".", e);
        }
        return lette;
    }

    /**
     * Riscrive il file con le righe date; se il file non c'è lo crea.
     */
    static void scrivi(File file, List<String> righe) throws PersistenzaException {
        try (PrintStream out = new PrintStream(file, CODIFICA)) {
            scriviRighe(out, righe, file);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new PersistenzaException("Impossibile scrivere il file " + file.getAbsolutePath() + ".", e);
        }
    }

    /**
     * Crea il file con le righe date. Se il file esiste già non lo tocca e segnala l'errore.
     */
    static void scriviNuovo(File file, List<String> righe) throws PersistenzaException {
        try (PrintStream out = new PrintStream(
                Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE_NEW), false, CODIFICA)) {
            scriviRighe(out, righe, file);
        } catch (FileAlreadyExistsException e) {
            throw new PersistenzaException("Il file " + file.getAbsolutePath()
                    + " esiste già e non viene sovrascritto.", e);
        } catch (IOException e) {
            throw new PersistenzaException("Impossibile scrivere il file " + file.getAbsolutePath() + ".", e);
        }
    }

    static PersistenzaException rigaNonValida(File file, int numeroRiga, String motivo) {
        return new PersistenzaException("Il file " + file.getAbsolutePath() + " non è valido alla riga "
                + numeroRiga + ": " + motivo + ".");
    }

    private static void scriviRighe(PrintStream out, List<String> righe, File file) throws PersistenzaException {
        for (String riga : righe) {
            out.println(riga);
        }
        // PrintStream non lancia eccezioni in scrittura: gli errori si scoprono solo così
        if (out.checkError()) {
            throw new PersistenzaException("Errore durante la scrittura del file " + file.getAbsolutePath() + ".");
        }
    }
}
