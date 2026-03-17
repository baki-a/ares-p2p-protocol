# Memòria de la Pràctica 1

## 1. Resum de les Sessions

### Sessió 1: Fonaments i Utilitats de Comunicació
En aquesta primera sessió ens vam familiaritzar amb l'arquitectura client-servidor bàsica utilitzant Sockets TCP. L'objectiu principal va ser establir la capa de comunicació de baix nivell, implementant la classe `ComUtils` per garantir una lectura i escriptura precisa de bytes (enters, cadenes de text, etc.) a través de la xarxa, establint les bases per enviar els futurs Opcodes del protocol ARES.

### Sessió 2: Connexió i Registre
Es va començar a donar forma al protocol. Vam implementar el servidor per acceptar múltiples connexions de manera concurrent mitjançant la creació de fils d'execució (`Threads`) amb la classe `ClientHandler`. Per part del client, es va programar l'inici de sessió, l'enviament del paquet de registre (`CLIENT_REGISTER`) i l'escaneig del directori local `public/` per recopilar la llista de fitxers disponibles.

### Sessió 3: Anunci de Fitxers i Cerca (Thread-Safety)
L'enfocament es va centrar en la gestió de la informació compartida. Al servidor, vam introduir estructures de dades segures per a fils (`ConcurrentHashMap`) per mantenir el registre global d'usuaris connectats i els seus fitxers sense problemes de condició de carrera. Vam implementar l'enviament del `FILE_ANNOUNCE` des del client i la capacitat de resoldre cerques (`SEARCH_REQUEST`), creuant les dades de tots els usuaris connectats per retornar resultats precisos.

### Sessió 4: Descàrrega per Fragments i Proves Creuades
Vam implementar l'arquitectura asíncrona al client mitjançant un `ServerListener` en segon pla per poder rebre peticions de descàrrega sense bloquejar la consola. Vam desenvolupar la triangulació al servidor (actuant com a proxy per als missatges d'inici de transferència) i vam programar la lectura i escriptura dinàmica de fitxers al disc dur mitjançant la fragmentació de dades (*Chunks* de 8192 bytes). Finalment, es van realitzar les proves d'interoperabilitat amb altres grups.

---

## 2. Problemes Trobats i Solucions Aplicades

Durant el desenvolupament de la pràctica, ens hem enfrontat a diversos reptes tècnics d'arquitectura i xarxes:

* **Bloqueig del fil principal al Client (I/O Blocking):** * *Problema:* El client no podia escoltar peticions entrants (com rebre fragments o respondre a descàrregues d'altres) mentre la consola estava bloquejada esperant un *input* de l'usuari amb el `Scanner`.
    * *Solució:* Es va dissenyar una classe interna `ServerListener` que s'executa en un `Thread` paral·lel exclusivament per llegir el socket constantment i processar els Opcodes entrants de manera asíncrona.
* **Corrupció de la canonada TCP (Cascada d'Opcode 0):**
    * *Problema:* En la fase final de descàrrega, el servidor rebia una infinitat d'errors "Opcode 0 inesperat" i es tancava la connexió temporalment.
    * *Solució:* Vam detectar que un client enviava el missatge `HASH_VERIFY` (Opcode `0x0C`) amb un payload de 32 bytes, però el servidor no estava programat per llegir-lo. Això deixava 32 zeros atrapats al *buffer* del socket, que el servidor interpretava erròniament com a nous Opcodes. Es va solucionar afegint la lectura buida pertinent al `ClientHandler` per netejar el flux de dades.
* **Tancaments sobtats per NullPointerException:**
    * *Problema:* En rebutjar una transferència (enviar status d'error `0x01`), passar una cadena buida `""` com a nom de fitxer provocava excepcions al moment d'escriure els bytes al socket.
    * *Solució:* Es va estandarditzar retornar sempre el nom original sol·licitat en el paquet de resposta, fins i tot en cas d'error, per mantenir la integritat del format del paquet ARES.

---

## 3. Sessió de proves creuades

| Grup | Components                         | Usuari GitHub |
|------|------------------------------------|---------------|
| B10  | ANASS BAKI ACHKOUKAR               | baki-a        |


### La vostra pràctica
En aquest apartat cal explicar l'estat inicial de la vostra pràctica:

- __Servidor__
- [x] El meu __Servidor__ arranca i permet que es connectin __Clients__, assignant-los un identificador.
- [x] El meu __Servidor__ té implementada la fase de configuració en que els __Clients__ actualitzen la llista de fitxers.
- [x] El meu __Servidor__ implementa la dinàmica de cerca de fitxers, en la qual els __Clients__ poden fer cerques i obtenen els fitxers que hi encaixen.
- [x] El meu __Servidor__ implementa la descàrrega de fitxers **amb un client**.
- [x] El meu __Servidor__ implementa la descàrrega de fitxers **multi-client**.

- __Client__
- [x] El meu __Client__ es connecta correctament al servidor i s'hi registra.
- [x] El meu __Client__ té implementada la fase de configuració en que actualitza la llista dels fitxers disponibles.
- [x] El meu __Client__ implementa la dinàmica de cerca de fitxers, en la qual l'usuari indica un patró de cerca i obté una llista de fitxers disponibles que hi encaixen.
- [x] El meu __Client__ implementa la dinàmica de transferència de fitxers, enviant els fitxers que se li demanen i rebent els que ha sol·licitat.

**Proves pròpies realitzades:**
Abans de la sessió, s'ha provat l'arquitectura instanciant 1 Servidor i 2 Clients en local (Origen i Destí). S'ha comprovat correctament l'anunci de fitxers, la cerca creuada i la descàrrega per fragments (Chunks de 8192 bytes) llegint i escrivint de disc dinàmicament sense bloquejar l'execució gràcies a l'ús de fils (ServerListener). També s'ha validat l'enviament final del HASH_VERIFY.


### Proves realitzades

Per cada grup que hagueu provat, caldra informar del nom del Grup que s'ha avaluat i la informació bàsica equivalent a la anterior:

**Grup Avaluat: B09**

- __Servidor__
- [x] El seu __Servidor__ arranca i permet que es connectin __Clients__, assignant-los un identificador.
- [x] El seu __Servidor__ té implementada la fase de configuració en que els __Clients__ actualitzen la llista de fitxers. * llegir resultats
- [x] El seu __Servidor__ implementa la dinàmica de cerca de fitxers, en la qual els __Clients__ poden fer cerques i obtenen els fitxers que hi encaixen.
- [x] El seu __Servidor__ implementa la descàrrega de fitxers **amb un client**.
- [x] El seu __Servidor__ implementa la descàrrega de fitxers **multi-client**.

- __Client__
- [x] El seu __Client__ es connecta correctament al servidor i s'hi registra.
- [x] El seu __Client__ té implementada la fase de configuració en que actualitza la llista dels fitxers disponibles.
- [x] El seu __Client__ implementa la dinàmica de cerca de fitxers, en la qual l'usuari indica un patró de cerca i obté una llista de fitxers disponibles que hi encaixen.
- [x] El seu __Client__ implementa la dinàmica de transferència de fitxers, enviant els fitxers que se li demanen i rebent els que ha sol·licitat.

**Resultats de les proves i errors detectats:**
El seu servidor es queda penjat al fer un ANNOUNCE, cosa que el meu client fa automàticament al connectar-se. Per provar altres funcions, s'ha provat sense fer cap announce les altres feature, que semblen estar correctament implementades.


**Grup Avaluat: C04**
- __Servidor__
- [x] El seu __Servidor__ arranca i permet que es connectin __Clients__, assignant-los un identificador.
- [ ] El seu __Servidor__ té implementada la fase de configuració en que els __Clients__ actualitzen la llista de fitxers.
- [ ] El seu __Servidor__ implementa la dinàmica de cerca de fitxers, en la qual els __Clients__ poden fer cerques i obtenen els fitxers que hi encaixen.
- [ ] El seu __Servidor__ implementa la descàrrega de fitxers **amb un client**.
- [ ] El seu __Servidor__ implementa la descàrrega de fitxers **multi-client**.

- __Client__
- [x] El seu __Client__ es connecta correctament al servidor i s'hi registra.
- [ ] El seu __Client__ té implementada la fase de configuració en que actualitza la llista dels fitxers disponibles.
- [ ] El seu __Client__ implementa la dinàmica de cerca de fitxers, en la qual l'usuari indica un patró de cerca i obté una llista de fitxers disponibles que hi encaixen.
- [ ] El seu __Client__ implementa la dinàmica de transferència de fitxers, enviant els fitxers que se li demanen i rebent els que ha sol·licitat.

- **NOTA:** El grup que em tocava avaluar ha estat ocupat durant tota la sessió, per la qual cosa he avaluat el grup C04.
- **Errors:** El grup C04 li arrancava el servidor i podia connectar-me. Malauradament, instantàniament em desconnectava, impossibilitant fer més proves.