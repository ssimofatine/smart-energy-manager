package com.smartenergy.model;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Dispositivo {

    private Long id;
    private String nombre;
    private String tipo;
    private Double potenciaNominalW;
    private Boolean activo;
    private List<Lectura> lecturas;
}
