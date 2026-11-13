package com.lucas.guild;

import com.lucas.guild.game.*;
import com.lucas.guild.model.*;
import org.junit.jupiter.api.Test;

class DebugTest {
    static int fails = 0, total = 0;
    static final java.util.List<String> failed = new java.util.ArrayList<>();
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

    @Test
    void run() {
        fails = 0; total = 0; failed.clear();
        DebugMode.setEnabled(false);
        GameSession s = GameSession.newGame("realiste");
        Adventurer p = s.getPlayer();
        GameClock c = s.getClock();
        DebugActions d = new DebugActions(s);

        check("debug eteint par defaut", !DebugMode.isEnabled() && DebugMode.getSpeedMultiplier() == 1f);
        DebugMode.setSpeedMultiplier(3f);
        check("hors debug, le multiplicateur de vitesse reste 1", DebugMode.getSpeedMultiplier() == 1f);
        DebugMode.enableFromArgs(new String[] {"--debug"});
        check("--debug allume le mode ; multiplicateur x3 actif", DebugMode.isEnabled() && DebugMode.getSpeedMultiplier() == 3f);
        DebugMode.setEnabled(false);
        check("l'eteindre remet la vitesse a 1", DebugMode.getSpeedMultiplier() == 1f);

        int g0 = p.getGold();
        check("+1000 Po", d.addGold(1000).contains("+1000") && p.getGold() == g0 + 1000);
        p.perdreVie(50);
        d.hurt(20);
        check("-20 PV de plus", p.getHealth() == p.getMaxHealth() - 70);
        d.healFull();
        check("soigner a fond", p.getHealth() == p.getMaxHealth());
        d.hurt(10000);
        check("les PV ne passent pas sous 0", p.getHealth() == 0);
        d.healFull();

        check("objet donne", d.giveItem("dagger").contains("ajouté") && p.getInventoryItems().size() == 1);
        check("objet inconnu : message, rien d'ajoute", d.giveItem("nope").contains("inconnu") && p.getInventoryItems().size() == 1);
        String last = "";
        for (int i = 0; i < 10; i++) last = d.giveItem("plate_armor");
        check("trop lourd : refuse avec message (25 kg en realiste)", last.contains("trop lourd") && p.getInventaire().getPoidsActuel() <= p.getInventaire().getPoidsMax());

        int hp0 = p.getMaxHealth();
        check("Vigueur +1 niveau : message, +5 PV max, soigne", d.skillLevelUp("vigor").contains("niveau 1") && p.getMaxHealth() == hp0 + 5 && p.getHealth() == p.getMaxHealth());
        for (int i = 0; i < 4; i++) d.skillLevelUp("vigor");
        check("niveau 5 atteint, puis refus", p.getSkills().getLevel("vigor") == 5 && d.skillLevelUp("vigor").contains("maximum"));
        check("competence inconnue", d.skillLevelUp("zzz").contains("inconnue"));
        d.skillLevelUp("athletics");
        check("Endurance 1 : vitesse +2 %", near(p.getSkillBonus("speedPercent"), 2));

        // temps
        int day0 = c.getCurrentDay();
        d.advanceHours(6);
        check("+6 heures : 08:00 -> 14:00", c.getCurrentHour() == 14 && c.getCurrentDay() == day0);
        d.advanceToHour(6);
        check("aller a 6h : lendemain 06:00", c.getCurrentHour() == 6 && c.getCurrentMinute() == 0 && c.getCurrentDay() == day0 + 1);
        d.advanceToHour(6);
        check("deja 6h : on ne bouge pas", c.getCurrentHour() == 6 && c.getCurrentDay() == day0 + 1);
        String season0 = c.getDate().seasonName;
        d.advanceDays(28);
        check("+1 saison : la saison change", !c.getDate().seasonName.equals(season0) && c.getCurrentDay() == day0 + 29);
        d.advanceDays(112);
        check("+1 an : 112 jours de plus", c.getCurrentDay() == day0 + 141);

        // age
        int age0 = p.getAge(c.getCurrentDay());
        while (c.pollMessage() != null) { }
        d.ageBy(25);
        check("vieillir de 25 ans : age +25, message de palier en file", p.getAge(c.getCurrentDay()) == age0 + 25 && c.hasPendingMessage());
        check("et les effets de l'age s'appliquent (charge reduite)", p.getInventaire().getCapacityFactor() < 1.0);
        d.ageBy(-1000);
        check("rajeunir trop : age 0, pas de plantage", p.getAge(c.getCurrentDay()) == 0);

        // vitesse du temps
        GameClock c2 = GameSession.newGame("normale").getClock();
        c2.setTimeScale(60f);
        int m0 = c2.getCurrentHour() * 60 + c2.getCurrentMinute();
        c2.update(1f);
        int m1 = c2.getCurrentHour() * 60 + c2.getCurrentMinute();
        check("temps x60 : 1 s reelle = 60 min de jeu (" + (m1 - m0) + ")", m1 - m0 == 60);
        c2.setTimeScale(-5f);
        check("echelle de temps jamais negative", c2.getTimeScale() == 0f);

        org.junit.jupiter.api.Assertions.assertTrue(failed.isEmpty(), "Verifications en echec : " + failed);
    }

    static void check(String n, boolean ok) { total++; if (!ok) { fails++; failed.add(n); } System.out.println((ok ? "OK     " : "ECHEC  ") + n); }
}
