package com.lucas.guild.libgdx;

import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucas.guild.data.SkillData;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.Skills;

/**
 * Le panneau du personnage (touche K) : qui il est (classe, origine, âge) et ses compétences,
 * avec ce qui les fait progresser. Une compétence jamais pratiquée n'est pas débloquée.
 */
class SkillsPanel {

    private final Table table;
    private final Skin skin;
    private final Adventurer player;
    private final GameClock clock;

    SkillsPanel(Table table, Skin skin, Adventurer player, GameClock clock) {
        this.table = table;
        this.skin = skin;
        this.player = player;
        this.clock = clock;
    }

    void rebuild() {
        table.clear();
        table.top().left();

        table.add(UiKit.label(skin, "Personnage")).left().pad(10).row();

        String who = player.getName();
        if (player.getClassLabel() != null) who += " - " + player.getClassLabel();
        if (player.getBackgroundLabel() != null) who += " - " + player.getBackgroundLabel();
        table.add(UiKit.label(skin, who)).left().padLeft(10).row();

        String age = player.getAge(clock.getCurrentDay()) + " ans";
        if (player.getAgingLabel() != null) age += " (" + player.getAgingLabel() + ")";
        table.add(UiKit.label(skin, age + " - PV " + player.getHealth() + " / " + player.getMaxHealth()
                + String.format(Locale.FRANCE, " - vitesse %.0f %%", player.getSpeedFactor() * 100))).left().padLeft(10).row();

        table.add(UiKit.label(skin, "-- Compétences --")).left().pad(10).row();
        Skills skills = player.getSkills();
        for (SkillData def : skills.definitions()) {
            skillRows(skills, def);
        }
        table.add(UiKit.label(skin, "Les compétences progressent en les pratiquant. (K ou Échap pour fermer)")).left().pad(10).row();
    }

    private void skillRows(Skills skills, SkillData def) {
        int level = skills.getLevel(def.id);
        double practice = skills.getPractice(def.id);

        String title;
        if (level == 0) {
            title = def.label + " - pas encore débloquée";
        } else {
            title = def.label + " - niveau " + level + " / " + def.maxLevel();
        }
        Label titleLabel = UiKit.label(skin, title);
        if (level == 0) {
            titleLabel.setColor(Color.GRAY);
        }
        table.add(titleLabel).left().padLeft(10).padTop(6).row();

        // progression vers le niveau suivant
        String progress;
        if (level >= def.maxLevel()) {
            progress = "Niveau maximum.";
        } else {
            progress = String.format(Locale.FRANCE, "Prochain niveau : %s / %s %s", number(practice),
                    number(def.thresholds.get(level)), def.unit);
        }
        table.add(UiKit.label(skin, "   " + def.trainedBy + " - " + progress)).left().padLeft(10).row();

        // effets : ceux gagnés maintenant, ou ceux que donnera le premier niveau
        StringBuilder effects = new StringBuilder();
        for (Map.Entry<String, Double> effect : def.effects.entrySet()) {
            String[] info = SkillData.EFFECTS.get(effect.getKey());
            double value = effect.getValue() * Math.max(level, 1);
            if (effects.length() > 0) effects.append(", ");
            effects.append(info[0]).append(" +").append(number(value)).append(info[1]);
        }
        Label effectLabel = UiKit.label(skin, "   " + (level == 0 ? "Au niveau 1 : " : "Effet : ") + effects);
        effectLabel.setColor(level == 0 ? Color.GRAY : Color.LIGHT_GRAY);
        table.add(effectLabel).left().padLeft(10).row();
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.format(Locale.FRANCE, "%.0f", value) : String.format(Locale.FRANCE, "%.1f", value);
    }
}
