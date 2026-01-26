public class Joueur {

    private String name;
    private Integer health;
    private Integer stamina;

    public Joueur(String name) {
        this.name = name;
        health = 100;
        stamina = 100;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getStamina() {
        return stamina;
    }

    public void setStamina(Integer stamina) {
        this.stamina = stamina;
    }
}
