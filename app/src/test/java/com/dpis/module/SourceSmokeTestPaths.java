package com.dpis.module;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

final class SourceSmokeTestPaths {
    private SourceSmokeTestPaths() {
    }

    static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolve(relativePath)), StandardCharsets.UTF_8);
    }

    static InputStream open(String relativePath) throws IOException {
        return Files.newInputStream(resolve(relativePath));
    }

    static boolean exists(String relativePath) {
        return Files.exists(resolveIfPresent(relativePath));
    }

    static boolean exists(String first, String... more) {
        return exists(Path.of(first, more).toString());
    }

    static String readRepositoryRoot(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(resolveRepositoryRoot(relativePath)),
                StandardCharsets.UTF_8);
    }

    private static Path resolve(String relativePath) throws NoSuchFileException {
        Path path = resolveIfPresent(relativePath);
        if (Files.exists(path)) {
            return path;
        }
        throw new NoSuchFileException(relativePath, path.toString(), "source smoke test path not found");
    }

    private static Path resolveIfPresent(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path fromRepositoryRoot = Path.of("app").resolve(relativePath);
        if (Files.exists(fromRepositoryRoot)) {
            return fromRepositoryRoot;
        }
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path fromModuleRoot = current.resolve(relativePath);
            if (Files.exists(fromModuleRoot)) {
                return fromModuleRoot;
            }
            Path fromParentRepositoryRoot = current.resolve("app").resolve(relativePath);
            if (Files.exists(fromParentRepositoryRoot)) {
                return fromParentRepositoryRoot;
            }
            current = current.getParent();
        }
        return direct;
    }

    private static Path resolveRepositoryRoot(String relativePath) throws NoSuchFileException {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            Path appParent = current.getFileName() != null
                    && "app".equals(current.getFileName().toString())
                            ? current.getParent()
                            : null;
            if (appParent != null) {
                Path fromAppParent = appParent.resolve(relativePath);
                if (Files.exists(fromAppParent)) {
                    return fromAppParent;
                }
            }
            current = current.getParent();
        }
        throw new NoSuchFileException(relativePath, direct.toString(), "repository root path not found");
    }
}
