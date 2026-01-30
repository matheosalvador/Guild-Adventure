package model;

public class AttaqueMelee extends Attaque {

    public AttaqueMelee(String nom, int degats) {
        super(nom, degats);
    }

    @Override
    public void executer(Entite attaquant, Entite cible) {
        System.out.println(attaquant.getName() + " utilise " + nom + " sur " + cible.getName() + " !");
        if (cible instanceof Combatant) {
            ((Combatant) cible).subirDegats(degats);
        } else {
            // Fallback pour les entités qui ne seraient pas des combattants
            cible.perdreVie(degats);
            System.out.println(cible.getName() + " a subi " + degats + " dégâts.");
        }
    }
}