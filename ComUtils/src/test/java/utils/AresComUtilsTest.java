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


    @Test
    public void test_file_announce() throws IOException {
        File file = new File("test_announce.bin");
        try {
            file.createNewFile();
            AresComUtils aresComUtils = new AresComUtils(new FileInputStream(file), new FileOutputStream(file));

            // Simulem un hash de 32 bytes
            byte[] fakeHash = new byte[32];
            java.util.List<AresFile> expectedFiles = new java.util.ArrayList<>();
            expectedFiles.add(new AresFile("peli.mp4", 1024L * 1024L, fakeHash));

            // Enviem i llegim
            aresComUtils.writeFileAnnounce(expectedFiles);
            byte opcode = aresComUtils.getDataInputStream().readByte();
            java.util.List<AresFile> actualFiles = aresComUtils.readFileAnnounce();

            assertEquals(AresComUtils.OPCODE_FILE_ANNOUNCE, opcode);
            assertEquals(expectedFiles.size(), actualFiles.size());
            assertEquals(expectedFiles.get(0).getFilename(), actualFiles.get(0).getFilename());
            assertEquals(expectedFiles.get(0).getFileSize(), actualFiles.get(0).getFileSize());
        } finally {
            file.delete();
        }
    }
    @Test
    public void test_client_register_response() throws IOException {
        File file = new File("test_response.bin");
        try {
            file.createNewFile();
            AresComUtils aresComUtils = new AresComUtils(new FileInputStream(file), new FileOutputStream(file));

            byte expectedStatus = 0x00; // Èxit
            String expectedId = "B1";
            int expectedChunkSize = 4096;

            // Enviem
            aresComUtils.writeClientRegisterResponse(expectedStatus, expectedId, expectedChunkSize);

            // Llegim
            byte receivedOpcode = aresComUtils.getDataInputStream().readByte();
            assertEquals(AresComUtils.OPCODE_CLIENT_REGISTER_RESPONSE, receivedOpcode);

            byte[] actualStatus = new byte[1];
            String[] actualId = new String[1];
            int[] actualChunkSize = new int[1];

            aresComUtils.readClientRegisterResponse(actualStatus, actualId, actualChunkSize);

            // Comprovem
            assertEquals(expectedStatus, actualStatus[0]);
            assertEquals(expectedId, actualId[0]);
            assertEquals(expectedChunkSize, actualChunkSize[0]);

        } finally {
            file.delete();
        }
    }
}