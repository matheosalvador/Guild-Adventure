package com.lucas.guild.model;

public class Monstre extends Entite {

    private int xpValue;
    private String imagePath;

    public Monstre(String name, int maxHealth, int xpValue, String imagePath) {
        super(name, maxHealth);
        this.xpValue = xpValue;
        this.imagePath = imagePath;
        
        // Apprend ses attaques en fonction de son nom
        switch (name) {
            case "Rat Géant":
                this.apprendreAttaque(new AttaqueSimple("Morsure", 8));
                this.apprendreAttaque(new AttaqueSimple("Griffe", 6));
                break;
            case "Chef Gobelin":
                this.apprendreAttaque(new AttaqueSimple("Coup de gourdin", 15));
                this.apprendreAttaque(new AttaqueSimple("Cri de guerre", 5));
                break;
        }
    }

    public Item getLoot() {
        switch (this.name) {
            case "Rat Géant":
                return new ObjetDeQuete("Queue de rat");
            case "Chef Gobelin":
                return new ObjetDeQuete("Tête de Gobelin");
            default:
                return new ObjetDeQuete("Tissu sans valeur");
        }
    }
    
    public int getXpValue() {
        return xpValue;
    }

    public String getImagePath() {
        return imagePath;
    }

    @Override
    public void updateVitalSigns() {

    }
}
