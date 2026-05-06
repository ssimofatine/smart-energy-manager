package com.smartenergy.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.smartenergy.model.Lectura;

public interface LecturaDAO {

    Lectura save(Lectura lectura);

    Lectura update(Lectura lectura);

    void delete(Long id);

    Lectura findById(Long id);

    List<Lectura> findAll();

    List<Lectura> findByDispositivo(Long dispositivoId);

    List<Lectura> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    Double calcularConsumoTotalPorDispositivo(Long dispositivoId);
}
