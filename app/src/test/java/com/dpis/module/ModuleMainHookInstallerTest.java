package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class ModuleMainHookInstallerTest {
    @Test
    public void moduleMainUsesExplicitSystemServerPolicyGuard() throws IOException {
        String source = read("src/main/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("SystemServerMutationPolicy.shouldInstallSystemServerHooks("));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
