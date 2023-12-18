package fr.umontpellier.model.request;

import fr.umontpellier.model.Backup;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SaveRequest implements Request{
    /*
     * Méthode pour gérer la sauvegarde des fichiers
     */
    public void handleBackup(Backup backupDetails, String username) {
        System.out.println("Démarrage de la sauvegarde pour l'utilisateur : " + username);
        String directoryPath = backupDetails.getDirectoryPath();
        List<String> extensions = backupDetails.getFileExtensions();

        File backupRoot = new File("./users/" + username);
        if (!backupRoot.exists()) {
            backupRoot.mkdirs();
        }

        // Créer le fichier zip de sauvegarde
        // On  récupère la date est l'heure de la sauvegarde
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm"));
        String fileName = date + "-" + time + "_backup.zip";
        File zipFile = new File(backupRoot, fileName);
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Set<Path> addedDirs = new HashSet<>();

            Path startPath = Paths.get(directoryPath);
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Créer une entrée zip pour chaque dossier
                    Path relDir = startPath.relativize(dir);
                    if (!relDir.toString().isEmpty() && addedDirs.add(relDir)) {
                        ZipEntry dirEntry = new ZipEntry(relDir.toString() + "/");
                        zipOut.putNextEntry(dirEntry);
                        zipOut.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String fileName = filePath.getFileName().toString();
                    if (extensions.stream().anyMatch(fileName::endsWith)) {
                        // Créer une entrée zip pour le fichier
                        String zipEntryName = startPath.relativize(filePath).toString();
                        zipOut.putNextEntry(new ZipEntry(zipEntryName));

                        // Lire le contenu du fichier et l'écrire dans le zip
                        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
                            byte[] buffer = new byte[4096];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, length);
                            }
                        }
                        zipOut.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier zip: " + e.getMessage());
        }
    }
}
