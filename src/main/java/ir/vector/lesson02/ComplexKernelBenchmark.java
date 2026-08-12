package ir.vector.lesson02;

import ir.vector.common.Benchmark;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Random;

/**
 * Lesson 02 — same SIMD idea as lesson 01, but on a heavier kernel.
 *
 * Per element we do many FLOPs:
 *   poly   = ((c3 * x + c2) * x + c1) * x + c0     (Horner cubic)
 *   energy = wX*x*x + wY*y*y + wZ*z*z
 *   seed   = sqrt(energy) * gain + poly
 *   then REFINE_STEPS of Newton-style updates on seed
 *
 * Why this matters for learning:
 * simple add is often memory-bound / auto-vectorized by HotSpot,
 * so scalar and Vector API look similar. More arithmetic per load
 * makes the SIMD lane work more visible.
 */
public final class ComplexKernelBenchmark {

    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    private static final int ARRAY_SIZE = 2_097_152; // 2^21 — smaller array, heavier math
    private static final int WARMUP_ROUNDS = 3;
    private static final int MEASURE_ROUNDS = 8;
    private static final int REFINE_STEPS = 48;

    private static final float C0 = 0.15f;
    private static final float C1 = -0.35f;
    private static final float C2 = 0.55f;
    private static final float C3 = 0.05f;

    private static final float WX = 1.10f;
    private static final float WY = 0.85f;
    private static final float WZ = 1.25f;

    private static final float GAIN = 1.7f;
    private static final float EPS = 1.0e-6f;

    public static void main(String[] args) {
        float[] x = randomFloats(ARRAY_SIZE, 7);
        float[] y = randomFloats(ARRAY_SIZE, 11);
        float[] z = randomFloats(ARRAY_SIZE, 19);

        float[] scalarResult = new float[ARRAY_SIZE];
        float[] vectorResult = new float[ARRAY_SIZE];

        System.out.println("=== Lesson 02: Complex kernel (Horner + energy + sqrt + Newton refine) ===");
        System.out.println("Array length : " + ARRAY_SIZE);
        System.out.println("Vector species: " + SPECIES);
        System.out.println("Lanes per op  : " + SPECIES.length());
        System.out.println("Refine steps  : " + REFINE_STEPS);
        System.out.println("Formula       : refine(sqrt(energy)*gain + cubic(x))");
        System.out.println();

        long scalarMillis = Benchmark.millis(
                () -> computeScalar(x, y, z, scalarResult),
                WARMUP_ROUNDS,
                MEASURE_ROUNDS
        );

        long vectorMillis = Benchmark.millis(
                () -> computeVector(x, y, z, vectorResult),
                WARMUP_ROUNDS,
                MEASURE_ROUNDS
        );

        boolean closeEnough = almostEqual(scalarResult, vectorResult, 1e-4f);

        Benchmark.printResult("Scalar loop", scalarMillis);
        Benchmark.printResult("Vector API ", vectorMillis);
        System.out.println("Results close: " + closeEnough + " (float tolerance 1e-4)");

        if (vectorMillis > 0) {
            double speedup = (double) scalarMillis / (double) vectorMillis;
            System.out.printf("Approx speedup: %.2fx%n", speedup);
        }
    }

    /**
     * Scalar path: one element at a time, many FLOPs each iteration.
     * Math.fma is used so the math matches Vector API fma more closely.
     */
    static void computeScalar(float[] x, float[] y, float[] z, float[] result) {
        for (int index = 0; index < x.length; index++) {
            result[index] = evaluateKernel(x[index], y[index], z[index]);
        }
    }

    /**
     * SIMD path: SPECIES.length() elements travel through the same kernel together.
     */
    static void computeVector(float[] x, float[] y, float[] z, float[] result) {
        int index = 0;
        int upperBound = SPECIES.loopBound(x.length);

        FloatVector c0 = FloatVector.broadcast(SPECIES, C0);
        FloatVector c1 = FloatVector.broadcast(SPECIES, C1);
        FloatVector c2 = FloatVector.broadcast(SPECIES, C2);
        FloatVector c3 = FloatVector.broadcast(SPECIES, C3);

        FloatVector wX = FloatVector.broadcast(SPECIES, WX);
        FloatVector wY = FloatVector.broadcast(SPECIES, WY);
        FloatVector wZ = FloatVector.broadcast(SPECIES, WZ);
        FloatVector gain = FloatVector.broadcast(SPECIES, GAIN);

        for (; index < upperBound; index += SPECIES.length()) {
            FloatVector xVec = FloatVector.fromArray(SPECIES, x, index);
            FloatVector yVec = FloatVector.fromArray(SPECIES, y, index);
            FloatVector zVec = FloatVector.fromArray(SPECIES, z, index);

            FloatVector poly = hornerCubic(xVec, c0, c1, c2, c3);
            FloatVector energy = weightedEnergy(xVec, yVec, zVec, wX, wY, wZ);
            FloatVector seed = energy.sqrt().fma(gain, poly);
            FloatVector out = refine(seed, energy, poly);

            out.intoArray(result, index);
        }

        for (; index < x.length; index++) {
            result[index] = evaluateKernel(x[index], y[index], z[index]);
        }
    }

    static float evaluateKernel(float xValue, float yValue, float zValue) {
        float poly = hornerCubic(xValue);
        float energy = weightedEnergy(xValue, yValue, zValue);
        float seed = Math.fma((float) Math.sqrt(energy), GAIN, poly);
        return refine(seed, energy, poly);
    }

    /**
     * Extra ALU work per element: several Newton-like updates.
     * Same formula in scalar and vector paths so results stay comparable.
     */
    static float refine(float value, float energy, float poly) {
        for (int step = 0; step < REFINE_STEPS; step++) {
            float mixed = Math.fma(value, 0.999f, poly * 0.001f);
            value = 0.5f * mixed + 0.5f * (energy / (mixed + EPS));
        }
        return value;
    }

    static FloatVector refine(FloatVector value, FloatVector energy, FloatVector poly) {
        FloatVector almostOne = FloatVector.broadcast(SPECIES, 0.999f);
        FloatVector polyWeight = FloatVector.broadcast(SPECIES, 0.001f);
        FloatVector half = FloatVector.broadcast(SPECIES, 0.5f);
        FloatVector eps = FloatVector.broadcast(SPECIES, EPS);

        for (int step = 0; step < REFINE_STEPS; step++) {
            FloatVector mixed = value.mul(almostOne).add(poly.mul(polyWeight));
            FloatVector reciprocalPart = energy.div(mixed.add(eps));
            value = mixed.mul(half).add(reciprocalPart.mul(half));
        }
        return value;
    }

    static float hornerCubic(float xValue) {
        float poly = Math.fma(C3, xValue, C2);
        poly = Math.fma(poly, xValue, C1);
        return Math.fma(poly, xValue, C0);
    }

    static FloatVector hornerCubic(
            FloatVector xVec,
            FloatVector c0,
            FloatVector c1,
            FloatVector c2,
            FloatVector c3
    ) {
        FloatVector poly = c3.fma(xVec, c2);
        poly = poly.fma(xVec, c1);
        return poly.fma(xVec, c0);
    }

    static float weightedEnergy(float xValue, float yValue, float zValue) {
        float termX = WX * xValue * xValue;
        float termY = WY * yValue * yValue;
        float termZ = WZ * zValue * zValue;
        return termX + termY + termZ;
    }

    static FloatVector weightedEnergy(
            FloatVector xVec,
            FloatVector yVec,
            FloatVector zVec,
            FloatVector wX,
            FloatVector wY,
            FloatVector wZ
    ) {
        FloatVector termX = xVec.mul(xVec).mul(wX);
        FloatVector termY = yVec.mul(yVec).mul(wY);
        FloatVector termZ = zVec.mul(zVec).mul(wZ);
        return termX.add(termY).add(termZ);
    }

    private static float[] randomFloats(int size, long seed) {
        Random random = new Random(seed);
        float[] values = new float[size];
        for (int index = 0; index < size; index++) {
            // keep values in a mild range so sqrt/poly stay well-behaved
            values[index] = random.nextFloat() * 2.0f - 1.0f;
        }
        return values;
    }

    private static boolean almostEqual(float[] left, float[] right, float tolerance) {
        if (left.length != right.length) {
            return false;
        }

        for (int index = 0; index < left.length; index++) {
            float delta = Math.abs(left[index] - right[index]);
            if (delta > tolerance) {
                return false;
            }
        }
        return true;
    }
}
