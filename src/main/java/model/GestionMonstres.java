package model;

import java.util.ArrayList;
import java.util.Iterator;

public class GestionMonstres {

    private ArrayList<Monstre> monstres;

    public GestionMonstres() {
        monstres = new ArrayList<>();
    }

    public void ajouterMonstre(Monstre monstre) {
        monstres.add(monstre);
    }

    public ArrayList<Monstre> getMonstres() {
        return monstres;
    }

    public void supprimerMonstresMorts() {
        Iterator<Monstre> iterator = monstres.iterator();

        while (iterator.hasNext()) {
            Monstre monstre = iterator.next();
            if (!monstre.estVivant()) {
                iterator.remove(); // suppression sûre
            }
        }
    }

    public boolean resteDesMonstres() {
        return !monstres.isEmpty();
    }
}
