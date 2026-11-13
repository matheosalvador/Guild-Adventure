package com.lucas.guild;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.Shop;
import com.lucas.guild.model.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class InventoryTest {
    static int fails = 0, total = 0;
    static final java.util.List<String> failed = new java.util.ArrayList<>();

    static Item potion() { return GameContent.newItem("healing_potion"); }

    static Inventaire inv(String diff) {
        Inventaire i = new Inventaire();
        i.applyDifficulty(diff);
        return i;
    }

    static int fill(Inventaire i, int n) {
        int added = 0;
        for (int k = 0; k < n; k++) if (i.ajouterItem(potion())) added++;
        return added;
    }

    @Test
    void run() throws Exception {
        fails = 0; total = 0; failed.clear();
        System.out.println("--- 1. catalogue");
        check("14 objets au catalogue", GameContent.items().size() == 14);
        Item bp = GameContent.newItem("backpack");
        check("sac a dos : Sac, +15 kg, 1.5 kg, 60 Po", bp instanceof Sac && ((Sac) bp).getCapacityBonus() == 15 && bp.getPoids() == 1.5 && bp.getPrix() == 60);
        Item dg = GameContent.newItem("dagger");
        check("dague : Arme +3, 1 kg", dg instanceof Arme && ((Arme) dg).getBonusDegats() == 3 && dg.getPoids() == 1.0);
        check("cotte de mailles : Armure 5, 8 kg", GameContent.newItem("chain_mail") instanceof Armure && GameContent.newItem("chain_mail").getPoids() == 8.0);
        check("potion de soin : PotionSoin 0.5 kg utilisable hors combat", GameContent.newItem("healing_potion") instanceof PotionSoin && potion().estUtilisableHorsCombat());
        check("potion de degats : pas utilisable hors combat", !GameContent.newItem("damage_potion").estUtilisableHorsCombat());
        check("objet inconnu -> null", GameContent.newItem("nope") == null);
        check("chaque newItem est une instance distincte", GameContent.newItem("dagger") != GameContent.newItem("dagger"));
        check("les anciens objets crees en code gardent leur poids par defaut", new Arme("X", 1).getPoids() == 2.0 && new Armure("X", 1).getPoids() == 5.0 && new PotionDeSoin("X", 5).getPoids() == 0.5 && new ObjetDeQuete("X").getPoids() == 0.1);

        System.out.println("--- 2. poids et capacite (difficulte normale : 30 kg)");
        Inventaire a = inv("normale");
        check("capacite de base 30 kg", a.getPoidsMax() == 30.0 && a.getPoidsDeBase() == 30.0);
        check("60 potions (0.5 kg) tiennent dans 30 kg", fill(a, 70) == 60 && a.getItems().size() == 60);
        check("la 61e est refusee et RIEN n'est ajoute", !a.ajouterItem(potion()) && a.getItems().size() == 60 && a.getPoidsActuel() == 30.0);
        check("une arme ne passe pas non plus (avant : renvoyait true sans rien ajouter)", !a.ajouterItem(GameContent.newItem("dagger")) && a.getItems().size() == 60);
        Inventaire b = inv("normale");
        Item dagger = GameContent.newItem("dagger");
        check("une arme s'ajoute vraiment quand il y a la place", b.ajouterItem(dagger) && b.getItems().contains(dagger) && b.getPoidsActuel() == 1.0);
        check("facile = 35 kg, realiste = 25 kg", inv("facile").getPoidsMax() == 35.0 && inv("realiste").getPoidsMax() == 25.0);

        System.out.println("--- 3. sacs");
        Inventaire c = inv("normale");
        Item pouch = GameContent.newItem("pouch");
        c.ajouterItem(pouch);
        check("equiper la besace : sort de la liste, compte dans le poids (0.5 kg)", c.equip(pouch) == null && c.getItems().isEmpty() && c.getBag() == pouch && c.getPoidsActuel() == 0.5);
        check("capacite = 30 + 5 = 35", c.getPoidsMax() == 35.0 && c.getCapaciteSac() == 5.0);
        int more = fill(c, 100);
        check("69 potions + la besace = 35 kg pile (" + more + ")", more == 69 && near(c.getPoidsActuel(), 35.0) && !c.ajouterItem(potion()));
        String refuse = c.unequip(Inventaire.Slot.BAG);
        check("poser le sac plein est refuse (35 kg > 30 de base)", refuse != null && refuse.contains("Trop lourd") && c.getBag() == pouch);
        for (int k = 0; k < 10; k++) c.retirerItem(c.getItems().get(0));
        check("apres avoir jete 10 potions (30 kg) on peut poser le sac", c.unequip(Inventaire.Slot.BAG) == null && c.getBag() == null && c.getItems().contains(pouch) && near(c.getPoidsActuel(), 30.0) && c.getPoidsMax() == 30.0);

        Inventaire d = inv("normale");
        Item pack = GameContent.newItem("backpack");
        d.ajouterItem(pack);
        d.equip(pack);
        fill(d, 77);   // 38.5 kg + 1.5 sac = 40
        Item smallPouch = GameContent.newItem("pouch");
        check("sac a dos porte : capacite 45", d.getPoidsMax() == 45.0 && d.ajouterItem(smallPouch));
        double weightBefore = d.getPoidsActuel();
        String refuse2 = d.equip(smallPouch);
        check("passer a un sac plus petit (35) alors qu'on porte 40+ est refuse", refuse2 != null && d.getBag() == pack && d.getItems().contains(smallPouch) && near(d.getPoidsActuel(), weightBefore));
        Item bigBag = GameContent.newItem("travel_bag");
        d.ajouterItem(bigBag);
        double w2 = d.getPoidsActuel();
        check("passer a un sac plus grand : ok, l'ancien retourne dans l'inventaire, poids total inchange", d.equip(bigBag) == null && d.getBag() == bigBag && d.getItems().contains(pack) && near(d.getPoidsActuel(), w2) && d.getPoidsMax() == 60.0);

        Inventaire e = inv("normale");
        Item w1 = GameContent.newItem("dagger"), w3 = GameContent.newItem("short_sword"), arm = GameContent.newItem("chain_mail");
        e.ajouterItem(w1); e.ajouterItem(w3); e.ajouterItem(arm);
        double tw = e.getPoidsActuel();
        e.equip(w1); e.equip(arm);
        check("arme + armure equipees : toujours comptees dans le poids", e.getWeapon() == w1 && e.getArmor() == arm && near(e.getPoidsActuel(), tw));
        e.equip(w3);
        check("changer d'arme : l'ancienne retourne dans l'inventaire", e.getWeapon() == w3 && e.getItems().contains(w1) && near(e.getPoidsActuel(), tw));
        check("retirer l'armure : retourne dans l'inventaire", e.unequip(Inventaire.Slot.ARMOR) == null && e.getArmor() == null && e.getItems().contains(arm));
        check("objet non equipable refuse", e.equip(potion()) != null && e.equip(GameContent.newItem("healing_potion")) != null);
        check("retirer un emplacement vide : message, pas d'erreur", e.unequip(Inventaire.Slot.BAG) != null);

        System.out.println("--- 4. age x capacite");
        Inventaire f = inv("normale");
        f.setCapacityFactor(0.7);
        check("a 70 ans (x0.7) : 30 kg -> 21 kg", near(f.getPoidsMax(), 21.0));
        Item pk = GameContent.newItem("backpack"); f.ajouterItem(pk); f.equip(pk);
        check("avec le sac a dos : (30+15) x 0.7 = 31.5", near(f.getPoidsMax(), 31.5));

        System.out.println("--- 5. encombrement (vitesse selon la charge)");
        Inventaire g = inv("normale");   // demarre a 60 %, minimum 75 %
        check("vide : 100 %", g.getEncumbranceSpeedFactor() == 1.0);
        fill(g, 36);
        check("a 60 % de la charge : encore 100 %", near(g.getEncumbranceSpeedFactor(), 1.0));
        fill(g, 12);
        check("a 80 % : 87,5 % (milieu)", near(g.getEncumbranceSpeedFactor(), 0.875));
        fill(g, 12);
        check("a 100 % : 75 % (minimum)", near(g.getEncumbranceSpeedFactor(), 0.75));
        Inventaire gf = inv("facile");   // 35 kg, 75 % -> 85 %
        fill(gf, 70);
        Inventaire gr = inv("realiste"); // 25 kg, 50 % -> 65 %
        fill(gr, 50);
        check("facile a pleine charge : 85 % ; realiste : 65 %", near(gf.getEncumbranceSpeedFactor(), 0.85) && near(gr.getEncumbranceSpeedFactor(), 0.65));
        Inventaire over = inv("normale"); fill(over, 60); over.setCapacityFactor(0.7);   // le vieillissement fait passer sous la charge actuelle
        check("charge au-dessus de la capacite (vieillissement) : vitesse au minimum, pas de plantage", near(over.getEncumbranceSpeedFactor(), 0.75) && !over.ajouterItem(potion()));

        System.out.println("--- 6. or");
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        check("normale : 60 Po de depart", p.getGold() == 60);
        check("depenser 25 : reste 35", p.spendGold(25) && p.getGold() == 35);
        check("depenser plus que ce qu'on a : refuse, or inchange", !p.spendGold(36) && p.getGold() == 35);
        check("montant negatif refuse", !p.spendGold(-5) && p.getGold() == 35);
        p.addGold(15); p.addGold(-100);
        check("gain de 15 -> 50 (un gain negatif est ignore)", p.getGold() == 50);

        System.out.println("--- 7. boutiques");
        Shop forge = new Shop("forge"), guild = new Shop("guild");
        check("forge : 7 articles ; guilde : 7 articles", forge.getStock().size() == 7 && guild.getStock().size() == 7);
        check("titre de la forge", forge.getTitle().equals("La Forge du Nain Grincheux"));
        GameSession s2 = GameSession.newGame("normale");
        Adventurer q = s2.getPlayer();               // 60 Po, 1 potion
        Item wantDagger = forge.getStock().get(0);
        int itemsBefore = q.getInventoryItems().size();
        check("acheter une dague (15 Po)", forge.buy(q, wantDagger) == null && q.getGold() == 45 && q.getInventoryItems().size() == itemsBefore + 1);
        check("la dague achetee est une nouvelle instance, pas le modele du stock", q.getInventoryItems().stream().noneMatch(i -> i == wantDagger));
        Item plate = forge.getStock().get(6);        // armure de plaques 220 Po
        String broke = forge.buy(q, plate);
        check("achat impossible faute d'or : message, or et inventaire inchanges", broke != null && broke.contains("Pas assez d'or") && q.getGold() == 45 && q.getInventoryItems().size() == itemsBefore + 1);
        q.addGold(500);
        // on remplit presque tout : 30 kg - (0.5 + 1) = 28.5 ; on met 56 potions = 28 -> reste 0.5 ; la plaque (15 kg) ne passe pas
        int g0 = q.getGold();
        while (q.getInventaire().peutPorter(potion())) q.addItem(potion());
        String heavy = forge.buy(q, plate);
        check("achat impossible si trop lourd : message avec le poids manquant, or intact", heavy != null && heavy.contains("Trop lourd") && heavy.contains("kg") && q.getGold() == g0);
        Item daggerToSell = q.getInventoryItems().stream().filter(i -> "dagger".equals(i.getId())).findFirst().get();
        int gold1 = q.getGold();
        check("revente de la dague a 50 % (7 Po, prix 15 x 50 %)", forge.sellPrice(daggerToSell) == 7 && forge.sell(q, daggerToSell) == null && q.getGold() == gold1 + 7 && !q.getInventoryItems().contains(daggerToSell));
        ObjetDeQuete tail = new ObjetDeQuete("Queue de rat");
        q.getInventaire().retirerItem(q.getInventoryItems().get(0));
        q.addItem(tail);
        check("un objet sans valeur (objet de quete) ne se revend pas", forge.sell(q, tail) != null && q.getInventoryItems().contains(tail));
        Item eq = GameContent.newItem("short_sword");
        q.getInventaire().retirerItem(q.getInventoryItems().get(0)); q.getInventaire().retirerItem(q.getInventoryItems().get(0));
        q.addItem(eq); q.equip(eq);
        check("un objet equipe ne se revend pas (il faut d'abord le retirer)", forge.sell(q, eq) != null && q.getInventaire().getWeapon() == eq);

        System.out.println("--- 8. kit de depart par difficulte");
        Adventurer pf = GameSession.newGame("facile").getPlayer();
        check("facile : 100 Po, besace PORTEE, 2 potions, capacite 35+5", pf.getGold() == 100 && pf.getInventaire().getBag() != null && pf.getInventaire().getBag().getName().equals("Besace") && pf.getInventoryItems().size() == 2 && pf.getInventaire().getPoidsMax() == 40.0);
        Adventurer pn = GameSession.newGame("normale").getPlayer();
        check("normale : 60 Po, 1 potion, pas de sac", pn.getGold() == 60 && pn.getInventoryItems().size() == 1 && pn.getInventaire().getBag() == null);
        Adventurer pr = GameSession.newGame("realiste").getPlayer();
        check("realiste : 30 Po, rien d'autre", pr.getGold() == 30 && pr.getInventoryItems().isEmpty() && pr.getInventaire().getPoidsMax() == 25.0);

        System.out.println("--- 9. vitesse = age x charge");
        GameSession s3 = GameSession.newGame("normale");
        Adventurer v = s3.getPlayer();
        check("a vide a 20 ans : pleine vitesse", near(v.getSpeedFactor(), 1.0 * v.getInventaire().getEncumbranceSpeedFactor()) && near(v.getSpeedFactor(), 1.0));
        int day = s3.getClock().getCurrentDay();
        v.setBirthDay(day - 70 * 112);
        v.refreshAging(day);
        while (v.getInventaire().peutPorter(potion())) v.addItem(potion());
        double enc = v.getInventaire().getEncumbranceSpeedFactor();
        check("a 70 ans et charge pleine : 82 % x 75 % = " + String.format("%.3f", v.getSpeedFactor()), near(enc, 0.75) && near(v.getSpeedFactor(), 0.82 * 0.75));

        System.out.println("--- 10. sauvegarde version 3");
        Path run = Paths.get("save.json");
        Files.deleteIfExists(run);
        SaveManager sm = new SaveManager();
        GameSession s4 = GameSession.newGame("normale");
        Adventurer h = s4.getPlayer();
        h.setName("Test");
        h.addGold(200);
        Item hd = GameContent.newItem("long_sword"), ha = GameContent.newItem("chain_mail"), hb = GameContent.newItem("backpack");
        h.addItem(hd); h.addItem(ha); h.addItem(hb);
        h.equip(hd); h.equip(ha); h.equip(hb);
        h.addItem(GameContent.newItem("greater_healing_potion")); h.addItem(GameContent.newItem("damage_potion")); h.addItem(new ObjetDeQuete("Queue de rat"));
        double wBefore = h.getInventaire().getPoidsActuel(), cBefore = h.getInventaire().getPoidsMax();
        int nBefore = h.getInventoryItems().size(), gBefore = h.getGold();
        check("sauvegarde ecrite", sm.saveGame(h, s4.getClock(), "normale", 0, 2, 0, "Town"));
        String json = new String(Files.readAllBytes(run), "UTF-8");
        check("le fichier contient le type des objets de la liste, l'or et les 3 emplacements d'equipement", json.contains("\"itemType\": \"PotionDeSoin\"") && json.contains("\"itemType\": \"PotionDps\"") && json.contains("\"gold\"") && json.contains("\"bag\"") && json.contains("\"weapon\"") && json.contains("\"armor\""));
        check("aucune trace de l'ancien format (potionsSoin...) ni des effets recalcules", !json.contains("potionsSoin") && !json.contains("capacityFactor") && !json.contains("encumbranceStart"));
        GameData data = sm.loadGame();
        check("version 3", data != null && data.version == 3);
        Adventurer r = GameSession.fromSave(data).getPlayer();
        check("arme / armure / sac equipes restaures (meme types, memes valeurs)",
                r.getInventaire().getWeapon() != null && r.getInventaire().getWeapon().getBonusDegats() == 12
                        && r.getInventaire().getArmor() != null && r.getInventaire().getArmor().getReductionDegats() == 5
                        && r.getInventaire().getBag() != null && r.getInventaire().getBag().getCapacityBonus() == 15);
        check("objets (" + nBefore + "), or (" + gBefore + "), poids et capacite identiques apres chargement",
                r.getInventoryItems().size() == nBefore && r.getGold() == gBefore && near(r.getInventaire().getPoidsActuel(), wBefore) && near(r.getInventaire().getPoidsMax(), cBefore));
        check("ids conserves (la boutique peut les revendre)", r.getInventoryItems().stream().anyMatch(i -> "greater_healing_potion".equals(i.getId())));
        check("bonus de degats de l'arme equipee pris en compte", r.getBonusDegats() == 12 && r.getDefense() == 5);

        System.out.println("--- 11. anciennes sauvegardes (inventaire a 3 listes de potions, sans or)");
        String legacy = "{\"version\":2,\"playerData\":{\"name\":\"Lucas\",\"health\":100,\"maxHealth\":100,\"location\":\"Caserne\",\"level\":1,\"birthDay\":-2239,"
                + "\"inventory\":{\"potionsSoin\":[{\"name\":\"Potion de Soin\",\"description\":\"Restaure 25 points de vie.\",\"soin\":25},{\"name\":\"Potion de Soin\",\"description\":\"Restaure 25 points de vie.\",\"soin\":25}],"
                + "\"potionsDps\":[{\"name\":\"Potion de Dégâts\",\"description\":\"x\",\"degats\":20}],\"potionsMix\":[],\"poidsMax\":30.0}},"
                + "\"currentHour\":9,\"currentDay\":5,\"difficulty\":\"normale\",\"playerX\":1,\"playerY\":2,\"playerZ\":3,\"currentScreen\":\"Town\"}";
        Files.write(run, legacy.getBytes("UTF-8"));
        GameData ld = sm.loadGame();
        check("ancienne sauvegarde v2 chargee", ld != null && ld.version == 2);
        Adventurer lp = GameSession.fromSave(ld).getPlayer();
        check("les 3 potions sont migrees dans la liste unique (2 soin + 1 degats)", lp.getInventoryItems().size() == 3 && lp.getInventoryItems().stream().filter(i -> i instanceof PotionSoin).count() == 2 && lp.getInventoryItems().stream().anyMatch(i -> i instanceof PotionDps));
        check("poids = 0.5 + 0.5 + 0.7 = 1.7 kg ; capacite 30", near(lp.getInventaire().getPoidsActuel(), 1.7) && lp.getInventaire().getPoidsMax() == 30.0);
        check("or de depart donne (60 Po)", lp.getGold() == 60);
        // resauvegarde : plus d'ancien format
        sm.saveGame(lp, new GameClock(lp), "normale", 0, 2, 0, "Town");
        String again = new String(Files.readAllBytes(run), "UTF-8");
        check("une fois resauvegardee, l'ancienne structure a disparu et la version est 3", !again.contains("potionsSoin") && again.contains("\"version\": 3"));

        System.out.println("--- 12. sauvegardes tres anciennes (sans aucun inventaire, version 0)");
        Files.write(run, "{\"playerData\":{\"name\":\"Lucas\",\"health\":80,\"maxHealth\":100,\"location\":\"Ville\"},\"currentHour\":9,\"currentDay\":42}".getBytes("UTF-8"));
        Adventurer vp = GameSession.fromSave(sm.loadGame()).getPlayer();
        check("inventaire cree, vide, utilisable ; or de depart", vp.getInventaire() != null && vp.getInventoryItems().isEmpty() && vp.getGold() == 60 && vp.addItem(potion()));

        org.junit.jupiter.api.Assertions.assertTrue(failed.isEmpty(), "Verifications en echec : " + failed);
    }

    static boolean near(double x, double y) { return Math.abs(x - y) < 1e-6; }

    static void check(String n, boolean ok) {
        total++;
        if (!ok) { fails++; failed.add(n); }
        System.out.println((ok ? "OK     " : "ECHEC  ") + n);
    }
}
