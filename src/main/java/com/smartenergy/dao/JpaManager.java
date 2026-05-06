package com.smartenergy.dao;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class JpaManager {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("smartEnergyPU");

    private JpaManager() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMF;
    }
}
