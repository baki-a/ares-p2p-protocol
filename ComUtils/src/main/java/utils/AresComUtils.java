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
        // 1. Escriure l'Opcode (el tipus de missatge)
        getDataOutputStream().writeByte(OPCODE_CLIENT_REGISTER); 
        
        // 2. Escriure la longitud del nom del client (int32)
        write_int32(clientId.length());
        
        // 3. Escriure el text del nom del client
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
}