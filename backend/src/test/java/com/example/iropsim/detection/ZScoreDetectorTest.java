package com.example.iropsim.detection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class ZScoreDetectorTest {

    private ZScoreDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ZScoreDetector();
    }

    @Test
    void testNormalData_NoAnomaly() {
        // Test with normal data following a normal distribution
        List<Double> normalData = Arrays.asList(
            2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9,
            2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.0
        );

        DetectionResult result = detector.detect(normalData);

        assertFalse(result.isAnomaly());
    }

    @Test
    void testOutlier_Anomaly() {
        // Test with a clear outlier
        List<Double> outlierData = Arrays.asList(
            2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9,
            2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 10.0 // Outlier
        );

        DetectionResult result = detector.detect(outlierData);

        assertTrue(result.isAnomaly());
        assertTrue(result.getSeverity().equals("WARN") || result.getSeverity().equals("CRITICAL"));
        assertTrue(result.getScore() > 3.0); // Z-score should be > 3
        assertNotNull(result.getEvidence());
    }

    @Test
    void testInsufficientData_NoAnomaly() {
        // Test with insufficient data for statistical analysis
        List<Double> insufficientData = Arrays.asList(2.0, 2.1, 2.2);

        DetectionResult result = detector.detect(insufficientData);

        assertFalse(result.isAnomaly());
        assertEquals("INFO", result.getSeverity());
        assertNotNull(result.getEvidence());
    }

    @Test
    void testConstantData_NoAnomaly() {
        // Test with constant data (zero variance)
        List<Double> constantData = Arrays.asList(
            2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5
        );

        DetectionResult result = detector.detect(constantData);

        assertFalse(result.isAnomaly());
        assertEquals("INFO", result.getSeverity());
    }

    @Test
    void testEmptyData_NoAnomaly() {
        List<Double> emptyData = Arrays.asList();

        DetectionResult result = detector.detect(emptyData);

        assertFalse(result.isAnomaly());
        assertEquals("INFO", result.getSeverity());
    }

    @Test
    void testDetectorType() {
        assertEquals("Z_SCORE", detector.getType());
    }

    @Test
    void testEvidenceStructure() {
        List<Double> testData = Arrays.asList(
            2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9,
            2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 5.0
        );

        DetectionResult result = detector.detect(testData);

        assertNotNull(result.getEvidence());
        // The evidence should contain statistical information
        assertTrue(result.getScore() > 0);
    }
}
