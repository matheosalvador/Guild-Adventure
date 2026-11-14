package com.lucas.guild;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ProfileData;
import com.lucas.guild.data.SkillData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.ProfileSummary;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.Shop;
import com.lucas.guild.model.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CreationTest {
    static int fails = 0, total = 0;
    static final java.util.List<String> failed = new java.util.ArrayList<>();

    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

    @Test
    void run() throws Exception {
        fails = 0; total = 0; failed.clear();
        logic();
        org.junit.jupiter.api.Assertions.assertTrue(failed.isEmpty(), "Verifications en echec : " + failed);
    }

    static void logic() throws Exception {
        System.out.println("--- 1. donnees");
        List<SkillData> skills = GameContent.skills();
        check("7 competences", skills.size() == 7);
        check("4 classes, 4 origines", GameContent.characterClasses().size() == 4 && GameContent.backgrounds().size() == 4);
        check("toutes les competences ont des effets connus", skills.stream().allMatch(s -> SkillData.EFFECTS.keySet().containsAll(s.effects.keySet())));
        SkillData haggling = skills.stream().filter(s -> s.id.equals("haggling")).findFirst().get();
        check("seuils du marchandage : 49.9 -> 0, 50 -> 1, 199 -> 1, 200 -> 2, 1e9 -> 5",
                haggling.levelFor(49.9) == 0 && haggling.levelFor(50) == 1 && haggling.levelFor(199) == 1 && haggling.levelFor(200) == 2 && haggling.levelFor(1e9) == 5);
        check("classe inconnue / null -> null", GameContent.characterClass("zzz") == null && GameContent.characterClass(null) == null && GameContent.background("zzz") == null);
        check("copies neuves a chaque appel", GameContent.characterClass("warrior") != GameContent.characterClass("warrior"));

        System.out.println("--- 2. Skills : pratique et niveaux");
        Skills sk = new Skills();
        check("depart : niveau 0, aucun bonus", sk.getLevel("athletics") == 0 && sk.bonus("speedPercent") == 0);
        check("499 m : rien ne se passe", sk.train("athletics", 499) == null && sk.getLevel("athletics") == 0);
        String m1 = sk.train("athletics", 1);
        check("500 m : competence debloquee, message", m1 != null && m1.contains("débloquée") && m1.contains("Endurance") && sk.getLevel("athletics") == 1);
        check("effet niveau 1 : +2 % de vitesse", near(sk.bonus("speedPercent"), 2));
        check("pratique en plus sans nouveau seuil : pas de message", sk.train("athletics", 100) == null);
        String m2 = sk.train("athletics", 1400);
        check("2000 m : niveau 2, message", m2 != null && m2.contains("niveau 2") && near(sk.bonus("speedPercent"), 4));
        sk.train("athletics", 1_000_000);
        check("niveau maximum 5 : +10 %, pas de depassement", sk.getLevel("athletics") == 5 && near(sk.bonus("speedPercent"), 10) && sk.train("athletics", 1e6) == null);
        check("pratique negative / nulle ignoree", sk.train("carrying", 0) == null && sk.train("carrying", -5) == null && sk.getPractice("carrying") == 0);
        boolean threw = false;
        try { sk.train("inconnue", 1); } catch (IllegalArgumentException e) { threw = true; }
        check("competence inconnue : erreur claire", threw);
        Skills sk2 = new Skills();
        sk2.setLevel("haggling", 2);
        check("setLevel(2) : pratique = seuil (200), niveau 2, effets", sk2.getPractice("haggling") == 200 && sk2.getLevel("haggling") == 2 && near(sk2.bonus("buyDiscountPercent"), 4) && near(sk2.bonus("sellBonusPercent"), 4));
        sk2.setLevel("haggling", 1);
        check("setLevel ne fait jamais baisser", sk2.getLevel("haggling") == 2);
        sk2.setLevel("haggling", 99);
        check("setLevel au-dela du max : plafonne a 5", sk2.getLevel("haggling") == 5);
        Skills sk3 = new Skills();
        sk3.bind(Map.of("athletics", 50));
        sk3.train("athletics", 334);
        check("bonus de progression +50 % : 334 m comptent pour 501", near(sk3.getPractice("athletics"), 501) && sk3.getLevel("athletics") == 1);
        Skills sk4 = new Skills();
        sk4.bind(Map.of("athletics", -30));
        sk4.train("athletics", 500);
        check("malus de progression -30 % : 500 m comptent pour 350, pas de niveau", near(sk4.getPractice("athletics"), 350) && sk4.getLevel("athletics") == 0);

        System.out.println("--- 3. personnage sans classe (parties existantes, partie par defaut)");
        GameSession plain = GameSession.newGame("normale");
        Adventurer p = plain.getPlayer();
        check("nom par defaut, 100 PV, pas de classe ni d'origine", p.getName().equals("Aventurier") && p.getMaxHealth() == 100 && p.getClassId() == null && p.getBackgroundId() == null && p.getClassLabel() == null);
        check("60 Po, 1 potion, 30 kg, vitesse 1.0", p.getGold() == 60 && p.getInventoryItems().size() == 1 && near(p.getInventaire().getPoidsMax(), 30) && near(p.getSpeedFactor(), 1.0));
        check("nom blanc -> nom par defaut ; nom avec espaces -> nettoye",
                GameSession.newGame("normale", "   ", null, null).getPlayer().getName().equals("Aventurier")
                        && GameSession.newGame("normale", "  Bob ", null, null).getPlayer().getName().equals("Bob"));
        check("classe/origine inconnues : ignorees", GameSession.newGame("normale", "X", "zzz", "yyy").getPlayer().getClassId() == null);

        System.out.println("--- 4. creation : effets des choix");
        Adventurer w = GameSession.newGame("normale", "Conan", "warrior", "veteran").getPlayer();
        check("guerrier + ancien soldat : 100+20+10 = 130 PV", w.getMaxHealth() == 130 && w.getHealth() == 130);
        check("charge 30+5+3 = 38 kg", near(w.getInventaire().getPoidsMax(), 38));
        check("or 60-10-30 = 20 Po", w.getGold() == 20);
        check("epee courte de depart PORTEE ; la potion de la difficulte est en inventaire", w.getInventaire().getWeapon() != null && w.getInventaire().getWeapon().getId().equals("short_sword") && w.getInventoryItems().size() == 1);
        check("ids et libelles", w.getClassId().equals("warrior") && w.getClassLabel().equals("Guerrier") && w.getBackgroundLabel().equals("Ancien soldat"));
        check("bonus de degats de l'epee pris en compte", w.getBonusDegats() == 6);
        check("poids porte = potion 0.5 + epee 2 = 2.5", near(w.getInventaire().getPoidsActuel(), 2.5));

        Adventurer sc = GameSession.newGame("normale", "Ayla", "scout", "orphan").getPlayer();
        check("eclaireur + orphelin : 100-10-5 = 85 PV", sc.getMaxHealth() == 85);
        check("charge 30-3 = 27 kg", near(sc.getInventaire().getPoidsMax(), 27));
        check("vitesse : +6 +4 +2 (Endurance niveau 1 de depart) = 1.12", near(sc.getSpeedFactor(), 1.12));
        check("Endurance deja debloquee au niveau 1", sc.getSkills().getLevel("athletics") == 1 && sc.getSkills().getLevel("haggling") == 0);
        check("or 60-30 = 30 Po ; dague portee", sc.getGold() == 30 && sc.getInventaire().getWeapon().getId().equals("dagger"));

        Adventurer mn = GameSession.newGame("normale", "Marco", "merchant", "noble").getPlayer();
        check("marchand + noble : 80 PV, 140 Po, vitesse 0.97", mn.getMaxHealth() == 80 && mn.getGold() == 140 && near(mn.getSpeedFactor(), 0.97));
        check("besace de depart portee : 30 + 5 = 35", mn.getInventaire().getBag() != null && near(mn.getInventaire().getPoidsMax(), 35));
        check("Marchandage niveau 1 de depart : -2 % a l'achat, +2 % a la revente", near(mn.getSkillBonus("buyDiscountPercent"), 2) && near(mn.getSkillBonus("sellBonusPercent"), 2));

        Adventurer hb = GameSession.newGame("normale", "Flora", "herbalist", "farmer").getPlayer();
        check("herboriste + paysan : 100 PV, 30 Po, 3 potions", hb.getMaxHealth() == 100 && hb.getGold() == 30 && hb.getInventoryItems().size() == 3);

        System.out.println("--- 5. creation : or jamais negatif, objets trop lourds ignores");
        Adventurer broke = GameSession.newGame("realiste", "Pauvre", "herbalist", "veteran").getPlayer();
        check("realiste (30) -10 -30 : or borne a 0", broke.getGold() == 0);
        boolean allOk = true;
        for (String diff : new String[] {"facile", "normale", "realiste"}) {
            for (ProfileData c : GameContent.characterClasses()) {
                for (ProfileData b : GameContent.backgrounds()) {
                    Adventurer x = GameSession.newGame(diff, "T", c.id, b.id).getPlayer();
                    boolean ok = x.getMaxHealth() > 0 && x.getHealth() == x.getMaxHealth() && x.getGold() >= 0
                            && x.getInventaire().getPoidsActuel() <= x.getInventaire().getPoidsMax() + 1e-9
                            && x.getSpeedFactor() > 0.5 && x.getSpeedFactor() < 1.5;
                    if (!ok) { allOk = false; System.out.println("   probleme : " + diff + " " + c.id + " " + b.id); }
                }
            }
        }
        check("les 48 combinaisons (3 difficultes x 4 classes x 4 origines) donnent un depart valide", allOk);

        System.out.println("--- 6. effets des competences gagnees");
        GameSession s6 = GameSession.newGame("normale");
        Adventurer q = s6.getPlayer();
        List<String> seen = new ArrayList<>();
        q.setNotifier(seen::add);
        q.train("vigor", 4);
        check("Vigueur 1 : +5 PV max, et soigne d'autant (105 / 105)", q.getMaxHealth() == 105 && q.getHealth() == 105);
        q.perdreVie(50);
        q.train("vigor", 11);
        check("Vigueur 2 : 110 PV max, soigne de +5 seulement (55 -> 60)", q.getMaxHealth() == 110 && q.getHealth() == 60);
        q.train("carrying", 500);
        check("Portage 1 : capacite 31 kg", near(q.getInventaire().getPoidsMax(), 31));
        q.train("athletics", 2000);
        check("Endurance 2 : vitesse 1.04", near(q.getSpeedFactor(), 1.04));
        check("4 messages de competence, dans l'ordre (Vigueur 1, Vigueur 2, Portage, Endurance)", seen.size() == 4 && seen.get(0).contains("Vigueur") && seen.get(3).contains("Endurance"));
        GameSession s6b = GameSession.newGame("normale");
        s6b.getPlayer().train("athletics", 500);
        check("la partie branche les messages du joueur sur l'horloge (toast)", s6b.getClock().hasPendingMessage() && s6b.getClock().pollMessage().contains("Endurance"));

        Adventurer h = GameSession.newGame("normale", "H", "herbalist", "farmer").getPlayer();
        h.perdreVie(60);
        String used = h.useHealingItem(h.getInventoryItems().get(0));
        check("Soins 1 (+10 %) : potion de 25 -> 28 PV rendus, vie 40 -> 68", h.getHealth() == 68 && used.contains("+28 PV") && h.getInventoryItems().size() == 2);
        check("l'usage a entraine Soins (3 de depart + 1 x 1.5 = 4.5)", near(h.getSkills().getPractice("healing"), 4.5));
        Adventurer full = GameSession.newGame("normale").getPlayer();
        String refused = full.useHealingItem(full.getInventoryItems().get(0));
        check("vie pleine : potion refusee, non consommee, rien d'entraine", refused.contains("pleine forme") && full.getInventoryItems().size() == 1 && full.getSkills().getPractice("healing") == 0);

        System.out.println("--- 7. boutique : marchandage");
        Shop forge = new Shop("forge");
        Item plateModel = forge.getStock().stream().filter(i -> i.getId().equals("plate_armor")).findFirst().get();
        Adventurer m = GameSession.newGame("normale", "Marco", "merchant", "noble").getPlayer();
        check("prix de l'armure de plaques pour un marchand niveau 1 : 220 -> 216 (-2 %, arrondi au plus proche)", forge.buyPrice(m, plateModel) == 216);
        Adventurer plain2 = GameSession.newGame("normale").getPlayer();
        check("sans competence : 220", forge.buyPrice(plain2, plateModel) == 220 && forge.sellPrice(plain2, plateModel) == 110);
        Item potionModel = new Shop("guild").getStock().get(0);
        check("petit prix : potion de soin a 10 Po, -2 % -> 10 Po (arrondi, pas 9)", new Shop("guild").buyPrice(m, potionModel) == 10);
        check("revente par un marchand niveau 1 : 52 % de 220 = 114", forge.sellPrice(m, plateModel) == 114);
        m.addGold(500);
        int g0 = m.getGold();
        check("achat au prix reduit : -216 Po", forge.buy(m, plateModel) == null && m.getGold() == g0 - 216);
        double practice = m.getSkills().getPractice("haggling");
        check("l'achat a entraine le Marchandage : 50 + 216 x 1.7 = " + practice, near(practice, 50 + 216 * 1.7));
        check("Marchandage niveau 2 atteint (415 >= 200, < 600)", m.getSkills().getLevel("haggling") == 2);
        Item boughtPlate = m.getInventoryItems().stream().filter(i -> "plate_armor".equals(i.getId())).findFirst().get();
        int gold1 = m.getGold();
        check("revente au prix ameliore (niveau 2 : 54 % de 220 = 118)", forge.sellPrice(m, boughtPlate) == 118 && forge.sell(m, boughtPlate) == null && m.getGold() == gold1 + 118);
        Adventurer poor = GameSession.newGame("normale").getPlayer();
        poor.setNotifier(null);
        check("un refus (pas assez d'or) n'entraine rien", forge.buy(poor, plateModel) != null && poor.getSkills().getPractice("haggling") == 0);
        Adventurer cap = GameSession.newGame("normale").getPlayer();
        cap.getSkills().setLevel("haggling", 5);
        check("niveau 5 : -10 % (220 -> 198) et revente 60 % (132)", forge.buyPrice(cap, plateModel) == 198 && forge.sellPrice(cap, plateModel) == 132);

        System.out.println("--- 8. sauvegarde et chargement");
        Path run = Paths.get("").toAbsolutePath();
        GameSession s8 = GameSession.newGame("normale", "Sauve", "scout", "orphan");
        Adventurer d = s8.getPlayer();
        d.train("carrying", 600);
        d.train("vigor", 4);
        double speedBefore = d.getSpeedFactor();
        double capBefore = d.getInventaire().getPoidsMax();
        int maxBefore = d.getMaxHealth();
        int carryLevel = d.getSkills().getLevel("carrying");
        SaveManager sm = new SaveManager();
        check("sauvegarde ecrite", sm.saveGame(d, s8.getClock(), "normale", 0, 2, 0, "Town"));
        String json = new String(Files.readAllBytes(run.resolve("save.json")), "UTF-8");
        check("le JSON contient la classe, l'origine et la pratique, mais pas les effets calcules", json.contains("\"classId\": \"scout\"") && json.contains("\"backgroundId\": \"orphan\"") && json.contains("\"practice\"") && !json.contains("profileSpeedPercent") && !json.contains("bonuses") && !json.contains("skillTree"));
        GameData data = sm.loadGame();
        GameSession r = GameSession.fromSave(data);
        Adventurer rp = r.getPlayer();
        check("nom, classe, origine conserves", rp.getName().equals("Sauve") && rp.getClassId().equals("scout") && rp.getBackgroundId().equals("orphan"));
        check("pratique conservee : memes niveaux", rp.getSkills().getLevel("carrying") == carryLevel && rp.getSkills().getLevel("athletics") == 1 && near(rp.getSkills().getPractice("carrying"), d.getSkills().getPractice("carrying")));
        check("effets recalcules : meme vitesse, meme capacite, meme vie max", near(rp.getSpeedFactor(), speedBefore) && near(rp.getInventaire().getPoidsMax(), capBefore) && rp.getMaxHealth() == maxBefore);
        rp.train("athletics", 100000);
        check("la progression continue apres chargement, avec le bonus de progression de la classe", rp.getSkills().getLevel("athletics") == 5);
        check("les messages du joueur charge arrivent a l'horloge", r.getClock().hasPendingMessage());

        Path legacyRun = Files.createTempDirectory("legacy");
        String legacy = "{\"playerData\":{\"name\":\"Vieux\",\"health\":80,\"maxHealth\":100,\"location\":\"Caserne\",\"level\":3,\"experience\":40,\"skillPoints\":2,\"skillTree\":{\"className\":\"Caserne\",\"skills\":[{\"name\":\"Attaque Puissante\",\"learned\":true}]}},\"currentHour\":9,\"currentDay\":42}";
        GameData old = new com.google.gson.Gson().fromJson(legacy, GameData.class);
        GameSession ro = GameSession.fromSave(old);
        Adventurer op = ro.getPlayer();
        check("ancienne sauvegarde (arbre de competences, niveau, XP) : se charge sans erreur", op.getName().equals("Vieux") && op.getClassId() == null && op.getSkills() != null);
        check("   ... sans competence, sans bonus : vitesse 1.0, vie max 100", op.getSkills().getLevel("athletics") == 0 && near(op.getSpeedFactor(), 1.0) && op.getMaxHealth() == 100);
        op.train("athletics", 500);
        check("   ... et progresse normalement (500 m : Endurance 1)", op.getSkills().getLevel("athletics") == 1);
        GameData oldBadClass = new com.google.gson.Gson().fromJson("{\"playerData\":{\"name\":\"X\",\"health\":90,\"maxHealth\":100,\"classId\":\"zzz\",\"backgroundId\":\"yyy\"},\"currentHour\":9,\"currentDay\":42}", GameData.class);
        Adventurer bad = GameSession.fromSave(oldBadClass).getPlayer();
        check("classe supprimee des donnees : ignoree sans plantage, aucun bonus", near(bad.getSpeedFactor(), 1.0) && bad.getClassLabel() == null);

        System.out.println("--- 9. textes des choix (generes depuis les chiffres)");
        ProfileData war = GameContent.characterClass("warrior");
        List<String> adv = ProfileSummary.advantages(war);
        List<String> con = ProfileSummary.drawbacks(war);
        check("guerrier : +20 PV, +5 kg, epee courte", adv.contains("20 points de vie") && adv.contains("5 kg de charge portable") && adv.contains("Départ avec : Épée courte"));
        check("guerrier : -10 Po, Marchandage 30 % moins vite", con.contains("10 Po au départ") && con.contains("Marchandage progresse 30 % moins vite"));
        ProfileData scout = GameContent.characterClass("scout");
        check("eclaireur : +6 % vitesse, Endurance niveau 1, Endurance 50 % plus vite ; -10 PV, -3 kg",
                ProfileSummary.advantages(scout).contains("6 % de vitesse de déplacement") && ProfileSummary.advantages(scout).contains("Compétence Endurance déjà au niveau 1")
                        && ProfileSummary.advantages(scout).contains("Endurance progresse 50 % plus vite")
                        && ProfileSummary.drawbacks(scout).contains("10 points de vie") && ProfileSummary.drawbacks(scout).contains("3 kg de charge portable"));
        boolean everyChoiceHasBoth = true;
        for (ProfileData c : GameContent.characterClasses()) if (ProfileSummary.advantages(c).isEmpty() || ProfileSummary.drawbacks(c).isEmpty()) everyChoiceHasBoth = false;
        for (ProfileData c : GameContent.backgrounds()) if (ProfileSummary.advantages(c).isEmpty() || ProfileSummary.drawbacks(c).isEmpty()) everyChoiceHasBoth = false;
        check("chaque classe et chaque origine a au moins un avantage ET un inconvenient (pas de choix evident)", everyChoiceHasBoth);

        System.out.println("--- equilibrage (information)");
        for (ProfileData c : GameContent.characterClasses()) {
            for (ProfileData b : GameContent.backgrounds()) {
                Adventurer x = GameSession.newGame("normale", "T", c.id, b.id).getPlayer();
                System.out.printf("   %-10s + %-8s : %3d PV  %4.1f kg  %3d Po  vitesse %.2f%n", c.id, b.id, x.getMaxHealth(), x.getInventaire().getPoidsMax(), x.getGold(), x.getSpeedFactor());
            }
        }
    }

    static void check(String n, boolean ok) {
        total++;
        if (!ok) { fails++; failed.add(n); }
        System.out.println((ok ? "OK     " : "ECHEC  ") + n);
    }
}
