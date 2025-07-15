import java.io.File;

public class FileData {
    private String filename;
    private long fileSize;
    private File fileBytes;
    
    public FileData(String filename, long fileSize, File fileBytes) {
        this.filename = filename;
        this.fileSize = fileSize;
        this.fileBytes = fileBytes;
    }

    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    public File getFileBytes() {
        return fileBytes;
    }
    public void setFileBytes(File fileBytes) {
        this.fileBytes = fileBytes;
    }
}
