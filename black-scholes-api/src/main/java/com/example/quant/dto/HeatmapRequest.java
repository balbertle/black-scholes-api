package com.example.quant.dto;

public record HeatmapRequest(
    // Parameters
    double strike,
    double riskFreeRate,
    double volatility,

    // Axes
    String xAxisVariable,
    Range xAxisRange,
    String yAxisVariable,
    Range yAxisRange,
    
    String dataType // input
) {
    public record Range(double start, double end, int steps) {}
}

