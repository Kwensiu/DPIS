package com.dpis.displaytool;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ComposeContractSourceTest {
    private static final Pattern DIRECT_WEB_VIEW_USAGE = Pattern.compile("\\bWebView\\b");

    @Test
    public void composePhase2DoesNotAddWebViewDependency() throws IOException {
        assertNoWebViewUsage("build.gradle.kts", readCompanionFile("build.gradle.kts"));

        Path mainDir = companionModuleDir().resolve("src/main");
        try (Stream<Path> paths = Files.walk(mainDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(ComposeContractSourceTest::isRelevantMainSource)
                    .forEach(ComposeContractSourceTest::assertSourceFileHasNoWebViewUsage);
        }
    }

    @Test
    public void composeScenesRejectFragileVariant() throws IOException {
        List<String> sceneFiles = Arrays.asList(
                "src/main/java/com/dpis/displaytool/scene/ComposeBaselineTextScene.kt",
                "src/main/java/com/dpis/displaytool/scene/ComposeNestedScrollTextScene.kt",
                "src/main/java/com/dpis/displaytool/scene/ComposeLazyListTextScene.kt",
                "src/main/java/com/dpis/displaytool/scene/ComposeStyledTextScene.kt"
        );

        for (String sceneFile : sceneFiles) {
            String source = readCompanionFile(sceneFile);

            assertTrue(sceneFile, source.contains("supportsComposeVariant(variant)"));
            assertFalse(sceneFile, source.contains("VARIANT_FRAGILE"));
        }
    }

    private static void assertSourceFileHasNoWebViewUsage(Path path) {
        try {
            Path moduleDir = companionModuleDir();
            String relativePath = moduleDir.relativize(path).toString().replace('\\', '/');
            assertNoWebViewUsage(relativePath, readFile(path));
        } catch (IOException e) {
            throw new AssertionError("Unable to read " + path, e);
        }
    }

    private static void assertNoWebViewUsage(String sourceName, String source) {
        assertFalse(sourceName, source.contains("android.webkit.WebView"));
        assertFalse(sourceName, source.contains("androidx.webkit"));
        assertFalse(sourceName, DIRECT_WEB_VIEW_USAGE.matcher(source).find());
    }

    private static boolean isRelevantMainSource(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java")
                || name.endsWith(".kt")
                || name.endsWith(".kts")
                || name.endsWith(".xml");
    }

    private static String readCompanionFile(String relativePath) throws IOException {
        return readFile(companionModuleDir().resolve(relativePath));
    }

    private static String readFile(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static Path companionModuleDir() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (Path path = current; path != null; path = path.getParent()) {
            if (isCompanionModule(path)) {
                return path;
            }

            Path child = path.resolve("companion-display-tool");
            if (isCompanionModule(child)) {
                return child;
            }
        }

        throw new AssertionError("Unable to find companion-display-tool from " + current);
    }

    private static boolean isCompanionModule(Path path) {
        return Files.isRegularFile(path.resolve("build.gradle.kts"))
                && Files.isDirectory(path.resolve("src/test/java/com/dpis/displaytool"));
    }
}
