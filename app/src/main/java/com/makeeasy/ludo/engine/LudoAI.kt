package com.makeeasy.ludo.engine

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  LudoAI — Pure-Kotlin Expert Ludo Engine  (no NDK/JNI)
 *
 *  Player indices:  0=Red  1=Green  2=Blue  3=Yellow
 *
 *  walked[p][i]:
 *    0        → piece is in home base (not yet entered)
 *    1 .. 56  → steps walked on the board path
 *    57       → piece has finished (reached centre)
 *
 *  How it works:
 *    • Converts each piece's walked-position to a board coordinate.
 *    • Checks every candidate move against a rich scoring function
 *      that mirrors how a skilled human player thinks:
 *        1. Win the game immediately
 *        2. Kill opponents (never pass up a kill)
 *        3. Escape a threatened square
 *        4. Enter a new piece (dice = 6 and pieces in home)
 *        5. Land on a safe / star square
 *        6. Avoid landing where opponents can kill us next turn
 *        7. Stack two pieces together for mutual protection
 *        8. Push the most-advanced piece closer to home
 *        9. Spread pieces out (don't leave all 4 in base)
 *       10. General forward progress bonus
 * ╚══════════════════════════════════════════════════════════════╝
 */
class LudoAI(
    /** paths[0]=red, paths[1]=green, paths[2]=blue, paths[3]=yellow.
     *  Each path is exactly 57 coordinate strings "x,y".
     *  path[0] = first board square after leaving home (walked=1)
     *  path[56] = the finishing square (walked=57).            */
    private val paths: Array<Array<String>>,

    /** Board coordinates of safe/star squares (no kill allowed). */
    private val safeCoords: Set<String>
) {

    // ── Constants ─────────────────────────────────────────────────
    companion object {
        const val FINISH      = 57   // walked value when piece finishes
        const val HOME_COL    = 51   // walked >= 51 → inside coloured home column (safe)
        // How many dice-steps ahead we consider "danger zone" for threat analysis
        private const val THREAT_HORIZON = 6
    }

    // ═════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ═════════════════════════════════════════════════════════════

    /**
     * Returns the index (0-3) of the piece that should be moved,
     * or -1 if no legal move exists.
     *
     * @param walked  Current state: walked[playerIdx][pieceIdx]
     * @param playerIdx  Which AI player is moving (0-3)
     * @param dice    Dice roll (1-6)
     */
    fun getBestMove(
        walked: Array<IntArray>,
        playerIdx: Int,
        dice: Int
    ): Int {
        val myWalked = walked[playerIdx]

        // Collect legal moves
        val candidates = (0..3).filter { i ->
            val w = myWalked[i]
            (w == 0 && dice == 6) ||
            (w in 1 until FINISH && w + dice <= FINISH)
        }

        return when {
            candidates.isEmpty() -> -1
            candidates.size == 1 -> candidates[0]
            else -> candidates.maxByOrNull { pieceIdx ->
                scoreMove(walked, playerIdx, pieceIdx, dice)
            } ?: candidates[0]
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  SCORING ENGINE
    // ═════════════════════════════════════════════════════════════

    private fun scoreMove(
        walked: Array<IntArray>,
        playerIdx: Int,
        pieceIdx: Int,
        dice: Int
    ): Int {
        val myWalked   = walked[playerIdx]
        val myPath     = paths[playerIdx]
        val curWalked  = myWalked[pieceIdx]
        val newWalked  = curWalked + dice

        var score = 0

        // ── 1. WIN IMMEDIATELY ────────────────────────────────────
        if (newWalked == FINISH) return 1_000_000

        // ── 2. ENTER FROM HOME BASE (dice = 6) ───────────────────
        val isEntering = (curWalked == 0)
        if (isEntering) {
            score += 4_000
            // Extra: if we kill an opponent on the entry square
            val entryCoord = myPath[0]   // walked=1 → path[0]
            val killsOnEntry = countKillsAt(walked, playerIdx, entryCoord)
            if (killsOnEntry > 0) score += killsOnEntry * 9_000
            // Prefer entering when we still have pieces at home
            val inHome = myWalked.count { it == 0 }
            score += inHome * 300
            return score   // entering is always decisive — return early
        }

        // ── From here: piece is already on the board ──────────────
        val newCoord   = coordOf(playerIdx, newWalked) ?: return 0
        val curCoord   = coordOf(playerIdx, curWalked)

        val newIsSafe  = isSafeSquare(newWalked, newCoord)
        val curIsSafe  = curCoord != null && isSafeSquare(curWalked, curCoord)

        // ── 3. KILL OPPONENTS ─────────────────────────────────────
        val kills = countKillsAt(walked, playerIdx, newCoord)
        if (kills > 0) {
            score += kills * 10_000
            // Bonus if the killed piece was far along (more damage)
            for (oppIdx in 0..3) {
                if (oppIdx == playerIdx) continue
                for (w in walked[oppIdx]) {
                    if (w in 1 until FINISH) {
                        val oc = coordOf(oppIdx, w)
                        if (oc == newCoord) score += w * 40
                    }
                }
            }
        }

        // ── 4. ESCAPE DANGER ──────────────────────────────────────
        //    If current square is threatened and new square is safer
        val curThreat = if (!curIsSafe && curCoord != null)
            threatLevel(walked, playerIdx, curWalked, myPath) else 0
        val newThreat = if (!newIsSafe)
            threatLevel(walked, playerIdx, newWalked, myPath) else 0

        if (curThreat > 0) {
            score += curThreat * 50          // reward for escaping danger
            if (newIsSafe) score += 5_000    // escaping TO a safe square is great
            else if (newThreat < curThreat) score += 2_000  // at least less dangerous
        }

        // ── 5. LAND ON SAFE / STAR SQUARE ────────────────────────
        if (newIsSafe && !curIsSafe) score += 3_000

        // ── 6. AVOID CREATING NEW DANGER ─────────────────────────
        if (!newIsSafe && newThreat > 0) {
            score -= newThreat * 60
        }

        // ── 7. STACK WITH OWN PIECE (mutual protection) ───────────
        val stackCount = myWalked.count { w ->
            w != curWalked && w == newWalked  // another piece already here
        }
        if (stackCount > 0) {
            if (newIsSafe) score += 2_500    // stack on safe = fortress
            else           score += 1_200    // stack on normal = some protection
        }

        // ── 8. PUSH MOST-ADVANCED PIECE ───────────────────────────
        val maxWalked = myWalked.filter { it in 1 until FINISH }.maxOrNull() ?: 0
        if (curWalked == maxWalked) score += 800

        // ── 9. SPREAD: prefer to have multiple pieces on board ────
        val activePieces = myWalked.count { it in 1 until FINISH }
        if (activePieces <= 1 && curWalked > 0) score += 400

        // ── 10. FORWARD PROGRESS ──────────────────────────────────
        score += newWalked * 20

        // ── 11. INSIDE HOME COLUMN → PUSH TO FINISH ──────────────
        if (newWalked >= HOME_COL) score += (newWalked - HOME_COL) * 80

        // ── 12. PENALISE LEAVING A SAFE SQUARE NEEDLESSLY ────────
        if (curIsSafe && !newIsSafe && kills == 0 && curThreat == 0) {
            score -= 1_500
        }

        return score
    }

    // ═════════════════════════════════════════════════════════════
    //  HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    /** Convert a walked-position to board coordinate string, or null. */
    private fun coordOf(playerIdx: Int, walked: Int): String? {
        if (walked <= 0 || walked > FINISH) return null
        val path = paths[playerIdx]
        val idx  = walked - 1
        return if (idx < path.size) path[idx] else null
    }

    /** Is this square a safe square (star or home column)? */
    private fun isSafeSquare(walked: Int, coord: String): Boolean =
        walked >= HOME_COL || safeCoords.contains(coord)

    /**
     * Count how many OPPONENT pieces (not on safe squares) occupy [coord].
     * These would be captured if we land there.
     */
    private fun countKillsAt(
        walked: Array<IntArray>,
        myIdx: Int,
        coord: String
    ): Int {
        if (safeCoords.contains(coord)) return 0   // safe square → can't kill
        var count = 0
        for (oppIdx in 0..3) {
            if (oppIdx == myIdx) continue
            for (w in walked[oppIdx]) {
                if (w in 1 until FINISH) {
                    val oc = coordOf(oppIdx, w) ?: continue
                    if (oc == coord) count++
                }
            }
        }
        return count
    }

    /**
     * Threat level at a position = how many different opponent pieces
     * can reach this coordinate within THREAT_HORIZON dice values.
     * Higher = more dangerous.
     */
    private fun threatLevel(
        walked: Array<IntArray>,
        myIdx: Int,
        myW: Int,
        myPath: Array<String>
    ): Int {
        val myCoord = coordOf(myIdx, myW) ?: return 0
        var threat  = 0
        for (oppIdx in 0..3) {
            if (oppIdx == myIdx) continue
            val oppPath = paths[oppIdx]
            for (oppW in walked[oppIdx]) {
                if (oppW <= 0 || oppW >= FINISH) continue
                for (roll in 1..THREAT_HORIZON) {
                    val oppTarget = oppW + roll
                    if (oppTarget > FINISH) break
                    val idx = oppTarget - 1
                    if (idx >= oppPath.size) break
                    if (oppPath[idx] == myCoord) {
                        // Weight by proximity: closer opponent = bigger threat
                        threat += (THREAT_HORIZON - roll + 1) * 10
                        break   // count this opponent piece once
                    }
                }
            }
        }
        return threat
    }
}
