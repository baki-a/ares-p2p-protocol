package p1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import utils.AresSearchResult;
import utils.AresFile;

/**
 * Classe principal del servidor que gestiona les connexions entrants
 * i manté el registre global thread-safe.
 */
public class Server {
    public static final String INIT_ERROR = "Server should be initialized with -p <port>";

    private ServerSocket ss;
    private int port;

    // El mapa concurrent segur per a fils (Thread-safe) que demana la Sessió 3
    private final ConcurrentHashMap<String, ClientHandler> clientsRegistrats = new ConcurrentHashMap<>();
    //  Generador d'IDs únics per a les transferències
    private final java.util.concurrent.atomic.AtomicInteger transferCounter = new java.util.concurrent.atomic.AtomicInteger(1);

    //  Mapa per recordar quin ClientHandler està esperant la resposta d'una transferència
    private final ConcurrentHashMap<Integer, ClientHandler> transferenciesActives = new ConcurrentHashMap<>();
    // NOU: Mapa per recordar quin ClientHandler és l'ORIGEN del fitxer
    private final ConcurrentHashMap<Integer, ClientHandler> origensActius = new ConcurrentHashMap<>();
    public Server(int port) {
        this.port = port;
        setConnection();
    }

    public void registrarOrigen(int transferId, ClientHandler handlerOrigen) {
        origensActius.put(transferId, handlerOrigen);
    }

    public ClientHandler getOrigenPerTransferencia(int transferId) {
        return origensActius.get(transferId);
    }

    public void setConnection() {
        if (this.ss == null) {
            try {
                ss = new ServerSocket(port);
                System.out.println("Servidor ARES operatiu i escoltant al port " + port + "...");
                System.out.println("Prem Ctrl + C per aturar-lo.");
            } catch (IOException e) {
                throw new RuntimeException("Error d'E/S a l'obrir el ServerSocket:\n" + e.getMessage());
            }
        }
    }

    public void init() {
        while(true) {
            try {
                Socket clientSocket = ss.accept();
                System.out.println("Nova connexió acceptada: " + clientSocket.getInetAddress());

                // Creem el ClientHandler passant el Server ('this')
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();

            } catch (IOException e) {
                System.err.println("Error a l'acceptar un client: " + e.getMessage());
            }
        }
    }

    public synchronized boolean registrarClient(String nickname, ClientHandler handler) {
        if (clientsRegistrats.containsKey(nickname)) {
            return false;
        }
        clientsRegistrats.put(nickname, handler);
        System.out.println("[Registre Global] Usuari afegit: " + nickname + ". Total connectats: " + clientsRegistrats.size());
        return true;
    }

    public void eliminarClient(String nickname) {
        if (nickname != null) {
            clientsRegistrats.remove(nickname);
            System.out.println("[Registre Global] Usuari eliminat: " + nickname + ". Total connectats: " + clientsRegistrats.size());
        }
    }

    /** Genera un ID únic de transferència de forma segura per a fils */
    public int generarTransferId() {
        return transferCounter.getAndIncrement();
    }

    /** Permet registrar qui ha demanat un fitxer per respondre-li més tard */
    public void registrarTransferencia(int transferId, ClientHandler handlerSolLicitant) {
        transferenciesActives.put(transferId, handlerSolLicitant);
    }

    /** Permet recuperar el ClientHandler sol·licitant o el client origen */
    public ClientHandler getClientHandler(String nickname) {
        return clientsRegistrats.get(nickname);
    }

    public ClientHandler getSolLicitantPerTransferencia(int transferId) {
        return transferenciesActives.get(transferId);
    }

    /**
     * Motor de cerca que recorre tots els clients buscant coincidències.
     */
    public synchronized List<AresSearchResult> cercarFitxers(String query) {
        Map<String, AresSearchResult> mapResultats = new HashMap<>();
        String queryLower = query.toLowerCase();

        for (ClientHandler handler : clientsRegistrats.values()) {
            if (handler.getMeusFitxers() == null || handler.getMyNickname() == null) continue;

            for (AresFile file : handler.getMeusFitxers()) {
                if (query.equals("*") || file.getFilename().toLowerCase().contains(queryLower)) {
                    if (mapResultats.containsKey(file.getFilename())) {
                        mapResultats.get(file.getFilename()).getPeers().add(handler.getMyNickname());
                    } else {
                        List<String> peers = new ArrayList<>();
                        peers.add(handler.getMyNickname());
                        mapResultats.put(file.getFilename(), new AresSearchResult(file, peers));
                    }
                }
            }
        }
        return new ArrayList<>(mapResultats.values());
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Nombre d'arguments incorrecte.\n" + INIT_ERROR);
        }
        if (!args[0].equals("-p")) {
            throw new IllegalArgumentException("Paraula clau d'argument incorrecta.\n" + INIT_ERROR);
        }
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("<port> ha de ser un enter.");
        }

        Server server = new Server(port);
        server.init();
    }
}