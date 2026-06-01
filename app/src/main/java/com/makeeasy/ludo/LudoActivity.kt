package com.makeeasy.ludo

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.makeeasy.ludo.engine.LudoAI
import com.makeeasy.ludo.game.Dice
import com.makeeasy.ludo.game.GamePath

class LudoActivity : AppCompatActivity() {

    @SuppressLint("StaticFieldLeak")
    companion object {
        var playerCount    = 0
        var isComputerMode = false
        lateinit var admob: Admob
    }

    private val PLAY_TIME = 15000L

    // AI — initialised after paths are ready
    private lateinit var ludoAI: LudoAI

    private var height = 0
    private var width  = 0
    private var top    = 0
    private var d      = 0
    private var number = 0
    private var playerNo = 0
    private var temp   = 0
    private lateinit var dice: Dice
    private var extraChance = false
    private var isDestinationComplete = false
    private lateinit var jumping: MediaPlayer
    private lateinit var boing:   MediaPlayer

    private lateinit var red1: ImageView;    private lateinit var red2: ImageView
    private lateinit var red3: ImageView;    private lateinit var red4: ImageView
    private lateinit var green1: ImageView;  private lateinit var green2: ImageView
    private lateinit var green3: ImageView;  private lateinit var green4: ImageView
    private lateinit var yellow1: ImageView; private lateinit var yellow2: ImageView
    private lateinit var yellow3: ImageView; private lateinit var yellow4: ImageView
    private lateinit var blue1: ImageView;   private lateinit var blue2: ImageView
    private lateinit var blue3: ImageView;   private lateinit var blue4: ImageView

    private lateinit var dice4Red:    FrameLayout
    private lateinit var dice4Blue:   FrameLayout
    private lateinit var dice4Green:  FrameLayout
    private lateinit var dice4Yellow: FrameLayout

    private lateinit var walkedRed:    IntArray
    private lateinit var walkedGreen:  IntArray
    private lateinit var walkedBlue:   IntArray
    private lateinit var walkedYellow: IntArray

    private lateinit var redPath:    Array<String>
    private lateinit var greenPath:  Array<String>
    private lateinit var bluePath:   Array<String>
    private lateinit var yellowPath: Array<String>
    private lateinit var starsPath:  Array<String>

    private lateinit var playerList: List<ImageView>
    private lateinit var mainView: RelativeLayout
    private var count: CountDownTimer? = null
    private lateinit var count1: TextView; private lateinit var count2: TextView
    private lateinit var count3: TextView; private lateinit var count4: TextView
    private lateinit var winnerList: MutableList<Int>

    private lateinit var redPlayerPanel:  LinearLayout
    private lateinit var bluePlayerPanel: LinearLayout

    private var hasRolledDice       = false
    private var lastMovedPieceIndex = -1
    private var lastMovedPlayerNo   = -1

    // ── Returns true if current player is AI-controlled ──────────
    // Yellow (playerNo=4) is always the human.
    private fun isAiPlayer(): Boolean = isComputerMode && playerNo != 4

    // ── Build walked state for the AI engine ─────────────────────
    private fun walkedState(): Array<IntArray> =
        arrayOf(walkedRed, walkedGreen, walkedBlue, walkedYellow)

    // ── Map playerNo (1-based) to AI player index (0-based) ──────
    private fun aiPlayerIndex(): Int = playerNo - 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ludo)

        boing   = MediaPlayer.create(this, R.raw.boing)
        jumping = MediaPlayer.create(this, R.raw.jump)

        dice4Red    = findViewById(R.id.redDice)
        dice4Blue   = findViewById(R.id.blueDice)
        dice4Green  = findViewById(R.id.greenDice)
        dice4Yellow = findViewById(R.id.yellowDice)

        redPlayerPanel  = findViewById(R.id.redPlayerPanel)
        bluePlayerPanel = findViewById(R.id.bluePlayerPanel)

        red1    = findViewById(R.id.red1);    red2    = findViewById(R.id.red2)
        red3    = findViewById(R.id.red3);    red4    = findViewById(R.id.red4)
        blue1   = findViewById(R.id.blue1);   blue2   = findViewById(R.id.blue2)
        blue3   = findViewById(R.id.blue3);   blue4   = findViewById(R.id.blue4)
        green1  = findViewById(R.id.green1);  green2  = findViewById(R.id.green2)
        green3  = findViewById(R.id.green3);  green4  = findViewById(R.id.green4)
        yellow1 = findViewById(R.id.yellow1); yellow2 = findViewById(R.id.yellow2)
        yellow3 = findViewById(R.id.yellow3); yellow4 = findViewById(R.id.yellow4)
        mainView = findViewById(R.id.main)
        count1 = findViewById(R.id.count1); count2 = findViewById(R.id.count2)
        count3 = findViewById(R.id.count3); count4 = findViewById(R.id.count4)

        winnerList   = ArrayList()
        walkedRed    = intArrayOf(0, 0, 0, 0)
        walkedGreen  = intArrayOf(0, 0, 0, 0)
        walkedBlue   = intArrayOf(0, 0, 0, 0)
        walkedYellow = intArrayOf(0, 0, 0, 0)

        playerList = listOf(
            red1, red2, red3, red4,
            green1, green2, green3, green4,
            blue1, blue2, blue3, blue4,
            yellow1, yellow2, yellow3, yellow4
        )

        height = resources.displayMetrics.heightPixels
        width  = resources.displayMetrics.widthPixels
        top    = (height - width) / 2
        d      = width / 15

        val gamePath = GamePath(d, top)
        redPath    = gamePath.redPath()
        greenPath  = gamePath.greenPath()
        bluePath   = gamePath.bluePath()
        yellowPath = gamePath.yellowPath()
        starsPath  = gamePath.starPath()

        // ── Initialise AI after paths are known ───────────────────
        ludoAI = LudoAI(
            paths      = arrayOf(redPath, greenPath, bluePath, yellowPath),
            safeCoords = starsPath.toSet()
        )

        for (element in playerList) placePlayerInHome(element)

        when (playerCount) {
            2 -> {
                redPlayerPanel.visibility  = View.GONE
                bluePlayerPanel.visibility = View.GONE
            }
            3 -> bluePlayerPanel.visibility = View.GONE
        }

        dice = Dice(this)
        dice.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        dice.setOnClickListener {
            hasRolledDice = true
            val walked:   IntArray
            val allGutty: Array<ImageView>
            val path:     Array<String>
            when (playerNo) {
                1    -> { walked=walkedRed;    allGutty=arrayOf(red1,red2,red3,red4);             path=redPath    }
                2    -> { walked=walkedGreen;  allGutty=arrayOf(green1,green2,green3,green4);     path=greenPath  }
                3    -> { walked=walkedBlue;   allGutty=arrayOf(blue1,blue2,blue3,blue4);         path=bluePath   }
                else -> { walked=walkedYellow; allGutty=arrayOf(yellow1,yellow2,yellow3,yellow4); path=yellowPath }
            }
            dice.startRolling(object : Dice.OnRollingCompleteListener {
                override fun onComplete(num: Int) {
                    number = num
                    val movable = allGutty.indices.filter { i ->
                        val w = walked[i]
                        (w == 0 && num == 6) ||
                        (w in 1 until 57 && w + num <= 57)
                    }
                    when {
                        movable.isEmpty() -> moveToNextPlayer()

                        // ── AI picks the best piece ───────────────
                        isAiPlayer() -> {
                            Handler(Looper.myLooper()!!).postDelayed({
                                val best = ludoAI.getBestMove(
                                    walked    = walkedState(),
                                    playerIdx = aiPlayerIndex(),
                                    dice      = num
                                )
                                val pieceToMove = if (best >= 0 && movable.contains(best))
                                    best else movable[0]
                                guttyClickListener(pieceToMove, allGutty[pieceToMove], path)
                            }, 700)
                        }

                        movable.size == 1 ->
                            guttyClickListener(movable[0], allGutty[movable[0]], path)

                        else -> movable.forEach { i ->
                            allGutty[i].bringToFront()
                            allGutty[i].isClickable = true
                        }
                    }
                }
            })
        }

        // Human piece click listeners (only active when it's their turn)
        red1.setOnClickListener    { if (playerNo==1 && !isAiPlayer()) guttyClickListener(0,red1,redPath) }
        red2.setOnClickListener    { if (playerNo==1 && !isAiPlayer()) guttyClickListener(1,red2,redPath) }
        red3.setOnClickListener    { if (playerNo==1 && !isAiPlayer()) guttyClickListener(2,red3,redPath) }
        red4.setOnClickListener    { if (playerNo==1 && !isAiPlayer()) guttyClickListener(3,red4,redPath) }
        green1.setOnClickListener  { if (playerNo==2 && !isAiPlayer()) guttyClickListener(0,green1,greenPath) }
        green2.setOnClickListener  { if (playerNo==2 && !isAiPlayer()) guttyClickListener(1,green2,greenPath) }
        green3.setOnClickListener  { if (playerNo==2 && !isAiPlayer()) guttyClickListener(2,green3,greenPath) }
        green4.setOnClickListener  { if (playerNo==2 && !isAiPlayer()) guttyClickListener(3,green4,greenPath) }
        blue1.setOnClickListener   { if (playerNo==3 && !isAiPlayer()) guttyClickListener(0,blue1,bluePath) }
        blue2.setOnClickListener   { if (playerNo==3 && !isAiPlayer()) guttyClickListener(1,blue2,bluePath) }
        blue3.setOnClickListener   { if (playerNo==3 && !isAiPlayer()) guttyClickListener(2,blue3,bluePath) }
        blue4.setOnClickListener   { if (playerNo==3 && !isAiPlayer()) guttyClickListener(3,blue4,bluePath) }
        yellow1.setOnClickListener { if (playerNo==4) guttyClickListener(0,yellow1,yellowPath) }
        yellow2.setOnClickListener { if (playerNo==4) guttyClickListener(1,yellow2,yellowPath) }
        yellow3.setOnClickListener { if (playerNo==4) guttyClickListener(2,yellow3,yellowPath) }
        yellow4.setOnClickListener { if (playerNo==4) guttyClickListener(3,yellow4,yellowPath) }

        playerNo = 4
        mainView.visibility = View.VISIBLE
        setDiceClickable()
    }

    private fun guttyClickListener(p: Int, gutty: ImageView, path: Array<String>) {
        val walked = when (playerNo) {
            1    -> walkedRed
            2    -> walkedGreen
            3    -> walkedBlue
            else -> walkedYellow
        }
        if (isMovePossible(walked[p])) {
            for (element in playerList) element.isClickable = false
            count?.cancel()
            lastMovedPieceIndex = p
            lastMovedPlayerNo   = playerNo
            temp = walked[p]
            walked[p] += number
            if (number != 0) number -= 1
            temp += 1
            stepByStep(temp - 1, gutty, path)
        } else if (walked[p] == 0 && number == 6) {
            for (element in playerList) element.isClickable = false
            count?.cancel()
            lastMovedPieceIndex = p
            lastMovedPlayerNo   = playerNo
            walked[p] = 1
            val lp = gutty.layoutParams as RelativeLayout.LayoutParams
            lp.leftMargin = path[0].split(",")[0].toInt()
            lp.topMargin  = path[0].split(",")[1].toInt()
            gutty.layoutParams = lp
            jumping.start()
            number = 0
            extraChance = false
            setDiceClickable()
        }
    }

    private fun moveToNextPlayer() {
        playerNo = when {
            playerCount == 2 -> if (playerNo == 4) 2 else 4
            playerNo == 4    -> 1
            playerNo == 1    -> 2
            playerNo == 2    -> if (playerCount == 4) 3 else 4
            playerNo == 3    -> 4
            else             -> 4
        }
        if (winnerList.contains(playerNo)) moveToNextPlayer()
        else { extraChance = false; setDiceClickable() }
    }

    private fun isMovePossible(guttyWalked: Int): Boolean {
        val total = guttyWalked + number
        if (!extraChance) extraChance = number == 6
        isDestinationComplete = total == 57
        return guttyWalked > 0 && guttyWalked < 57 && total <= 57
    }

    private fun stepByStep(jump: Int, gutty: ImageView, path: Array<String>) {
        Handler(Looper.myLooper()!!).postDelayed({
            val lp = gutty.layoutParams as RelativeLayout.LayoutParams
            lp.leftMargin = path[jump].split(",")[0].toInt()
            lp.topMargin  = path[jump].split(",")[1].toInt()
            gutty.layoutParams = lp
            if (jumping.isPlaying) { jumping.seekTo(0); jumping.start() } else jumping.start()
            if (number > 0) {
                temp += 1; number -= 1
                stepByStep(temp - 1, gutty, path)
            } else {
                checkConflictOfPosition(gutty)
            }
        }, 500)
    }

    private fun placePlayerInHome(player: ImageView) {
        player.layoutParams.height = d - (d / 10)
        player.layoutParams.width  = d - (d / 10)
        val lp = player.layoutParams as RelativeLayout.LayoutParams
        when (player.id) {
            R.id.red1    -> { lp.leftMargin=3*d/2;       lp.topMargin=top+3*d/2;       player.layoutParams=lp; walkedRed[0]=0    }
            R.id.red2    -> { lp.leftMargin=2*d+3*d/2;   lp.topMargin=top+3*d/2;       player.layoutParams=lp; walkedRed[1]=0    }
            R.id.red3    -> { lp.leftMargin=3*d/2;       lp.topMargin=2*d+top+3*d/2;   player.layoutParams=lp; walkedRed[2]=0    }
            R.id.red4    -> { lp.leftMargin=2*d+3*d/2;   lp.topMargin=2*d+top+3*d/2;   player.layoutParams=lp; walkedRed[3]=0    }
            R.id.green1  -> { lp.leftMargin=9*d+3*d/2;   lp.topMargin=top+3*d/2;       player.layoutParams=lp; walkedGreen[0]=0  }
            R.id.green2  -> { lp.leftMargin=11*d+3*d/2;  lp.topMargin=top+3*d/2;       player.layoutParams=lp; walkedGreen[1]=0  }
            R.id.green3  -> { lp.leftMargin=9*d+3*d/2;   lp.topMargin=2*d+top+3*d/2;   player.layoutParams=lp; walkedGreen[2]=0  }
            R.id.green4  -> { lp.leftMargin=11*d+3*d/2;  lp.topMargin=2*d+top+3*d/2;   player.layoutParams=lp; walkedGreen[3]=0  }
            R.id.yellow1 -> { lp.leftMargin=3*d/2;       lp.topMargin=9*d+top+3*d/2;   player.layoutParams=lp; walkedYellow[0]=0 }
            R.id.yellow2 -> { lp.leftMargin=2*d+3*d/2;   lp.topMargin=9*d+top+3*d/2;   player.layoutParams=lp; walkedYellow[1]=0 }
            R.id.yellow3 -> { lp.leftMargin=3*d/2;       lp.topMargin=11*d+top+3*d/2;  player.layoutParams=lp; walkedYellow[2]=0 }
            R.id.yellow4 -> { lp.leftMargin=2*d+3*d/2;   lp.topMargin=11*d+top+3*d/2;  player.layoutParams=lp; walkedYellow[3]=0 }
            R.id.blue1   -> { lp.leftMargin=9*d+3*d/2;   lp.topMargin=9*d+top+3*d/2;   player.layoutParams=lp; walkedBlue[0]=0   }
            R.id.blue2   -> { lp.leftMargin=11*d+3*d/2;  lp.topMargin=9*d+top+3*d/2;   player.layoutParams=lp; walkedBlue[1]=0   }
            R.id.blue3   -> { lp.leftMargin=9*d+3*d/2;   lp.topMargin=11*d+top+3*d/2;  player.layoutParams=lp; walkedBlue[2]=0   }
            R.id.blue4   -> { lp.leftMargin=11*d+3*d/2;  lp.topMargin=11*d+top+3*d/2;  player.layoutParams=lp; walkedBlue[3]=0   }
        }
    }

    private fun setDiceClickable() {
        count?.cancel()
        setPlayerInactive()
        hasRolledDice = false
        when (playerNo) {
            1    -> { dice4Red.addView(dice);    count1.visibility=View.VISIBLE }
            2    -> { dice4Green.addView(dice);  count2.visibility=View.VISIBLE }
            3    -> { dice4Blue.addView(dice);   count3.visibility=View.VISIBLE }
            else -> { dice4Yellow.addView(dice); count4.visibility=View.VISIBLE }
        }
        dice.active()

        // ── AI auto-rolls ─────────────────────────────────────────
        if (isAiPlayer()) {
            Handler(Looper.myLooper()!!).postDelayed({
                dice.performClick()
            }, 1000)
            return   // No countdown timer for AI
        }

        // Human countdown
        count = object : CountDownTimer(PLAY_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val s = (millisUntilFinished / 1000).toString()
                when (playerNo) {
                    1    -> count1.text = s
                    2    -> count2.text = s
                    3    -> count3.text = s
                    else -> count4.text = s
                }
            }
            override fun onFinish() {
                if (!hasRolledDice) dice.performClick()
                else autoMoveLastPiece()
            }
        }
        count!!.start()
    }

    private fun autoMoveLastPiece() {
        val allGutty: Array<ImageView>
        val path:     Array<String>
        val walked:   IntArray
        when (playerNo) {
            1    -> { allGutty=arrayOf(red1,red2,red3,red4);             path=redPath;    walked=walkedRed    }
            2    -> { allGutty=arrayOf(green1,green2,green3,green4);     path=greenPath;  walked=walkedGreen  }
            3    -> { allGutty=arrayOf(blue1,blue2,blue3,blue4);         path=bluePath;   walked=walkedBlue   }
            else -> { allGutty=arrayOf(yellow1,yellow2,yellow3,yellow4); path=yellowPath; walked=walkedYellow }
        }
        val movable = allGutty.indices.filter { i ->
            val w = walked[i]
            (w==0 && number==6) || (w in 1 until 57 && w+number<=57)
        }
        if (movable.isEmpty()) { moveToNextPlayer(); return }
        val preferred = if (lastMovedPlayerNo==playerNo &&
            lastMovedPieceIndex>=0 && movable.contains(lastMovedPieceIndex))
            lastMovedPieceIndex else movable[0]
        guttyClickListener(preferred, allGutty[preferred], path)
    }

    private fun setPlayerInactive() {
        dice4Red.removeAllViews(); dice4Green.removeAllViews()
        dice4Blue.removeAllViews(); dice4Yellow.removeAllViews()
        dice.reset()
        for (element in playerList) element.isClickable = false
        count1.visibility=View.GONE; count1.text=""
        count2.visibility=View.GONE; count2.text=""
        count3.visibility=View.GONE; count3.text=""
        count4.visibility=View.GONE; count4.text=""
    }

    private fun checkConflictOfPosition(gutty: ImageView) {
        var lp=""; var iv: ImageView?=null; var actionRequired=false
        val reds    = listOf(green1,green2,green3,green4,blue1,blue2,blue3,blue4,yellow1,yellow2,yellow3,yellow4)
        val greens  = listOf(red1,red2,red3,red4,blue1,blue2,blue3,blue4,yellow1,yellow2,yellow3,yellow4)
        val blues   = listOf(red1,red2,red3,red4,green1,green2,green3,green4,yellow1,yellow2,yellow3,yellow4)
        val yellows = listOf(red1,red2,red3,red4,green1,green2,green3,green4,blue1,blue2,blue3,blue4)
        val opponents = when (playerNo) { 1->reds; 2->greens; 3->blues; else->yellows }
        for (element in opponents) {
            val plp = element.layoutParams as RelativeLayout.LayoutParams
            val alp = gutty.layoutParams   as RelativeLayout.LayoutParams
            lp = "${plp.leftMargin},${plp.topMargin}"
            if (plp.leftMargin==alp.leftMargin && plp.topMargin==alp.topMargin
                && !starsPath.contains(lp) && !opponents.contains(gutty)) {
                iv = element; actionRequired = true; break
            }
        }
        if (actionRequired) { moveBackward(iv!!, lp); boing.start() }
        else when {
            isDestinationComplete -> checkWinCase(gutty)
            extraChance           -> { extraChance=false; setDiceClickable() }
            else                  -> moveToNextPlayer()
        }
    }

    private fun moveBackward(p: ImageView, c: String) {
        val path = when (p.id) {
            R.id.red1,R.id.red2,R.id.red3,R.id.red4         -> redPath
            R.id.green1,R.id.green2,R.id.green3,R.id.green4 -> greenPath
            R.id.blue1,R.id.blue2,R.id.blue3,R.id.blue4     -> bluePath
            else                                              -> yellowPath
        }
        Handler(Looper.myLooper()!!).postDelayed({
            val cp = path.indexOf(c)
            if (cp > 0) {
                val lp = p.layoutParams as RelativeLayout.LayoutParams
                lp.leftMargin = path[cp-1].split(",")[0].toInt()
                lp.topMargin  = path[cp-1].split(",")[1].toInt()
                p.layoutParams = lp
                moveBackward(p, path[cp-1])
            } else {
                placePlayerInHome(p)
                extraChance = false
                setDiceClickable()
            }
        }, 100)
    }

    private fun checkWinCase(p: ImageView) {
        var winner=9; var who: View?=null
        when (p.id) {
            R.id.red1,R.id.red2,R.id.red3,R.id.red4 ->
                if (walkedRed[0]==57&&walkedRed[1]==57&&walkedRed[2]==57&&walkedRed[3]==57)
                { winner=1; who=findViewById(R.id.rankRed) }
            R.id.green1,R.id.green2,R.id.green3,R.id.green4 ->
                if (walkedGreen[0]==57&&walkedGreen[1]==57&&walkedGreen[2]==57&&walkedGreen[3]==57)
                { winner=2; who=findViewById(R.id.rankGreen) }
            R.id.blue1,R.id.blue2,R.id.blue3,R.id.blue4 ->
                if (walkedBlue[0]==57&&walkedBlue[1]==57&&walkedBlue[2]==57&&walkedBlue[3]==57)
                { winner=3; who=findViewById(R.id.rankBlue) }
            R.id.yellow1,R.id.yellow2,R.id.yellow3,R.id.yellow4 ->
                if (walkedYellow[0]==57&&walkedYellow[1]==57&&walkedYellow[2]==57&&walkedYellow[3]==57)
                { winner=4; who=findViewById(R.id.rankYellow) }
        }
        if (winner!=9) declareWinner(winner, who!!)
        else if (extraChance) { extraChance=false; setDiceClickable() }
        else moveToNextPlayer()
    }

    private fun declareWinner(winner: Int, who: View) {
        if (winnerList.isEmpty()) {
            winnerList.add(winner); who.setBackgroundResource(R.drawable.fst); who.visibility=View.VISIBLE
            if (playerCount==2) Result(this,winnerList).show() else moveToNextPlayer()
        } else if (winnerList.size==1) {
            winnerList.add(winner); who.setBackgroundResource(R.drawable.scnd); who.visibility=View.VISIBLE
            if (playerCount==3) Result(this,winnerList).show() else moveToNextPlayer()
        } else if (winnerList.size==2) {
            winnerList.add(winner); who.setBackgroundResource(R.drawable.trd); who.visibility=View.VISIBLE
            Result(this,winnerList).show()
        }
    }
}
