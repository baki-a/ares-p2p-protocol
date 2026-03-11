    package utils;

    public class AresFile {
        private String filename;
        private long fileSize;
        private byte[] fileHash;

        public AresFile(String filename, long fileSize, byte[] fileHash) {
            this.filename = filename;
            this.fileSize = fileSize;
            this.fileHash = fileHash;
        }

        public String getFilename() { return filename; }
        public long getFileSize() { return fileSize; }
        public byte[] getFileHash() { return fileHash; }
    }