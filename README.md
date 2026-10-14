# Rubrica

Rubrica telefonica desktop scritta in Java con Swing. Permette di aggiungere, modificare ed eliminare contatti; i dati sono salvati in un file di testo e ricaricati a ogni avvio.

Ogni contatto ha nome, cognome, indirizzo, telefono ed età. La finestra principale mostra in una tabella nome, cognome e telefono; indirizzo ed età si vedono e si cambiano nell'editor.

## Uso

- **Nuovo** apre l'editor con i campi vuoti; **Modifica** lo apre con i dati della persona selezionata (si può anche fare doppio click sulla riga).
- **Elimina** chiede conferma prima di rimuovere la persona selezionata.
- Modifica ed Elimina senza una riga selezionata mostrano un messaggio d'errore.
- Nell'editor **Salva** (o Invio) salva e chiude, **Annulla** (o Esc) chiude senza salvare.
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

I dati sono in `informazioni.txt`, nella stessa cartella di `Rubrica.jar`; il file viene creato al primo salvataggio. Il jar si può spostare ovunque, portando con sé `informazioni.txt` per non perdere i contatti. La cartella deve essere scrivibile.

Avviando le classi da un IDE, senza jar, il file si trova nella cartella corrente, cioè di norma la radice del progetto.

## Formato dei dati

Una riga per persona, con nome, cognome, indirizzo, telefono ed età separati da `;`. Il file è in UTF-8.

```
Steve;Jobs;via Cupertino 13;0612344;56
Bill;Gates;via Redmond 10;06688989;60
Babbo;Natale;via del Polo Nord;00000111;99
```

Le righe vuote sono ignorate. Se una riga non è valida (campi diversi da cinque, età non numerica) il programma mostra il numero della riga e si chiude senza toccare il file. Si chiude allo stesso modo, indicando la causa, se il file non è in UTF-8. Aprire una rubrica vuota farebbe credere che i dati siano andati persi.

## Struttura e scelte principali

Il codice è nel package `it.taraborrelli.rubrica`:

- `modello`: la classe `Persona`.
- `logica`: `Rubrica`, la lista delle persone con aggiunta, modifica ed eliminazione.
- `persistenza`: l'interfaccia `PersonaDao` e l'implementazione su file `FilePersonaDao`.
- `gui`: finestra principale, editor della persona e modello della tabella.
- `Main` avvia l'applicazione e crea le classi concrete.

Scelte:

- La GUI usa solo `Rubrica`, che conosce soltanto l'interfaccia `PersonaDao`: il salvataggio su file si può sostituire, per esempio con un database, senza toccare logica e interfaccia.
- La lista è un `ArrayList` e non un `Vector`: la sincronizzazione non serve, perché tutto gira sul thread di Swing.
- `PersonaTableModel` estende `AbstractTableModel` e legge direttamente la lista della rubrica; `DefaultTableModel` ne terrebbe una seconda copia.
- Ogni inserimento, modifica o eliminazione riscrive l'intero file. La lista in memoria cambia solo dopo che il salvataggio è riuscito, così lista e file restano coerenti anche in caso di errore. `PrintStream` non lancia eccezioni, perciò dopo la scrittura si controlla `checkError()`.
- Il `;` non è accettato nei campi perché romperebbe il formato del file.
- I dati stanno accanto al jar e non nella cartella corrente: con il doppio click su Mac e Linux la cartella corrente può essere un'altra.
- Il bytecode è per Java 8, così `Rubrica.jar` parte anche su installazioni di Java datate.
