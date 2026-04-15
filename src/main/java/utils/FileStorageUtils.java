package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileStorageUtils {

    // 📁 Dossier racine des uploads — dans le dossier du projet
    private static final String BASE_DIR = System.getProperty("user.dir") + "/uploads/";

    /**
     * Copie un fichier dans uploads/{subFolder}/
     * @return le chemin relatif sauvegardé en BDD (ex: "profiles/uuid.jpg")
     *         ou null si aucun fichier sélectionné
     */
    public static String save(File file, String subFolder) throws IOException {
        if (file == null) return null;

        // Créer le dossier s'il n'existe pas
        Path dir = Paths.get(BASE_DIR + subFolder);
        Files.createDirectories(dir);

        // Générer un nom unique pour éviter les conflits
        String extension = getExtension(file.getName());
        String uniqueName = UUID.randomUUID() + "." + extension;

        Path destination = dir.resolve(uniqueName);
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        // On sauvegarde uniquement le chemin relatif en BDD
        return subFolder + "/" + uniqueName;
    }

    /**
     * Retourne le chemin absolu d'un fichier stocké
     * (utile pour afficher une image depuis la BDD)
     */
    public static String getAbsolutePath(String relativePath) {
        if (relativePath == null) return null;
        return BASE_DIR + relativePath;
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot != -1) ? filename.substring(dot + 1).toLowerCase() : "bin";
    }
}