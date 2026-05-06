package com.smartenergy.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.smartenergy.model.Lectura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class LecturaDAOImpl implements LecturaDAO {

    @Override
    public Lectura save(Lectura lectura) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(lectura);
            tx.commit();
            return lectura;
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
    public Lectura update(Lectura lectura) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Lectura merged = em.merge(lectura);
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
            Lectura lectura = em.find(Lectura.class, id);
            if (lectura != null) {
                em.remove(lectura);
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
    public Lectura findById(Long id) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Lectura lectura = em.find(Lectura.class, id);
            tx.commit();
            return lectura;
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
    public List<Lectura> findAll() {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<Lectura> lecturas = em
                    .createQuery("SELECT l FROM Lectura l JOIN FETCH l.dispositivo", Lectura.class)
                    .getResultList();
            tx.commit();
            return lecturas;
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
    public List<Lectura> findByDispositivo(Long dispositivoId) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<Lectura> lecturas = em.createQuery(
                            "SELECT l FROM Lectura l JOIN FETCH l.dispositivo WHERE l.dispositivo.id = :dispositivoId ORDER BY l.fechaHora",
                            Lectura.class)
                    .setParameter("dispositivoId", dispositivoId)
                    .getResultList();
            tx.commit();
            return lecturas;
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
    public List<Lectura> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<Lectura> lecturas = em.createQuery(
                            "SELECT l FROM Lectura l JOIN FETCH l.dispositivo WHERE l.fechaHora BETWEEN :desde AND :hasta ORDER BY l.fechaHora",
                            Lectura.class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .getResultList();
            tx.commit();
            return lecturas;
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
    public Double calcularConsumoTotalPorDispositivo(Long dispositivoId) {
        EntityManager em = JpaManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Double total = em.createQuery(
                            "SELECT COALESCE(SUM(l.consumoKwh), 0) FROM Lectura l WHERE l.dispositivo.id = :dispositivoId",
                            Double.class)
                    .setParameter("dispositivoId", dispositivoId)
                    .getSingleResult();
            tx.commit();
            return total;
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
