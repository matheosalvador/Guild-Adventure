package com.lucas.guild.game;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Armure;
import com.lucas.guild.model.Attaque;
import com.lucas.guild.model.AttaqueDistance;
import com.lucas.guild.model.AttaqueMelee;
import com.lucas.guild.model.AttaqueSimple;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.GameData;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.PotionDeSoin;
import com.lucas.guild.model.PotionDps;
import com.lucas.guild.model.PotionMix;
import com.lucas.guild.model.PotionSoin;

/**
 * Lecture / écriture de la sauvegarde (un seul emplacement, {@code save.json}
 * dans le dossier de travail).
 *
 * <ul>
 *   <li>Écriture en UTF-8, dans un fichier temporaire puis déplacé : un plantage
 *       en cours d'écriture ne détruit jamais la sauvegarde précédente.</li>
 *   <li>Lecture tolérante : un fichier illisible n'arrête pas le jeu, il est mis
 *       de côté en {@code save.json.corrupt}.</li>
 *   <li>Les anciennes sauvegardes (jeu de caractères Windows) restent lisibles.</li>
 * </ul>
 */
public class SaveManager {

    private static final String SAVE_FILE = "save.json";
    private static final Charset LEGACY_CHARSET = Charset.forName("windows-1252");

    private final Gson gson;

    public SaveManager() {
        RuntimeTypeAdapterFactory<Item> itemAdapter = RuntimeTypeAdapterFactory.of(Item.class, "itemType")
                .registerSubtype(Arme.class)
                .registerSubtype(Armure.class)
                .registerSubtype(PotionSoin.class)
                .registerSubtype(PotionDps.class)
                .registerSubtype(PotionMix.class)
                .registerSubtype(PotionDeSoin.class)
                .registerSubtype(ObjetDeQuete.class);

        RuntimeTypeAdapterFactory<Attaque> attaqueAdapter = RuntimeTypeAdapterFactory.of(Attaque.class, "attaqueType")
                .registerSubtype(AttaqueSimple.class)
                .registerSubtype(AttaqueMelee.class)
                .registerSubtype(AttaqueDistance.class);

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(itemAdapter)
                .registerTypeAdapterFactory(attaqueAdapter)
                .create();
    }

    /** @return true si la sauvegarde a bien été écrite. */
    public boolean saveGame(Adventurer player, GameClock clock) {
        return write(new GameData(player, clock));
    }

    /** @return true si la sauvegarde a bien été écrite. */
    public boolean saveGame(Adventurer player, GameClock clock, String difficulty,
                            float playerX, float playerY, float playerZ, String currentScreen) {
        return write(new GameData(player, clock, difficulty, playerX, playerY, playerZ, currentScreen));
    }

    /**
     * Charge la sauvegarde.
     *
     * @return les données, ou {@code null} s'il n'y a pas de sauvegarde exploitable
     *         (absente, vide, illisible ou d'une version plus récente que le jeu).
     */
    public GameData loadGame() {
        Path file = saveFile();
        if (!Files.isRegularFile(file)) {
            System.out.println("Aucune sauvegarde trouvee.");
            return null;
        }

        try {
            String json = readText(file);
            GameData data = gson.fromJson(new StringReader(json), GameData.class);
            if (data == null || data.playerData == null) {
                System.out.println("Sauvegarde vide ou invalide : " + file);
                return null;
            }
            if (data.version > GameData.CURRENT_VERSION) {
                System.out.println("Sauvegarde d'une version plus recente du jeu (v" + data.version
                        + " > v" + GameData.CURRENT_VERSION + ") : non chargee.");
                return null;
            }
            System.out.println("Sauvegarde chargee : " + file);
            return data;
        } catch (JsonParseException e) {
            System.out.println("Sauvegarde corrompue (" + e.getMessage() + ").");
            quarantine(file);
            return null;
        } catch (IOException e) {
            System.out.println("Erreur de lecture de la sauvegarde : " + e.getMessage());
            return null;
        }
    }

    public boolean hasSave() {
        Path file = saveFile();
        try {
            return Files.isRegularFile(file) && Files.size(file) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteSave() {
        try {
            return Files.deleteIfExists(saveFile());
        } catch (IOException e) {
            System.out.println("Impossible de supprimer la sauvegarde : " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ interne

    private static Path saveFile() {
        return Paths.get(SAVE_FILE).toAbsolutePath();
    }

    private boolean write(GameData data) {
        Path target = saveFile();
        Path tmp = target.resolveSibling(SAVE_FILE + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Partie sauvegardee : " + target);
            return true;
        } catch (IOException | JsonIOException e) {
            System.out.println("Erreur lors de la sauvegarde : " + e.getMessage());
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // rien de plus à faire
            }
            return false;
        }
    }

    /** Lit en UTF-8 ; si le fichier n'est pas de l'UTF-8 valide, retombe sur l'ancien encodage Windows. */
    private static String readText(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, LEGACY_CHARSET);
        }
    }

    /** Met un fichier illisible de côté pour qu'il ne soit pas écrasé sans trace. */
    private static void quarantine(Path file) {
        Path corrupt = file.resolveSibling(SAVE_FILE + ".corrupt");
        try {
            Files.move(file, corrupt, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Fichier mis de cote : " + corrupt);
        } catch (IOException e) {
            System.out.println("Impossible de mettre le fichier de cote : " + e.getMessage());
        }
    }
}
