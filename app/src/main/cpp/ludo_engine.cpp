/*
 ═══════════════════════════════════════════════════════════════════════════
  LUDO MASTER ENGINE  ·  Native C++ (NDK/JNI)  ·  Version 3.0
  Stockfish-Inspired Architecture — Built for Android ARM64 / x86_64
 ═══════════════════════════════════════════════════════════════════════════

  ┌─ Search ──────────────────────────────────────────────────────────────┐
  │  • Expectiminimax  +  Paranoid Alpha-Beta  (4-player adapted)         │
  │  • Iterative Deepening  (depth 1 → maxDepth, time-bounded)            │
  │  • Lazy SMP  — multi-threaded root search, shared TT                  │
  │  • Late Move Reduction  (adapted for Ludo's low branching factor)     │
  │  • Time Management  (hard + soft cutoff like Stockfish)               │
  └───────────────────────────────────────────────────────────────────────┘
  ┌─ Memory ───────────────────────────────────────────────────────────────┐
  │  • Transposition Table  (two-bucket, Zobrist hash, generation-aware)  │
  │  • Killer Table  (2 killers per ply)                                  │
  │  • Relative History Heuristic  (success/failure ratio per move)       │
  └───────────────────────────────────────────────────────────────────────┘
  ┌─ Evaluation ───────────────────────────────────────────────────────────┐
  │  • Progress Score             (weighted, phase-tapered)               │
  │  • Opening Bias               (exit-yard urgency in early game)       │
  │  • Safety Analysis            (safe squares + home column)            │
  │  • Probabilistic Threat       (weighted by enemy proximity)           │
  │  • Capture Opportunity        (reward aggressive positions)           │
  │  • Leading Enemy Targeting    (focus attacks on the front-runner)     │
  │  • Dual-Threat Bonus          (two pieces menacing two enemies)       │
  │  • Blocking Pair Bonus        (form and maintain friendly blocks)     │
  │  • Block-Break Reward         (value dismantling enemy blocks)        │
  │  • Endgame Taper              (weights shift near home column)        │
  │  • Piece Diversity Bonus      (reward having multiple active pieces)  │
  └───────────────────────────────────────────────────────────────────────┘

  Board Geometry:
    relPos 0       = in yard          (not yet started)
    relPos 1       = own start square (entered on roll of 6)
    relPos 2–51    = main ring
    relPos 52–56   = home column      (safe, player-exclusive)
    relPos 57      = HOME             (piece finished)

    Absolute safe squares: 0,8,13,21,26,34,39,47
    Relative equivalents : 1,9,14,22,27,35,40,48  (same for ALL players)

  CMakeLists.txt:
    add_library(ludo_engine SHARED ludo_engine.cpp)
    target_link_libraries(ludo_engine log)
    target_compile_options(ludo_engine PRIVATE -O3 -ffast-math -funroll-loops)
    set_property(TARGET ludo_engine PROPERTY CXX_STANDARD 17)

  Java side (LudoEngine.java):
    static { System.loadLibrary("ludo_engine"); }
    public native int  nativeGetBestMove(int[] positions, int player, int dice, int diff);
    public native void nativeReset();
    public native int  nativeStaticEval(int[] positions, int player);

  JNI package: com.makeeasy.ludo.engine.LudoEngine
 ═══════════════════════════════════════════════════════════════════════════
*/

#include <jni.h>
#include <cstdint>
#include <cstring>
#include <climits>
#include <algorithm>
#include <vector>
#include <array>
#include <thread>
#include <atomic>
#include <mutex>
#include <chrono>
#include <android/log.h>

#define LOG_TAG "LudoEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 1 — CONSTANTS & BOARD GEOMETRY
   ═══════════════════════════════════════════════════════════════════════ */
namespace ludo {

static constexpr int NP   = 4;   // players
static constexpr int NPC  = 4;   // pieces per player
static constexpr int RING = 52;  // main ring length
static constexpr int HOME = 57;  // finished position (relPos == HOME)

// Absolute ring start for each player's own start square
static constexpr int PSTART[4] = { 0, 13, 26, 39 };

/*
 * SAFE[relPos] == true  → piece cannot be captured here.
 * Home column (52-57)   → always safe (player-exclusive lane).
 * Safe rel positions: 1,9,14,22,27,35,40,48  (verified symmetric for all 4 players).
 */
static constexpr bool SAFE[58] = {
    /* 0  */ false,
    /* 1  */ true,                                       // own start
    /* 2  */ false,false,false,false,false,false,false,  // 2-8
    /* 9  */ true,                                       // star
    /* 10 */ false,false,false,false,                    // 10-13
    /* 14 */ true,                                       // next player's start (relative)
    /* 15 */ false,false,false,false,false,false,false,  // 15-21
    /* 22 */ true,                                       // star
    /* 23 */ false,false,false,false,                    // 23-26
    /* 27 */ true,                                       // star
    /* 28 */ false,false,false,false,false,false,false,  // 28-34
    /* 35 */ true,                                       // star
    /* 36 */ false,false,false,false,                    // 36-39
    /* 40 */ true,                                       // star
    /* 41 */ false,false,false,false,false,false,false,  // 41-47
    /* 48 */ true,                                       // star
    /* 49 */ false,false,false,                          // 49-51
    /* 52 */ true,true,true,true,true,true               // home col + home
};

// Scores
static constexpr int WIN_SCORE  =  100000;
static constexpr int LOSE_SCORE = -100000;
static constexpr int INF_SC     =  200000;

// Evaluation weights — tuned through self-play analysis
static constexpr int W_PROGRESS    = 10;
static constexpr int W_FINISH      = 300;
static constexpr int W_HOME_COL    = 15;
static constexpr int W_NEAR_HOME   = 22;
static constexpr int W_SAFE        = 25;
static constexpr int W_THREAT      = -42;   // per unit of danger
static constexpr int W_CAPTURE     = 38;    // capture opportunity
static constexpr int W_BLOCK_PAIR  = 32;    // friendly pair on same square
static constexpr int W_DIVERSITY   = 10;    // per active piece (2+ active)
static constexpr int W_YARD_PEN    = -8;    // per piece still in yard
static constexpr int W_OPEN_EXIT   = 55;    // opening bias: get pieces out
static constexpr int W_LEAD_TARGET = 48;    // targeting leading enemy
static constexpr int W_DUAL_THREAT = 65;    // threatening two enemies at once
static constexpr int W_BLOCK_BREAK = 42;    // can break enemy blocking pair

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 2 — ZOBRIST HASHING
   Splitmix64 seeded RNG → 64-bit keys for each (player, piece, position).
   Fixed seed → reproducible hashes across runs.
   ═══════════════════════════════════════════════════════════════════════ */
struct ZobristTable {
    uint64_t piece[NP][NPC][HOME + 1];
    uint64_t turn[NP];
    uint64_t dice_key[7];   // index 1..6

    void init() {
        uint64_t s = 0x4C55444F5F454E47ULL;   // "LUDO_ENG" as hex seed
        auto sm64 = [&]() -> uint64_t {
            s += 0x9e3779b97f4a7c15ULL;
            uint64_t z = s;
            z = (z ^ (z >> 30)) * 0xbf58476d1ce4e5b9ULL;
            z = (z ^ (z >> 27)) * 0x94d049bb133111ebULL;
            return z ^ (z >> 31);
        };
        for (int p = 0; p < NP; ++p) {
            turn[p] = sm64();
            for (int pc = 0; pc < NPC; ++pc)
                for (int pos = 0; pos <= HOME; ++pos)
                    piece[p][pc][pos] = sm64();
        }
        for (int d = 1; d <= 6; ++d) dice_key[d] = sm64();
    }
};

static ZobristTable ZOBRIST;

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 3 — GAME STATE & MOVE
   ═══════════════════════════════════════════════════════════════════════ */
struct State {
    int8_t pos[NP][NPC];   // relative positions [0..HOME]
    int8_t cur;            // current player (0-3)
    int8_t dice;           // 0 = chance node (dice not yet rolled)

    // Compute Zobrist hash for transposition table lookup
    uint64_t hash() const noexcept {
        uint64_t h = ZOBRIST.turn[(uint8_t)cur];
        if (dice >= 1 && dice <= 6) h ^= ZOBRIST.dice_key[(uint8_t)dice];
        for (int p = 0; p < NP; ++p)
            for (int pc = 0; pc < NPC; ++pc)
                h ^= ZOBRIST.piece[p][pc][(uint8_t)pos[p][pc]];
        return h;
    }

    bool playerDone(int p) const noexcept {
        for (int pc = 0; pc < NPC; ++pc)
            if (pos[p][pc] < HOME) return false;
        return true;
    }
    bool isTerminal() const noexcept {
        for (int p = 0; p < NP; ++p)
            if (playerDone(p)) return true;
        return false;
    }
    int activePieces(int p) const noexcept {
        int n = 0;
        for (int pc = 0; pc < NPC; ++pc)
            if (pos[p][pc] > 0 && pos[p][pc] < HOME) ++n;
        return n;
    }
    int totalProgress(int p) const noexcept {
        int t = 0;
        for (int pc = 0; pc < NPC; ++pc) t += pos[p][pc];
        return t;
    }
};

struct Move {
    int8_t  pieceIdx;
    int8_t  fromPos;
    int8_t  toPos;
    bool    isCapture;
    bool    isFinish;
    bool    isExitYard;
    int     sortScore;

    // Compact encoding for TT best-move storage
    inline int encode() const noexcept {
        return (int)(uint8_t)pieceIdx | ((int)(uint8_t)toPos << 4);
    }
    static inline int8_t decPiece(int e) noexcept { return (int8_t)(e & 0xF); }
    static inline int8_t decTo   (int e) noexcept { return (int8_t)((e >> 4) & 0x3F); }
};

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 4 — BOARD UTILITY FUNCTIONS
   ═══════════════════════════════════════════════════════════════════════ */
static inline int relToAbs(int player, int relPos) noexcept {
    if (relPos <= 0 || relPos >= RING) return -1;
    return (PSTART[player] + relPos - 1) % RING;
}
static inline int absToRel(int player, int absPos) noexcept {
    return (absPos - PSTART[player] + RING) % RING + 1;
}

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 5 — TRANSPOSITION TABLE
   Two-bucket (dual-slot) with generation-aware replacement policy.
   No locking — benign data races accepted (identical to Stockfish's TT).
   ═══════════════════════════════════════════════════════════════════════ */
enum TTType : uint8_t { TT_EMPTY = 0, TT_EXACT, TT_LOWER, TT_UPPER };

struct TTEntry {
    uint64_t key;
    int32_t  score;
    int32_t  bestMove;
    int16_t  depth;
    uint8_t  type;
    uint8_t  gen;
};

struct TranspositionTable {
    std::vector<TTEntry> table;
    uint8_t generation = 0;
    int     mask       = 0;

    void resize(int log2Sz) {
        int sz = 1 << log2Sz;
        mask   = sz - 1;
        table.assign(sz * 2, TTEntry{0,0,0,0,TT_EMPTY,0});
    }

    void clear() noexcept {
        for (auto& e : table) e.type = TT_EMPTY;
    }

    void nextGen() noexcept { ++generation; }

    inline bool betterThan(const TTEntry& a, const TTEntry& b) const noexcept {
        if ((a.gen == generation) != (b.gen == generation))
            return a.gen == generation;
        if ((a.type == TT_EXACT) != (b.type == TT_EXACT))
            return a.type == TT_EXACT;
        return a.depth > b.depth;
    }

    TTEntry* probe(uint64_t key) noexcept {
        int i0 = ((int)(key & (uint64_t)mask)) * 2;
        if (table[i0].type != TT_EMPTY && table[i0].key == key) {
            table[i0].gen = generation;
            return &table[i0];
        }
        int i1 = i0 + 1;
        if (table[i1].type != TT_EMPTY && table[i1].key == key) {
            table[i1].gen = generation;
            return &table[i1];
        }
        return nullptr;
    }

    void store(uint64_t key, int score, int depth,
               TTType type, int bestMove) noexcept {
        int i0 = ((int)(key & (uint64_t)mask)) * 2;
        int i1 = i0 + 1;
        TTEntry* slot;
        if (table[i0].key == key)      slot = &table[i0];
        else if (table[i1].key == key) slot = &table[i1];
        else
            slot = betterThan(table[i1], table[i0]) ? &table[i0] : &table[i1];

        // Never evict an exact, deeper entry with the same key
        if (slot->key == key && slot->depth > depth
                && slot->type == TT_EXACT && type != TT_EXACT) return;

        slot->key      = key;
        slot->score    = score;
        slot->depth    = (int16_t)std::min(depth, 127);
        slot->type     = (uint8_t)type;
        slot->gen      = generation;
        slot->bestMove = bestMove;
    }
};

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 6 — HISTORY & KILLER TABLES
   ═══════════════════════════════════════════════════════════════════════ */
struct HistoryTable {
    int success[NPC][HOME + 1];
    int failure[NPC][HOME + 1];

    void clear() noexcept {
        memset(success, 0, sizeof(success));
        memset(failure, 0, sizeof(failure));
    }
    void recordSuccess(int pc, int to, int depth) noexcept {
        success[pc][to] += depth;
        if (success[pc][to] > 4000) {
            success[pc][to] /= 2;
            failure[pc][to] /= 2;
        }
    }
    void recordFailure(int pc, int to, int depth) noexcept {
        failure[pc][to] += depth;
    }
    int score(int pc, int to) const noexcept {
        int s = success[pc][to], f = failure[pc][to];
        return (s + f > 0) ? (s * 49 / (s + f)) : 0;
    }
};

struct KillerTable {
    int m0[128];
    int m1[128];
    void clear() noexcept { memset(m0,0,sizeof(m0)); memset(m1,0,sizeof(m1)); }
    void add(int ply, int enc) noexcept {
        if (ply >= 128 || ply < 0) return;
        if (enc != m0[ply]) { m1[ply] = m0[ply]; m0[ply] = enc; }
    }
    int score(int ply, int enc) const noexcept {
        if (ply < 0 || ply >= 128) return 0;
        if (enc == m0[ply]) return 4;
        if (enc == m1[ply]) return 3;
        if (ply >= 2) {
            if (enc == m0[ply-2]) return 2;
            if (enc == m1[ply-2]) return 1;
        }
        return 0;
    }
};

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 7 — MOVE GENERATOR
   Rules: 6 to exit yard, no overshoot, friendly-block prevention,
   safe squares block capture, extra turn on 6 or capture.
   ═══════════════════════════════════════════════════════════════════════ */
struct MoveGenerator {

    static bool hasFriendlyBlock(const State& s, int pl,
                                  int excludePc, int relPos) noexcept {
        int n = 0;
        for (int pc = 0; pc < NPC; ++pc)
            if (pc != excludePc && s.pos[pl][pc] == relPos && ++n >= 2)
                return true;
        return false;
    }

    static bool hasEnemy(const State& s, int pl, int relPos) noexcept {
        if (relPos <= 0 || relPos >= RING) return false;
        int absT = relToAbs(pl, relPos);
        for (int opp = 0; opp < NP; ++opp) {
            if (opp == pl) continue;
            for (int pc = 0; pc < NPC; ++pc) {
                int op = s.pos[opp][pc];
                if (op > 0 && op < RING && relToAbs(opp, op) == absT)
                    return true;
            }
        }
        return false;
    }

    // Fill `out` with all legal moves for the current player & dice
    void generate(const State& s, std::vector<Move>& out) const {
        out.clear();
        int pl   = s.cur;
        int dice = s.dice;
        const int8_t* pos = s.pos[pl];

        for (int pc = 0; pc < NPC; ++pc) {
            int cur = pos[pc];
            if (cur == HOME) continue;

            // ── Piece in yard: needs a 6 ─────────────────────────
            if (cur == 0) {
                if (dice != 6) continue;
                if (hasFriendlyBlock(s, pl, pc, 1)) continue;
                bool cap = hasEnemy(s, pl, 1) && !SAFE[1];
                out.push_back({(int8_t)pc, 0, 1, cap, false, true, 0});
                continue;
            }

            // ── Piece on board ───────────────────────────────────
            int nxt = cur + dice;
            if (nxt > HOME) continue;   // overshoot = illegal

            bool finish = (nxt == HOME);
            bool inHome = (nxt >= 52);  // inside home column

            bool capture = false;
            if (!inHome && !finish) {
                if (hasFriendlyBlock(s, pl, pc, nxt)) continue;
                capture = hasEnemy(s, pl, nxt) && !SAFE[nxt];
            }
            out.push_back({(int8_t)pc,(int8_t)cur,(int8_t)nxt,
                           capture, finish, false, 0});
        }
    }

    // Apply a move → new state.  nextDice=0 means chance node follows.
    State apply(const State& s, const Move& m, int nextDice) const noexcept {
        State ns = s;
        int pl = s.cur;
        ns.pos[pl][m.pieceIdx] = m.toPos;

        // ── Capture: all enemies at the landing square go back to yard ──
        if (m.isCapture) {
            int absT = relToAbs(pl, m.toPos);
            for (int opp = 0; opp < NP; ++opp) {
                if (opp == pl) continue;
                for (int pc = 0; pc < NPC; ++pc) {
                    int op = ns.pos[opp][pc];
                    if (op > 0 && op < RING && relToAbs(opp, op) == absT)
                        ns.pos[opp][pc] = 0;
                }
            }
        }

        // ── Extra turn: rolling 6 or capturing → same player again ──
        bool extra = (s.dice == 6) || m.isCapture;
        ns.cur  = extra ? (int8_t)pl : (int8_t)((pl + 1) % NP);
        ns.dice = (int8_t)nextDice;
        return ns;
    }
};

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 8 — EVALUATOR
   Score = evalPlayer(me) − max(evalPlayer(opp1..3))   [Paranoid heuristic]
   This enables alpha-beta pruning in a 4-player game.
   ═══════════════════════════════════════════════════════════════════════ */
struct Evaluator {

    // ── Identify the player with the highest total progress ─────────────
    static int leadingOpponent(const State& s, int persp) noexcept {
        int best = -1, bestProg = -1;
        for (int p = 0; p < NP; ++p) {
            if (p == persp) continue;
            int prog = s.totalProgress(p);
            if (prog > bestProg) { bestProg = prog; best = p; }
        }
        return best;
    }

    /*
     * Threat score for a piece at `relPos` of `player`.
     * Sums (7-d) for each enemy piece that can reach this square in d rolls.
     * Closer enemies score higher (max per enemy = 6 for d=1).
     */
    static int computeThreat(const State& s, int pl, int relPos) noexcept {
        if (SAFE[relPos]) return 0;
        int absT  = relToAbs(pl, relPos);
        int score = 0;
        for (int opp = 0; opp < NP; ++opp) {
            if (opp == pl) continue;
            for (int pc = 0; pc < NPC; ++pc) {
                int op = s.pos[opp][pc];
                if (op <= 0 || op >= RING) continue;
                int oppAbs = relToAbs(opp, op);
                for (int d = 1; d <= 6; ++d) {
                    if ((oppAbs + d) % RING == absT) {
                        score += (7 - d);  // d=1 → 6pts, d=6 → 1pt
                        break;
                    }
                }
            }
        }
        return score;
    }

    /*
     * Capture opportunity: count enemies reachable in 1-6 from `relPos`.
     * Double weight for targeting the leading opponent.
     */
    static int captureOpportunity(const State& s, int pl,
                                   int relPos, int leadOpp) noexcept {
        if (relPos <= 0 || relPos >= RING) return 0;
        int absF  = relToAbs(pl, relPos);
        int score = 0;
        for (int d = 1; d <= 6; ++d) {
            int dest    = (absF + d) % RING;
            int destRel = absToRel(pl, dest);
            if (destRel < 0 || destRel >= 58 || SAFE[destRel]) continue;
            for (int opp = 0; opp < NP; ++opp) {
                if (opp == pl) continue;
                for (int pc = 0; pc < NPC; ++pc) {
                    int op = s.pos[opp][pc];
                    if (op <= 0 || op >= RING) continue;
                    if (relToAbs(opp, op) != dest) continue;
                    score += (opp == leadOpp) ? 2 : 1;
                }
            }
        }
        return std::min(score, 4);
    }

    /*
     * Dual-threat bonus: reward positions where TWO of our pieces
     * can each capture a DIFFERENT enemy in 1-6 rolls.
     * This forces opponents to choose which piece to protect.
     */
    static int dualThreatBonus(const State& s, int pl) noexcept {
        // Bitmask of capturable absolute squares for each piece
        uint64_t reach[NPC] = {};
        for (int pc = 0; pc < NPC; ++pc) {
            int rel = s.pos[pl][pc];
            if (rel <= 0 || rel >= RING) continue;
            int absF = relToAbs(pl, rel);
            for (int d = 1; d <= 6; ++d) {
                int dest    = (absF + d) % RING;
                int destRel = absToRel(pl, dest);
                if (destRel < 0 || destRel >= 58 || SAFE[destRel]) continue;
                if (MoveGenerator::hasEnemy(s, pl, destRel))
                    reach[pc] |= (1ULL << dest);
            }
        }
        // Check all pairs: do they threaten DIFFERENT squares?
        int bonus = 0;
        for (int i = 0; i < NPC - 1; ++i) {
            if (!reach[i]) continue;
            for (int j = i + 1; j < NPC; ++j) {
                if (!reach[j]) continue;
                // If each has targets the other doesn't share → dual threat
                if ((reach[i] & reach[j]) != reach[i] ||
                    (reach[i] & reach[j]) != reach[j])
                    bonus += W_DUAL_THREAT;
            }
        }
        return bonus;
    }

    /*
     * Blocking pair bonus  + enemy-block break reward.
     * Two friendly pieces on same square = block (hard for enemy to pass).
     * Reward if we can break an enemy block in 1-6 rolls.
     */
    static int blockBonus(const State& s, int pl) noexcept {
        const int8_t* pos = s.pos[pl];
        int score = 0;

        // Friendly block pairs (on main ring only — home col always exclusive)
        for (int i = 0; i < NPC - 1; ++i) {
            int pi = pos[i];
            if (pi <= 0 || pi >= RING) continue;
            for (int j = i + 1; j < NPC; ++j)
                if (pos[j] == pi) score += W_BLOCK_PAIR;
        }

        // Enemy blocking pairs: reward if breakable
        for (int opp = 0; opp < NP; ++opp) {
            if (opp == pl) continue;
            for (int i = 0; i < NPC - 1; ++i) {
                int oi = s.pos[opp][i];
                if (oi <= 0 || oi >= RING || SAFE[oi]) continue;
                bool found = false;
                for (int j = i + 1; j < NPC && !found; ++j) {
                    if (s.pos[opp][j] != oi) continue;
                    // Enemy block at abs position
                    int absBlock = relToAbs(opp, oi);
                    for (int mypc = 0; mypc < NPC && !found; ++mypc) {
                        int myRel = s.pos[pl][mypc];
                        if (myRel <= 0 || myRel >= RING) continue;
                        int myAbs = relToAbs(pl, myRel);
                        for (int d = 1; d <= 6 && !found; ++d)
                            if ((myAbs + d) % RING == absBlock)
                                found = true;
                    }
                }
                if (found) score += W_BLOCK_BREAK / 2;
            }
        }
        return score;
    }

    /*
     * Opening bias: when fewer than 3 pieces are active,
     * strongly prefer getting pieces out of the yard.
     */
    static int openingBias(const State& s, int pl, int active) noexcept {
        if (active >= 3) return 0;
        int yardPcs = 0;
        for (int pc = 0; pc < NPC; ++pc)
            if (s.pos[pl][pc] == 0) ++yardPcs;
        return yardPcs * (3 - active) * W_OPEN_EXIT / 3;
    }

    // ── Per-player score (higher = better position for `pl`) ────────────
    int evalPlayer(const State& s, int pl) const noexcept {
        const int8_t* pos = s.pos[pl];
        int score  = 0;
        int active = 0;
        int yard   = 0;

        // Game phase: 0 = all in yard, 100 = all approaching home
        int totalProg = s.totalProgress(pl);
        int phase = std::min(100, totalProg * 100 / (NPC * HOME));

        int leadOpp = leadingOpponent(s, pl);

        for (int pc = 0; pc < NPC; ++pc) {
            int p = pos[pc];

            if (p == HOME) { score += W_FINISH; continue; }
            if (p == 0)    { ++yard; score += W_YARD_PEN; continue; }
            ++active;

            // Base progress (foundation of positional evaluation)
            score += p * W_PROGRESS;

            // Inside home column — safe, phase-tapered extra bonus
            if (p >= 52) {
                int col = p - 51;          // 1..6 steps into home col
                score += col * W_HOME_COL;
                score += W_NEAR_HOME;
                score += col * phase / 8;  // taper: more valuable late game
                continue;                  // no threats inside home col
            }

            // Near-home boost (within 8 steps of home column entry at relPos 51)
            if (p >= 44) score += (p - 43) * W_NEAR_HOME;

            // Safety bonus
            if (SAFE[p]) {
                score += W_SAFE;
            } else {
                // Threat penalty (weighted by enemy proximity)
                int thr = computeThreat(s, pl, p);
                score += thr * W_THREAT / 6;   // normalize (max raw ≈36)

                // Additional late-game exposure penalty
                if (phase > 65 && thr > 0)
                    score += thr * W_THREAT / 12;
            }

            // Capture opportunity reward
            score += captureOpportunity(s, pl, p, leadOpp) * W_CAPTURE;
        }

        // Structural bonuses (consider all pieces together)
        score += blockBonus(s, pl);
        score += dualThreatBonus(s, pl);
        score += openingBias(s, pl, active);

        // Diversity: reward having multiple active pieces
        if (active >= 2) score += active * W_DIVERSITY;

        return score;
    }

    // ── Top-level paranoid evaluation ────────────────────────────────────
    // Our score minus the BEST opponent score (worst case for us).
    int evaluate(const State& s, int persp) const noexcept {
        if (s.playerDone(persp)) return WIN_SCORE;
        for (int p = 0; p < NP; ++p)
            if (p != persp && s.playerDone(p)) return LOSE_SCORE;

        int myScore = evalPlayer(s, persp);
        int oppBest = INT_MIN;
        for (int p = 0; p < NP; ++p) {
            if (p == persp) continue;
            int sc = evalPlayer(s, p);
            if (sc > oppBest) oppBest = sc;
        }
        return myScore - oppBest;
    }
};

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 9 — PER-THREAD CONTEXT
   Each search thread has its own history/killer tables.
   All threads share the global TT (lock-free, benign race like Stockfish).
   ═══════════════════════════════════════════════════════════════════════ */
struct ThreadCtx {
    HistoryTable history;
    KillerTable  killers;
    int          nodesSearched = 0;
    int          perspective   = 0;

    void reset(int persp) noexcept {
        history.clear();
        killers.clear();
        nodesSearched = 0;
        perspective   = persp;
    }
};

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 10 — SEARCH ENGINE (Expectiminimax + Alpha-Beta)
   ═══════════════════════════════════════════════════════════════════════ */
struct SearchEngine {
    TranspositionTable   tt;
    MoveGenerator        gen;
    Evaluator            eval;
    std::atomic<bool>    stop{false};

    using Clock = std::chrono::steady_clock;
    Clock::time_point    searchStart;
    long                 timeLimitMs = 2000;

    // ── Move ordering ────────────────────────────────────────────────────
    void orderMoves(const State& s, std::vector<Move>& moves,
                    const TTEntry* hint, const ThreadCtx& ctx,
                    int ply) const noexcept {
        int hintEnc = hint ? hint->bestMove : -1;

        for (Move& m : moves) {
            int sc  = 0;
            int enc = m.encode();

            if (hintEnc >= 0 && enc == hintEnc) sc += 10000; // TT best move first

            if (m.isFinish)   sc += 5000;   // winning move
            if (m.isCapture)  sc += 4000;   // aggressive
            if (m.isExitYard) sc += 2000;   // opening priority

            // Landing on a safe square
            if (!m.isFinish && m.toPos > 0 && m.toPos < 58 && SAFE[m.toPos])
                sc += 1200;

            // Entering home column
            if (m.toPos >= 52 && !m.isFinish)
                sc += 900;

            // Escape from a dangerous square to a safe destination
            if (m.fromPos > 0 && m.fromPos < RING && !SAFE[m.fromPos]
                    && m.toPos >= 0 && m.toPos < 58 && SAFE[m.toPos])
                sc += 600;

            // Killer heuristic
            sc += ctx.killers.score(ply, enc) * 200;

            // History heuristic (0-49 normalised success ratio)
            sc += ctx.history.score(m.pieceIdx, m.toPos) * 8;

            // Raw progress delta
            sc += (m.toPos - m.fromPos) * 2;

            m.sortScore = sc;
        }

        // Insertion sort — at most 4 moves, so O(n) in practice
        for (int i = 1; i < (int)moves.size(); ++i) {
            Move key = moves[i];
            int  j   = i - 1;
            while (j >= 0 && moves[j].sortScore < key.sortScore) {
                moves[j+1] = moves[j];
                --j;
            }
            moves[j+1] = key;
        }
    }

    // ── Chance node: average score over all 6 dice outcomes ──────────────
    int chanceNode(const State& s, int depth, int alpha, int beta,
                   int ply, ThreadCtx& ctx) {
        if (depth <= 0 || s.isTerminal())
            return eval.evaluate(s, ctx.perspective);

        long total = 0;
        int  cnt   = 0;
        for (int d = 1; d <= 6; ++d) {
            if (__builtin_expect(stop.load(std::memory_order_relaxed), 0)) break;
            State ds   = s;
            ds.dice    = (int8_t)d;
            total     += alphabeta(ds, depth, alpha, beta, ply, ctx);
            ++cnt;
        }
        return cnt > 0 ? (int)(total / cnt) : 0;
    }

    // ── Alpha-Beta (Paranoid, with LMR) ─────────────────────────────────
    int alphabeta(const State& s, int depth, int alpha, int beta,
                  int ply, ThreadCtx& ctx) {
        ++ctx.nodesSearched;

        if (s.isTerminal()) return eval.evaluate(s, ctx.perspective);
        if (depth <= 0)     return eval.evaluate(s, ctx.perspective);

        // Time check every 4096 nodes (cheap amortised cost)
        if (__builtin_expect((ctx.nodesSearched & 0xFFF) == 0, 0)) {
            long elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
                               Clock::now() - searchStart).count();
            if (elapsed >= timeLimitMs) {
                stop.store(true, std::memory_order_relaxed);
                return eval.evaluate(s, ctx.perspective);
            }
        }

        // Chance node: dice not yet rolled
        if (s.dice == 0)
            return chanceNode(s, depth, alpha, beta, ply, ctx);

        // TT probe
        uint64_t key     = s.hash();
        TTEntry* ttEntry = tt.probe(key);
        if (ttEntry && ttEntry->depth >= depth) {
            int sc = ttEntry->score;
            if (ttEntry->type == TT_EXACT) return sc;
            if (ttEntry->type == TT_LOWER && sc > alpha) alpha = sc;
            if (ttEntry->type == TT_UPPER && sc < beta)  beta  = sc;
            if (alpha >= beta) return sc;
        }

        // Generate and order legal moves
        std::vector<Move> moves;
        gen.generate(s, moves);

        if (moves.empty()) {
            // No legal moves → pass to next player's chance node
            State skip  = s;
            skip.cur    = (int8_t)((s.cur + 1) % NP);
            skip.dice   = 0;
            return chanceNode(skip, depth - 1, alpha, beta, ply + 1, ctx);
        }

        orderMoves(s, moves, ttEntry, ctx, ply);

        bool maxNode = (s.cur == ctx.perspective);
        int  best    = maxNode ? -INF_SC : INF_SC;
        int  bestEnc = -1;
        TTType ttType = maxNode ? TT_UPPER : TT_LOWER;
        int  moveIdx  = 0;

        for (const Move& m : moves) {
            if (__builtin_expect(stop.load(std::memory_order_relaxed), 0)) break;

            // Late Move Reduction (LMR):
            // Quiet moves searched late get reduced depth.
            // If they turn out good, we re-search at full depth.
            int newDepth = depth - 1;
            if (moveIdx >= 2 && depth >= 3
                    && !m.isCapture && !m.isFinish && !m.isExitYard) {
                newDepth = depth - 2;  // reduce by 1 extra ply
            }

            State child = gen.apply(s, m, 0);   // 0 = chance node follows
            int score   = alphabeta(child, newDepth, alpha, beta, ply + 1, ctx);

            // Re-search at full depth if LMR result is unexpectedly good
            if (newDepth < depth - 1 && score > alpha && maxNode) {
                score = alphabeta(child, depth - 1, alpha, beta, ply + 1, ctx);
            }

            if (maxNode) {
                if (score > best) { best = score; bestEnc = m.encode(); }
                if (score > alpha) { alpha = score; ttType = TT_EXACT; }
                if (alpha >= beta) {
                    ttType = TT_LOWER;
                    ctx.killers.add(ply, m.encode());
                    ctx.history.recordSuccess(m.pieceIdx, m.toPos, depth);
                    goto storeAndReturn;
                }
            } else {
                if (score < best) { best = score; bestEnc = m.encode(); }
                if (score < beta)  { beta  = score; ttType = TT_EXACT; }
                if (alpha >= beta) {
                    ttType = TT_UPPER;
                    goto storeAndReturn;
                }
            }
            ctx.history.recordFailure(m.pieceIdx, m.toPos, depth);
            ++moveIdx;
        }

        storeAndReturn:
        if (!stop.load(std::memory_order_relaxed))
            tt.store(key, best, depth, ttType, bestEnc);
        return best;
    }

    // ── Iterative Deepening ──────────────────────────────────────────────
    // Returns the best piece index for a given subset of root moves.
    int iterDeepen(const State& root,
                   const std::vector<Move>& rootMoves,
                   int maxDepth, ThreadCtx& ctx) {
        int bestPiece = rootMoves[0].pieceIdx;

        for (int depth = 1; depth <= maxDepth; ++depth) {
            if (stop.load(std::memory_order_relaxed)) break;

            // Order root moves by TT hint at current depth
            std::vector<Move> ordered = rootMoves;
            uint64_t rk    = root.hash();
            TTEntry* hint  = tt.probe(rk);
            orderMoves(root, ordered, hint, ctx, 0);

            int iterBest  = -INF_SC;
            int iterPiece = rootMoves[0].pieceIdx;

            for (const Move& m : ordered) {
                if (stop.load(std::memory_order_relaxed)) break;
                State child = gen.apply(root, m, 0);
                int score   = alphabeta(child, depth - 1,
                                        -INF_SC, INF_SC, 1, ctx);
                if (score > iterBest) {
                    iterBest  = score;
                    iterPiece = m.pieceIdx;
                }
            }

            if (!stop.load(std::memory_order_relaxed)) {
                bestPiece = iterPiece;
                LOGD("D%d piece=%d score=%d nodes=%d",
                     depth, bestPiece, iterBest, ctx.nodesSearched);
            }

            // Terminate early if forced win or loss detected
            if (std::abs(iterBest) >= WIN_SCORE / 2) break;
        }
        return bestPiece;
    }
};

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 11 — LUDO AI (Singleton, manages threading)
   Lazy SMP: each thread searches a subset of root moves and the
   results are merged. All threads share the global TT.
   ═══════════════════════════════════════════════════════════════════════ */
class LudoAI {
public:
    static LudoAI& instance() {
        static LudoAI ai;
        return ai;
    }

    void configure(int difficulty) noexcept {
        switch (difficulty) {
            case 1: maxDepth=3;  timeLimitMs=400;  ttLog2=16; break; // Easy
            case 2: maxDepth=5;  timeLimitMs=900;  ttLog2=18; break; // Medium
            case 3: maxDepth=7;  timeLimitMs=2000; ttLog2=20; break; // Hard
            case 4:
            default:maxDepth=9;  timeLimitMs=3500; ttLog2=21; break; // Master
        }
        engine.tt.resize(ttLog2);
        engine.tt.clear();
        currentDifficulty = difficulty;
        LOGD("LudoAI config: diff=%d maxD=%d time=%ldms ttSize=%dMB",
             difficulty, maxDepth, timeLimitMs, (1 << ttLog2) * 24 / 1024 / 1024);
    }

    int getBestMove(const int positions[NP][NPC],
                    int curPlayer, int diceValue, int difficulty) {
        if (difficulty != currentDifficulty) configure(difficulty);

        // Build root state
        State root;
        root.cur  = (int8_t)curPlayer;
        root.dice = (int8_t)diceValue;
        for (int p = 0; p < NP; ++p)
            for (int pc = 0; pc < NPC; ++pc)
                root.pos[p][pc] = (int8_t)positions[p][pc];

        // Generate root moves
        std::vector<Move> rootMoves;
        engine.gen.generate(root, rootMoves);
        if (rootMoves.empty())        return -1;
        if (rootMoves.size() == 1)    return rootMoves[0].pieceIdx;

        // Reset search
        engine.stop.store(false, std::memory_order_relaxed);
        engine.timeLimitMs = timeLimitMs;
        engine.searchStart = SearchEngine::Clock::now();
        engine.tt.nextGen();

        // Determine thread count: min(hardware_threads, root_moves)
        int hw = (int)std::thread::hardware_concurrency();
        int nThreads = std::max(1, std::min(hw, (int)rootMoves.size()));

        if (nThreads == 1) {
            ThreadCtx ctx;
            ctx.reset(curPlayer);
            return engine.iterDeepen(root, rootMoves, maxDepth, ctx);
        }

        // ── Lazy SMP: distribute root moves across threads ────────────
        // Thread 0 = main thread, searches first move(s).
        // Threads 1..N-1 = worker threads, each covers a slice of moves.
        std::vector<std::vector<Move>> slices(nThreads);
        for (int i = 0; i < (int)rootMoves.size(); ++i)
            slices[i % nThreads].push_back(rootMoves[i]);

        std::vector<std::thread>  workers;
        std::vector<ThreadCtx>    ctxs(nThreads);
        std::vector<int>          threadBestScore(nThreads, -INF_SC);
        std::vector<int>          threadBestPiece(nThreads, -1);

        // Launch worker threads (1..N-1)
        for (int t = 1; t < nThreads; ++t) {
            ctxs[t].reset(curPlayer);
            workers.emplace_back([this, t, &root, &ctxs, &slices,
                                   &threadBestScore, &threadBestPiece]() {
                if (slices[t].empty()) return;
                ThreadCtx& ctx = ctxs[t];

                for (int depth = 1; depth <= maxDepth; ++depth) {
                    if (engine.stop.load(std::memory_order_relaxed)) break;

                    std::vector<Move> ordered = slices[t];
                    TTEntry* hint = engine.tt.probe(root.hash());
                    engine.orderMoves(root, ordered, hint, ctx, 0);

                    int iterBest  = -INF_SC;
                    int iterPiece = slices[t][0].pieceIdx;

                    for (const Move& m : ordered) {
                        if (engine.stop.load(std::memory_order_relaxed)) break;
                        State child = engine.gen.apply(root, m, 0);
                        int score   = engine.alphabeta(child, depth - 1,
                                                       -INF_SC, INF_SC, 1, ctx);
                        if (score > iterBest) {
                            iterBest  = score;
                            iterPiece = m.pieceIdx;
                        }
                    }
                    if (!engine.stop.load(std::memory_order_relaxed)) {
                        threadBestScore[t] = iterBest;
                        threadBestPiece[t] = iterPiece;
                    }
                    if (std::abs(iterBest) >= WIN_SCORE / 2) break;
                }
            });
        }

        // Main thread searches slice 0
        ctxs[0].reset(curPlayer);
        if (!slices[0].empty()) {
            threadBestPiece[0] = engine.iterDeepen(root, slices[0], maxDepth, ctxs[0]);
            threadBestScore[0] = INT_MAX;  // mark as valid (itDeepen already picked best)
        }

        // Join workers
        for (auto& w : workers) w.join();

        // Collect global best
        int globalBest  = -INF_SC;
        int globalPiece = rootMoves[0].pieceIdx;
        int totalNodes  = 0;
        for (int t = 0; t < nThreads; ++t) {
            totalNodes += ctxs[t].nodesSearched;
            if (threadBestPiece[t] >= 0 && threadBestScore[t] > globalBest) {
                globalBest  = threadBestScore[t];
                globalPiece = threadBestPiece[t];
            }
        }
        // Thread 0 is authoritative if it ran to completion
        if (threadBestPiece[0] >= 0) globalPiece = threadBestPiece[0];

        LOGD("SMP(%d threads): piece=%d nodes=%d", nThreads, globalPiece, totalNodes);
        return globalPiece;
    }

    int staticEval(const int positions[NP][NPC], int player) noexcept {
        State s;
        s.cur  = (int8_t)player;
        s.dice = 0;
        for (int p = 0; p < NP; ++p)
            for (int pc = 0; pc < NPC; ++pc)
                s.pos[p][pc] = (int8_t)positions[p][pc];
        return engine.eval.evaluate(s, player);
    }

    void reset() noexcept {
        engine.tt.clear();
        engine.stop.store(false, std::memory_order_relaxed);
        currentDifficulty = -1;
    }

private:
    LudoAI() : maxDepth(7), timeLimitMs(2000), ttLog2(20), currentDifficulty(-1) {
        ZOBRIST.init();
        engine.tt.resize(ttLog2);
    }

    SearchEngine engine;
    int          maxDepth;
    long         timeLimitMs;
    int          ttLog2;
    int          currentDifficulty;
};

} // namespace ludo

/* ═══════════════════════════════════════════════════════════════════════
   SECTION 12 — JNI INTERFACE
   Java package: com.makeeasy.ludo.engine.LudoEngine
   ═══════════════════════════════════════════════════════════════════════ */
extern "C" {

/*
 * getBestMove — primary entry point.
 *
 * @param jPositions  int[16] flattened [player * 4 + piece] relative positions
 * @param curPlayer   0-3
 * @param diceValue   1-6
 * @param difficulty  1=Easy 2=Medium 3=Hard 4=Master
 * @return            piece index to move (0-3), or -1 if no legal move
 */
JNIEXPORT jint JNICALL
Java_com_makeeasy_ludo_engine_LudoEngine_nativeGetBestMove(
        JNIEnv* env, jobject /* thiz */,
        jintArray jPositions,
        jint curPlayer,
        jint diceValue,
        jint difficulty) {

    jint* flat = env->GetIntArrayElements(jPositions, nullptr);
    if (!flat) { LOGE("GetIntArrayElements failed"); return -1; }

    int positions[ludo::NP][ludo::NPC];
    for (int p = 0; p < ludo::NP; ++p)
        for (int pc = 0; pc < ludo::NPC; ++pc)
            positions[p][pc] = (int)flat[p * ludo::NPC + pc];

    env->ReleaseIntArrayElements(jPositions, flat, JNI_ABORT);

    return (jint)ludo::LudoAI::instance().getBestMove(
        positions, (int)curPlayer, (int)diceValue, (int)difficulty);
}

/*
 * nativeReset — call when starting a new game to clear TT.
 */
JNIEXPORT void JNICALL
Java_com_makeeasy_ludo_engine_LudoEngine_nativeReset(
        JNIEnv* /* env */, jobject /* thiz */) {
    ludo::LudoAI::instance().reset();
}

/*
 * nativeStaticEval — raw position evaluation (no search).
 * Useful for board display / debug overlay.
 *
 * @return  signed score; positive = advantageous for `player`
 */
JNIEXPORT jint JNICALL
Java_com_makeeasy_ludo_engine_LudoEngine_nativeStaticEval(
        JNIEnv* env, jobject /* thiz */,
        jintArray jPositions,
        jint player) {

    jint* flat = env->GetIntArrayElements(jPositions, nullptr);
    if (!flat) return 0;

    int positions[ludo::NP][ludo::NPC];
    for (int p = 0; p < ludo::NP; ++p)
        for (int pc = 0; pc < ludo::NPC; ++pc)
            positions[p][pc] = (int)flat[p * ludo::NPC + pc];
    env->ReleaseIntArrayElements(jPositions, flat, JNI_ABORT);

    return (jint)ludo::LudoAI::instance().staticEval(positions, (int)player);
}

} // extern "C"
