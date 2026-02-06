package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;

public class Town {
    private String name;
    private List<Building> buildings;

    public Town(String name) {
        this.name = name;
        this.buildings = new ArrayList<>();
    }

    public void addBuilding(Building building) {
        buildings.add(building);
    }

    public String getName() {
        return name;
    }

    public List<Building> getBuildings() {
        return buildings;
    }
}
