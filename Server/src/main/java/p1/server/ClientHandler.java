package p1.server;

import utils.AresComUtils;
import utils.AresFile;
import utils.AresSearchResult;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket socket;
    private AresComUtils aresComUtils;
    private ServerState state = ServerState.ESPERANT;

    private Server server;
    private String myNickname = null;
    private List<AresFile> meusFitxers = new ArrayList<>();

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public List<AresFile> getMeusFitxers() { return meusFitxers; }
    public String getMyNickname() { return myNickname; }

    private void changeState(ServerState newState) {
        System.out.println("[ClientHandler " + (myNickname != null ? myNickname : socket.getInetAddress()) + "] Transició: " + state + " -> " + newState);
        this.state = newState;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(30000);
            aresComUtils = new AresComUtils(socket.getInputStream(), socket.getOutputStream());

            System.out.println("Nou client connectat des de: " + socket.getInetAddress());

            while (!socket.isClosed()) {
                byte opcode = aresComUtils.readOpcode();
                processMessage(opcode);
            }

        } catch (SocketTimeoutException e) {
            System.err.println("Timeout: S'han superat els 30 segons sense activitat.");
            changeState(ServerState.ERROR);
        } catch (IOException e) {
            System.err.println("Desconnexió o error de socket: " + e.getMessage());
            changeState(ServerState.ERROR);
        } finally {
            tancarConnexio();
        }
    }

    private void tancarConnexio() {
        try {
            if (myNickname != null) {
                server.eliminarClient(myNickname);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Connexió tancada correctament.");
            }
        } catch (IOException e) {
            System.err.println("Error al tancar el socket: " + e.getMessage());
        }
    }

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
                System.err.println("S'ha rebut un missatge en un estat no preparat: " + state);
                aresComUtils.writeError((byte) 0x06, "Violacio del protocol");
                changeState(ServerState.ERROR);
                break;
        }
    }

    private void handleEsperant(byte opcode) throws IOException {
        if (opcode == AresComUtils.OPCODE_CLIENT_REGISTER) {
            changeState(ServerState.REGISTRANT_CLIENT);

            String intentNickname = aresComUtils.readClientRegister();
            System.out.println("Intent de registre: " + intentNickname);

            boolean registrat = server.registrarClient(intentNickname, this);

            if (registrat) {
                this.myNickname = intentNickname;
                aresComUtils.writeClientRegisterResponse((byte) 0x00, "C1", 8192); //
            } else {
                aresComUtils.writeClientRegisterResponse((byte) 0x01, "00", 0);
                System.out.println("Registre denegat: Nickname ja existeix.");
            }
            changeState(ServerState.ESPERANT);

        } else if (opcode == AresComUtils.OPCODE_FILE_ANNOUNCE) {
            changeState(ServerState.REBENT_ANUNCI);

            this.meusFitxers = aresComUtils.readFileAnnounce(); //
            System.out.println("[" + myNickname + "] Ha anunciat " + meusFitxers.size() + " fitxers.");
            aresComUtils.writeAck(); //

            changeState(ServerState.ESPERANT);

        } else if (opcode == AresComUtils.OPCODE_SEARCH_REQUEST) {
            changeState(ServerState.PROCESSANT_CERCA);

            String query = aresComUtils.readSearchRequest(); //
            System.out.println("[" + myNickname + "] Cerca sol·licitada: '" + query + "'");

            List<AresSearchResult> resultats = server.cercarFitxers(query);
            aresComUtils.writeSearchResponse(resultats); //

            changeState(ServerState.ESPERANT);

        } else if (opcode == AresComUtils.OPCODE_DOWNLOAD_REQUEST) {
            changeState(ServerState.PROCESSANT_DESCARGA);

            String[] reqFilename = new String[1];
            String[] reqSourceId = new String[1];
            aresComUtils.readDownloadRequest(reqFilename, reqSourceId); //

            System.out.println("[" + myNickname + "] Sol·licita descàrrega de: " + reqFilename[0]);

            List<AresSearchResult> resultats = server.cercarFitxers(reqFilename[0]);

            if (resultats.isEmpty()) {
                // Si no hi és, responem immediatament amb error 0x01
                System.out.println("Fitxer no trobat al registre central.");
                aresComUtils.writeDownloadResponse((byte)0x01, reqFilename[0], 0, "00", 0);
            } else {
                // 1. Generem el Transfer ID únic
                int transferId = server.generarTransferId();

                // 2. Guardem que NOSALTRES (aquest ClientHandler) estem esperant aquest fitxer
                server.registrarTransferencia(transferId, this);

                // 3. Busquem el Handler de l'usuari que té el fitxer
                String peerOrigen = resultats.get(0).getPeers().get(0);
                ClientHandler handlerOrigen = server.getClientHandler(peerOrigen);

                if (handlerOrigen != null) {
                    System.out.println("Enviant SERVER_FILE_REQUEST (0x08) a l'origen: " + peerOrigen);
                    // 4. Li diem a l'altre fil que envïi la petició al seu usuari
                    handlerOrigen.demanarFitxerAlClientOrigen(reqFilename[0], transferId, this.myNickname);
                } else {
                    System.out.println("Client origen desconnectat.");
                    aresComUtils.writeDownloadResponse((byte)0x02, reqFilename[0], 0, peerOrigen, 0); //
                }
            }
            changeState(ServerState.ESPERANT);

        } else if (opcode == AresComUtils.OPCODE_SERVER_FILE_RESPONSE) {
            // Llegim la resposta del Client Origen (0x09)
            byte[] status = new byte[1];
            int[] transferId = new int[1];
            String[] filename = new String[1];
            long[] fileSize = new long[1];
            byte[][] fileHash = new byte[1][];

            aresComUtils.readServerFileResponse(status, transferId, filename, fileSize, fileHash);
            System.out.println("[" + myNickname + "] Ha acceptat enviar el fitxer (0x09) amb status: " + status[0]);
            // Registrem que NOSALTRES som l'origen d'aquesta transferència
            server.registrarOrigen(transferId[0], this);
            // 2. Busquem al mapa concurrent qui havia demanat aquesta transferència
            ClientHandler solLicitant = server.getSolLicitantPerTransferencia(transferId[0]);

            if (solLicitant != null) {
                // 3. Reenviem la decisió al Sol·licitant amb un 0x07
                System.out.println("Avisant al sol·licitant que la transferència " + transferId[0] + " pot començar.");
                solLicitant.enviarRespostaDescarga(status[0], filename[0], fileSize[0], this.myNickname, transferId[0]);
            } else {
                System.err.println("Error: El client sol·licitant de la transferència " + transferId[0] + " s'ha desconnectat.");
            }
            changeState(ServerState.ESPERANT);
        }
        else if (opcode == AresComUtils.OPCODE_CHUNK_REQUEST) {
        int[] tId = new int[1];
        int[] chunkNum = new int[1];
        aresComUtils.readChunkRequest(tId, chunkNum); //

        // El Sol·licitant demana un tros, l'enviem a l'Origen
        ClientHandler origen = server.getOrigenPerTransferencia(tId[0]);
        if (origen != null) {
            origen.aresComUtils.writeChunkRequest(tId[0], chunkNum[0]);
        }

    } else if (opcode == AresComUtils.OPCODE_CHUNK_RESPONSE) {
            int[] tId = new int[1];
            int[] chunkNum = new int[1];
            int[] chunkSize = new int[1];
            byte[][] chunkData = new byte[1][];
            aresComUtils.readChunkResponse(tId, chunkNum, chunkSize, chunkData); //

            // L'Origen envia un tros, l'enviem al Sol·licitant
            ClientHandler sollicitant = server.getSolLicitantPerTransferencia(tId[0]);
            if (sollicitant != null) {
                sollicitant.aresComUtils.writeChunkResponse(tId[0], chunkNum[0], chunkSize[0], chunkData[0]);
            }
        } else if (opcode == AresComUtils.OPCODE_HASH_VERIFY) {
            byte[][] fileHash = new byte[1][];
            // Això llegeix i treu els 32 bytes de zeros de la canonada
            aresComUtils.readHashVerify(fileHash);
            System.out.println("[" + myNickname + "] Verifica el hash. Tot correcte!");
            // Responem al client que tot està bé
            aresComUtils.writeAck();
        }

        else {
            System.err.println("Opcode inesperat (" + opcode + ") en estat ESPERANT.");
            aresComUtils.writeError((byte) 0x01, "Format de missatge invalid o inesperat");
        }
    }

    /** * Mètode cridat per un ALTRE fil per forçar a aquest fil a enviar un 0x08 al seu client
     */
    public synchronized void demanarFitxerAlClientOrigen(String filename, int transferId, String solLicitantId) throws IOException {
        aresComUtils.writeServerFileRequest(filename, transferId, solLicitantId);
    }

    /** * Mètode cridat per un ALTRE fil per forçar a aquest fil a enviar la resposta 0x07
     */
    public synchronized void enviarRespostaDescarga(byte status, String filename, long size, String sourceId, int transferId) throws IOException {
        aresComUtils.writeDownloadResponse(status, filename, size, sourceId, transferId);
    }
}