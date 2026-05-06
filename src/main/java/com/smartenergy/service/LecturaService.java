package com.smartenergy.service;

import java.time.LocalDateTime;
import java.util.List;

import com.smartenergy.dao.LecturaDAO;
import com.smartenergy.dao.LecturaDAOImpl;
import com.smartenergy.model.Lectura;

public class LecturaService {

    private final LecturaDAO lecturaDAO;

    public LecturaService() {
        this(new LecturaDAOImpl());
    }

    public LecturaService(LecturaDAO lecturaDAO) {
        this.lecturaDAO = lecturaDAO;
    }

    public Lectura save(Lectura lectura) {
        return lecturaDAO.save(lectura);
    }

    public Lectura update(Lectura lectura) {
        return lecturaDAO.update(lectura);
    }

    public void delete(Long id) {
        lecturaDAO.delete(id);
    }

    public Lectura findById(Long id) {
        return lecturaDAO.findById(id);
    }

    public List<Lectura> findAll() {
        return lecturaDAO.findAll();
    }

    public List<Lectura> findByDispositivo(Long dispositivoId) {
        return lecturaDAO.findByDispositivo(dispositivoId);
    }

    public List<Lectura> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta) {
        return lecturaDAO.findByFechaBetween(desde, hasta);
    }

    public Double calcularConsumoTotalPorDispositivo(Long dispositivoId) {
        return lecturaDAO.calcularConsumoTotalPorDispositivo(dispositivoId);
    }
}
