package p1.server;

import utils.AresComUtils;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Cada client que es connecti al servidor tindrà una instància d'aquesta classe
 * executant-se en un fil (Thread) independent.
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private AresComUtils aresComUtils;
    // L'estat inicial segons el diagrama és ESPERANT
    private ServerState state = ServerState.ESPERANT;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Mètode per centralitzar els canvis d'estat i fer-ne un seguiment a la consola.
     */
    private void changeState(ServerState newState) {
        System.out.println("[ClientHandler " + socket.getInetAddress() + "] Transició: " + state + " -> " + newState);
        this.state = newState;
    }

    /**
     * Bucle principal de vida del client. S'executa constantment fins que es desconnecta.
     */
    @Override
    public void run() {
        try {
            // Sessió 2: Timeout de 30 segons per comprovar que la connexió segueix activa
            socket.setSoTimeout(30000);

            // Inicialitzem l'eina de comunicació amb els streams del socket
            aresComUtils = new AresComUtils(socket.getInputStream(), socket.getOutputStream());

            System.out.println("Nou client connectat des de: " + socket.getInetAddress());

            while (!socket.isClosed()) {
                // El servidor es queda adormit aquí fins que rep el primer byte (Opcode)
                byte opcode = aresComUtils.readOpcode();

                // Passem l'Opcode a la màquina d'estats
                processMessage(opcode);
            }

        } catch (SocketTimeoutException e) {
            System.err.println("Timeout: S'han superat els 30 segons sense activitat del client.");
            changeState(ServerState.ERROR);
        } catch (IOException e) {
            System.err.println("Desconnexió o error de socket: " + e.getMessage() + "");
            changeState(ServerState.ERROR);
        } finally {
            // Sessió 2: Quan un socket ja no sigui necessari, caldrà tancar-lo per alliberar recursos
            tancarConnexio();
        }
    }

    /**
     * Allibera els recursos del sistema de forma segura.
     */
    private void tancarConnexio() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Connexió tancada correctament per a " + socket.getInetAddress());
            }
        } catch (IOException e) {
            System.err.println("Error al tancar el socket: " + e.getMessage());
        }
    }

    /**
     * El "cervell" de la màquina d'estats. Decideix què fer basant-se en l'estat actual.
     */
    private void processMessage(byte opcode) throws IOException {
        switch (state) {
            case ESPERANT:
                handleEsperant(opcode);
                break;

            case ERROR:
                System.err.println("El handler està en estat d'error. Es forçarà la desconnexió.");
                tancarConnexio();
                break;

            default:
                System.err.println("S'ha rebut un missatge en un estat intermedi no preparat: " + state);
                aresComUtils.writeError((byte) 0x06, "Violacio del protocol: no s'esperava aquest missatge ara");
                changeState(ServerState.ERROR);
                break;
        }
    }

    /**
     * Lògica exclusiva per quan el servidor està en repòs escoltant (ESPERANT).
     */
    private void handleEsperant(byte opcode) throws IOException {
        if (opcode == AresComUtils.OPCODE_CLIENT_REGISTER) {
            // 1. Canviem a l'estat indicat pel diagrama
            changeState(ServerState.REGISTRANT_CLIENT);

            // 2. Llegim les dades fent servir la nostra súper classe de la Sessió 1
            String clientNickname = aresComUtils.readClientRegister();
            System.out.println("S'ha rebut un intent de registre amb Nickname: " + clientNickname);

            // 3. Simulem un registre correcte (Més endavant aquí hi haurà la lògica del servidor central)
            byte status = 0x00; // Èxit
            String generatedId = "C1"; // ID temporal de 2 bytes
            int chunkSize = 8192; // 8 KB per defecte segons la guia Ares

            // 4. Responem al client
            aresComUtils.writeClientRegisterResponse(status, generatedId, chunkSize);

            // 5. Tornem a ESPERANT segons exigeix la màquina d'estats
            changeState(ServerState.ESPERANT);

        } else if (opcode == AresComUtils.OPCODE_FILE_ANNOUNCE) {
            changeState(ServerState.REBENT_ANUNCI);
            // TODO: Llegir la llista i respondre amb ACK
            changeState(ServerState.ESPERANT);

        } else if (opcode == AresComUtils.OPCODE_SEARCH_REQUEST) {
            changeState(ServerState.PROCESSANT_CERCA);
            // TODO: Llegir la cerca i retornar resultats
            changeState(ServerState.ESPERANT);

        } else if (opcode == AresComUtils.OPCODE_DOWNLOAD_REQUEST) {
            changeState(ServerState.PROCESSANT_DESCARGA);
            // TODO: Llegir petició de descàrrega i començar la triangulació

        } else {
            System.err.println("Opcode inesperat (" + opcode + ") en estat ESPERANT.");
            aresComUtils.writeError((byte) 0x01, "Format de missatge invalid o inesperat");
        }
    }
}