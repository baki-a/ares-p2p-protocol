package p1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Classe principal del servidor que gestiona les connexions entrants
 * i crea instàncies de ClientHandler per a cada client connectat.
 */
public class Server {
    public static final String INIT_ERROR = "Server should be initialized with -p <port>";

    private ServerSocket ss;
    private int port;

    public Server(int port) {
        this.port = port;
        setConnection();
    }

    /**
     * Obre el port del servidor per escoltar connexions.
     */
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

    /**
     * Bucle infinit que accepta clients i els deriva a un fil independent.
     */
    public void init() {
        while(true) {
            try {
                // Acceptem un client (es queda bloquejat aquí fins que algú es connecta)
                Socket clientSocket = ss.accept();
                System.out.println("Nova connexió acceptada: " + clientSocket.getInetAddress());

                // Creem el ClientHandler (Sessió 1 i 2) i l'arranquem en un fil nou
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();

            } catch (IOException e) {
                System.err.println("Error a l'acceptar un client: " + e.getMessage());
            }
        }
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