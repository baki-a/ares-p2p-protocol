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
}