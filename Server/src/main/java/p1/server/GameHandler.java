package p1.server;

import utils.ComUtils;

/**
 * Handles the game logic for a connected client.
 * This class is responsible for managing the communication and 
 * executing the game protocol from the server's side.
 */
public class GameHandler {

    /** Communication utility for handling input and output streams. */
    ComUtils comutils;

    /**
     * Initializes the game handler with a communication utility.
     * 
     * @param comutils The communication utility for interacting with the client.
     */
    public GameHandler(ComUtils comutils) {
        this.comutils = comutils;
    }

    /**
     * Starts the game handler.
     * This method will be responsible for initializing and managing the game session.
     */
    public void start() {
        System.out.println("GameHandler started");
    }
    
    /*
     * TO DO:
     * Implement the following methods according to the game protocol:
     * 
     * - run(): Handles the execution loop of the game session.
     * - init(): Initializes game settings and prepares the game session.
     * - play(): Manages the game flow and interactions.
     */
    
    
}
