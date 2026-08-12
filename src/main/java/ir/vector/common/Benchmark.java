package ir.vector.common;

/**
 * Tiny helper for comparing scalar vs SIMD timings.
 * Kept small on purpose so lessons stay focused on vector ideas, not tooling.
 */
public final class Benchmark {

    private Benchmark() {
    }

    public static long millis(Runnable action, int warmupRounds, int measureRounds) {
        for (int i = 0; i < warmupRounds; i++) {
            action.run();
        }

        long startedAt = System.nanoTime();
        for (int i = 0; i < measureRounds; i++) {
            action.run();
        }
        long elapsedNanos = System.nanoTime() - startedAt;
        return elapsedNanos / 1_000_000L;
    }

    public static void printResult(String label, long millis) {
        System.out.println(label + ": " + millis + " ms");
    }
}
