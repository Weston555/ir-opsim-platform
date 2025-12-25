package com.example.iropsim.detection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class ThresholdDetectorTest {

    private ThresholdDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ThresholdDetector();
    }

    @Test
    void testNormalData_NoAnomaly() {
        // Test with normal data within thresholds
        List<Double> normalData = Arrays.asList(2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9);

        DetectionResult result = detector.detect(normalData);

        assertFalse(result.isAnomaly());
        assertEquals("INFO", result.getSeverity());
    }

    @Test
    void testHighValue_Anomaly() {
        // Test with value exceeding upper threshold
        List<Double> highValueData = Arrays.asList(2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 5.0);

        DetectionResult result = detector.detect(highValueData);

        assertTrue(result.isAnomaly());
        assertTrue(result.getSeverity().equals("WARN") || result.getSeverity().equals("CRITICAL"));
        assertNotNull(result.getEvidence());
    }

    @Test
    void testLowValue_Anomaly() {
        // Test with value below lower threshold
        List<Double> lowValueData = Arrays.asList(2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 0.5);

        DetectionResult result = detector.detect(lowValueData);

        assertTrue(result.isAnomaly());
        assertTrue(result.getSeverity().equals("WARN") || result.getSeverity().equals("CRITICAL"));
        assertNotNull(result.getEvidence());
    }

    @Test
    void testEmptyData_NoAnomaly() {
        List<Double> emptyData = Arrays.asList();

        DetectionResult result = detector.detect(emptyData);

        assertFalse(result.isAnomaly());
        assertEquals("INFO", result.getSeverity());
    }

    @Test
    void testSingleDataPoint_NoAnomaly() {
        List<Double> singleData = Arrays.asList(2.5);

        DetectionResult result = detector.detect(singleData);

        assertFalse(result.isAnomaly());
        assertEquals("INFO", result.getSeverity());
    }

    @Test
    void testDetectorType() {
        assertEquals("THRESHOLD", detector.getType());
    }
}
