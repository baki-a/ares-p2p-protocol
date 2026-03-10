package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AresComUtils extends ComUtils {

    // Definim constants pels tipus de missatge per mantenir-ho net
    public static final byte OPCODE_CLIENT_REGISTER = 0x01; 

    public AresComUtils(InputStream inputStream, OutputStream outputStream) throws IOException {
        super(inputStream, outputStream);
    }
    
    // Constructor de còpia a partir de ComUtils
    public AresComUtils(ComUtils obj) {
        super(obj);
    }

    /**
     * MÈTODE D'ESCRIPTURA (El cridarà el Client)
     * Converteix la informació a bytes i l'envia pel socket.
     */
    public void writeClientRegister(String clientId) throws IOException {
        // Escriure l'Opcode (el tipus de missatge)
        getDataOutputStream().writeByte(OPCODE_CLIENT_REGISTER); 
        
        // Escriure la longitud del nom del client (int32)
        write_int32(clientId.length());
        
        //Escriure el text del nom del client
        write_string(clientId); 
    }

    /**
     * MÈTODE DE LECTURA (El cridarà el Servidor)
     * Llegeix els bytes que arriben del socket i els converteix en text.
     * *NOTA: Assumim que el servidor ja ha llegit l'Opcode i sap que toca llegir això.
     */
    public String readClientRegister() throws IOException {
        // 1. Llegir la longitud del nom del client
        int length = read_int32();
        
        // 2. Llegir els caràcters per aconseguir el nom
        return read_string(length);
    }
    public static final byte OPCODE_FILE_ANNOUNCE = 0x03;

    /**
     * Escriptura del missatge 0x03
     */
    public void writeFileAnnounce(java.util.List<AresFile> files) throws IOException {
        getDataOutputStream().writeByte(OPCODE_FILE_ANNOUNCE);
        write_int32(files.size()); // Nombre de fitxers
        
        for (AresFile file : files) {
            write_int32(file.getFilename().length());
            write_string(file.getFilename());
            getDataOutputStream().writeLong(file.getFileSize()); // 8 bytes
            getDataOutputStream().write(file.getFileHash(), 0, 32); // 32 bytes
        }
    }

    /**
     * Lectura del missatge 0x03
     */
    public java.util.List<AresFile> readFileAnnounce() throws IOException {
        java.util.List<AresFile> files = new java.util.ArrayList<>();
        int fileCount = read_int32();
        
        for (int i = 0; i < fileCount; i++) {
            int nameLength = read_int32();
            String name = read_string(nameLength);
            long size = getDataInputStream().readLong();
            byte[] hash = read_bytes(32);
            files.add(new AresFile(name, size, hash));
        }
        return files;
    }

    public static final byte OPCODE_CLIENT_REGISTER_RESPONSE = 0x02;

    /**
     * Escriptura del missatge 0x02 (Servidor -> Client)
     */
    public void writeClientRegisterResponse(byte status, String clientId, int chunkSize) throws IOException {
        getDataOutputStream().writeByte(OPCODE_CLIENT_REGISTER_RESPONSE);

        // 1. Codi d'estat (1 byte: 0x00 èxit, 0x01 error)
        getDataOutputStream().writeByte(status);

        // 2. ID del client (2 bytes fixos)
        String idFixed = clientId;
        if (idFixed.length() < 2) idFixed = (idFixed + "  ").substring(0, 2);
        else idFixed = idFixed.substring(0, 2);
        write_string(idFixed);

        // 3. Mida del fragment (4 bytes)
        write_int32(chunkSize);
    }

    /**
     * Lectura del missatge 0x02 (Client <- Servidor)
     * Com que hem de retornar tres valors diferents, els llegim en ordre.
     */
    public void readClientRegisterResponse(byte[] outStatus, String[] outClientId, int[] outChunkSize) throws IOException {
        outStatus[0] = getDataInputStream().readByte();
        outClientId[0] = read_string(2);
        outChunkSize[0] = read_int32();
    }

    // NOU MISSATGE: SEARCH_REQUEST (0x04)
    public static final byte OPCODE_SEARCH_REQUEST = 0x04;

    /**
     * Escriptura del missatge 0x04 (Client -> Servidor)
     */
    public void writeSearchRequest(String searchQuery) throws IOException {
        getDataOutputStream().writeByte(OPCODE_SEARCH_REQUEST);

        // Enviem la longitud de la paraula i després la paraula en si
        write_int32(searchQuery.length());
        write_string(searchQuery);
    }

    /**
     * Lectura del missatge 0x04 (Servidor <- Client)
     */
    public String readSearchRequest() throws IOException {
        // Llegim la longitud per saber quants caràcters hem d'agafar
        int length = read_int32();
        return read_string(length);
    }


    // --- NOU MISSATGE: SEARCH_RESPONSE (0x05) ---
    public static final byte OPCODE_SEARCH_RESPONSE = 0x05;

    /**
     * Escriptura del missatge 0x05 (Servidor -> Client)
     */
    public void writeSearchResponse(java.util.List<AresSearchResult> results) throws java.io.IOException {
        getDataOutputStream().writeByte(OPCODE_SEARCH_RESPONSE);

        // 1. Quants resultats únics (fitxers) hem trobat?
        write_int32(results.size());

        for (AresSearchResult result : results) {
            // 2. Dades del fitxer
            AresFile file = result.getFile();
            write_int32(file.getFilename().length());
            write_string(file.getFilename());
            getDataOutputStream().writeLong(file.getFileSize());
            getDataOutputStream().write(file.getFileHash(), 0, 32);

            // 3. Quants usuaris tenen aquest fitxer?
            java.util.List<String> peers = result.getPeers();
            write_int32(peers.size());

            // 4. Llista d'usuaris
            for (String peer : peers) {
                write_int32(peer.length());
                write_string(peer);
            }
        }
    }

    /**
     * Lectura del missatge 0x05 (Client <- Servidor)
     */
    public java.util.List<AresSearchResult> readSearchResponse() throws java.io.IOException {
        java.util.List<AresSearchResult> results = new java.util.ArrayList<>();

        // Llegim la quantitat de fitxers
        int resultCount = read_int32();

        for (int i = 0; i < resultCount; i++) {
            // Reconstruïm el fitxer
            int nameLength = read_int32();
            String name = read_string(nameLength);
            long size = getDataInputStream().readLong();
            byte[] hash = read_bytes(32);
            AresFile file = new AresFile(name, size, hash);

            // Reconstruïm la llista d'usuaris per a aquest fitxer
            int peerCount = read_int32();
            java.util.List<String> peers = new java.util.ArrayList<>();
            for (int j = 0; j < peerCount; j++) {
                int peerLen = read_int32();
                peers.add(read_string(peerLen));
            }

            // Ho ajuntem tot i ho afegim a la llista final
            results.add(new AresSearchResult(file, peers));
        }
        return results;
    }


    // --- FASE DE DESCÀRREGA: Sol·licitud i Resposta (0x06 i 0x07) ---
    public static final byte OPCODE_DOWNLOAD_REQUEST = 0x06;
    public static final byte OPCODE_DOWNLOAD_RESPONSE = 0x07;

    /**
     * Escriptura 0x06: Client demana un fitxer
     */
    public void writeDownloadRequest(String filename, String sourceClientId) throws IOException {
        getDataOutputStream().writeByte(OPCODE_DOWNLOAD_REQUEST);
        write_int32(filename.length());
        write_string(filename);

        // El document exigeix 2 bytes per al Source Client ID
        String idFixed = sourceClientId;
        if (idFixed.length() < 2) idFixed = (idFixed + "  ").substring(0, 2);
        else idFixed = idFixed.substring(0, 2);
        write_string(idFixed);
    }

    /**
     * Lectura 0x06
     */
    public void readDownloadRequest(String[] outFilename, String[] outSourceClientId) throws IOException {
        int filenameLength = read_int32();
        outFilename[0] = read_string(filenameLength);
        outSourceClientId[0] = read_string(2); // 2 bytes fixos
    }

    /**
     * Escriptura 0x07: Servidor respon al client si pot començar la transferència
     */
    public void writeDownloadResponse(byte status, String filename, long fileSize, String sourceClientId, int transferId) throws IOException {
        getDataOutputStream().writeByte(OPCODE_DOWNLOAD_RESPONSE);
        getDataOutputStream().writeByte(status); // 0x00 OK, 0x01 Not Found, 0x02 Unavailable

        write_int32(filename.length());
        if (filename.length() > 0) {
            write_string(filename);
        }

        getDataOutputStream().writeLong(fileSize);

        // 2 bytes per l'ID de l'origen
        String idFixed = sourceClientId;
        if (idFixed.length() < 2) idFixed = (idFixed + "  ").substring(0, 2);
        else idFixed = idFixed.substring(0, 2);
        write_string(idFixed);

        write_int32(transferId);
    }

    /**
     * Lectura 0x07
     */
    public void readDownloadResponse(byte[] outStatus, String[] outFilename, long[] outFileSize, String[] outSourceClientId, int[] outTransferId) throws IOException {
        outStatus[0] = getDataInputStream().readByte();

        int filenameLength = read_int32();
        if (filenameLength > 0) {
            outFilename[0] = read_string(filenameLength);
        } else {
            outFilename[0] = "";
        }

        outFileSize[0] = getDataInputStream().readLong();
        outSourceClientId[0] = read_string(2);
        outTransferId[0] = read_int32();
    }


    // FASE DE DESCÀRREGA: Peticions al Client Origen (0x08 i 0x09) ---
    public static final byte OPCODE_SERVER_FILE_REQUEST = 0x08;
    public static final byte OPCODE_SERVER_FILE_RESPONSE = 0x09;

    /**
     * Escriptura 0x08: Servidor demana fitxer al client origen
     */
    public void writeServerFileRequest(String filename, int transferId, String requestingClientId) throws IOException {
        getDataOutputStream().writeByte(OPCODE_SERVER_FILE_REQUEST);

        write_int32(filename.length());
        write_string(filename);
        write_int32(transferId);

        // 2 bytes per al Requesting Client ID
        String idFixed = requestingClientId;
        if (idFixed.length() < 2) idFixed = (idFixed + "  ").substring(0, 2);
        else idFixed = idFixed.substring(0, 2);
        write_string(idFixed);
    }

    /**
     * Lectura 0x08
     */
    public void readServerFileRequest(String[] outFilename, int[] outTransferId, String[] outRequestingClientId) throws IOException {
        int len = read_int32();
        outFilename[0] = read_string(len);
        outTransferId[0] = read_int32();
        outRequestingClientId[0] = read_string(2);
    }

    /**
     * Escriptura 0x09: Client origen respon al servidor
     */
    public void writeServerFileResponse(byte status, int transferId, String filename, long fileSize, byte[] fileHash) throws IOException {
        getDataOutputStream().writeByte(OPCODE_SERVER_FILE_RESPONSE);
        getDataOutputStream().writeByte(status);
        write_int32(transferId);

        write_int32(filename.length());
        if (filename.length() > 0) {
            write_string(filename);
        }

        getDataOutputStream().writeLong(fileSize);

        // El document diu que si l'status no és 0x00, el hash són tots zeros
        if (fileHash != null && fileHash.length == 32) {
            getDataOutputStream().write(fileHash, 0, 32);
        } else {
            getDataOutputStream().write(new byte[32], 0, 32);
        }
    }

    /**
     * Lectura 0x09
     */
    public void readServerFileResponse(byte[] outStatus, int[] outTransferId, String[] outFilename, long[] outFileSize, byte[][] outFileHash) throws IOException {
        outStatus[0] = getDataInputStream().readByte();
        outTransferId[0] = read_int32();

        int len = read_int32();
        if (len > 0) {
            outFilename[0] = read_string(len);
        } else {
            outFilename[0] = "";
        }

        outFileSize[0] = getDataInputStream().readLong();
        outFileHash[0] = read_bytes(32);
    }
}