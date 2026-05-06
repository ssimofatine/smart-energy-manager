package com.smartenergy.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.smartenergy.model.Dispositivo;
import com.smartenergy.model.Lectura;

public class DataInitializer {

    private static final double PRICE_PER_KWH_EUR = 0.22;

    private final DispositivoService dispositivoService;
    private final LecturaService lecturaService;

    public DataInitializer() {
        this(new DispositivoService(), new LecturaService());
    }

    public DataInitializer(DispositivoService dispositivoService, LecturaService lecturaService) {
        this.dispositivoService = dispositivoService;
        this.lecturaService = lecturaService;
    }

    public void initializeIfEmpty() {
        if (!dispositivoService.findAll().isEmpty()) {
            return;
        }

        List<Dispositivo> dispositivos = crearDispositivosEjemplo();
        for (Dispositivo dispositivo : dispositivos) {
            dispositivoService.save(dispositivo);
        }

        insertarLecturasUltimosSieteDias(dispositivos);
    }

    private List<Dispositivo> crearDispositivosEjemplo() {
        List<Dispositivo> dispositivos = new ArrayList<>();

        dispositivos.add(crearDispositivo("Frigorifico Cocina", "Frigorifico", 150.0));
        dispositivos.add(crearDispositivo("Aire Acondicionado Salon", "Climatizacion", 2200.0));
        dispositivos.add(crearDispositivo("Lavadora", "Electrodomestico", 500.0));
        dispositivos.add(crearDispositivo("Televisor OLED", "Entretenimiento", 120.0));

        return dispositivos;
    }

    private Dispositivo crearDispositivo(String nombre, String tipo, Double potenciaNominalW) {
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setNombre(nombre);
        dispositivo.setTipo(tipo);
        dispositivo.setPotenciaNominalW(potenciaNominalW);
        dispositivo.setActivo(true);
        return dispositivo;
    }

    private void insertarLecturasUltimosSieteDias(List<Dispositivo> dispositivos) {
        LocalDateTime inicio = LocalDateTime.now().minusDays(7);
        int intervalHours = 8;

        for (int index = 0; index < dispositivos.size(); index++) {
            Dispositivo dispositivo = dispositivos.get(index);
            double consumoHoraBase = consumoHoraPorTipo(index);

            for (int i = 0; i < 5; i++) {
                LocalDateTime fechaHora = inicio.plusHours((long) i * intervalHours + index);
                double variacion = 1.0 + (i - 2) * 0.06;
                double consumoKwh = round(consumoHoraBase * variacion);
                double costeEuros = round(consumoKwh * PRICE_PER_KWH_EUR);

                Lectura lectura = new Lectura();
                lectura.setDispositivo(dispositivo);
                lectura.setFechaHora(fechaHora);
                lectura.setConsumoKwh(consumoKwh);
                lectura.setCosteEuros(costeEuros);

                lecturaService.save(lectura);
            }
        }
    }

    private double consumoHoraPorTipo(int index) {
        return switch (index) {
            case 0 -> 0.15; // Frigorifico ~0.15 kWh/h
            case 1 -> 2.20; // Aire acondicionado ~2.2 kWh/h
            case 2 -> 0.50; // Lavadora en uso medio
            case 3 -> 0.10; // Televisor eficiente
            default -> 0.20;
        };
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
