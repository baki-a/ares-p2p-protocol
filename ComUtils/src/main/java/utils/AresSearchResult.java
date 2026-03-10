package utils;

import java.util.List;

public class AresSearchResult {
    private AresFile file;
    private List<String> peers; // Llista d'IDs dels clients que tenen el fitxer

    public AresSearchResult(AresFile file, List<String> peers) {
        this.file = file;
        this.peers = peers;
    }

    public AresFile getFile() { return file; }
    public List<String> getPeers() { return peers; }
}