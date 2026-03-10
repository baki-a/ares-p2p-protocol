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

    @Test
    public void test_search_request() throws IOException {
        File file = new File("test_search.bin");
        try {
            file.createNewFile();
            AresComUtils aresComUtils = new AresComUtils(new FileInputStream(file), new FileOutputStream(file));

            String expectedQuery = "apunts_distribuit";

            // 1. Enviem
            aresComUtils.writeSearchRequest(expectedQuery);

            // 2. Llegim l'opcode
            byte opcode = aresComUtils.getDataInputStream().readByte();
            assertEquals(AresComUtils.OPCODE_SEARCH_REQUEST, opcode);

            // 3. Llegim la dada
            String actualQuery = aresComUtils.readSearchRequest();
            assertEquals(expectedQuery, actualQuery);

        } finally {
            file.delete();
        }
    }

    @Test
    public void test_search_response() throws IOException {
        File file = new File("test_search_response.bin");
        try {
            file.createNewFile();
            AresComUtils aresComUtils = new AresComUtils(new FileInputStream(file), new FileOutputStream(file));

            // 1. Preparem les dades falses
            byte[] fakeHash = new byte[32]; // Hash buit per provar
            fakeHash[0] = 1; // Posem algun valor per assegurar-nos que es llegeix bé

            AresFile fakeFile = new AresFile("apunts_sd.pdf", 5000L, fakeHash);

            java.util.List<String> fakePeers = new java.util.ArrayList<>();
            fakePeers.add("UsuariA");
            fakePeers.add("UsuariB");

            AresSearchResult expectedResult = new AresSearchResult(fakeFile, fakePeers);

            java.util.List<AresSearchResult> expectedList = new java.util.ArrayList<>();
            expectedList.add(expectedResult);

            // 2. Enviem
            aresComUtils.writeSearchResponse(expectedList);

            // 3. Llegim i comprovem
            byte opcode = aresComUtils.getDataInputStream().readByte();
            assertEquals(AresComUtils.OPCODE_SEARCH_RESPONSE, opcode);

            java.util.List<AresSearchResult> actualList = aresComUtils.readSearchResponse();

            // 4. Verifiquem que tot coincideix
            assertEquals(1, actualList.size());

            AresSearchResult actualResult = actualList.get(0);
            assertEquals("apunts_sd.pdf", actualResult.getFile().getFilename());
            assertEquals(5000L, actualResult.getFile().getFileSize());
            assertEquals(1, actualResult.getFile().getFileHash()[0]); // Comprovem el primer byte del hash

            assertEquals(2, actualResult.getPeers().size());
            assertEquals("UsuariA", actualResult.getPeers().get(0));
            assertEquals("UsuariB", actualResult.getPeers().get(1));

        } finally {
            file.delete();
        }
    }


    @Test
    public void test_server_file_request_and_response() throws IOException {
        File file = new File("test_server_file.bin");
        try {
            file.createNewFile();
            AresComUtils aresComUtils = new AresComUtils(new FileInputStream(file), new FileOutputStream(file));

            // --- PROVA 0x08 ---
            String expectedFilename = "video.mp4";
            int expectedTransferId = 12345;
            String expectedReqId = "C1";

            aresComUtils.writeServerFileRequest(expectedFilename, expectedTransferId, expectedReqId);

            assertEquals(AresComUtils.OPCODE_SERVER_FILE_REQUEST, aresComUtils.getDataInputStream().readByte());

            String[] actualFilename = new String[1];
            int[] actualTransferId = new int[1];
            String[] actualReqId = new String[1];

            aresComUtils.readServerFileRequest(actualFilename, actualTransferId, actualReqId);

            assertEquals(expectedFilename, actualFilename[0]);
            assertEquals(expectedTransferId, actualTransferId[0]);
            assertEquals(expectedReqId, actualReqId[0]);

            // --- PROVA 0x09 ---
            byte expectedStatus = 0x00;
            long expectedSize = 5000000L;
            byte[] expectedHash = new byte[32];
            expectedHash[0] = 7; // Valor de prova per assegurar que no es perd

            aresComUtils.writeServerFileResponse(expectedStatus, expectedTransferId, expectedFilename, expectedSize, expectedHash);

            assertEquals(AresComUtils.OPCODE_SERVER_FILE_RESPONSE, aresComUtils.getDataInputStream().readByte());

            byte[] actualStatus = new byte[1];
            long[] actualSize = new long[1];
            byte[][] actualHash = new byte[1][];

            aresComUtils.readServerFileResponse(actualStatus, actualTransferId, actualFilename, actualSize, actualHash);

            assertEquals(expectedStatus, actualStatus[0]);
            assertEquals(expectedTransferId, actualTransferId[0]);
            assertEquals(expectedFilename, actualFilename[0]);
            assertEquals(expectedSize, actualSize[0]);
            assertEquals(expectedHash[0], actualHash[0][0]);

        } finally {
            file.delete();
        }
    }
}