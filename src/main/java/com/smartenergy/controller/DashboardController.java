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
import java.util.concurrent.CompletableFuture;

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
        runAsync(this::fetchDashboardData, this::renderDashboard);
    }

    private DashboardData fetchDashboardData() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Lectura> lecturasMes = lecturaService.findByFechaBetween(startMonth, endMonth);
        double totalKwh = lecturasMes.stream().mapToDouble(l -> safe(l.getConsumoKwh())).sum();
        double totalCoste = lecturasMes.stream().mapToDouble(l -> safe(l.getCosteEuros())).sum();
        long activos = dispositivoService.findAll().stream().filter(d -> Boolean.TRUE.equals(d.getActivo())).count();
        
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = today.atTime(23, 59, 59);

        List<Lectura> lecturas = lecturaService.findByFechaBetween(from, to);
        Map<LocalDate, Double> totalPorDia = new LinkedHashMap<>();
        LocalDate pointer = start;
        while (!pointer.isAfter(today)) {
            totalPorDia.put(pointer, 0.0);
            pointer = pointer.plusDays(1);
        }

        for (Lectura lectura : lecturas) {
            if (lectura.getFechaHora() == null) {
                continue;
            }
            LocalDate date = lectura.getFechaHora().toLocalDate();
            if (totalPorDia.containsKey(date)) {
                totalPorDia.put(date, totalPorDia.get(date) + safe(lectura.getConsumoKwh()));
            }
        }
        return new DashboardData(totalKwh, totalCoste, activos, totalPorDia);
    }

    private void renderDashboard(DashboardData data) {
        totalKwhMesLabel.setText(String.format("%.3f kWh", data.totalKwhMes()));
        costeTotalMesLabel.setText(String.format("%.2f EUR", data.totalCosteMes()));
        dispositivosActivosLabel.setText(String.valueOf(data.dispositivosActivos()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consumo diario (kWh)");
        for (Map.Entry<LocalDate, Double> entry : data.totalPorDia().entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey().getDayOfWeek();
            String label = dayOfWeek.getDisplayName(TextStyle.FULL, ES_LOCALE);
            series.getData().add(new XYChart.Data<>(capitalize(label), round3(entry.getValue())));
        }

        consumoBarChart.getData().clear();
        consumoBarChart.getData().add(series);
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(ES_LOCALE) + value.substring(1);
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private <T> void runAsync(Callable<T> task, java.util.function.Consumer<T> onSuccess) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        totalKwhMesLabel.setText("-");
                        costeTotalMesLabel.setText("-");
                        dispositivosActivosLabel.setText("-");
                    });
                    return null;
                });
    }

    private record DashboardData(double totalKwhMes, double totalCosteMes, long dispositivosActivos,
                                 Map<LocalDate, Double> totalPorDia) {
    }
}
