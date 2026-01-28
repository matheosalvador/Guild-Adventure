import model.*;

public class Main {
    public static void main(String[] args) {
        /*model.Joueur joueur1 = new model.Joueur("Bobby");
        joueur1.getHealth();
        joueur1.getStamina();
        joueur1.getName();
        joueur1.setName("Bob");
        joueur1.setHealth(15);
        joueur1.setStamina(15);
        System.out.println(joueur1.getHealth());
        System.out.println(joueur1.getStamina());
        System.out.println(joueur1.getName());*/

        // Initialisation du Joueur et du Monstre
        Joueur joueur = new Joueur("Arthur");
        Monstre monstre = new Monstre("Gobelin");

        // Initialisation des Potions
        PotionDps dps = new PotionDps(monstre);
        PotionSoin soin = new PotionSoin(joueur);
        PotionMix mix = new PotionMix(joueur, monstre);

        // Initialisation des Armes
        AttaqueMelee coup = new AttaqueMelee("Coup d'épée", 15);
        AttaqueDistance fireball = new AttaqueDistance("FireBall", 300, 2);


//        joueur.perdreVie(50); test pour potion
        System.out.println(monstre.getHealth()); // 200 - 30 = 170
        System.out.println(joueur.getHealth()); // 200 - 30 = 170

        fireball.executer(monstre, monstre); // ⚠️on peut se suicide ⚠️
        fireball.executer(joueur, joueur); // ⚠️on peut se suicide ⚠️
        coup.executer(joueur, monstre);
        coup.executer(monstre, joueur);
        //dps.use();   // Gobelin perd 30 PV
        //soin.use();  // Arthur gagne 20 PV

        System.out.println(monstre.getHealth()); // 200 - 30 = 170
        System.out.println(monstre.getHealth()); // 200 - 30 = 170

        //mix.use();   // Gobelin perd 30 PV et Arthur gagne 20 PV

        System.out.println(monstre.getHealth()); // 170 - 30 = 140
        System.out.println(joueur.getHealth());  // 70 + 20 = 90
        System.out.println(monstre.estVivant());
        System.out.println(joueur.estVivant());

    }
}
