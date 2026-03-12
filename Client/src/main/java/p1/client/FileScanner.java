package p1.client;

import utils.AresFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aquesta classe s'encarrega d'escanejar un directori local,
 * llegir els fitxers, calcular-ne el tamany i el Hash SHA-256.
 */
public class FileScanner {

    private String directoryPath;

    public FileScanner(String directoryPath) {
        this.directoryPath = directoryPath;
        assegurarDirectori();
    }

    /**
     * Comprova si la carpeta existeix. Si no, la crea perquè el programa no peti.
     */
    private void assegurarDirectori() {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("S'ha creat el directori de publicació: " + dir.getAbsolutePath());
        }
    }

    /**
     * Llegeix tots els fitxers de la carpeta i retorna una llista d'AresFile.
     */
    public List<AresFile> scanFiles() {
        List<AresFile> filesList = new ArrayList<>();
        File dir = new File(directoryPath);

        File[] files = dir.listFiles();
        if (files == null) return filesList; // Directori buit o error

        for (File file : files) {
            if (file.isFile()) {
                try {
                    byte[] hash = calcularSHA256(file);
                    long size = file.length();
                    // Afegim el fitxer a la llista amb el seu hash real
                    filesList.add(new AresFile(file.getName(), size, hash));
                } catch (Exception e) {
                    System.err.println("Error al llegir el fitxer " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        return filesList;
    }

    /**
     * Calcula el Hash SHA-256 d'un fitxer llegint-lo a trossos perquè no col·lapsi la RAM.
     */
    private byte[] calcularSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        return digest.digest(); // Retorna exactament 32 bytes
    }
}