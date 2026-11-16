package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lucas.guild.game.Settings;

/** Les réglages du menu Options (settings.json) : un fichier absent, illisible ou faux ne plante jamais. */
class SettingsTest {

    @TempDir
    Path tmp;

    private Path write(String json) throws IOException {
        Path file = tmp.resolve("settings.json");
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static void assertDefaults(Settings s) {
        assertEquals(1f, s.mouseSensitivity, 1e-6);
        assertFalse(s.thirdPerson);
        assertTrue(s.shadows);
        assertFalse(s.fullscreen);
    }

    @Test
    void missingFileGivesTheDefaults() {
        assertDefaults(Settings.load(tmp.resolve("settings.json")));
    }

    @Test
    void unreadableFileGivesTheDefaults() throws IOException {
        assertDefaults(Settings.load(write("{ ceci n'est pas du json")));
    }

    @Test
    void wrongTypeGivesTheDefaults() throws IOException {
        assertDefaults(Settings.load(write("{\"mouseSensitivity\": \"vite\", \"shadows\": false}")));
    }

    @Test
    void emptyFileGivesTheDefaults() throws IOException {
        assertDefaults(Settings.load(write("")));
    }

    @Test
    void aDirectoryInsteadOfAFileGivesTheDefaults() throws IOException {
        Path dir = tmp.resolve("settings.json");
        Files.createDirectory(dir);
        assertDefaults(Settings.load(dir));
    }

    @Test
    void missingFieldsKeepTheirDefault() throws IOException {
        Settings s = Settings.load(write("{\"shadows\": false}"));
        assertFalse(s.shadows);
        assertEquals(1f, s.mouseSensitivity, 1e-6);
        assertFalse(s.fullscreen);
    }

    @Test
    void sensitivityOutOfLimitsIsBroughtBack() throws IOException {
        assertEquals(Settings.MAX_SENSITIVITY, Settings.load(write("{\"mouseSensitivity\": 50}")).mouseSensitivity, 1e-6);
        assertEquals(Settings.MIN_SENSITIVITY, Settings.load(write("{\"mouseSensitivity\": -2}")).mouseSensitivity, 1e-6);
        assertEquals(1f, Settings.load(write("{\"mouseSensitivity\": NaN}")).mouseSensitivity, 1e-6);
    }

    @Test
    void savedThenReadBack() {
        Path file = tmp.resolve("settings.json");
        Settings s = new Settings();
        s.mouseSensitivity = 1.7f;
        s.thirdPerson = true;
        s.shadows = false;
        s.fullscreen = true;
        assertTrue(s.save(file));
        Settings back = Settings.load(file);
        assertEquals(1.7f, back.mouseSensitivity, 1e-6);
        assertTrue(back.thirdPerson);
        assertFalse(back.shadows);
        assertTrue(back.fullscreen);
        assertFalse(Files.exists(tmp.resolve("settings.json.tmp")), "pas de fichier temporaire laissé");
    }

    @Test
    void aFailedSaveDoesNotThrow() {
        Settings s = new Settings();
        assertFalse(s.save(tmp.resolve("dossier-absent").resolve("settings.json")));
    }

    @Test
    void sensitivityStepsStayWithinLimits() {
        Settings s = new Settings();
        s.changeSensitivity(3);
        assertEquals("1,3", s.sensitivityLabel());
        s.changeSensitivity(-100);
        assertEquals(Settings.MIN_SENSITIVITY, s.mouseSensitivity, 1e-6);
        assertEquals("0,2", s.sensitivityLabel());
        s.changeSensitivity(100);
        assertEquals("3,0", s.sensitivityLabel());
    }
}
