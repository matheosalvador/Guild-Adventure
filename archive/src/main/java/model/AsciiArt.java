package model;
// a neutraliser plus tard si inutile
import java.util.HashMap;
import java.util.Map;

public class AsciiArt {

    public static final boolean GAME_TITLE = Boolean.parseBoolean("Quete d'un aventurier");
    private static final Map<String, String> ART_MAP = new HashMap<>();

    static {
        ART_MAP.put("GAME_TITLE",
              "              ,---. \n"
            + "             /    | \n"
            + "            /     | \n"
            + "           /      | \n"
            + "          /       | \n"
            + "  ,------'        | \n"
            + " /                | \n"
            + "/                 | \n"
            + "|     ,-----.     | \n"
            + "|    /  |  _ `\\.   | \n"
            + "|   |   | |_) |   | \n"
            + "|   |   | .--'    | \n"
            + "|    \\  | |       / \n"
            + "|     `-| |'     / \n"
            + "|       `\"'      | \n"
            + "|                 | \n"
            + "\\                 / \n"
            + " \\               / \n"
            + "  `------.        | \n"
            + "          \\       | \n"
            + "           \\      | \n"
            + "            \\     | \n"
            + "             \\    | \n"
            + "              `---' \n"
            + "LA GROTTE DU ROI DEMON\n");

        ART_MAP.put("GAME_OVER",
              "       .-. \n"
            + "      (   ) \n"
            + "       '-' \n"
            + "     /`---'\\ \n"
            + "    |       | \n"
            + "    |       | \n"
            + "    \\       / \n"
            + "     '.   .' \n"
            + "       \"\"\" \n"
            + "    GAME OVER\n");

        ART_MAP.put("TREASURE_CHEST",
              "         __________ \n"
            + "        /\\____;;___\\ \n"
            + "       | /         / \n"
            + "       `.         / \n"
            + "         `--'-\"\"\"'\"' \n");

        ART_MAP.put("DEMON_KING",
              "      /`-.   .-'\\ \n"
            + "     |   `-'   | \n"
            + "     | | ( ) | | \n"
            + "     | |  |  | | \n"
            + "     | `-----' | \n"
            + "     \\  `\"\"\"`  / \n"
            + "      `-.   .-' \n"
            + "         `-' \n");

        ART_MAP.put("Arthur", // Player
              "    O \n"
            + "   /|\\ \n"
            + "   / \\ \n");

        ART_MAP.put("Loup Fidèle",
              "      / \\__\n"
            + " (    @\\___)\n"
            + " /         O\n"
            + "/   (_____/\n"
            + "/_____/   U\n");

        ART_MAP.put("Mercenaire Aguerri",
              "    _|_ \n"
            + "   |o o| \n"
            + "   | - | \n"
            + "   \\_-_/ \n");
            
        ART_MAP.put("Gobelin",
              "    o \n"
            + "   <|>\n"
            + "   /`\\ \n");

        ART_MAP.put("Orque",
              "   ,---. \n"
            + "  / O O \\ \n"
            + " | \\_^_/ | \n"
            + "  \\_`\"\"\"`_/ \n");

        ART_MAP.put("Troll",
              "   (---) \n"
            + "  ( O O ) \n"
            + "  |  ^  | \n"
            + "  \\ --- / \n");
    }

    /**
     * Récupère un dessin ASCII par sa clé.
     * @param key La clé identifiant l'art (ex: "GAME_TITLE", "Arthur").
     * @return Le dessin ASCII sous forme de String, ou une chaîne vide si non trouvé.
     */
    public static String getArt(String key) {
        return ART_MAP.getOrDefault(key, "");
    }
}