# Rubrica

Rubrica telefonica desktop scritta in Java con Swing. Permette di aggiungere, modificare ed eliminare contatti; i dati sono salvati in un file di testo e ricaricati a ogni avvio. L'accesso richiede utente e password.

Ogni contatto ha nome, cognome, indirizzo, telefono ed età. La finestra principale mostra in una tabella nome, cognome e telefono; indirizzo ed età si vedono e si cambiano nell'editor. I comandi sono in una barra degli strumenti in cima alla finestra principale e all'editor.

## Uso

- All'avvio compare solo la finestra di login: si entra con utente e password, anche premendo Invio. Al primo avvio l'unico utente è `admin`, con password `admin`.
- Con credenziali errate compare il messaggio "Login errato" e la finestra principale non si apre. Non c'è una schermata per creare utenti o cambiare password.
- Dalla barra della finestra principale, **Nuovo** apre l'editor con i campi vuoti; **Modifica** lo apre con i dati della persona selezionata (si può anche fare doppio click sulla riga).
- **Elimina** chiede conferma prima di rimuovere la persona selezionata.
- Modifica ed Elimina senza una riga selezionata mostrano un messaggio d'errore.
- Dalla barra dell'editor, **Salva** (o Invio) salva e chiude, **Annulla** (o Esc) chiude senza salvare.
- Nome, cognome e telefono sono obbligatori, l'età deve essere un numero intero tra 0 e 150 e il carattere `;` non è ammesso.

## Requisiti

- Per eseguire il programma: Java 8 o successivo.
- Per compilarlo: JDK 9 o successivo (il codice è compilato per Java 8 con `--release 8`, opzione disponibile dal JDK 9) e Maven 3.6.3 o successivo. È stato sviluppato con JDK 21 e Maven 3.9.

## Compilazione

```
mvn package
```

Compila, esegue i test e crea `target/Rubrica.jar`. I soli test si lanciano con `mvn test`.

## Avvio

Doppio click su `Rubrica.jar`, oppure:

```
java -jar Rubrica.jar
```

I dati sono nella stessa cartella di `Rubrica.jar`: `informazioni.txt` con le persone, creato al primo salvataggio, e `utenti.txt` con gli utenti, creato al primo avvio. Il jar si può spostare ovunque, portando con sé i due file per non perdere contatti e utenti. La cartella deve essere scrivibile.

Avviando le classi da un IDE, senza jar, i file si trovano nella cartella corrente, cioè di norma la radice del progetto.

## Formato dei dati

Una riga per persona, con nome, cognome, indirizzo, telefono ed età separati da `;`. Il file è in UTF-8.

```
Steve;Jobs;via Cupertino 13;0612344;56
Bill;Gates;via Redmond 10;06688989;60
Babbo;Natale;via del Polo Nord;00000111;99
```

Le righe vuote sono ignorate. Se una riga non è valida (campi diversi da cinque, età non numerica) il programma mostra il numero della riga e si chiude senza toccare il file. Si chiude allo stesso modo, indicando la causa, se il file non è in UTF-8. Aprire una rubrica vuota farebbe credere che i dati siano andati persi.

`utenti.txt` ha una riga per utente, con username e hash della password separati da `;`. La password non è mai salvata in chiaro.

```
admin;pbkdf2_sha256$600000$<salt>$<hash>
```

L'hash ha il formato `pbkdf2_sha256$iterazioni$salt$hash`, con salt e hash in Base64. Per il resto valgono le regole di `informazioni.txt`: UTF-8, righe vuote ignorate, errore con il numero di riga.

## Struttura e scelte principali

Il codice è nel package `it.taraborrelli.rubrica`:

- `modello`: le classi `Persona` e `Utente`.
- `logica`: `Rubrica`, la lista delle persone con aggiunta, modifica ed eliminazione, e `Autenticazione`, che controlla le credenziali.
- `persistenza`: le interfacce `PersonaDao` e `UtenteDao` e le implementazioni su file `FilePersonaDao` e `FileUtenteDao`.
- `gui`: finestra di login, finestra principale, editor della persona, modello della tabella e barra degli strumenti.
- `Main` avvia l'applicazione e crea le classi concrete.

Scelte:

- La GUI usa solo `Rubrica`, che conosce soltanto l'interfaccia `PersonaDao`: il salvataggio su file si può sostituire, per esempio con un database, senza toccare logica e interfaccia.
- La lista è un `ArrayList` e non un `Vector`: la sincronizzazione non serve, perché tutto gira sul thread di Swing.
- `PersonaTableModel` estende `AbstractTableModel` e legge direttamente la lista della rubrica; `DefaultTableModel` ne terrebbe una seconda copia.
- La barra degli strumenti è una `JToolBar` non staccabile, costruita dalla stessa classe (`BarraStrumenti`) per le due finestre. I pulsanti hanno icona e testo, così restano chiari anche senza conoscere le icone.
- Ogni inserimento, modifica o eliminazione riscrive l'intero file. La lista in memoria cambia solo dopo che il salvataggio è riuscito, così lista e file restano coerenti anche in caso di errore. `PrintStream` non lancia eccezioni, perciò dopo la scrittura si controlla `checkError()`.
- Il `;` non è accettato nei campi perché romperebbe il formato del file.
- Le password sono salvate come hash PBKDF2-HMAC-SHA256 con 600.000 iterazioni, il valore che l'OWASP raccomanda per questo algoritmo, e un salt casuale di 16 byte per ogni utente; servono solo classi del JDK. Il confronto è a tempo costante (`MessageDigest.isEqual`) e la password resta un `char[]` che si azzera dopo l'uso. Il numero di iterazioni è scritto nell'hash, quindi si può aumentare senza invalidare le password già salvate.
- `FinestraLogin` riceve da `Main` l'azione da eseguire dopo l'accesso e non conosce la finestra principale: l'ordine delle finestre lo decide `Main`. Persone e utenti si leggono prima di mostrare il login, così un file illeggibile si segnala subito.
- I dati stanno accanto al jar e non nella cartella corrente: con il doppio click su Mac e Linux la cartella corrente può essere un'altra.
- Il bytecode è per Java 8, così `Rubrica.jar` parte anche su installazioni di Java datate.

## Icone

Le icone della barra degli strumenti, in `src/main/resources/icone`, sono dei [Material Icons](https://github.com/google/material-design-icons) di Google, distribuiti con licenza [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0). I file non sono modificati, solo rinominati: `nuovo.png` è `person_add`, `modifica.png` è `edit`, `elimina.png` è `delete`, `salva.png` è `save`, `annulla.png` è `close`. Sono nella versione da 18 dp a risoluzione doppia (36 px) e il programma le disegna a 18 px: così restano nitide anche con la scala dello schermo al 125% o 150%, dove un'immagine da 18 px verrebbe ingrandita pixel per pixel e risulterebbe sgranata. Il testo della licenza è in `src/main/resources/icone/LICENSE.txt`.
