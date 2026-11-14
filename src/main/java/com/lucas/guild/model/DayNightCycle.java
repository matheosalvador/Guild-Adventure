package com.lucas.guild.model;

/**
 * Calcule l'éclairage selon l'heure de la journée : couleur du ciel, direction et couleur du
 * soleil (puis de la lune), lumière ambiante. Pur calcul, sans dépendance à LibGDX.
 *
 * En plein jour, les valeurs sont celles du décor d'origine (ciel bleu, soleil blanc à 0,8,
 * ambiante = valeur de base), la nuit tout devient sombre et bleuté, avec un passage orangé
 * au lever et au coucher du soleil.
 */
public final class DayNightCycle {

    private DayNightCycle() {
    }

    /** Résultat du calcul pour un instant donné. Les couleurs sont en [r, g, b] entre 0 et 1. */
    public static final class Light {
        /** 0 = nuit noire, 1 = plein jour. */
        public float daylight;
        /** Élévation du soleil : 1 = zénith, 0 = horizon, négatif = sous l'horizon. */
        public float elevation;
        public final float[] sky = new float[3];
        /** Couleur (intensité comprise) de la lumière directionnelle : soleil le jour, lune la nuit. */
        public final float[] lightColor = new float[3];
        /** Direction dans laquelle voyage la lumière (pas forcément normalisée). */
        public final float[] lightDirection = new float[3];
        public final float[] ambient = new float[3];
    }

    private static final float[] SKY_DAY = {0.5f, 0.8f, 1.0f};
    private static final float[] SKY_NIGHT = {0.02f, 0.03f, 0.08f};
    private static final float[] SKY_TWILIGHT = {1.0f, 0.55f, 0.28f};

    private static final float[] SUN_WHITE = {1f, 1f, 1f};
    private static final float[] SUN_WARM = {1f, 0.55f, 0.30f};
    private static final float SUN_INTENSITY = 0.8f;
    private static final float[] MOON = {0.17f, 0.21f, 0.38f};

    private static final float[] AMBIENT_NIGHT_TINT = {0.55f, 0.70f, 1.0f};

    /**
     * @param timeOfDay   heure de la journée, 0 à 24 (les minutes en fraction)
     * @param sunrise     heure du lever du soleil
     * @param sunset      heure du coucher du soleil
     * @param baseAmbient lumière ambiante en plein jour (0,4 en ville)
     */
    public static Light compute(double timeOfDay, double sunrise, double sunset, float baseAmbient) {
        double t = ((timeOfDay % 24) + 24) % 24;
        double dayLength = sunset - sunrise;

        double elevation;
        double sunPhase;      // 0 = lever, 1 = coucher
        boolean daytime = t >= sunrise && t <= sunset;
        if (daytime) {
            sunPhase = (t - sunrise) / dayLength;
            elevation = Math.sin(Math.PI * sunPhase);
        } else {
            double nightLength = 24 - dayLength;
            double sinceSunset = ((t - sunset) % 24 + 24) % 24;
            double nightPhase = sinceSunset / nightLength;
            elevation = -Math.sin(Math.PI * nightPhase);
            sunPhase = nightPhase < 0.5 ? 1.0 : 0.0; // juste après le coucher : à l'ouest ; avant l'aube : à l'est
        }

        float daylight = (float) smoothstep(-0.10, 0.30, elevation);
        float twilight = (float) Math.max(0.0, 1.0 - Math.abs(elevation) / 0.28);

        Light light = new Light();
        light.daylight = daylight;
        light.elevation = (float) elevation;

        // --- ciel
        for (int i = 0; i < 3; i++) {
            float sky = lerp(SKY_NIGHT[i], SKY_DAY[i], daylight);
            light.sky[i] = lerp(sky, SKY_TWILIGHT[i], twilight * 0.80f);
        }

        // --- lumière directionnelle : soleil le jour, lune la nuit, fondu entre les deux
        double theta = Math.PI * sunPhase;
        double sunX = Math.cos(theta) * 0.9;
        double sunY = 0.15 + 0.85 * Math.max(elevation, 0.0);
        double sunZ = 0.3;
        double moonX = -0.3, moonY = 0.8, moonZ = 0.2;
        light.lightDirection[0] = (float) lerp(-moonX, -sunX, daylight);
        light.lightDirection[1] = (float) lerp(-moonY, -sunY, daylight);
        light.lightDirection[2] = (float) lerp(-moonZ, -sunZ, daylight);

        float warm = Math.min(1f, twilight * 0.8f);
        for (int i = 0; i < 3; i++) {
            float sun = lerp(SUN_WHITE[i], SUN_WARM[i], warm) * SUN_INTENSITY * daylight;
            float moon = MOON[i] * (1f - daylight);
            light.lightColor[i] = sun + moon;
        }

        // --- ambiante : plus faible et bleutée la nuit
        float ambientScale = baseAmbient * (0.42f + 0.58f * daylight);
        for (int i = 0; i < 3; i++) {
            light.ambient[i] = ambientScale * lerp(AMBIENT_NIGHT_TINT[i], 1f, daylight);
        }
        return light;
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
