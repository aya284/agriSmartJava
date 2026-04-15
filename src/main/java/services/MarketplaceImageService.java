package services;

import javafx.scene.image.Image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class MarketplaceImageService {

    public Image resolveProductImage(String rawImagePath, Class<?> resourceClass) {
        String raw = safe(rawImagePath).trim().replace("\"", "");
        if (raw.isEmpty()) {
            return loadPlaceholderImage(resourceClass);
        }

        try {
            String normalizedRaw = raw.replace('\\', '/');

            if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:")) {
                Image direct = new Image(raw, false);
                return direct.isError() ? loadPlaceholderImage(resourceClass) : direct;
            }

            if (normalizedRaw.startsWith("/uploads/") || normalizedRaw.startsWith("uploads/")) {
                String relative = normalizedRaw.startsWith("/") ? normalizedRaw.substring(1) : normalizedRaw;

                Path sharedUploads = getSharedUploadsDir();
                if (relative.startsWith("uploads/")) {
                    String subPath = relative.substring("uploads/".length());
                    Path sharedPath = sharedUploads.resolve(subPath).toAbsolutePath();
                    Image sharedImage = loadImageIfExists(sharedPath);
                    if (sharedImage != null) {
                        return sharedImage;
                    }
                }

                for (Path symfonyPublic : getSymfonyPublicCandidates()) {
                    Path symfonyPath = symfonyPublic.resolve(relative).toAbsolutePath();
                    Image symfonyImage = loadImageIfExists(symfonyPath);
                    if (symfonyImage != null) {
                        return symfonyImage;
                    }
                }

                Image remote = loadFromRemoteMedia(relative);
                if (remote != null) {
                    return remote;
                }
            }

            Path absolutePath = Paths.get(raw).toAbsolutePath();
            Image localAbsolute = loadImageIfExists(absolutePath);
            if (localAbsolute != null) {
                return localAbsolute;
            }

            Path projectRelative = Paths.get(System.getProperty("user.dir"), raw).toAbsolutePath();
            Image localRelative = loadImageIfExists(projectRelative);
            if (localRelative != null) {
                return localRelative;
            }

            Path rawPath = Paths.get(raw);
            if (rawPath.getFileName() != null) {
                String fileName = rawPath.getFileName().toString();
                Image byName = findByFilenameFallback(fileName);
                if (byName != null) {
                    return byName;
                }
            }
        } catch (Exception ignored) {
            return loadPlaceholderImage(resourceClass);
        }

        return loadPlaceholderImage(resourceClass);
    }

    public Path getSharedUploadsDir() {
        String configured = System.getenv("AGRI_UPLOADS_DIR");
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get(System.getProperty("user.dir"), "uploads");
    }

    private Image loadImageIfExists(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        Image image = new Image(path.toUri().toString(), false);
        return image.isError() ? null : image;
    }

    private List<Path> getSymfonyPublicCandidates() {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        String envPath = System.getenv("SYMFONY_PUBLIC_DIR");
        if (envPath != null && !envPath.isBlank()) {
            candidates.add(Paths.get(envPath).toAbsolutePath());
        }

        Path homeSymfonyPublic = Paths.get(System.getProperty("user.home"), "agrismart", "public").toAbsolutePath();
        candidates.add(homeSymfonyPublic);

        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path parent = current.getParent();
        if (parent != null) {
            candidates.add(parent.resolve("agriSmartSymfony").resolve("public").toAbsolutePath());
            candidates.add(parent.resolve("agrismart-symfony").resolve("public").toAbsolutePath());
            candidates.add(parent.resolve("symfony").resolve("public").toAbsolutePath());
            candidates.add(parent.resolve("project-symfony").resolve("public").toAbsolutePath());
        }

        return new ArrayList<>(candidates);
    }

    private Image loadFromRemoteMedia(String relative) {
        LinkedHashSet<String> bases = new LinkedHashSet<>();

        String envBase = System.getenv("MEDIA_BASE_URL");
        if (envBase != null && !envBase.isBlank()) {
            bases.add(envBase);
        }

        bases.add("http://localhost:8000");
        bases.add("http://127.0.0.1:8000");

        for (String base : bases) {
            String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
            Image image = new Image(normalizedBase + "/" + relative, false);
            if (!image.isError()) {
                return image;
            }
        }

        return null;
    }

    private Image findByFilenameFallback(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        Path sharedCandidate = getSharedUploadsDir().resolve("produits").resolve(fileName).toAbsolutePath();
        Image shared = loadImageIfExists(sharedCandidate);
        if (shared != null) {
            return shared;
        }

        for (Path symfonyPublic : getSymfonyPublicCandidates()) {
            Path candidate = symfonyPublic.resolve("uploads").resolve("produits").resolve(fileName).toAbsolutePath();
            Image found = loadImageIfExists(candidate);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private Image loadPlaceholderImage(Class<?> resourceClass) {
        try {
            String[] candidates = {
                    "/images/placeholder_agrismart.png",
                    "/images/product_placeholder.png",
                    "/images/logo.png"
            };

            for (String candidate : candidates) {
                var resource = resourceClass.getResource(candidate);
                if (resource == null) {
                    continue;
                }
                Image placeholder = new Image(resource.toExternalForm(), true);
                if (!placeholder.isError()) {
                    return placeholder;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
