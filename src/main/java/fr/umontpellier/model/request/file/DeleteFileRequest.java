package fr.umontpellier.model.request.file;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import fr.umontpellier.model.request.Request;

public class DeleteFileRequest extends Request{

    private final String username;
    private final List<String> files;
    private final ObjectOutputStream objectOut;

    public DeleteFileRequest(String username, List<String> files, ObjectOutputStream objectOut) {
        this.username = username;
        this.files = files;
        this.objectOut = objectOut;
    }

    @Override
    public void execute() {
        try{
            Path userDir = Paths.get("./users", username);
            boolean deleteFilesSuccessful = files.stream()
                .map(userDir::resolve)
                .allMatch(this::deleteFile);
            objectOut.writeObject(deleteFilesSuccessful ? "SUCCESS" : "ERROR");
            objectOut.flush();
        } catch (IOException e) {
            System.err.println("Error while deleting files: " + e.getMessage());
        }
    }

    /**
     * Supprime un fichier
     *
     * @param path le chemin du fichier à supprimer
     * @return true si le fichier a été supprimé, false sinon
     */
    private boolean deleteFile(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Error while deleting file: " + path + " - " + e.getMessage());
            return false;
        }
    }
    
}