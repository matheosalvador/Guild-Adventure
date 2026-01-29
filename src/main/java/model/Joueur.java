package model;

public class Joueur extends Entite implements Combatant {

    private int stamina;
    private int maxStamina;
    private int level; // pour getLevel/setLevel

    public Joueur(String name) {
        super(name, 100); // santé max = 100 par défaut
        this.stamina = 100;
        this.maxStamina = 100;
        this.level = 1;
    }

    // --- Méthodes abstraites de Entite ---
    @Override
    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(1, level); // niveau minimum 1
    }

    // --- Stamina ---
    public int getStamina() { return stamina; }
    public int getMaxStamina() { return maxStamina; }
    public void ajouterStamina(int montant) {
        if (montant <= 0) return;
        stamina = Math.min(stamina + montant, maxStamina);
    }
    public void perdreStamina(int montant) {
        if (montant <= 0) return;
        stamina = Math.max(stamina - montant, 0);
    }

    // --- Combat ---
    @Override
    public void attaquer(Combatant cible) {
        int degats = 15;
        System.out.println(this.getName() + " attaque !");
        cible.subirDegats(degats);
    }

    @Override
    public void subirDegats(int montant) {
        this.perdreVie(montant);
        System.out.println(this.getName() + " a subi " + montant + " dégâts !");
    }
}
