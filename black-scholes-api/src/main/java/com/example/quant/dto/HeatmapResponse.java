package com.example.quant.dto;

import java.util.List;

public record HeatmapResponse(
    double[][] data,
    List<Double> xLabels,
    List<Double> yLabels
) {}