package utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AresComUtilsTest {

    @Test
    public void test_client_register() {
        File file = new File("test_register.bin");
        try {
            file.createNewFile();
            AresComUtils aresComUtils = new AresComUtils(new FileInputStream(file), new FileOutputStream(file));

            String expectedClientId = "EstudiantB10"; 
            
            // 1. Escrivim al fitxer simulant l'enviament
            aresComUtils.writeClientRegister(expectedClientId);
            
            // 2. Llegim del fitxer simulant la recepció (primer l'Opcode)
            byte receivedOpcode = aresComUtils.getDataInputStream().readByte();
            assertEquals(AresComUtils.OPCODE_CLIENT_REGISTER, receivedOpcode);

            // 3. Llegim la resta i comprovem que coincideix
            String actualClientId = aresComUtils.readClientRegister();
            assertEquals(expectedClientId, actualClientId);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            file.delete(); 
        }
    }
}