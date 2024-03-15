package fr.umontpellier.model.request.file;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.umontpellier.model.request.Request;

public class ReadFileRequest extends Request{

    private final ObjectOutputStream objectOut;
    private final String username;
    private final String backupName;

    public ReadFileRequest(ObjectOutputStream objectOut, String username, String backupName) {
        this.objectOut = objectOut;
        this.username = username;
        this.backupName = backupName;
    }

    @Override
    public void execute() {
        Path backupDir = Paths.get("./users", username, backupName);

        if (Files.exists(backupDir) && Files.isDirectory(backupDir)) {
            try (Stream<Path> stream = Files.walk(backupDir)) {
                objectOut.writeObject(stream
                        .filter(Files::isRegularFile)
                        .map(backupDir::relativize)
                        .map(Path::toString)
                        .collect(Collectors.toList()));
                objectOut.flush();

            } catch (IOException e) {
                System.err.println("Error while listing files: " + e.getMessage());
            }
        }
    }
}