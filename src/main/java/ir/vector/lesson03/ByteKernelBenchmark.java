package ir.vector.lesson03;

import ir.vector.common.Benchmark;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;
import java.util.Random;

/**
 * Lesson 03 — heavy kernel on 8-bit lanes (byte), not float.
 *
 * On AVX-256, SPECIES_PREFERRED for byte is usually 32 lanes:
 *   256 bits / 8 bits = 32
 * So the theoretical ALU ceiling is much higher than float's 8 lanes.
 *
 * Kernel (wraps like Java byte arithmetic, mod 256):
 *   acc = x
 *   repeat MIX_STEPS:
 *     acc = acc * y + z
 *     acc = acc ^ (acc << 1)
 *     acc = acc + 17
 *   result = acc
 */
public final class ByteKernelBenchmark {

    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    private static final int ARRAY_SIZE = 1_048_57600; // 2^20 bytes
    private static final int WARMUP_ROUNDS = 3;
    private static final int MEASURE_ROUNDS = 8;
    private static final int MIX_STEPS = 64;

    private static final byte ADD_CONSTANT = 17;

    public static void main(String[] args) {
        byte[] x = randomBytes(ARRAY_SIZE, 3);
        byte[] y = randomBytes(ARRAY_SIZE, 5);
        byte[] z = randomBytes(ARRAY_SIZE, 9);

        byte[] scalarResult = new byte[ARRAY_SIZE];
        byte[] vectorResult = new byte[ARRAY_SIZE];

        int bitsPerRegister = SPECIES.vectorBitSize();
        int theoreticalMax = SPECIES.length();

        System.out.println("=== Lesson 03: Heavy byte (8-bit) kernel ===");
        System.out.println("Array length      : " + ARRAY_SIZE);
        System.out.println("Vector species    : " + SPECIES);
        System.out.println("Lanes per op      : " + SPECIES.length());
        System.out.println("Register width    : " + bitsPerRegister + " bit");
        System.out.println("Theoretical ceiling ~ " + theoreticalMax + "x (if pure ALU, scalar stays scalar)");
        System.out.println("Mix steps         : " + MIX_STEPS);
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

        boolean same = Arrays.equals(scalarResult, vectorResult);

        Benchmark.printResult("Scalar loop", scalarMillis);
        Benchmark.printResult("Vector API ", vectorMillis);
        System.out.println("Results equal: " + same);

        if (vectorMillis > 0) {
            double speedup = (double) scalarMillis / (double) vectorMillis;
            System.out.printf("Approx speedup: %.2fx%n", speedup);
            System.out.printf(
                    "Efficiency vs lane count: %.0f%%%n",
                    100.0 * speedup / theoreticalMax
            );
        }
    }

    static void computeScalar(byte[] x, byte[] y, byte[] z, byte[] result) {
        for (int index = 0; index < x.length; index++) {
            result[index] = mix(x[index], y[index], z[index]);
        }
    }

    static void computeVector(byte[] x, byte[] y, byte[] z, byte[] result) {
        int index = 0;
        int upperBound = SPECIES.loopBound(x.length);
        ByteVector addConstant = ByteVector.broadcast(SPECIES, ADD_CONSTANT);

        for (; index < upperBound; index += SPECIES.length()) {
            ByteVector xVec = ByteVector.fromArray(SPECIES, x, index);
            ByteVector yVec = ByteVector.fromArray(SPECIES, y, index);
            ByteVector zVec = ByteVector.fromArray(SPECIES, z, index);

            ByteVector out = mix(xVec, yVec, zVec, addConstant);
            out.intoArray(result, index);
        }

        for (; index < x.length; index++) {
            result[index] = mix(x[index], y[index], z[index]);
        }
    }

    /**
     * Scalar mixer. Casts keep Java byte wrap-around (mod 256).
     */
    static byte mix(byte xValue, byte yValue, byte zValue) {
        byte acc = xValue;
        for (int step = 0; step < MIX_STEPS; step++) {
            acc = (byte) (acc * yValue + zValue);
            acc = (byte) (acc ^ (acc << 1));
            acc = (byte) (acc + ADD_CONSTANT);
        }
        return acc;
    }

    /**
     * Same mixer on SPECIES.length() lanes at once.
     */
    static ByteVector mix(
            ByteVector xVec,
            ByteVector yVec,
            ByteVector zVec,
            ByteVector addConstant
    ) {
        ByteVector acc = xVec;
        for (int step = 0; step < MIX_STEPS; step++) {
            acc = acc.mul(yVec).add(zVec);
            ByteVector shifted = acc.lanewise(VectorOperators.LSHL, 1);
            acc = acc.lanewise(VectorOperators.XOR, shifted);
            acc = acc.add(addConstant);
        }
        return acc;
    }

    private static byte[] randomBytes(int size, long seed) {
        Random random = new Random(seed);
        byte[] values = new byte[size];
        random.nextBytes(values);
        // avoid zero multipliers so the mix stays "busy"
        for (int index = 0; index < values.length; index++) {
            if (values[index] == 0) {
                values[index] = 1;
            }
        }
        return values;
    }
}
