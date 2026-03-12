# Test Report - Sessió 4 (Proves Creuades ARES)

## 1. Estat de la implementació pròpia
Abans d'iniciar les proves creuades, aquest és l'estat real del nostre codi pel protocol ARES P2P:

### Servidor
- [x] El Servidor accepta connexions concurrents utilitzant Threads.
- [x] Processa `CLIENT_REGISTER` (0x01) i gestiona usuaris duplicats de forma *thread-safe*.
- [x] Processa `FILE_ANNOUNCE` (0x03) i actualitza el registre global.
- [x] Respon a `SEARCH_REQUEST` (0x04) creuant dades de tots els usuaris connectats.
- [x] Coordina correctament l'inici d'una descàrrega enviant `SERVER_FILE_REQUEST` (0x08) a l'origen i rutant les respostes.
- [x] Actua de *proxy* retransmetent paquets `CHUNK_REQUEST` (0x0A) i `CHUNK_RESPONSE` (0x0B) entre sol·licitant i origen.

### Client
- [x] Escaneja la carpeta `public/[nickname]` per obtenir mida i hashing dels fitxers locals.
- [x] Implementa fil d'escolta (`ServerListener`) per no bloquejar l'entrada de consola i processar missatges de xarxa en segon pla.
- [x] Permet executar comandes `search` i `download`.
- [x] Quan actua com a origen: atén peticions (0x08), llegeix bytes del disc dur i serveix fragments sota demanda.
- [x] Quan actua com a sol·licitant: rep fragments de la xarxa, els escriu seqüencialment a la carpeta `downloads/` i finalitza amb `HASH_VERIFY` (0x0C).

## 2. Proves Creuades a realitzar al Laboratori
*Aquesta secció s'omplirà durant la sessió presencial un cop se'ns assignin els grups.*

