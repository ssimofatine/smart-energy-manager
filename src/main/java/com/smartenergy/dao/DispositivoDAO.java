package com.smartenergy.dao;

import java.util.List;

import com.smartenergy.model.Dispositivo;

public interface DispositivoDAO {

    Dispositivo save(Dispositivo dispositivo);

    Dispositivo update(Dispositivo dispositivo);

    void delete(Long id);

    Dispositivo findById(Long id);

    List<Dispositivo> findAll();
}
