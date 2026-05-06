package com.smartenergy.controller;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import com.smartenergy.model.Lectura;
import com.smartenergy.service.DispositivoService;
import com.smartenergy.service.LecturaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

public class DashboardController implements Initializable {

    private static final Locale ES_LOCALE = Locale.forLanguageTag("es-ES");

    private final LecturaService lecturaService = new LecturaService();
    private final DispositivoService dispositivoService = new DispositivoService();

    @FXML
    private Label totalKwhMesLabel;

    @FXML
    private Label costeTotalMesLabel;

    @FXML
    private Label dispositivosActivosLabel;

    @FXML
    private BarChart<String, Number> consumoBarChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        runAsync(() -> {
            return fetchDashboardData();
        }, datos -> {
            renderDashboard(datos);
        });
    }

    private DashboardData fetchDashboardData() {
        YearMonth mesActual = YearMonth.now();
        LocalDateTime inicioDelMes = mesActual.atDay(1).atStartOfDay();
        LocalDateTime finDelMes = mesActual.atEndOfMonth().atTime(23, 59, 59);

        List<Lectura> lecturasDelMes = lecturaService.findByFechaBetween(inicioDelMes, finDelMes);

        double sumaEnergia = 0.0;
        double sumaDinero = 0.0;

        for (int i = 0; i < lecturasDelMes.size(); i++) {
            Lectura lec = lecturasDelMes.get(i);
            sumaEnergia = sumaEnergia + safe(lec.getConsumoKwh());
            sumaDinero = sumaDinero + safe(lec.getCosteEuros());
        }

        long dispositivosEncendidos = 0;
        for (var dispositivo : dispositivoService.findAll()) {
            if (dispositivo.getActivo() != null && dispositivo.getActivo() == true) {
                dispositivosEncendidos++;
            }
        }

        LocalDate hoy = LocalDate.now();
        LocalDate haceSeisDias = hoy.minusDays(6);
        LocalDateTime fechaDesde = haceSeisDias.atStartOfDay();
        LocalDateTime fechaHasta = hoy.atTime(23, 59, 59);

        List<Lectura> lecturasSemana = lecturaService.findByFechaBetween(fechaDesde, fechaHasta);
        Map<LocalDate, Double> mapaDias = new LinkedHashMap<>();

        LocalDate diaBucle = haceSeisDias;
        while (diaBucle.isBefore(hoy) || diaBucle.isEqual(hoy)) {
            mapaDias.put(diaBucle, 0.0);
            diaBucle = diaBucle.plusDays(1);
        }

        for (int i = 0; i < lecturasSemana.size(); i++) {
            Lectura lectura = lecturasSemana.get(i);
            if (lectura.getFechaHora() != null) {
                LocalDate fechaDeLaLectura = lectura.getFechaHora().toLocalDate();
                if (mapaDias.containsKey(fechaDeLaLectura)) {
                    double valorViejo = mapaDias.get(fechaDeLaLectura);
                    double valorNuevo = safe(lectura.getConsumoKwh());
                    mapaDias.put(fechaDeLaLectura, valorViejo + valorNuevo);
                }
            }
        }

        DashboardData misDatos = new DashboardData(sumaEnergia, sumaDinero, dispositivosEncendidos, mapaDias);
        return misDatos;
    }

    private void renderDashboard(DashboardData data) {
        String textoKwh = String.format("%.3f kWh", data.getTotalKwhMes());
        totalKwhMesLabel.setText(textoKwh);

        String textoCoste = String.format("%.2f EUR", data.getTotalCosteMes());
        costeTotalMesLabel.setText(textoCoste);

        String textoDispositivos = String.valueOf(data.getDispositivosActivos());
        dispositivosActivosLabel.setText(textoDispositivos);

        XYChart.Series<String, Number> serieGrafico = new XYChart.Series<>();
        serieGrafico.setName("Consumo diario (kWh)");

        for (Map.Entry<LocalDate, Double> fila : data.getTotalPorDia().entrySet()) {
            LocalDate fechaDia = fila.getKey();
            Double valorConsumo = fila.getValue();

            DayOfWeek diaSemana = fechaDia.getDayOfWeek();
            String nombreDia = diaSemana.getDisplayName(TextStyle.FULL, ES_LOCALE);

            String diaMayuscula = capitalize(nombreDia);
            double valorRedondeado = round3(valorConsumo);

            XYChart.Data<String, Number> datosGrafico = new XYChart.Data<>(diaMayuscula, valorRedondeado);
            serieGrafico.getData().add(datosGrafico);
        }

        consumoBarChart.getData().clear();
        consumoBarChart.getData().add(serieGrafico);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String primeraLetra = value.substring(0, 1).toUpperCase(ES_LOCALE);
        String restoPalabra = value.substring(1);
        return primeraLetra + restoPalabra;
    }

    private double safe(Double value) {
        if (value == null) {
            return 0.0;
        } else {
            return value;
        }
    }

    private double round3(double value) {
        double multiplicado = value * 1000.0;
        double redondeado = Math.round(multiplicado);
        return redondeado / 1000.0;
    }

    private <T> void runAsync(Callable<T> task, java.util.function.Consumer<T> onSuccess) {
        Thread hilo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    T resultado = task.call();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            onSuccess.accept(resultado);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            totalKwhMesLabel.setText("-");
                            costeTotalMesLabel.setText("-");
                            dispositivosActivosLabel.setText("-");
                        }
                    });
                }
            }
        });
        hilo.start();
    }

    private class DashboardData {
        private double totalKwhMes;
        private double totalCosteMes;
        private long dispositivosActivos;
        private Map<LocalDate, Double> totalPorDia;

        public DashboardData(double totalKwhMes, double totalCosteMes, long dispositivosActivos, Map<LocalDate, Double> totalPorDia) {
            this.totalKwhMes = totalKwhMes;
            this.totalCosteMes = totalCosteMes;
            this.dispositivosActivos = dispositivosActivos;
            this.totalPorDia = totalPorDia;
        }

        public double getTotalKwhMes() {
            return totalKwhMes;
        }

        public double getTotalCosteMes() {
            return totalCosteMes;
        }

        public long getDispositivosActivos() {
            return dispositivosActivos;
        }

        public Map<LocalDate, Double> getTotalPorDia() {
            return totalPorDia;
        }
    }
}