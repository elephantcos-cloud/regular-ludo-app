package com.makeeasy.ludo

import android.annotation.SuppressLint
import android.content.Intent
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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.makeeasy.ludo.Result
import com.makeeasy.ludo.game.Dice
import com.makeeasy.ludo.game.GamePath

class LudoActivity : AppCompatActivity() {

    @SuppressLint("StaticFieldLeak")
    companion object {
        var playerCount = 0
        lateinit var admob: Admob
    }
    private val PLAY_TIME = 15000L
    private var height = 0
    private var width = 0
    private var top = 0
    private var bottom = 0
    private var d = 0
    private var number = 0
    private var playerNo = 0
    private var temp = 0
    private lateinit var dice: Dice
    private var extraChance = false
    private var isDestinationComplete = false
    private lateinit var jumping: MediaPlayer
    private lateinit var boing: MediaPlayer
    private lateinit var red1: ImageView
    private lateinit var red2: ImageView
    private lateinit var red3: ImageView
    private lateinit var red4: ImageView
    private lateinit var green1: ImageView
    private lateinit var green2: ImageView
    private lateinit var green3: ImageView
    private lateinit var green4: ImageView
    private lateinit var yellow1: ImageView
    private lateinit var yellow2: ImageView
    private lateinit var yellow3: ImageView
    private lateinit var yellow4: ImageView
    private lateinit var blue1: ImageView
    private lateinit var blue2: ImageView
    private lateinit var blue3: ImageView
    private lateinit var blue4: ImageView
    private lateinit var dice4Red: FrameLayout
    private lateinit var dice4Blue: FrameLayout
    private lateinit var dice4Green: FrameLayout
    private lateinit var dice4Yellow: FrameLayout
    private lateinit var walkedRed: IntArray
    private lateinit var walkedGreen: IntArray
    private lateinit var walkedBlue: IntArray
    private lateinit var walkedYellow: IntArray
    private lateinit var redPath: Array<String>
    private lateinit var greenPath: Array<String>
    private lateinit var bluePath: Array<String>
    private lateinit var yellowPath: Array<String>
    private lateinit var starsPath: Array<String>
    private lateinit var playerList: List<ImageView>
    private lateinit var winnerListm: IntArray
    private lateinit var mainView: RelativeLayout
    private var count: CountDownTimer? = null
    private lateinit var count1: TextView
    private lateinit var count2: TextView
    private lateinit var count3: TextView
    private lateinit var count4: TextView
    private lateinit var winnerList: MutableList<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ludo)
        boing = MediaPlayer.create(this,R.raw.boing)
        jumping = MediaPlayer.create(this,R.raw.jump)
        dice4Red = findViewById(R.id.redDice)
        dice4Blue = findViewById(R.id.blueDice)
        dice4Green = findViewById(R.id.greenDice)
        dice4Yellow = findViewById(R.id.yellowDice)
        red1 = findViewById(R.id.red1)
        red2 = findViewById(R.id.red2)
        red3 = findViewById(R.id.red3)
        red4 = findViewById(R.id.red4)
        blue1 = findViewById(R.id.blue1)
        blue2 = findViewById(R.id.blue2)
        blue3 = findViewById(R.id.blue3)
        blue4 = findViewById(R.id.blue4)
        green1 = findViewById(R.id.green1)
        green2 = findViewById(R.id.green2)
        green3 = findViewById(R.id.green3)
        green4 = findViewById(R.id.green4)
        yellow1 = findViewById(R.id.yellow1)
        yellow2 = findViewById(R.id.yellow2)
        yellow3 = findViewById(R.id.yellow3)
        yellow4 = findViewById(R.id.yellow4)
        mainView = findViewById(R.id.main)
        count1 = findViewById(R.id.count1)
        count2 = findViewById(R.id.count2)
        count3 = findViewById(R.id.count3)
        count4 = findViewById(R.id.count4)
        winnerList = ArrayList()
        walkedRed = intArrayOf(0,0,0,0)
        walkedGreen = intArrayOf(0,0,0,0)
        walkedBlue = intArrayOf(0,0,0,0)
        walkedYellow = intArrayOf(0,0,0,0)
        playerList = listOf(red1,red2,red3,red4,green1,green2,green3,green4,blue1,blue2,blue3,blue4,yellow1,yellow2,yellow3,yellow4)
        height = resources.displayMetrics.heightPixels
        width = resources.displayMetrics.widthPixels
        top = (height - width) / 2
        bottom = top + width / 2
        d = width / 15
        val gamePath = GamePath(d,top)
        redPath = gamePath.redPath()
        greenPath = gamePath.greenPath()
        bluePath = gamePath.bluePath()
        yellowPath = gamePath.yellowPath()
        starsPath = gamePath.starPath()
        for (element in playerList) {
            placePlayerInHome(element)
        }
        dice = Dice(this)
        dice.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        dice.setOnClickListener {
            var walked  = walkedRed
            var allGutty = arrayOf(red1,red2,red3,red4)
            when (playerNo) {
                1 -> {
                    walked = walkedRed
                    allGutty = arrayOf(red1,red2,red3,red4)
                }
                2 -> {
                    walked = walkedGreen
                    allGutty = arrayOf(green1,green2,green3,green4)
                }
                3 -> {
                    walked = walkedBlue
                    allGutty = arrayOf(blue1,blue2,blue3,blue4)
                }
                4 -> {
                    walked = walkedYellow
                    allGutty = arrayOf(yellow1,yellow2,yellow3,yellow4)
                }
            }
            dice.startRolling(object : Dice.OnRollingCompleteListener {
                override fun onComplete(num: Int) {
                    if(walked[0]>0 || walked[1]>0 || walked[2]>0 || walked[3]>0 || num==6) {
                        number = num
                        for(e in allGutty.indices) {
                            allGutty[e].bringToFront()
                            allGutty[e].isClickable = true
                        }
                    } else {
                        moveToNextPlayer()
                    }
                }
            })
        }
        red1.setOnClickListener {
            if(playerNo == 1) {
                guttyClickListener(0,red1,redPath)
            }
        }
        red2.setOnClickListener {
            if(playerNo == 1) {
                guttyClickListener(1,red2,redPath)
            }
        }
        red3.setOnClickListener {
            if(playerNo == 1) {
                guttyClickListener(2,red3,redPath)
            }
        }
        red4.setOnClickListener {
            if(playerNo == 1) {
                guttyClickListener(3,red4,redPath)
            }
        }
        green1.setOnClickListener {
            if(playerNo == 2) {
                guttyClickListener(0,green1,greenPath)
            }
        }
        green2.setOnClickListener {
            if(playerNo == 2) {
                guttyClickListener(1,green2,greenPath)
            }
        }
        green3.setOnClickListener {
            if(playerNo == 2) {
                guttyClickListener(2,green3,greenPath)
            }
        }
        green4.setOnClickListener {
            if(playerNo == 2) {
                guttyClickListener(3,green4,greenPath)
            }
        }
        blue1.setOnClickListener {
            if(playerNo == 3) {
                guttyClickListener(0,blue1,bluePath)
            }
        }
        blue2.setOnClickListener {
            if(playerNo == 3) {
                guttyClickListener(1,blue2,bluePath)
            }
        }
        blue3.setOnClickListener {
            if(playerNo == 3) {
                guttyClickListener(2,blue3,bluePath)
            }
        }
        blue4.setOnClickListener {
            if(playerNo == 3) {
                guttyClickListener(3,blue4,bluePath)
            }
        }
        yellow1.setOnClickListener {
            if(playerNo == 4) {
                guttyClickListener(0,yellow1,yellowPath)
            }
        }
        yellow2.setOnClickListener {
            if(playerNo == 4) {
                guttyClickListener(1,yellow2,yellowPath)
            }
        }
        yellow3.setOnClickListener {
            if(playerNo == 4) {
                guttyClickListener(2,yellow3,yellowPath)
            }
        }
        yellow4.setOnClickListener {
            if(playerNo == 4) {
                guttyClickListener(3,yellow4,yellowPath)
            }
        }
        playerNo = 1
        mainView.visibility = View.VISIBLE
        setDiceClickable()
    }

    private fun guttyClickListener(p:Int,gutty:ImageView,path:Array<String>) {
        if(playerNo == 1) {
            if(isMovePossible(walkedRed[p])) {
                count!!.cancel()
                temp = walkedRed[p]
                walkedRed[p] = walkedRed[p] + number
                if (number != 0) {
                    number -= 1
                }
                temp += 1
                val goto = temp - 1
                stepByStep(goto, gutty,path)
            } else if(walkedRed[p] == 0 && number == 6) {
                walkedRed[p] = 1
                val lp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp.leftMargin = path[0].split(",")[0].toInt()
                lp.topMargin = path[0].split(",")[1].toInt()
                gutty.layoutParams = lp
                jumping.start()
                extraChance = false
                setDiceClickable()
            }
        } else if(playerNo == 2) {
            if(isMovePossible(walkedGreen[p])) {
                count!!.cancel()
                temp = walkedGreen[p]
                walkedGreen[p] = walkedGreen[p] + number
                if (number != 0) {
                    number -= 1
                }
                temp += 1
                val goto = temp - 1
                stepByStep(goto, gutty,path)
            } else if(walkedGreen[p] == 0 && number == 6) {
                walkedGreen[p] = 1
                val lp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp.leftMargin = path[0].split(",")[0].toInt()
                lp.topMargin = path[0].split(",")[1].toInt()
                gutty.layoutParams = lp
                jumping.start()
                extraChance = false
                setDiceClickable()
            }
        } else if(playerNo == 3) {
            if(isMovePossible(walkedBlue[p])) {
                count!!.cancel()
                temp = walkedBlue[p]
                walkedBlue[p] = walkedBlue[p] + number
                if (number != 0) {
                    number -= 1
                }
                temp += 1
                val goto = temp - 1
                stepByStep(goto, gutty,path)
            } else if(walkedBlue[p] == 0 && number == 6) {
                walkedBlue[p] = 1
                val lp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp.leftMargin = path[0].split(",")[0].toInt()
                lp.topMargin = path[0].split(",")[1].toInt()
                gutty.layoutParams = lp
                jumping.start()
                extraChance = false
                setDiceClickable()
            }
        } else if(playerNo == 4) {
            if(isMovePossible(walkedYellow[p])) {
                count!!.cancel()
                temp = walkedYellow[p]
                walkedYellow[p] = walkedYellow[p] + number
                if (number != 0) {
                    number -= 1
                }
                temp += 1
                val goto = temp - 1
                stepByStep(goto, gutty,path)
            } else if(walkedYellow[p] == 0 && number == 6) {
                walkedYellow[p] = 1
                val lp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp.leftMargin = path[0].split(",")[0].toInt()
                lp.topMargin = path[0].split(",")[1].toInt()
                gutty.layoutParams = lp
                jumping.start()
                extraChance = false
                setDiceClickable()
            }
        }
    }

    private fun moveToNextPlayer() {
        if(playerCount == 1 || playerCount == 2) {
            playerNo = if(playerNo == 1) {
                3
            } else {
                1
            }
        } else {
            if(playerNo < playerCount) {
                playerNo += 1
            } else {
                playerNo = 1
            }
        }
        if(winnerList.contains(playerNo)) {
            moveToNextPlayer()
        } else {
            extraChance = false
            setDiceClickable()
        }
    }

    private fun isMovePossible(gutty: Int):Boolean {
        val total = gutty + number
        if(!extraChance) {
            extraChance = number == 6
        }
        isDestinationComplete = total == redPath.size
        return gutty > 0 && gutty < redPath.size && total <= redPath.size
    }

    private fun stepByStep(jump:Int,gutty:ImageView,path:Array<String>) {
        Handler(Looper.myLooper()!!).postDelayed({
            val lp = gutty.layoutParams as RelativeLayout.LayoutParams
            lp.leftMargin = path[jump].split(",")[0].toInt()
            lp.topMargin = path[jump].split(",")[1].toInt()
            gutty.layoutParams = lp
            if(jumping.isPlaying) {
                jumping.seekTo(0)
                jumping.start()
            } else {
                jumping.start()
            }
            if(number > 0) {
                temp += 1
                number -= 1
                val goto = temp - 1
                stepByStep(goto,gutty,path)
            } else {
                checkConflictOfPosition(gutty)
            }
        },500)
    }

    private fun placePlayerInHome(player: ImageView) {
        player.layoutParams.height = d - (d/10)
        player.layoutParams.width = d - (d/10)
        val lp = player.layoutParams as RelativeLayout.LayoutParams
        when (player.id) {
            R.id.red1 -> {
                lp.leftMargin = 3 * d / 2
                lp.topMargin = top + 3 * d / 2
                player.layoutParams = lp
                walkedRed[0] = 0
            }
            R.id.red2 -> {
                lp.leftMargin = 2 * d + 3 * d / 2
                lp.topMargin = top + 3 * d / 2
                player.layoutParams = lp
                walkedRed[1] = 0
            }
            R.id.red3 -> {
                lp.leftMargin = 3 * d / 2
                lp.topMargin = 2 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedRed[2] = 0
            }
            R.id.red4 -> {
                lp.leftMargin = 2 * d + 3 * d / 2
                lp.topMargin = 2 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedRed[3] = 0
            }
            R.id.green1 -> {
                lp.leftMargin = 9 * d + 3 * d / 2
                lp.topMargin = top + 3 * d / 2
                player.layoutParams = lp
                walkedGreen[0] = 0
            }
            R.id.green2 -> {
                lp.leftMargin = 11 * d + 3 * d / 2
                lp.topMargin = top + 3 * d / 2
                player.layoutParams = lp
                walkedGreen[1] = 0
            }
            R.id.green3 -> {
                lp.leftMargin = 9 * d + 3 * d / 2
                lp.topMargin = 2 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedGreen[2] = 0
            }
            R.id.green4 -> {
                lp.leftMargin = 11 * d + 3 * d / 2
                lp.topMargin = 2 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedGreen[3] = 0
            }
            R.id.blue1 -> {
                lp.leftMargin = 9 * d + 3 * d / 2
                lp.topMargin = 9 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedBlue[0] = 0
            }
            R.id.blue2 -> {
                lp.leftMargin = 11 * d + 3 * d / 2
                lp.topMargin = 9 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedBlue[1] = 0
            }
            R.id.blue3 -> {
                lp.leftMargin = 9 * d + 3 * d / 2
                lp.topMargin = 11 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedBlue[2] = 0
            }
            R.id.blue4 -> {
                lp.leftMargin = 11 * d + 3 * d / 2
                lp.topMargin = 11 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedBlue[3] = 0
            }
            R.id.yellow1 -> {
                lp.leftMargin = 3 * d / 2
                lp.topMargin = 9 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedYellow[0] = 0
            }
            R.id.yellow2 -> {
                lp.leftMargin = 2 * d + 3 * d / 2
                lp.topMargin = 9 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedYellow[1] = 0
            }
            R.id.yellow3 -> {
                lp.leftMargin = 3 * d / 2
                lp.topMargin = 11 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedYellow[2] = 0
            }
            R.id.yellow4 -> {
                lp.leftMargin = 2 * d + 3 * d / 2
                lp.topMargin = 11 * d + top + 3 * d / 2
                player.layoutParams = lp
                walkedYellow[3] = 0
            }
        }
    }

    private fun setDiceClickable() {
        if(count != null) {
            count!!.cancel()
        }
        setPlayerInactive()
        when (playerNo) {
            1 -> {
                dice4Red.addView(dice)
            }
            2 -> {
                dice4Green.addView(dice)
            }
            3 -> {
                dice4Blue.addView(dice)
            }
            4 -> {
                dice4Yellow.addView(dice)
            }
        }
        dice.active()
        count = object:CountDownTimer(PLAY_TIME,1000) {
            override fun onTick(millisUntilFinished: Long) {
                when (playerNo) {
                    1 -> {
                        count1.text = (millisUntilFinished/1000).toString()
                    }
                    2 -> {
                        count2.text = (millisUntilFinished/1000).toString()
                    }
                    3 -> {
                        count3.text = (millisUntilFinished/1000).toString()
                    }
                    4 -> {
                        count4.text = (millisUntilFinished/1000).toString()
                    }
                }
            }
            override fun onFinish() {
                moveToNextPlayer()
            }
        }
        count!!.start()
    }

    private fun setPlayerInactive() {
        dice4Red.removeAllViews()
        dice4Green.removeAllViews()
        dice4Blue.removeAllViews()
        dice4Yellow.removeAllViews()
        dice.reset()
        for (element in playerList) {
            element.isClickable = false
        }
        count1.text = ""
        count2.text = ""
        count3.text = ""
        count4.text = ""
    }

    private fun checkConflictOfPosition(gutty:ImageView) {
        var lp = ""
        var iv: ImageView? = null
        var actionRequired = false
        val reds = listOf(green1,green2,green3,green4,blue1,blue2,blue3,blue4,yellow1,yellow2,yellow3,yellow4)
        val greens = listOf(red1,red2,red3,red4,blue1,blue2,blue3,blue4,yellow1,yellow2,yellow3,yellow4)
        val blues = listOf(red1,red2,red3,red4,green1,green2,green3,green4,yellow1,yellow2,yellow3,yellow4)
        val yellows = listOf(red1,red2,red3,red4,green1,green2,green3,green4,blue1,blue2,blue3,blue4)
        if(playerNo == 1) {
            for(element in reds) {
                val plp = element.layoutParams as RelativeLayout.LayoutParams
                val alp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp = "${plp.leftMargin},${plp.topMargin}"
                if(plp.leftMargin == alp.leftMargin && plp.topMargin == alp.topMargin && !starsPath.contains(lp) && !reds.contains(gutty)) {
                    iv = element
                    actionRequired = true
                    break
                }
            }
        } else if(playerNo == 2) {
            for(element in greens) {
                val plp = element.layoutParams as RelativeLayout.LayoutParams
                val alp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp = "${plp.leftMargin},${plp.topMargin}"
                if(plp.leftMargin == alp.leftMargin && plp.topMargin == alp.topMargin && !starsPath.contains(lp) && !greens.contains(gutty)) {
                    iv = element
                    actionRequired = true
                    break
                }
            }
        } else if(playerNo == 3) {
            for(element in blues) {
                val plp = element.layoutParams as RelativeLayout.LayoutParams
                val alp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp = "${plp.leftMargin},${plp.topMargin}"
                if(plp.leftMargin == alp.leftMargin && plp.topMargin == alp.topMargin && !starsPath.contains(lp) && !blues.contains(gutty)) {
                    iv = element
                    actionRequired = true
                    break
                }
            }
        } else if(playerNo == 4) {
            for(element in yellows) {
                val plp = element.layoutParams as RelativeLayout.LayoutParams
                val alp = gutty.layoutParams as RelativeLayout.LayoutParams
                lp = "${plp.leftMargin},${plp.topMargin}"
                if(plp.leftMargin == alp.leftMargin && plp.topMargin == alp.topMargin && !starsPath.contains(lp) && !yellows.contains(gutty)) {
                    iv = element
                    actionRequired = true
                    break
                }
            }
        }
        if(actionRequired) {
            moveBackward(iv!!,lp)
            boing.start()
        } else {
            if(isDestinationComplete) {
                checkWinCase(gutty)
            } else if(extraChance) {
                extraChance = false
                setDiceClickable()
            } else {
                moveToNextPlayer()
            }
        }
    }

    private fun moveBackward(p:ImageView,c:String) {
        when (p.id) {
            R.id.red1, R.id.red2, R.id.red3, R.id.red4 -> {
                Handler(Looper.myLooper()!!).postDelayed({
                    val cp = redPath.indexOf(c)
                    if(cp > 0) {
                        val lp = p.layoutParams as RelativeLayout.LayoutParams
                        lp.leftMargin = redPath[cp - 1].split(",")[0].toInt()
                        lp.topMargin = redPath[cp - 1].split(",")[1].toInt()
                        p.layoutParams = lp
                        moveBackward(p,redPath[cp-1])
                    } else {
                        placePlayerInHome(p)
                        setDiceClickable()
                    }
                },100)
            }
            R.id.green1, R.id.green2, R.id.green3, R.id.green4 -> {
                Handler(Looper.myLooper()!!).postDelayed({
                    val cp = greenPath.indexOf(c)
                    if(cp > 0) {
                        val lp = p.layoutParams as RelativeLayout.LayoutParams
                        lp.leftMargin = greenPath[cp - 1].split(",")[0].toInt()
                        lp.topMargin = greenPath[cp - 1].split(",")[1].toInt()
                        p.layoutParams = lp
                        moveBackward(p,greenPath[cp-1])
                    } else {
                        placePlayerInHome(p)
                        setDiceClickable()
                    }
                },100)
            }
            R.id.blue1, R.id.blue2, R.id.blue3, R.id.blue4 -> {
                Handler(Looper.myLooper()!!).postDelayed({
                    val cp = bluePath.indexOf(c)
                    if(cp > 0) {
                        val lp = p.layoutParams as RelativeLayout.LayoutParams
                        lp.leftMargin = bluePath[cp - 1].split(",")[0].toInt()
                        lp.topMargin = bluePath[cp - 1].split(",")[1].toInt()
                        p.layoutParams = lp
                        moveBackward(p,bluePath[cp-1])
                    } else {
                        placePlayerInHome(p)
                        setDiceClickable()
                    }
                },100)
            }
            R.id.yellow1, R.id.yellow2, R.id.yellow3, R.id.yellow4 -> {
                Handler(Looper.myLooper()!!).postDelayed({
                    val cp = yellowPath.indexOf(c)
                    if(cp > 0) {
                        val lp = p.layoutParams as RelativeLayout.LayoutParams
                        lp.leftMargin = yellowPath[cp - 1].split(",")[0].toInt()
                        lp.topMargin = yellowPath[cp - 1].split(",")[1].toInt()
                        p.layoutParams = lp
                        moveBackward(p,yellowPath[cp-1])
                    } else {
                        placePlayerInHome(p)
                        setDiceClickable()
                    }
                },100)
            }
        }
    }

    private fun checkWinCase(p:ImageView) {
        var winner = 9
        var who: View? = null
        when (p.id) {
            R.id.red1, R.id.red2, R.id.red3, R.id.red4 -> {
                if(walkedRed[0]==57&&walkedRed[1]==57&&walkedRed[2]==57&&walkedRed[3]==57) {
                    winner = 1
                    who = findViewById(R.id.rankRed)
                }
            }
            R.id.green1, R.id.green2, R.id.green3, R.id.green4 -> {
                if(walkedRed[0]==57&&walkedRed[1]==57&&walkedRed[2]==57&&walkedRed[3]==57) {
                    winner = 2
                    who = findViewById(R.id.rankGreen)
                }
            }
            R.id.blue1, R.id.blue2, R.id.blue3, R.id.blue4 -> {
                if(walkedRed[0]==57&&walkedRed[1]==57&&walkedRed[2]==57&&walkedRed[3]==57) {
                    winner = 3
                    who = findViewById(R.id.rankBlue)
                }
            }
            R.id.yellow1, R.id.yellow2, R.id.yellow3, R.id.yellow4 -> {
                if(walkedRed[0]==57&&walkedRed[1]==57&&walkedRed[2]==57&&walkedRed[3]==57) {
                    winner = 4
                    who = findViewById(R.id.rankYellow)
                }
            }
        }
        if(winner != 9) {
            declareWinner(winner,who!!)
        } else if(extraChance) {
            extraChance = false
            setDiceClickable()
        } else {
            moveToNextPlayer()
        }
    }

    private fun declareWinner(winner: Int,who: View) {
        if(winnerList.isEmpty()) {
            winnerList.add(winner)
            who.setBackgroundResource(R.drawable.fst)
            who.visibility = View.VISIBLE
            if(playerCount == 1 || playerCount == 2) {
                Result(this,winnerList).show()
            } else {
                moveToNextPlayer()
            }
        } else if(winnerList.size == 1) {
            winnerList.add(winner)
            who.setBackgroundResource(R.drawable.scnd)
            who.visibility = View.VISIBLE
            if(playerCount == 3) {
                Result(this,winnerList).show()
            } else {
                moveToNextPlayer()
            }
        } else if(winnerList.size == 2) {
            winnerList.add(winner)
            who.setBackgroundResource(R.drawable.trd)
            who.visibility = View.VISIBLE
            Result(this,winnerList).show()
        }
    }
}