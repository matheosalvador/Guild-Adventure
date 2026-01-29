package model;

/**
 * Représente un monstre dans le jeu.
 */
public class Monstre extends Entite implements Combatant {

    private int cooldownInvocation = 0;
    private static final int COOLDOWN_MAX = 3;

    public Monstre(String name) {
        super(name, 200);
    }

    public boolean peutInvoquer() {
        return cooldownInvocation == 0;
    }

    public void decrementerCooldownInvocation() {
        if (cooldownInvocation > 0) {
            cooldownInvocation--;
        }
    }

    public void invoquer(GestionMonstres gestion) {
        if (!peutInvoquer()) return;

        System.out.println(this.name + " invoque des renforts !");

        for (int i = 1; i <= 3; i++) {
            Monstre invoque = new Monstre("Sbire " + i);
            invoque.health = 50;      // plus faible
            invoque.maxHealth = 50;
            gestion.ajouterMonstre(invoque);
            System.out.println("→ " + invoque.getName() + " rejoint le combat !");
        }

        cooldownInvocation = COOLDOWN_MAX;
    }

    @Override
    public void attaquer(Combatant cible) {
        if (this.estVivant()) {
            int degats = 10;
            System.out.println(this.getName() + " attaque !");
            cible.subirDegats(degats);
        }
    }

    @Override
    public void subirDegats(int montant) {
        perdreVie(montant);
        System.out.println(this.getName() + " subit " + montant + " dégâts.");

        if (!estVivant()) {
            System.out.println(this.getName() + " est mort !");
        }
    }

    @Override
    public void setHealth(int health) {

    }

    @Override
    public void setLevel(int level) {

    }

    @Override
    public int getLevel() {
        return 0;
    }
}
