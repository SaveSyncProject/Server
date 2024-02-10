package fr.umontpellier.model.request.backup;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.umontpellier.logging.LoggingService;
import fr.umontpellier.model.request.Request;

public class ReadBackupRequest extends Request{

    private final ObjectOutputStream objectOut;
    private final String username;

    public ReadBackupRequest(ObjectOutputStream objectOut, String username) {
        this.objectOut = objectOut;
        this.username = username;
    }

    public void execute() {
        Path userDir = Paths.get("./users", username);

        try (Stream<Path> stream = Files.list(userDir)) {
            objectOut.writeObject(stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith("_backup"))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            LoggingService.getLogger().log("Error while listing backups: " + e.getMessage());
        }
    }
}