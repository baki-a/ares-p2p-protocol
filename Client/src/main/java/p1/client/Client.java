package p1.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import utils.ComUtils;

/**
 * Client class to establish a connection to a server and manage communication.
 */
public class Client {

    public static final String INIT_ERROR = "Client should be initialized with -h <host> -p <port>";
    Socket socket;
    String host;
    int port;
    ComUtils comutils;

    /**
     * Constructs a Client instance and initializes the connection.
     * 
     * @param host The hostname or IP address of the server.
     * @param port The port number to connect to.
     */
    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.socket = setConnection();
        this.comutils = getComutils();
    }

    /**
     * Returns the ComUtils instance for communication.
     * If not initialized, it initializes it first.
     * 
     * @return The ComUtils instance.
     */
    public ComUtils getComutils() {
        if (comutils == null) {
            try {
                comutils = new ComUtils(socket.getInputStream(), socket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException("I/O Error when creating the ComUtils:\n"+e.getMessage());
            }
        }    
        return comutils;
    }

    /**
     * Establishes a connection to the specified server.
     * 
     * @return The established socket connection.
     */
    public Socket setConnection() {
            
        Socket connection = null;
        if (this.socket == null) {
            try {
                connection = new Socket(this.host, this.port);
                System.out.println("Client connected to server");
            } catch (IllegalArgumentException e) {
               throw new IllegalArgumentException("Proxy has invalid type or null:\n"+e.getMessage());
            } catch (SecurityException e) {
                throw new SecurityException("Connection to the proxy denied for security reasons:\n"+e.getMessage());
            } catch (UnknownHostException e) {
                throw new RuntimeException("Host is Unknown:\n"+e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException("I/O Error when creating the socket:\n"+e.getMessage()+". Is the host listening?");
            }
        }
        return connection;
    }
    
    /**
     * Returns the current socket connection.
     * 
     * @return The socket instance.
     */
    public Socket getSocket() {
        return this.socket;
    }    

    /**
     * Main method to initialize the client with command-line arguments.
     * 
     * @param args Command-line arguments specifying host and port.
     */
    public static void main(String[] args) {

        if (args.length != 4) {
            throw new IllegalArgumentException("Wrong amount of arguments.\n"+INIT_ERROR);
        }

        if (!args[0].equals("-h") || !args[2].equals("-p")) {
            throw new IllegalArgumentException("Wrong argument keywords.\n"+INIT_ERROR);
        }
        int port;
        try {
            port = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("<port> should be an Integer.");
        }
        String host = args[1];
        Client client = new Client(host, port);
           
        // TO DO: Create a new GameClient class and call the game execution.
    }
}
