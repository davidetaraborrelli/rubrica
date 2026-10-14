# Rubrica

Rubrica telefonica desktop scritta in Java con Swing. Permette di aggiungere, modificare ed eliminare contatti; i dati sono salvati in una cartella, un file di testo per persona, oppure su un database MySQL, e ricaricati a ogni avvio. L'accesso richiede utente e password.

Ogni contatto ha nome, cognome, indirizzo, telefono ed età. La finestra principale mostra in una tabella nome, cognome e telefono; indirizzo ed età si vedono e si cambiano nell'editor. I comandi sono in una barra degli strumenti in cima a ogni finestra: login, finestra principale ed editor.

## Uso

- All'avvio compare solo la finestra di login: si entra con utente e password, con il pulsante LOGIN o premendo Invio. Al primo avvio l'unico utente è `admin`, con password `admin`.
- Con credenziali errate compare il messaggio "Login errato" e la finestra principale non si apre. Non c'è una schermata per creare utenti o cambiare password.
- Dalla barra della finestra principale, **Nuovo** apre l'editor con i campi vuoti; **Modifica** lo apre con i dati della persona selezionata (si può anche fare doppio click sulla riga).
- **Elimina** chiede conferma prima di rimuovere la persona selezionata.
- Modifica ed Elimina senza una riga selezionata mostrano un messaggio d'errore.
- Dalla barra dell'editor, **Salva** (o Invio) salva e chiude, **Annulla** (o Esc) chiude senza salvare.
- Nome, cognome e telefono sono obbligatori, l'età deve essere un numero intero tra 0 e 150 e il carattere `;` non è ammesso.

## Requisiti

- Per eseguire il programma: Java 8 o successivo.
- Per il salvataggio su database: un server MySQL 8 o MariaDB. Il driver è già dentro `Rubrica.jar`.
- Per compilarlo: JDK 9 o successivo (il codice è compilato per Java 8 con `--release 8`, opzione disponibile dal JDK 9) e Maven 3.6.3 o successivo. È stato sviluppato con JDK 21 e Maven 3.9.

## Compilazione

```
mvn package
```

Compila, esegue i test e crea `target/Rubrica.jar`, con il driver MySQL incluso, e `target/Rubrica.zip`, il pacchetto per il salvataggio su database. I soli test si lanciano con `mvn test`. `target/original-Rubrica.jar` è il jar senza il driver, che il plugin di Maven lascia da parte: non serve.

## Avvio

Doppio click su `Rubrica.jar`, oppure:

```
java -jar Rubrica.jar
```

I dati sono nella stessa cartella di `Rubrica.jar`: la cartella `informazioni` con le persone, creata all'avvio se manca, e `utenti.txt` con gli utenti, creato al primo avvio. Il jar si può spostare ovunque, portando con sé la cartella e il file per non perdere contatti e utenti. La cartella che contiene il jar deve essere scrivibile.

Avviando le classi da un IDE, senza jar, la cartella e il file si trovano nella cartella corrente, cioè di norma la radice del progetto.

## Salvataggio su file

Le persone sono salvate nella cartella `informazioni`, un file per persona: `Persona1.txt`, `Persona2.txt` e così via. Ogni file ha una sola riga (formato in fondo). Il numero nel nome è l'id della persona: resta lo stesso a ogni avvio, e una persona nuova prende l'id più alto presente nella cartella più uno. Due persone con lo stesso nome e cognome stanno quindi in file diversi e non si sovrascrivono.

- I file con un altro nome, comprese le versioni con zeri iniziali come `Persona01.txt`, sono ignorati.
- Un file di persona che non si può leggere (campi sbagliati, vuoto, con più di una persona, non in UTF-8) fa comparire il motivo, con il nome del file, e il programma si chiude: una rubrica incompleta farebbe credere che i dati siano andati persi.
- Se qualcuno cancella a mano il file di una persona mentre il programma è aperto, modificarla o eliminarla mostra un messaggio d'errore.

Con un argomento si può scegliere un altro salvataggio:

| Comando | Dove si salvano le persone |
|---|---|
| `java -jar Rubrica.jar` | nella cartella `informazioni`; su database se accanto al jar c'è `credenziali_database.properties` |
| `java -jar Rubrica.jar cartella` | nella cartella `informazioni`, anche se accanto al jar c'è `credenziali_database.properties` |
| `java -jar Rubrica.jar file` | tutte in un solo file, `informazioni.txt`, una per riga, mai su database |

Un argomento è una richiesta esplicita e vale più del file delle credenziali. Con qualsiasi altro argomento il programma mostra l'uso corretto e si chiude. Gli utenti stanno sempre in `utenti.txt`, tranne che con il database. I salvataggi sono archivi separati: passare dall'uno all'altro non sposta i dati.

## Salvataggio su database

Se accanto a `Rubrica.jar` c'è il file `credenziali_database.properties` e non si passa nessun argomento, persone e utenti si salvano su un database MySQL 8 o MariaDB invece che su file; senza quel file si usano i file. Se il database non è raggiungibile il programma mostra il motivo e si chiude, senza ripiegare in silenzio sui file.

Il pacchetto `target/Rubrica.zip` contiene i tre file che chiede il documento, presi da `Rubrica.jar` e dalla cartella `dist/`:

- `Rubrica.jar`, con il driver MySQL incluso.
- `schema_database.sql` crea il database `rubrica` con le tabelle `persone` e `utenti` e l'utente `admin`. Si può lanciare più volte.
- `credenziali_database.properties` è il modello con username, password, indirizzo e porta del server, da compilare. Nei commenti ricorda anche il login iniziale.

Per provarlo, in sequenza:

1. decomprimere l'archivio;
2. aprire `credenziali_database.properties` e sostituire le credenziali con quelle del proprio sistema MySQL;
3. lanciare sul proprio MySQL lo script `schema_database.sql`, ad esempio con `mysql -u root -p < schema_database.sql` dalla cartella dello script;
4. eseguire `Rubrica.jar`, con il doppio click o con `java -jar Rubrica.jar`, e accedere con utente `admin` e password `admin`.

Gli errori più comuni (accesso negato, server irraggiungibile, database o tabelle mancanti perché lo script non è stato lanciato) compaiono come messaggio, e il programma si chiude.

## Formato dei dati

Ogni persona è una riga, con nome, cognome, indirizzo, telefono ed età separati da `;`. I file `Persona<id>.txt` hanno una riga sola; `informazioni.txt` (argomento `file`) ne ha una per persona, come in questo esempio. I file sono in UTF-8.

```
Steve;Jobs;via Cupertino 13;0612344;56
Bill;Gates;via Redmond 10;06688989;60
Babbo;Natale;via del Polo Nord;00000111;99
```

Le righe vuote sono ignorate. Se una riga non è valida (campi diversi da cinque, età non numerica) il programma mostra il nome del file e il numero della riga e si chiude senza toccare i dati. Si chiude allo stesso modo, indicando la causa, se un file non è in UTF-8. Aprire una rubrica vuota o incompleta farebbe credere che i dati siano andati persi.

`utenti.txt` ha una riga per utente, con username e hash della password separati da `;`. La password non è mai salvata in chiaro.

```
admin;pbkdf2_sha256$600000$<salt>$<hash>
```

L'hash ha il formato `pbkdf2_sha256$iterazioni$salt$hash`, con salt e hash in Base64. Per il resto valgono le stesse regole: UTF-8, righe vuote ignorate, errore con il numero di riga.

## Struttura e scelte principali

Il codice è nel package `it.taraborrelli.rubrica`:

- `modello`: le classi `Persona` e `Utente`.
- `logica`: `Rubrica`, la lista delle persone con aggiunta, modifica ed eliminazione, e `Autenticazione`, che controlla le credenziali.
- `persistenza`: le interfacce `PersonaDao` e `UtenteDao`; per le persone `CartellaPersonaDao` (un file per persona nella cartella `informazioni`), `FilePersonaDao` (tutte in `informazioni.txt`) e `JdbcPersonaDao`; per gli utenti `FileUtenteDao` (`utenti.txt`) e `JdbcUtenteDao`; `ConfigurazioneDatabase` legge le credenziali e apre le connessioni; `FileDiTesto` e `FormatoPersona` sono le classi di supporto dei DAO su file.
- `gui`: finestra di login, finestra principale, editor della persona, modello della tabella e barra degli strumenti.
- `Main` avvia l'applicazione, sceglie tra cartella, file singolo e database e crea le classi concrete.

Scelte:

- La GUI usa solo `Rubrica` e `Autenticazione`, che conoscono soltanto le interfacce `PersonaDao` e `UtenteDao`: il salvataggio in una cartella, in un file e su database sono intercambiabili senza toccare logica e interfaccia, e a sceglierli è solo `Main`.
- La lista è un `ArrayList` e non un `Vector`: la sincronizzazione non serve, perché tutto gira sul thread di Swing.
- `PersonaTableModel` estende `AbstractTableModel` e legge direttamente la lista della rubrica; `DefaultTableModel` ne terrebbe una seconda copia.
- La barra degli strumenti è una `JToolBar` non staccabile, costruita dalla stessa classe (`BarraStrumenti`) per le tre finestre, compreso il login. I pulsanti hanno icona e testo, così restano chiari anche senza conoscere le icone.
- `CartellaPersonaDao` è il salvataggio predefinito, come chiede l'estensione del documento: non un singolo file, ma una cartella con un file per persona. Prende l'id dal nome del file: non serve un indice a parte, l'id è stabile tra un avvio e l'altro e il nome non dipende dai dati, quindi gli omonimi non si scontrano. I file nuovi si creano con `CREATE_NEW`, che non permette di sovrascrivere il file di un'altra persona.
- `FilePersonaDao`, il salvataggio nel singolo `informazioni.txt` della parte base del documento, resta disponibile con l'argomento `file`. Ogni inserimento, modifica o eliminazione riscrive l'intero file.
- Come chiede il documento, i file si leggono con `Scanner`, si scrivono con `PrintStream` e si identificano con `File`. Il codice sta in `FileDiTesto`, usato da tutti i DAO su file, così non è copiato tre volte. `PrintStream` non lancia eccezioni, perciò dopo la scrittura si controlla `checkError()`; `Scanner` non lancia `IOException`, perciò dopo la lettura si chiede `ioException()`, che segnala ad esempio un file non in UTF-8.
- In ogni salvataggio la lista in memoria cambia solo dopo che la scrittura è riuscita, così lista e dati restano coerenti anche in caso di errore.
- Il `;` non è accettato nei campi perché romperebbe il formato del file.
- Le password sono salvate come hash PBKDF2-HMAC-SHA256 con 600.000 iterazioni, il valore che l'OWASP raccomanda per questo algoritmo, e un salt casuale di 16 byte per ogni utente; servono solo classi del JDK. Il confronto è a tempo costante (`MessageDigest.isEqual`) e la password resta un `char[]` che si azzera dopo l'uso. Il numero di iterazioni è scritto nell'hash, quindi si può aumentare senza invalidare le password già salvate.
- I DAO JDBC usano `PreparedStatement` e `try-with-resources`. Ogni operazione apre e chiude la propria connessione, così una connessione scaduta non può rovinare quelle successive; l'id lo genera il database.
- Gli errori SQL più comuni (accesso negato, server irraggiungibile, database o tabelle mancanti) diventano messaggi che dicono cosa fare. La connessione ha un timeout di 5 secondi, per non restare appesi con un server spento.
- Lo username ha la collation `utf8mb4_bin`, per distinguere maiuscole e minuscole come nel salvataggio su file.
- I test dei DAO JDBC usano H2 in memoria, in modalità MySQL, e creano le tabelle con lo stesso `schema_database.sql` che si consegna. Lo script e il programma sono stati provati anche su MySQL 8.4 e su MariaDB 12.
- `FinestraLogin` riceve da `Main` l'azione da eseguire dopo l'accesso e non conosce la finestra principale: l'ordine delle finestre lo decide `Main`. Persone e utenti si leggono prima di mostrare il login, così un file illeggibile si segnala subito.
- I dati stanno accanto al jar e non nella cartella corrente: con il doppio click su Mac e Linux la cartella corrente può essere un'altra.
- Il bytecode è per Java 8, così `Rubrica.jar` parte anche su installazioni di Java datate.

## Icone

Le icone della barra degli strumenti, in `src/main/resources/icone`, sono dei [Material Icons](https://github.com/google/material-design-icons) di Google, distribuiti con licenza [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0). I file non sono modificati, solo rinominati: `nuovo.png` è `person_add`, `modifica.png` è `edit`, `elimina.png` è `delete`, `salva.png` è `save`, `annulla.png` è `close`, `login.png` è `login`. Sono nella versione da 18 dp a risoluzione doppia (36 px) e il programma le disegna a 18 px: così restano nitide anche con la scala dello schermo al 125% o 150%, dove un'immagine da 18 px verrebbe ingrandita pixel per pixel e risulterebbe sgranata. Il testo della licenza è in `src/main/resources/icone/LICENSE.txt`.

## Driver MySQL

Il driver JDBC è [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/) di Oracle, versione 26.7.0, compilato per Java 8. È distribuito con licenza GPL v2 con la Universal FOSS Exception e viene incluso in `Rubrica.jar` con il maven-shade-plugin, insieme a protobuf-java (licenza BSD a tre clausole), da cui dipende; il file `LICENSE` del driver resta nel jar.

## Licenza

Il codice di questo progetto è distribuito con licenza MIT, il cui testo è nel file `LICENSE`: chiunque può leggerlo, usarlo e modificarlo. Le icone e il driver MySQL hanno la propria licenza, indicata nelle due sezioni precedenti.
