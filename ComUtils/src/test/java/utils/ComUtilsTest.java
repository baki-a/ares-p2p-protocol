package utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;


public class ComUtilsTest {

    @Test
    public void example_test() {
        File file = new File("test");
        try {
            file.createNewFile();
            ComUtils comUtils = new ComUtils(new FileInputStream(file), new FileOutputStream(file));
            comUtils.write_int32(2);
            int readedInt = comUtils.read_int32();

            assertEquals(2, readedInt);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test_download_request_and_response() throws IOException {
        File file = new File("test_download.bin");
        try {
            file.createNewFile();
            AresComUtils aresComUtils = new AresComUtils(new FileInputStream(file), new FileOutputStream(file));

            // --- PROVA 0x06 ---
            String expectedFilename = "apunts.pdf";
            String expectedSourceId = "U1";
            aresComUtils.writeDownloadRequest(expectedFilename, expectedSourceId);

            assertEquals(AresComUtils.OPCODE_DOWNLOAD_REQUEST, aresComUtils.getDataInputStream().readByte());
            String[] actualFilename = new String[1];
            String[] actualSourceId = new String[1];
            aresComUtils.readDownloadRequest(actualFilename, actualSourceId);
            assertEquals(expectedFilename, actualFilename[0]);
            assertEquals(expectedSourceId, actualSourceId[0]);

            // --- PROVA 0x07 ---
            byte expectedStatus = 0x00;
            long expectedSize = 1048576L;
            int expectedTransferId = 9999;

            aresComUtils.writeDownloadResponse(expectedStatus, expectedFilename, expectedSize, expectedSourceId, expectedTransferId);

            assertEquals(AresComUtils.OPCODE_DOWNLOAD_RESPONSE, aresComUtils.getDataInputStream().readByte());

            byte[] actualStatus = new byte[1];
            long[] actualSize = new long[1];
            int[] actualTransferId = new int[1];
            aresComUtils.readDownloadResponse(actualStatus, actualFilename, actualSize, actualSourceId, actualTransferId);

            assertEquals(expectedStatus, actualStatus[0]);
            assertEquals(expectedFilename, actualFilename[0]);
            assertEquals(expectedSize, actualSize[0]);
            assertEquals(expectedSourceId, actualSourceId[0]);
            assertEquals(expectedTransferId, actualTransferId[0]);

        } finally {
            file.delete();
        }
    }
}
