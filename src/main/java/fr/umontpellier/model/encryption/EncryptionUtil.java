package fr.umontpellier.model.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

public class EncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEYSIZE = 128;
    private static final byte[] IV = new byte[16];

    /**
     * Génère une clé AES
     *
     * @return la clé générée
     * @throws Exception
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(KEYSIZE, new SecureRandom());
        return keyGenerator.generateKey();
    }

    /**
     * Chiffre le contenu d'un dossier
     *
     * @param key        la clé de chiffrement
     * @param folder     le dossier à chiffrer
     * @param encrypt    true pour chiffrer, false pour déchiffrer
     * @throws Exception
     */
    public static void processFolder(SecretKey key, File folder, boolean encrypt) throws Exception {
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Le paramètre doit être un dossier");
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processFolder(key, file, encrypt);
                } else {
                    File tempFile = new File(file.getAbsolutePath() + ".tmp");
                    encryptFile(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, file, tempFile);
                    if (!file.delete()) {
                        throw new Exception("Impossible de supprimer le fichier original: " + file.getName());
                    }
                    if (!tempFile.renameTo(file)) {
                        throw new Exception("Impossible de renommer le fichier temporaire: " + tempFile.getName());
                    }
                }
            }
        }
    }

    /**
     * Chiffre un fichier
     *
     * @param cipherMode le mode de chiffrement
     * @param key la clé de chiffrement
     * @param inputFile le fichier à chiffrer
     * @param outputFile le fichier de sortie
     * @throws Exception
     */
    private static void encryptFile(int cipherMode, SecretKey key, File inputFile, File outputFile) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
        cipher.init(cipherMode, key, ivParameterSpec);

        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    outputStream.write(output);
                }
            }
            byte[] outputBytes = cipher.doFinal();
            if (outputBytes != null) {
                outputStream.write(outputBytes);
            }
        }
    }

    /**
     * Déchiffre un fichier
     *
     * @param key la clé de chiffrement
     * @param inputFile le fichier à déchiffrer
     * @param outputFile le fichier de sortie
     * @throws Exception
     */
    public static void decryptFile(SecretKey key, File inputFile, File outputFile) throws Exception {
        encryptFile(Cipher.DECRYPT_MODE, key, inputFile, outputFile);
    }
}
