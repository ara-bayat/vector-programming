package ir.vector.lesson01;

import ir.vector.common.Benchmark;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;
import java.util.Random;

/**
 * Lesson 01 — SIMD idea in one sentence:
 * one CPU instruction operates on several float lanes at once.
 *
 * Java entry point: incubator Vector API (jdk.incubator.vector) on JDK 17.
 * This is NOT java.util.Vector.
 */
public final class ScalarVsVectorAdd {

    /**
     * SPECIES_PREFERRED asks the JVM for the widest float vector
     * that this CPU can run efficiently (e.g. 4 / 8 / 16 lanes).
     */
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    private static final int ARRAY_SIZE = 16_777_216; // 2^24 floats
    private static final int WARMUP_ROUNDS = 3;
    private static final int MEASURE_ROUNDS = 10;

    public static void main(String[] args) {
        float[] left = randomFloats(ARRAY_SIZE);
        float[] right = randomFloats(ARRAY_SIZE);
        float[] scalarResult = new float[ARRAY_SIZE];
        float[] vectorResult = new float[ARRAY_SIZE];

        System.out.println("=== Lesson 01: Scalar vs SIMD float add ===");
        System.out.println("Array length : " + ARRAY_SIZE);
        System.out.println("Vector species: " + SPECIES);
        System.out.println("Lanes per op  : " + SPECIES.length());
        System.out.println();

        long scalarMillis = Benchmark.millis(
                () -> addScalar(left, right, scalarResult),
                WARMUP_ROUNDS,
                MEASURE_ROUNDS
        );

        long vectorMillis = Benchmark.millis(
                () -> addVector(left, right, vectorResult),
                WARMUP_ROUNDS,
                MEASURE_ROUNDS
        );

        boolean same = Arrays.equals(scalarResult, vectorResult);

        Benchmark.printResult("Scalar loop", scalarMillis);
        Benchmark.printResult("Vector API ", vectorMillis);
        System.out.println("Results equal: " + same);

        if (vectorMillis > 0) {
            double speedup = (double) scalarMillis / (double) vectorMillis;
            System.out.printf("Approx speedup: %.2fx%n", speedup);
        }
    }

    /**
     * Classic scalar loop: one addition per iteration.
     */
    static void addScalar(float[] left, float[] right, float[] result) {
        for (int index = 0; index < left.length; index++) {
            result[index] = left[index] + right[index];
        }
    }

    /**
     * SIMD loop:
     * 1) load SPECIES.length() floats from left
     * 2) load SPECIES.length() floats from right
     * 3) add all lanes in one vector instruction
     * 4) store the vector back into result
     * 5) finish the leftover tail with a scalar loop
     */
    static void addVector(float[] left, float[] right, float[] result) {
        int index = 0;
        int upperBound = SPECIES.loopBound(left.length);

        for (; index < upperBound; index += SPECIES.length()) {
            FloatVector leftVector = FloatVector.fromArray(SPECIES, left, index);
            FloatVector rightVector = FloatVector.fromArray(SPECIES, right, index);
            FloatVector sumVector = leftVector.add(rightVector);
            sumVector.intoArray(result, index);
        }

        for (; index < left.length; index++) {
            result[index] = left[index] + right[index];
        }
    }

    private static float[] randomFloats(int size) {
        Random random = new Random(42);
        float[] values = new float[size];
        for (int index = 0; index < size; index++) {
            values[index] = random.nextFloat();
        }
        return values;
    }
}
