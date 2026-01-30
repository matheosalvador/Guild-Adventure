package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gère la structure, la génération et l'affichage du donjon.
 * La carte est générée une seule fois au début de la partie et conservée dans le GameState.
 */
public class DungeonMap {

    /**
     * Énumération des différents types de salles possibles dans le donjon.
     */
    public enum RoomType { COMBAT, TREASURE, EMPTY, SHORTCUT, BOSS }

    /**
     * Représente un niveau (ou étage) du donjon.
     * Chaque niveau a une salle principale et potentiellement une salle secondaire.
     */
    public static class DungeonLevel {
        public RoomType mainRoomType = RoomType.COMBAT;
        public RoomType sideRoomType = null;
        public boolean sideRoomVisible = false;
        public boolean mainRoomVisited = false;
        public boolean sideRoomVisited = false;
    }

    private final List<DungeonLevel> levels;
    private final int totalLevels;
    // Le mot-clé 'transient' indique à Gson d'ignorer ce champ lors de la sauvegarde/chargement.
    private transient final Random random = new Random();

    /**
     * Construit une nouvelle carte de donjon avec un nombre de niveaux défini.
     * La disposition des salles est générée aléatoirement à la création.
     *
     * @param totalLevels Le nombre total de niveaux dans le donjon.
     */
    public DungeonMap(int totalLevels) {
        this.totalLevels = totalLevels;
        this.levels = new ArrayList<>(totalLevels);
        generateLayout();
    }

    /**
     * Génère la disposition aléatoire des salles principales et secondaires pour chaque niveau.
     */
    private void generateLayout() {
        for (int i = 0; i < totalLevels; i++) {
            DungeonLevel level = new DungeonLevel();
            if (i == totalLevels - 1) {
                level.mainRoomType = RoomType.BOSS;
            } else {
                if (random.nextDouble() < 0.4) { // 40% de chance de salle secondaire
                    double roomRoll = random.nextDouble();
                    if (roomRoll < 0.5) level.sideRoomType = RoomType.TREASURE;
                    else if (roomRoll < 0.8) level.sideRoomType = RoomType.EMPTY;
                    else level.sideRoomType = RoomType.SHORTCUT;
                }
            }
            levels.add(level);
        }
    }

    /**
     * Révèle les salles adjacentes au niveau actuel du joueur.
     *
     * @param currentLevel L'index du niveau actuel.
     */
    public void revealNextRooms(int currentLevel) {
        if (currentLevel < totalLevels) {
            levels.get(currentLevel).sideRoomVisible = true;
        }
    }

    public DungeonLevel getLevel(int levelIndex) {
        if (levelIndex >= 0 && levelIndex < totalLevels) {
            return levels.get(levelIndex);
        }
        return null;
    }

    /**
     * Marque la salle principale d'un niveau comme visitée.
     * @param levelIndex L'index du niveau.
     */
    public void visitMainRoom(int levelIndex) {
        if (levelIndex < totalLevels) {
            levels.get(levelIndex).mainRoomVisited = true;
        }
    }

    /**
     * Marque la salle secondaire d'un niveau comme visitée.
     * @param levelIndex L'index du niveau.
     */
    public void visitSideRoom(int levelIndex) {
        if (levelIndex < totalLevels) {
            levels.get(levelIndex).sideRoomVisited = true;
        }
    }

    /**
     * Affiche la carte du donjon en format ASCII, montrant la position du joueur,
     * les salles visitées et les salles adjacentes révélées.
     *
     * @param playerLevel L'index du niveau où se trouve le joueur.
     */
    public void display(int playerLevel) {
        System.out.println("\n--- Carte de la Grotte ---");
        StringBuilder mainPath = new StringBuilder();
        StringBuilder sidePath = new StringBuilder();
        boolean sidePathExists = false;

        for (int i = 0; i < totalLevels; i++) {
            DungeonLevel level = levels.get(i);
            String mainRoomSymbol;

            if (i < playerLevel) mainRoomSymbol = "[ X ]";
            else if (i == playerLevel) mainRoomSymbol = "[ P ]";
            else if (level.mainRoomType == RoomType.BOSS) mainRoomSymbol = "[ B ]";
            else mainRoomSymbol = "[ ? ]";

            mainPath.append(mainRoomSymbol);
            if (i < totalLevels - 1) mainPath.append("-->");

            if (level.sideRoomType != null && level.sideRoomVisible && !level.sideRoomVisited) {
                sidePathExists = true;
                String sideRoomSymbol = "?";
                switch(level.sideRoomType) {
                    case TREASURE: sideRoomSymbol = "T"; break;
                    case EMPTY: sideRoomSymbol = "."; break;
                    case SHORTCUT: sideRoomSymbol = "S"; break;
                }
                sidePath.append(" ".repeat(mainPath.length() - 3));
                sidePath.append("|\n");
                sidePath.append(" ".repeat(mainPath.length() - 3));
                sidePath.append("+->[ ").append(sideRoomSymbol).append(" ]");
                break; 
            }
        }

        System.out.println(mainPath);
        if (sidePathExists) {
            System.out.println(sidePath);
        }
        System.out.println("Légende: [P]layer, [X]=Visité, [?]=Inconnu, [B]oss, [T]résor, [.]=Vide, [S]hortcut");
    }
}