package fr.umontpellier.model;

import java.io.Serializable;
import java.util.List;

public class Backup implements Serializable {
    private String directoryPath;
    private List<String> fileExtensions;

    public Backup(String directoryPath, List<String> fileExtensions) {
        this.directoryPath = directoryPath;
        this.fileExtensions = fileExtensions;
    }

    // Getters and Setters
    public String getDirectoryPath() {
        return directoryPath;
    }

    public List<String> getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }
}
