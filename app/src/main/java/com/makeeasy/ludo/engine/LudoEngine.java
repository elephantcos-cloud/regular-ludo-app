package com.makeeasy.ludo.engine;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║   LudoEngine.java  —  Java/JNI wrapper for ludo_engine.so  ║
 * ║   Native C++ engine:  ludo_engine.cpp  (NDK / ARM64)        ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * HOW IT WORKS:
 *   Java calls → JNI → ludo_engine.so (C++, -O3, multi-threaded)
 *   The C++ engine runs directly on the phone's CPU cores using:
 *     • Expectiminimax + Alpha-Beta search
 *     • Lazy SMP multi-threading (all CPU cores)
 *     • Transposition Table (Zobrist hashing)
 *     • Killer + History heuristics
 *     • Late Move Reduction
 *
 * INTEGRATION IN YOUR GAME ACTIVITY:
 *   1. Add LudoEngine.java to  com/makeeasy/ludo/engine/
 *   2. Add ludo_engine.cpp + CMakeLists.txt to  app/src/main/cpp/
 *   3. In app/build.gradle add:
 *        externalNativeBuild { cmake { path "src/main/cpp/CMakeLists.txt" } }
 *   4. In your game class:
 *        private final LudoEngine ai = new LudoEngine(LudoEngine.HARD);
 *
 * GETTING THE BEST MOVE:
 *   // walkedX[player][piece] = your existing progress array (0-57)
 *   int piece = ai.getBestMove(walkedX, currentPlayer, diceValue);
 *   // piece 0-3 → move that piece;  -1 → no legal move exists
 *
 * PIECE POSITION VALUES (matches your game's walkedX array):
 *   0        = in yard (not yet started)
 *   1        = own start square
 *   2 – 51   = travelling the main ring
 *   52 – 56  = inside home column (safe lane)
 *   57       = HOME (piece finished)
 */
public final class LudoEngine {

    // ── Difficulty constants ──────────────────────────────────
    public static final int EASY   = 1;
    public static final int MEDIUM = 2;
    public static final int HARD   = 3;
    public static final int MASTER = 4;

    // ── Library load ─────────────────────────────────────────
    static {
        try {
            System.loadLibrary("ludo_engine");
        } catch (UnsatisfiedLinkError e) {
            // Fallback: engine unavailable (e.g. emulator without NDK build)
            android.util.Log.e("LudoEngine",
                "Native library not found — did you build with NDK?", e);
        }
    }

    // ── JNI declarations ─────────────────────────────────────

    /**
     * Compute the best piece to move using the native C++ engine.
     *
     * @param positionsFlat int[16] — positions[player * 4 + piece] (row-major)
     * @param curPlayer     0-3
     * @param diceValue     1-6
     * @param difficulty    EASY / MEDIUM / HARD / MASTER
     * @return              piece index 0-3 to move, or -1 if no legal move
     */
    private native int nativeGetBestMove(int[] positionsFlat,
                                         int curPlayer,
                                         int diceValue,
                                         int difficulty);

    /** Clear the transposition table. Call at the start of each new game. */
    public native void nativeReset();

    /**
     * Static (no-search) position evaluation.
     * Positive = advantageous for `player`.
     *
     * @param positionsFlat int[16] flattened positions
     * @param player        0-3
     * @return              raw evaluation score
     */
    public native int nativeStaticEval(int[] positionsFlat, int player);

    // ── Public Java API ───────────────────────────────────────
    private final int difficulty;
    private final int[] flat = new int[16];   // reusable flat buffer

    /**
     * Create a new engine instance.
     *
     * @param difficulty  EASY / MEDIUM / HARD / MASTER
     */
    public LudoEngine(int difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * Primary entry point: get the best piece index to move.
     *
     * @param walkedX   2-D array [4][4]: walkedX[player][piece] = relPos (0-57)
     *                  This is the same array used internally by your game.
     * @param curPlayer current player index (0-3)
     * @param diceValue dice roll result (1-6)
     * @return          piece index (0-3), or -1 if no legal move exists
     */
    public int getBestMove(int[][] walkedX, int curPlayer, int diceValue) {
        // Flatten 2-D array → 1-D (JNI is faster with a single int[])
        for (int p = 0; p < 4; p++)
            for (int pc = 0; pc < 4; pc++)
                flat[p * 4 + pc] = walkedX[p][pc];

        return nativeGetBestMove(flat, curPlayer, diceValue, difficulty);
    }

    /**
     * Static evaluation (no search).
     *
     * @param walkedX  [4][4] position array
     * @param player   whose perspective to evaluate
     * @return         signed score; positive = player is winning
     */
    public int staticEval(int[][] walkedX, int player) {
        for (int p = 0; p < 4; p++)
            for (int pc = 0; pc < 4; pc++)
                flat[p * 4 + pc] = walkedX[p][pc];
        return nativeStaticEval(flat, player);
    }

    /**
     * Reset the engine. Call at the start of each new game to ensure
     * the transposition table from a previous game doesn't interfere.
     */
    public void reset() {
        nativeReset();
    }
}
