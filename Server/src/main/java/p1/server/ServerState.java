package p1.server;

/**
 * Defineix els estats possibles de la màquina d'estats d'un ClientHandler
 * al servidor segons el protocol ARES.
 */
public enum ServerState {
    ESPERANT,
    REGISTRANT_CLIENT,
    REBENT_ANUNCI,
    PROCESSANT_CERCA,
    PROCESSANT_DESCARGA,
    CONTACTANT_ORIGEN,
    PREPARAT_TRANSFERENCIA,
    RETRANSMETENT_FRAGMENTS,
    VERIFICANT_HASH,
    FINALITZAT,
    ERROR
}