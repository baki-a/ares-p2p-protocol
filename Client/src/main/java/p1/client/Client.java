package p1.client;

import utils.AresComUtils;
import utils.AresFile;
import utils.AresSearchResult;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class Client {

    private String host;
    private int port;
    private String nickname;

    private Socket socket;
    private AresComUtils aresComUtils;
    private FileScanner fileScanner;

    public Client(String host, int port, String nickname) {
        this.host = host;
        this.port = port;
        this.nickname = nickname;
        this.fileScanner = new FileScanner("public/" + nickname);
    }

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.nickname = "TestUser";
        try {
            this.socket = new Socket(host, port);
            this.aresComUtils = new AresComUtils(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error de connexió al test: " + e.getMessage());
        }
    }

    public Socket getSocket() { return this.socket; }
    public utils.ComUtils getComutils() { return this.aresComUtils; }

    public void start() {
        try {
            System.out.println("Connectant al servidor " + host + ":" + port + "...");
            socket = new Socket(host, port);
            aresComUtils = new AresComUtils(socket.getInputStream(), socket.getOutputStream());
            System.out.println("Connexió establerta!");

            aresComUtils.writeClientRegister(nickname);

            byte opcodeRes = aresComUtils.readOpcode();
            if (opcodeRes == AresComUtils.OPCODE_CLIENT_REGISTER_RESPONSE) {
                byte[] status = new byte[1];
                String[] assignedId = new String[1];
                int[] chunkSize = new int[1];
                aresComUtils.readClientRegisterResponse(status, assignedId, chunkSize);

                if (status[0] != 0x00) {
                    System.err.println("Error: El servidor ha rebutjat el registre.");
                    return;
                }
                System.out.println("Registre completat amb èxit. El teu ID és: " + assignedId[0]);
            }

            System.out.println("Escanejant directori local...");
            enviarAnunciFitxers();

            // NOU: Arranquem el fil que escoltarà els missatges en segon pla
            new Thread(new ServerListener()).start();

            iniciarConsola();

        } catch (IOException e) {
            System.err.println("Error de xarxa: " + e.getMessage());
        } finally {
            tancarConnexio();
        }
    }

    private void enviarAnunciFitxers() throws IOException {
        List<AresFile> files = fileScanner.scanFiles();
        aresComUtils.writeFileAnnounce(files);

        byte opcode = aresComUtils.readOpcode();
        if (opcode == AresComUtils.OPCODE_ACK) {
            System.out.println("S'han anunciat " + files.size() + " fitxers correctament.");
        }
    }

    private void iniciarConsola() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("\n--- ARES GALAXY P2P ---");
        System.out.println("Comandes disponibles: search <text>, download <fitxer>, list, refresh, quit");

        while (running && !socket.isClosed()) {
            try {
                // Petit retard visual perquè els missatges del Listener no trepitgin l'indicador
                Thread.sleep(100);
                System.out.print("\n[" + nickname + "@ares]> ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                String[] parts = input.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String argument = parts.length > 1 ? parts[1] : "";

                switch (command) {
                    case "search":
                        if (argument.isEmpty()) {
                            System.out.println("Ús: search <nom_del_fitxer> o search *");
                        } else {
                            System.out.println("Enviant petició de cerca...");
                            aresComUtils.writeSearchRequest(argument); // El Listener rebrà la resposta!
                        }
                        break;

                    case "download":
                        if (argument.isEmpty()) {
                            System.out.println("Ús: download <nom_del_fitxer>");
                        } else {
                            System.out.println("Sol·licitant descàrrega...");
                            aresComUtils.writeDownloadRequest(argument, "00"); // El Listener rebrà la resposta!
                        }
                        break;

                    case "list":
                        List<AresFile> locals = fileScanner.scanFiles();
                        System.out.println("Tens " + locals.size() + " fitxers locals:");
                        for (AresFile f : locals) {
                            System.out.println(" - " + f.getFilename() + " (" + f.getFileSize() + " bytes)");
                        }
                        break;

                    case "quit":
                        System.out.println("Desconnectant...");
                        running = false;
                        tancarConnexio();
                        break;

                    default:
                        System.out.println("Comanda no reconeguda.");
                }
            } catch (Exception e) {
                System.err.println("Error a la consola: " + e.getMessage());
            }
        }
    }

    /**
     * El Listener que s'executa en segon pla vigilant els missatges entrants.
     */
    private class ServerListener implements Runnable {
        // Variables per guardar l'estat de la descàrrega
        private java.io.FileOutputStream fos = null;
        private String downloadingFilename = "";
        private java.util.Map<Integer, String> uploadsActius = new java.util.HashMap<>(); //Memòria per a l'Origen
        @Override
        public void run() {
            try {
                // Creem la carpeta de descàrregues
                new java.io.File("downloads/" + nickname).mkdirs();

                while (!socket.isClosed()) {
                    byte opcode = aresComUtils.readOpcode();

                    if (opcode == AresComUtils.OPCODE_SEARCH_RESPONSE) {
                        List<AresSearchResult> resultats = aresComUtils.readSearchResponse();
                        System.out.println("\n[XARXA] S'han trobat " + resultats.size() + " fitxers:");
                        for (AresSearchResult res : resultats) {
                            System.out.println(" 📄 " + res.getFile().getFilename() + " (" + res.getFile().getFileSize() + " bytes)");
                        }

                    } else if (opcode == AresComUtils.OPCODE_DOWNLOAD_RESPONSE) {
                        byte[] status = new byte[1]; String[] fname = new String[1];
                        long[] fsize = new long[1]; String[] srcId = new String[1]; int[] tId = new int[1];
                        aresComUtils.readDownloadResponse(status, fname, fsize, srcId, tId);

                        if (status[0] == 0x00) {
                            System.out.println("\n[XARXA] ✅ Transferència " + tId[0] + " acceptada. Descarregant...");
                            downloadingFilename = fname[0];
                            fos = new java.io.FileOutputStream("downloads/" + nickname + "/" + downloadingFilename);
                            // Demanem el primer fragment (Chunk 0)
                            aresComUtils.writeChunkRequest(tId[0], 0);
                        } else {
                            System.out.println("\n[XARXA] ❌ Descàrrega denegada.");
                        }

                    } else if (opcode == AresComUtils.OPCODE_SERVER_FILE_REQUEST) {
                        String[] reqFilename = new String[1]; int[] transferId = new int[1]; String[] reqClientId = new String[1];
                        aresComUtils.readServerFileRequest(reqFilename, transferId, reqClientId);

                        boolean trobat = new java.io.File("public/" + nickname + "/" + reqFilename[0]).exists();
                        if (trobat) {
                            System.out.println("\n[ALERTA] Ens demanen '" + reqFilename[0] + "'. Acceptant (0x09)...");

                            // Recordem quin fitxer estem pujant!
                            uploadsActius.put(transferId[0], reqFilename[0]);

                            aresComUtils.writeServerFileResponse((byte)0x00, transferId[0], reqFilename[0], 1024, new byte[32]);
                        } else {
                            aresComUtils.writeServerFileResponse((byte)0x01, transferId[0], "", 0, new byte[32]);
                        }

                        // LÒGICA DE FRAGMENTS (CHUNKS)
                    } else if (opcode == AresComUtils.OPCODE_CHUNK_REQUEST) {
                        int[] tId = new int[1]; int[] chunkNum = new int[1];
                        aresComUtils.readChunkRequest(tId, chunkNum);

                        // NOU: Recuperem el nom del fitxer real de la memòria
                        String nomFitxer = uploadsActius.get(tId[0]);

                        if (nomFitxer != null) {
                            java.io.File f = new java.io.File("public/" + nickname + "/" + nomFitxer);
                            if (f.exists()) {
                                try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                                    byte[] buffer = new byte[8192];
                                    fis.skip((long) chunkNum[0] * 8192);
                                    int bytesRead = fis.read(buffer);

                                    if (bytesRead > 0) {
                                        byte[] data = java.util.Arrays.copyOf(buffer, bytesRead);
                                        aresComUtils.writeChunkResponse(tId[0], chunkNum[0], bytesRead, data);
                                    } else {
                                        aresComUtils.writeChunkResponse(tId[0], chunkNum[0], 0, new byte[0]);
                                    }
                                }
                            }
                        }

                    } else if (opcode == AresComUtils.OPCODE_CHUNK_RESPONSE) {
                        // Som el SOL·LICITANT. Ens arriba un tros de fitxer
                        int[] tId = new int[1]; int[] chunkNum = new int[1]; int[] chunkSize = new int[1]; byte[][] chunkData = new byte[1][];
                        aresComUtils.readChunkResponse(tId, chunkNum, chunkSize, chunkData);

                        if (chunkSize[0] > 0) {
                            System.out.println("   └─ Rebut fragment " + chunkNum[0] + " (" + chunkSize[0] + " bytes)");
                            fos.write(chunkData[0]);
                            // Demanem el següent
                            aresComUtils.writeChunkRequest(tId[0], chunkNum[0] + 1);
                        } else {
                            System.out.println("\n[XARXA] 💾 Descàrrega de '" + downloadingFilename + "' COMPLETADA!");
                            fos.close();
                            // Finalitzem enviant el HASH_VERIFY
                            aresComUtils.writeHashVerify(new byte[32]);
                        }
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) System.err.println("\nConnexió amb el servidor perduda.");
            }
        }
    }

    private void tancarConnexio() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;
        String nickname = "local";

        if (args.length >= 3) {
            host = args[0];
            port = Integer.parseInt(args[1]);
            nickname = args[2];
        }

        Client client = new Client(host, port, nickname);
        client.start();
    }
}