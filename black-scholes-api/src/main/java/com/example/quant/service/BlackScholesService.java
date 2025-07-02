package com.example.quant.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.quant.dto.HeatmapRequest;
import com.example.quant.dto.HeatmapResponse;

@Service
public class BlackScholesService {

    // --- Core Black-Scholes Calculations ---

    // Cumulative Distribution Function (CDF) for the standard normal distribution
    // All from Abramowitz and Stegun... I have no idea how this math works as of now
    private double cdf(double x) {
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(x)); // Abramowitz and Stegun 
        double d = 0.3989423 * Math.exp(-x * x / 2.0); // PDF
        double prob = d * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274)))); // 5th order polynomial from Abramowitz and Stegun 26.2.17
        return (x > 0) ? 1.0 - prob : prob;
    }

    // Probability Density Function (PDF) for the standard normal distribution
    private double pdf(double x) {
        return (1.0 / Math.sqrt(2.0 * Math.PI)) * Math.exp(-0.5 * x * x);
    }
    
    //d1 and d2 factors for b-s formula

    private double[] calculateD1D2(double S, double K, double T, double r, double v) {
        double d1 = (Math.log(S / K) + (r + v * v / 2.0) * T) / (v * Math.sqrt(T));
        double d2 = d1 - v * Math.sqrt(T);
        return new double[]{d1, d2};
    }


    // -- Black-Scholes --
    public double calculateCallPrice(double S, double K, double T, double r, double v) {
        if (T <= 0) return Math.max(0, S - K);
        double[] d = calculateD1D2(S, K, T, r, v);
        return S * cdf(d[0]) - K * Math.exp(-r * T) * cdf(d[1]);
    }

    public double calculatePutPrice(double S, double K, double T, double r, double v) {
        if (T <= 0) return Math.max(0, K - S);
        double[] d = calculateD1D2(S, K, T, r, v);
        return K * Math.exp(-r * T) * cdf(-d[1]) - S * cdf(-d[0]);
    }
    
    // --- Greeks ---

    public double calculateDelta(String optionType, double S, double K, double T, double r, double v) {
        if (T <= 0) return "call".equalsIgnoreCase(optionType) ? (S > K ? 1 : 0) : (S < K ? -1 : 0);
        double d1 = calculateD1D2(S, K, T, r, v)[0];
        if ("call".equalsIgnoreCase(optionType)) {
            return cdf(d1);
        } else { // put
            return cdf(d1) - 1;
        }
    }

    public double calculateGamma(double S, double K, double T, double r, double v) {
        if (T <= 0) return 0;
        double d1 = calculateD1D2(S, K, T, r, v)[0];
        return pdf(d1) / (S * v * Math.sqrt(T));
    }

    public double calculateVega(double S, double K, double T, double r, double v) {
        if (T <= 0) return 0;
        double d1 = calculateD1D2(S, K, T, r, v)[0];
        return S * pdf(d1) * Math.sqrt(T) * 0.01; // Per 1% change in vol
    }

    public double calculateTheta(String optionType, double S, double K, double T, double r, double v) {
        if (T <= 0) return 0;
        double[] d = calculateD1D2(S, K, T, r, v);
        double term1 = -(S * pdf(d[0]) * v) / (2 * Math.sqrt(T));
        if ("call".equalsIgnoreCase(optionType)) {
            double term2 = r * K * Math.exp(-r * T) * cdf(d[1]);
            return (term1 - term2) / 365.0; // Per day
        } else { // put
            double term2 = r * K * Math.exp(-r * T) * cdf(-d[1]);
            return (term1 + term2) / 365.0; // Per day
        }
    }
    
    // --- Heatmap Generation ---
    
    public HeatmapResponse generateHeatmapData(HeatmapRequest request) {
        int xSteps = request.xAxisRange().steps();
        int ySteps = request.yAxisRange().steps();
        
        double[][] data = new double[ySteps][xSteps];
        List<Double> xLabels = new ArrayList<>();
        List<Double> yLabels = new ArrayList<>();

        double xStepSize = (request.xAxisRange().end() - request.xAxisRange().start()) / (xSteps - 1);
        double yStepSize = (request.yAxisRange().end() - request.yAxisRange().start()) / (ySteps - 1);

        for (int i = 0; i < ySteps; i++) {
            double yValue = request.yAxisRange().start() + i * yStepSize;
            yLabels.add(yValue);
            
            for (int j = 0; j < xSteps; j++) {
                double xValue = request.xAxisRange().start() + j * xStepSize;
                if (i == 0) {
                    xLabels.add(xValue);
                }

                // Dynamically assign variables based on request
                double S = "spotPrice".equals(request.xAxisVariable()) ? xValue : ("spotPrice".equals(request.yAxisVariable()) ? yValue : 100); // Default S
                double T = "timeToMaturity".equals(request.xAxisVariable()) ? xValue : ("timeToMaturity".equals(request.yAxisVariable()) ? yValue : 1.0); // Default T
                double K = request.strike();
                double r = request.riskFreeRate();
                double v = request.volatility();

                // Ensure time to maturity is not zero or negative for most calculations
                if (T <= 1e-6) T = 1e-6;

                data[i][j] = calculateValue(request.dataType(), S, K, T, r, v);
            }
        }

        return new HeatmapResponse(data, xLabels, yLabels);
    }

    private double calculateValue(String dataType, double S, double K, double T, double r, double v) {
        return switch (dataType) {
            case "callPrice" -> calculateCallPrice(S, K, T, r, v);
            case "putPrice" -> calculatePutPrice(S, K, T, r, v);
            case "deltaCall" -> calculateDelta("call", S, K, T, r, v);
            case "deltaPut" -> calculateDelta("put", S, K, T, r, v);
            case "gamma" -> calculateGamma(S, K, T, r, v);
            case "vega" -> calculateVega(S, K, T, r, v);
            case "thetaCall" -> calculateTheta("call", S, K, T, r, v);
            case "thetaPut" -> calculateTheta("put", S, K, T, r, v);
            default -> throw new IllegalArgumentException("Invalid data type: " + dataType);
        };
    }
}
