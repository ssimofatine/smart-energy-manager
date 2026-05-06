package com.smartenergy.dao;

import java.util.List;

import com.smartenergy.model.Dispositivo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class DispositivoDAOImpl implements DispositivoDAO {

    @Override
    public Dispositivo save(Dispositivo dispositivo) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(dispositivo);
            tx.commit();
            return dispositivo;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    @Override
    public Dispositivo update(Dispositivo dispositivo) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Dispositivo merged = em.merge(dispositivo);
            tx.commit();
            return merged;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    @Override
    public void delete(Long id) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Dispositivo dispositivo = em.find(Dispositivo.class, id);
            if (dispositivo != null) {
                em.remove(dispositivo);
            }
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    @Override
    public Dispositivo findById(Long id) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Dispositivo dispositivo = em.find(Dispositivo.class, id);
            tx.commit();
            return dispositivo;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Dispositivo> findAll() {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<Dispositivo> dispositivos = em
                    .createQuery("SELECT d FROM Dispositivo d", Dispositivo.class)
                    .getResultList();
            tx.commit();
            return dispositivos;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}
