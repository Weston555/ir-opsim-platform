package com.example.iropsim.detection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

class SlidingWindowBufferTest {

    private SlidingWindowBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new SlidingWindowBuffer();
    }

    @Test
    void testAddAndRetrieveSamples() {
        String robotId = "robot-001";
        int jointIndex = 0;
        Instant now = Instant.now();

        // Add samples
        buffer.addSample(robotId, jointIndex, 2.5, now);
        buffer.addSample(robotId, jointIndex, 2.6, now.plusSeconds(10));
        buffer.addSample(robotId, jointIndex, 2.7, now.plusSeconds(20));

        // Retrieve samples within time window
        List<Double> samples = buffer.getValuesInWindow(robotId, jointIndex, now.plusSeconds(30));

        assertEquals(3, samples.size());
        assertEquals(2.5, samples.get(0));
        assertEquals(2.6, samples.get(1));
        assertEquals(2.7, samples.get(2));
    }

    @Test
    void testTimeWindowFiltering() {
        String robotId = "robot-001";
        int jointIndex = 0;
        Instant now = Instant.now();

        // Add samples spanning a long time period
        buffer.addSample(robotId, jointIndex, 1.0, now.minusSeconds(120)); // Too old
        buffer.addSample(robotId, jointIndex, 2.0, now.minusSeconds(30));  // Within window
        buffer.addSample(robotId, jointIndex, 3.0, now);                   // Within window

        // Retrieve samples within 60-second window
        List<Double> samples = buffer.getValuesInWindow(robotId, jointIndex, now);

        assertEquals(2, samples.size());
        assertEquals(2.0, samples.get(0));
        assertEquals(3.0, samples.get(1));
    }

    @Test
    void testDifferentRobotsAndJoints() {
        Instant now = Instant.now();

        // Add samples for different robots and joints
        buffer.addSample("robot-001", 0, 1.0, now);
        buffer.addSample("robot-001", 1, 2.0, now);
        buffer.addSample("robot-002", 0, 3.0, now);

        List<Double> robot1Joint0 = buffer.getValuesInWindow("robot-001", 0, now);
        List<Double> robot1Joint1 = buffer.getValuesInWindow("robot-001", 1, now);
        List<Double> robot2Joint0 = buffer.getValuesInWindow("robot-002", 0, now);

        assertEquals(1, robot1Joint0.size());
        assertEquals(1.0, robot1Joint0.get(0));

        assertEquals(1, robot1Joint1.size());
        assertEquals(2.0, robot1Joint1.get(0));

        assertEquals(1, robot2Joint0.size());
        assertEquals(3.0, robot2Joint0.get(0));
    }

    @Test
    void testEmptyBuffer() {
        List<Double> samples = buffer.getValuesInWindow("nonexistent", 0, Instant.now());
        assertTrue(samples.isEmpty());
    }

    @Test
    void testBufferStatistics() {
        String robotId = "robot-001";
        int jointIndex = 0;
        Instant now = Instant.now();

        // Add samples with known statistics
        buffer.addSample(robotId, jointIndex, 1.0, now);
        buffer.addSample(robotId, jointIndex, 2.0, now);
        buffer.addSample(robotId, jointIndex, 3.0, now);
        buffer.addSample(robotId, jointIndex, 4.0, now);
        buffer.addSample(robotId, jointIndex, 5.0, now);

        SlidingWindowBuffer.BufferStats stats = buffer.getBufferStats(robotId, jointIndex);

        assertEquals(5, stats.count);
        assertEquals(3.0, stats.mean); // (1+2+3+4+5)/5 = 3
        assertEquals(1.0, stats.min);
        assertEquals(5.0, stats.max);
    }

    @Test
    void testBufferStatistics_Empty() {
        SlidingWindowBuffer.BufferStats stats = buffer.getBufferStats("nonexistent", 0);

        assertEquals(0, stats.count);
        assertEquals(0.0, stats.mean);
        assertEquals(0.0, stats.stdDev);
        assertEquals(0.0, stats.min);
        assertEquals(0.0, stats.max);
    }

    @Test
    void testBufferStatistics_SingleValue() {
        String robotId = "robot-001";
        int jointIndex = 0;
        Instant now = Instant.now();

        buffer.addSample(robotId, jointIndex, 2.5, now);

        SlidingWindowBuffer.BufferStats stats = buffer.getBufferStats(robotId, jointIndex);

        assertEquals(1, stats.count);
        assertEquals(2.5, stats.mean);
        assertEquals(0.0, stats.stdDev); // Single value has zero std deviation
        assertEquals(2.5, stats.min);
        assertEquals(2.5, stats.max);
    }
}
