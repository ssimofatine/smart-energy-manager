package com.smartenergy.model;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Lectura {

    private Long id;
    private LocalDate fechaHora;
    private Double consumoKwh;
    private Double costeEuros;
    private Dispositivo dispositivo;
}
