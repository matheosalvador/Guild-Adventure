package com.lucas.guild;

import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.model.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TimeTest {
    static int fails = 0, total = 0;
    static final java.util.List<String> failed = new java.util.ArrayList<>();

    @Test
    void run() throws Exception {
        fails = 0; total = 0; failed.clear();
        GameCalendar cal = GameCalendar.standard();

        System.out.println("--- 1. calendrier");
        check("112 jours par an (4 x 28)", cal.daysPerYear() == 112);
        CalendarDate d1 = cal.dateOf(1);
        check("jour 1 = Lundi 1 Printemps, An 1", d1.toString().equals("Lundi 1 Printemps, An 1"));
        check("jour 28 = dernier du Printemps (Dimanche)", cal.dateOf(28).toString().equals("Dimanche 28 Printemps, An 1"));
        check("jour 29 = 1er de l'Ete", cal.dateOf(29).toString().equals("Lundi 1 Été, An 1"));
        check("jour 57 = Automne", cal.dateOf(57).seasonName.equals("Automne") && cal.dateOf(57).dayOfSeason == 1);
        check("jour 85 = Hiver", cal.dateOf(85).seasonName.equals("Hiver"));
        check("jour 112 = dernier jour de l'annee 1", cal.dateOf(112).year == 1 && cal.dateOf(112).seasonName.equals("Hiver") && cal.dateOf(112).dayOfSeason == 28);
        check("jour 113 = An 2, Printemps 1", cal.dateOf(113).year == 2 && cal.dateOf(113).seasonName.equals("Printemps") && cal.dateOf(113).dayOfSeason == 1);
        check("semaine : jour 8 = Lundi, jour 7 = Dimanche", cal.dateOf(8).weekdayName.equals("Lundi") && cal.dateOf(7).weekdayName.equals("Dimanche"));
        check("jour 0 (veille) = An 0, Hiver 28", cal.dateOf(0).year == 0 && cal.dateOf(0).seasonName.equals("Hiver") && cal.dateOf(0).dayOfSeason == 28);
        check("jour -2239 (naissance a 20 ans) valide", cal.dateOf(1 - 20 * 112).year == -19 && cal.dateOf(1 - 20 * 112).dayOfSeason == 1);
        boolean allOk = true;
        for (int day = -500; day < 600; day++) {
            CalendarDate d = cal.dateOf(day);
            if (d.dayOfSeason < 1 || d.dayOfSeason > 28 || d.seasonIndex < 0 || d.seasonIndex > 3 || d.weekdayIndex < 0 || d.weekdayIndex > 6) allOk = false;
        }
        check("1100 jours consecutifs : toutes les dates valides", allOk);

        System.out.println("--- 2. soleil (lever / coucher)");
        check("milieu du printemps (jour 14) : 6h00 / 19h30", near(cal.sunrise(14), 6.0, 0.06) && near(cal.sunset(14), 19.5, 0.06));
        check("milieu de l'ete (jour 42) : 5h00 / 21h00", near(cal.sunrise(42), 5.0, 0.06) && near(cal.sunset(42), 21.0, 0.06));
        check("milieu de l'hiver (jour 98) : 8h00 / 17h00", near(cal.sunrise(98), 8.0, 0.06) && near(cal.sunset(98), 17.0, 0.06));
        double maxStep = 0;
        for (int day = 1; day < 400; day++) {
            maxStep = Math.max(maxStep, Math.abs(cal.sunrise(day + 1) - cal.sunrise(day)));
            maxStep = Math.max(maxStep, Math.abs(cal.sunset(day + 1) - cal.sunset(day)));
        }
        check("evolution douce d'un jour a l'autre, y compris au passage d'annee (max " + String.format("%.3f", maxStep) + " h)", maxStep < 0.25);
        check("jour d'ete plus long que jour d'hiver", (cal.sunset(42) - cal.sunrise(42)) > (cal.sunset(98) - cal.sunrise(98)) + 6);

        System.out.println("--- 3. cycle jour / nuit");
        DayNightCycle.Light noon = DayNightCycle.compute(12.75, 6.0, 19.5, 0.4f);
        DayNightCycle.Light midnight = DayNightCycle.compute(0.0, 6.0, 19.5, 0.4f);
        DayNightCycle.Light dawn = DayNightCycle.compute(6.0, 6.0, 19.5, 0.4f);
        DayNightCycle.Light morning = DayNightCycle.compute(8.0, 6.0, 19.5, 0.4f);
        check("midi : plein jour (daylight=1)", near(noon.daylight, 1.0, 1e-3));
        check("midi : ciel bleu d'origine (0.5, 0.8, 1.0)", near(noon.sky[0], 0.5, 1e-3) && near(noon.sky[1], 0.8, 1e-3) && near(noon.sky[2], 1.0, 1e-3));
        check("midi : soleil blanc 0.8 d'origine", near(noon.lightColor[0], 0.8, 1e-3) && near(noon.lightColor[1], 0.8, 1e-3) && near(noon.lightColor[2], 0.8, 1e-3));
        check("midi : ambiante = valeur de base 0.4", near(noon.ambient[0], 0.4, 1e-3) && near(noon.ambient[1], 0.4, 1e-3) && near(noon.ambient[2], 0.4, 1e-3));
        check("minuit : nuit (daylight=0, ciel sombre)", noon.daylight > midnight.daylight && midnight.daylight < 0.01 && midnight.sky[2] < 0.1);
        check("minuit : lumiere plus faible que de jour et bleutee", midnight.lightColor[2] > midnight.lightColor[0] && sum(midnight.lightColor) < 0.4 * sum(noon.lightColor));
        check("aube : orange (rouge > bleu dans le ciel)", dawn.sky[0] > dawn.sky[2]);
        check("8h au printemps : deja jour complet", near(morning.daylight, 1.0, 0.02));
        double worstJump = 0;
        DayNightCycle.Light prev = DayNightCycle.compute(0, 6.0, 19.5, 0.4f);
        for (int m = 1; m <= 24 * 60; m++) {
            DayNightCycle.Light cur = DayNightCycle.compute(m / 60.0, 6.0, 19.5, 0.4f);
            for (int i = 0; i < 3; i++) {
                worstJump = Math.max(worstJump, Math.abs(cur.sky[i] - prev.sky[i]));
                worstJump = Math.max(worstJump, Math.abs(cur.lightColor[i] - prev.lightColor[i]));
                worstJump = Math.max(worstJump, Math.abs(cur.ambient[i] - prev.ambient[i]));
                worstJump = Math.max(worstJump, Math.abs(cur.lightDirection[i] - prev.lightDirection[i]));
            }
            prev = cur;
        }
        check("aucun saut brutal sur 24h (max par minute " + String.format("%.4f", worstJump) + ")", worstJump < 0.03);
        DayNightCycle.Light wrap = DayNightCycle.compute(24.0, 6.0, 19.5, 0.4f), zero = DayNightCycle.compute(0.0, 6.0, 19.5, 0.4f);
        check("24h == 0h (continuite minuit)", near(wrap.sky[2], zero.sky[2], 1e-6));
        int summerLight = 0, winterLight = 0;
        for (int m = 0; m < 24 * 60; m++) {
            if (DayNightCycle.compute(m / 60.0, cal.sunrise(42), cal.sunset(42), 0.4f).daylight > 0.5) summerLight++;
            if (DayNightCycle.compute(m / 60.0, cal.sunrise(98), cal.sunset(98), 0.4f).daylight > 0.5) winterLight++;
        }
        check("heures de clarte : ete (" + summerLight / 60 + "h) > hiver (" + winterLight / 60 + "h)", summerLight > winterLight + 5 * 60);

        System.out.println("--- 4. horloge");
        GameSession s = GameSession.newGame("normale");
        GameClock clock = s.getClock();
        Adventurer p = s.getPlayer();
        check("nouvelle partie : jour 1, 08:00", clock.getCurrentDay() == 1 && clock.formatTime().equals("08:00"));
        clock.update(60f);
        check("60 s reelles = 1 h de jeu -> 09:00", clock.formatTime().equals("09:00"));
        clock.update(30f);
        check("+30 s reelles -> 09:30", clock.formatTime().equals("09:30"));
        clock.update(0.5f);
        check("temps de jeu fractionnaire (09:30 + 30 s de jeu)", clock.getTimeOfDay() > 9.5 && clock.getTimeOfDay() < 9.52);
        for (int i = 0; i < 600; i++) clock.update(1f / 60f);  // 10 s reelles en 600 images
        check("600 images a 60 fps = 10 s -> 09:40 (pas de derive)", clock.formatTime().equals("09:40") || clock.formatTime().equals("09:39"));
        clock.passTime(15);
        check("passTime(15h) franchit minuit -> jour 2", clock.getCurrentDay() == 2 && clock.getCurrentHour() == 0);

        clock.setCurrentDay(1); clock.setCurrentHour(8); clock.setCurrentMinute(0);
        clock.sleepUntilNextMorning();
        check("dormir a 8h -> reveil le lendemain 07:00", clock.getCurrentDay() == 2 && clock.formatTime().equals("07:00"));
        clock.setCurrentDay(5); clock.setCurrentHour(3); clock.setCurrentMinute(20);
        clock.sleepUntilNextMorning();
        check("dormir a 3h20 -> reveil le meme jour 07:00", clock.getCurrentDay() == 5 && clock.formatTime().equals("07:00"));
        clock.setCurrentDay(5); clock.setCurrentHour(7); clock.setCurrentMinute(0);
        clock.sleepUntilNextMorning();
        check("dormir a 7h pile -> 24 h plus tard", clock.getCurrentDay() == 6 && clock.formatTime().equals("07:00"));
        List<String> msgs = drain(clock);
        check("message de reveil poste", msgs.stream().anyMatch(m -> m.contains("réveillez") && m.contains("07:00")));

        System.out.println("--- 5. evenements (saison, annee, anniversaire)");
        drain(clock);
        clock.setCurrentDay(28); clock.setCurrentHour(23); clock.setCurrentMinute(59);
        clock.advanceMinutes(1);
        msgs = drain(clock);
        check("passage au jour 29 : message 'Ete'", clock.getCurrentDay() == 29 && msgs.stream().anyMatch(m -> m.contains("saison") && m.contains("Été")));
        clock.setCurrentDay(112); clock.setCurrentHour(23); clock.setCurrentMinute(59);
        clock.advanceMinutes(1);
        msgs = drain(clock);
        check("passage au jour 113 : message nouvelle annee An 2", msgs.stream().anyMatch(m -> m.contains("année") && m.contains("An 2")));

        System.out.println("--- 6. age et vieillissement");
        GameSession s2 = GameSession.newGame("normale");
        Adventurer q = s2.getPlayer();
        GameClock c2 = s2.getClock();
        int dpy = cal.daysPerYear();
        check("nouvelle partie : 20 ans", q.getAge(1) == 20);
        check("veille de l'anniversaire (jour 112) : 20 ans ; jour 113 : 21 ans", q.getAge(112) == 20 && q.getAge(113) == 21);
        check("anniversaire au jour 113 (An 2, Printemps 1) ; le jour 1 est celui des 20 ans", q.isBirthday(113) && !q.isBirthday(112) && !q.isBirthday(114) && !q.isBirthday(2));
        check("a 20 ans : aucun malus (100 %)", q.getMaxHealth() == 100 && q.getSpeedFactor() == 1.0 && near(q.getInventaire().getPoidsMax(), 30.0));
        // on vieillit en avancant le temps jour apres jour (declenche les evenements de nouveau jour)
        drain(c2);
        int daysToAge45 = 25 * dpy;       // 20 -> 45 ans
        for (int i = 0; i < daysToAge45; i++) c2.passTime(24);
        msgs = drain(c2);
        check("apres 25 ans de jeu : 45 ans", q.getAge(c2.getCurrentDay()) == 45);
        check("palier 'Maturité' : vie max 95, vitesse 98 %, charge 28.5 kg", q.getMaxHealth() == 95 && near(q.getSpeedFactor(), 0.98) && near(q.getInventaire().getPoidsMax(), 28.5));
        check("messages : anniversaires + changement de palier", msgs.stream().filter(m -> m.contains("anniversaire")).count() == 25 && msgs.stream().anyMatch(m -> m.contains("Maturité")));
        check("un seul message de palier (pas de repetition)", msgs.stream().filter(m -> m.contains("Les années se font sentir")).count() == 1);
        q.heal(1000);
        check("soin plafonne a la vie max reduite (95)", q.getHealth() == 95);
        for (int i = 0; i < 35 * dpy; i++) c2.passTime(24);   // 45 -> 80 ans
        check("a 80 ans : palier 'Très grand âge' vie 55, vitesse 70 %, charge 16.5", q.getAge(c2.getCurrentDay()) == 80 && q.getMaxHealth() == 55 && near(q.getSpeedFactor(), 0.70) && near(q.getInventaire().getPoidsMax(), 16.5));
        check("la vie actuelle est ramenee sous le nouveau plafond", q.getHealth() <= 55);
        // charge : ajouter des objets jusqu'a la limite reduite
        Adventurer old = q;
        int added = 0;
        while (old.addItem(new PotionSoin())) { added++; if (added > 1000) break; }
        check("a 80 ans on porte moins : " + added + " potions (0,5 kg) = 32 de plus (+1 potion du kit de depart) pour 16.5 kg", added == 32);
        old.train("vigor", 4);   // competence Vigueur 1 : +5 vie de base (soigne d'autant)
        check("Vigueur 1 a 80 ans : vie max (100+5) x 55 % = 58, soignee de +3", old.getMaxHealth() == 58 && old.getHealth() == 58);

        System.out.println("--- 7. sauvegarde : minute, naissance, effets recalcules");
        Path run = Paths.get("save.json");
        Files.deleteIfExists(run);
        SaveManager sm = new SaveManager();
        old.setName("Vieux Lucas");
        c2.setCurrentMinute(37);
        check("sauvegarde ecrite", sm.saveGame(old, c2, "normale", 0f, 2f, 0f, "Town"));
        GameData data = sm.loadGame();
        check("version courante, minute 37 sauvegardee", data != null && data.version == GameData.CURRENT_VERSION && data.currentMinute == 37);
        GameSession back = GameSession.fromSave(data);
        Adventurer r = back.getPlayer();
        check("minute restauree", back.getClock().getCurrentMinute() == 37 && back.getClock().getCurrentDay() == c2.getCurrentDay());
        check("date de naissance restauree : meme age", r.getBirthDay() == old.getBirthDay() && r.getAge(back.getClock().getCurrentDay()) == 80);
        check("effets de l'age recalcules apres chargement (vie 58 avec Vigueur 1, vitesse 70 %, charge 16.5)", r.getMaxHealth() == 58 && near(r.getSpeedFactor(), 0.70 * r.getInventaire().getEncumbranceSpeedFactor()) && near(r.getInventaire().getPoidsMax(), 16.5));
        check("les effets ne sont PAS ecrits dans le fichier (transient)", !new String(Files.readAllBytes(run), "UTF-8").contains("capacityFactor") && !new String(Files.readAllBytes(run), "UTF-8").contains("healthFactor"));
        check("objets conserves apres chargement", r.getInventoryItems().size() == 33 && r.getInventaire().getPoidsActuel() >= 16.4);

        System.out.println("--- 8. anciennes sauvegardes (avant le temps)");
        String legacy = "{\"playerData\":{\"name\":\"Lucas\",\"health\":80,\"maxHealth\":100,\"location\":\"Caserne\",\"level\":1},"
                + "\"currentHour\":9,\"currentDay\":42,\"difficulty\":\"facile\",\"playerX\":1,\"playerY\":2,\"playerZ\":3,\"currentScreen\":\"Town\"}";
        Files.write(run, legacy.getBytes("UTF-8"));
        GameData od = sm.loadGame();
        check("ancienne sauvegarde (version 0) chargee", od != null && od.version == 0);
        GameSession os = GameSession.fromSave(od);
        check("age de depart donne : 20 ans", os.getPlayer().getAge(os.getClock().getCurrentDay()) == 20);
        check("minute absente -> 0, heure et jour conserves", os.getClock().getCurrentMinute() == 0 && os.getClock().getCurrentHour() == 9 && os.getClock().getCurrentDay() == 42);
        check("pas de malus a 20 ans", os.getPlayer().getMaxHealth() == 100);

        org.junit.jupiter.api.Assertions.assertTrue(failed.isEmpty(), "Verifications en echec : " + failed);
    }

    static List<String> drain(GameClock c) {
        List<String> out = new ArrayList<>();
        String m;
        while ((m = c.pollMessage()) != null) out.add(m);
        return out;
    }

    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }
    static boolean near(double a, double b, double eps) { return Math.abs(a - b) < eps; }
    static double sum(float[] v) { return v[0] + v[1] + v[2]; }

    static void check(String n, boolean ok) {
        total++;
        if (!ok) { fails++; failed.add(n); }
        System.out.println((ok ? "OK     " : "ECHEC  ") + n);
    }
}
