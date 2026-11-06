package model;
// a neutraliser plus tard si inutile
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Gère une liste de monstres.
 */
public class GestionMonstres {

    private ArrayList<Monstre> monstres;

    /**
     * Constructeur de la gestion des monstres.
     */
    public GestionMonstres() {
        monstres = new ArrayList<>();
    }

    /**
     * Ajoute un monstre à la liste.
     * @param monstre Le monstre à ajouter.
     */
    public void ajouterMonstre(Monstre monstre) {
        monstres.add(monstre);
    }

    /**
     * Récupère la liste des monstres.
     * @return La liste des monstres.
     */
    public ArrayList<Monstre> getMonstres() {
        return monstres;
    }

    /**
     * Supprime les monstres morts de la liste.
     */
    public void supprimerMonstresMorts() {
        Iterator<Monstre> iterator = monstres.iterator();

        while (iterator.hasNext()) {
            Monstre monstre = iterator.next();
            if (!monstre.estVivant()) {
                iterator.remove(); // suppression sûre
            }
        }
    }

    /**
     * Vérifie s'il reste des monstres vivants.
     * @return true s'il reste des monstres, false sinon.
     */
    public boolean resteDesMonstres() {
        return !monstres.isEmpty();
    }
}
