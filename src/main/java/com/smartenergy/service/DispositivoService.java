package com.smartenergy.service;

import java.util.List;

import com.smartenergy.dao.DispositivoDAO;
import com.smartenergy.dao.DispositivoDAOImpl;
import com.smartenergy.model.Dispositivo;

public class DispositivoService {

    private final DispositivoDAO dispositivoDAO;

    public DispositivoService() {
        this(new DispositivoDAOImpl());
    }

    public DispositivoService(DispositivoDAO dispositivoDAO) {
        this.dispositivoDAO = dispositivoDAO;
    }

    public Dispositivo save(Dispositivo dispositivo) {
        return dispositivoDAO.save(dispositivo);
    }

    public Dispositivo update(Dispositivo dispositivo) {
        return dispositivoDAO.update(dispositivo);
    }

    public void delete(Long id) {
        dispositivoDAO.delete(id);
    }

    public Dispositivo findById(Long id) {
        return dispositivoDAO.findById(id);
    }

    public List<Dispositivo> findAll() {
        return dispositivoDAO.findAll();
    }
}
