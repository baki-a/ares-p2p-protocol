import p1.server.Server;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import utils.AresComUtils;

public class ClientHandlerTest {

    @Test
    public void test_servidor_accepta_registre_client() throws Exception {
        // 1. Arranquem el servidor en un fil secundari perquè no ens bloquegi el test
        int portTest = 8181;
        Server servidorTest = new Server(portTest);
        Thread filServidor = new Thread(() -> servidorTest.init());
        filServidor.start();

        // Li donem 100 mil·lisegons perquè tingui temps d'obrir el port
        Thread.sleep(100);

        // 2. Creem un "Client" simulat que es connecta al nostre servidor
        try (Socket clientSocket = new Socket("localhost", portTest)) {
            AresComUtils clientUtils = new AresComUtils(clientSocket.getInputStream(), clientSocket.getOutputStream());

            // 3. Enviem el missatge 0x01 (CLIENT_REGISTER) tal com marca el protocol
            String nicknameDeProva = "TestUser";
            clientUtils.writeClientRegister(nicknameDeProva);

            // 4. Llegim la resposta del servidor.
            // Si la màquina d'estats funciona (ESPERANT -> REGISTRANT_CLIENT), ens ha de respondre un 0x02.
            byte opcodeResposta = clientUtils.readOpcode();
            assertEquals("El servidor hauria de respondre amb OPCODE 0x02", AresComUtils.OPCODE_CLIENT_REGISTER_RESPONSE, opcodeResposta);

            // 5. Llegim el cos de la resposta per validar que l'status és 0x00 (Èxit)
            byte[] status = new byte[1];
            String[] clientId = new String[1];
            int[] chunkSize = new int[1];
            clientUtils.readClientRegisterResponse(status, clientId, chunkSize);

            assertEquals("L'estat del registre hauria de ser èxit (0x00)", (byte) 0x00, status[0]);
            assertNotNull("El servidor hauria d'haver generat un ID", clientId[0]);
            assertEquals("El tamany del chunk per defecte hauria de ser 8192", 8192, chunkSize[0]);

        } finally {
            // Netegem aturant el fil del servidor
            filServidor.interrupt();
        }
    }
}