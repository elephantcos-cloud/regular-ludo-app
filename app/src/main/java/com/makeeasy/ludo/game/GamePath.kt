package com.makeeasy.ludo.game

class GamePath(d:Int, top:Int) {

    private var redPath: Array<String> = arrayOf(
        "${d},${top+6*d}","${d*2},${top+6*d}","${d*3},${top+6*d}","${d*4},${top+6*d}","${d*5},${top+6*d}",
        "${d*6},${top+5*d}","${d*6},${top+4*d}","${d*6},${top+3*d}","${d*6},${top+2*d}","${d*6},${top+d}","${d*6},${top}",
        "${d*7},${top}","${d*8},${top}",
        "${d*8},${top+d}","${d*8},${top+2*d}","${d*8},${top+3*d}","${d*8},${top+4*d}","${d*8},${top+5*d}",
        "${d*9},${top+6*d}","${d*10},${top+6*d}","${d*11},${top+6*d}","${d*12},${top+6*d}","${d*13},${top+6*d}","${d*14},${top+6*d}",
        "${d*14},${top+7*d}","${d*14},${top+8*d}",
        "${d*13},${top+8*d}","${d*12},${top+8*d}","${d*11},${top+8*d}","${d*10},${top+8*d}","${d*9},${top+8*d}",
        "${d*8},${top+9*d}","${d*8},${top+10*d}","${d*8},${top+11*d}","${d*8},${top+12*d}","${d*8},${top+13*d}","${d*8},${top+14*d}",
        "${d*7},${top+14*d}","${d*6},${top+14*d}",
        "${d*6},${top+13*d}","${d*6},${top+12*d}","${d*6},${top+11*d}","${d*6},${top+10*d}","${d*6},${top+9*d}",
        "${d*5},${top+8*d}","${d*4},${top+8*d}","${d*3},${top+8*d}","${d*2},${top+8*d}","${d},${top+8*d}","0,${top+8*d}",
        "0,${top+7*d}","${d},${top+7*d}","${d*2},${top+7*d}","${d*3},${top+7*d}","${d*4},${top+7*d}","${d*5},${top+7*d}",
        "${d*6},${top+7*d}")
    private var greenPath: Array<String> = arrayOf(
        "${d*8},${top+d}","${d*8},${top+2*d}","${d*8},${top+3*d}","${d*8},${top+4*d}","${d*8},${top+5*d}",
        "${d*9},${top+6*d}","${d*10},${top+6*d}","${d*11},${top+6*d}","${d*12},${top+6*d}","${d*13},${top+6*d}","${d*14},${top+6*d}",
        "${d*14},${top+7*d}","${d*14},${top+8*d}",
        "${d*13},${top+8*d}","${d*12},${top+8*d}","${d*11},${top+8*d}","${d*10},${top+8*d}","${d*9},${top+8*d}",
        "${d*8},${top+9*d}","${d*8},${top+10*d}","${d*8},${top+11*d}","${d*8},${top+12*d}","${d*8},${top+13*d}","${d*8},${top+14*d}",
        "${d*7},${top+14*d}","${d*6},${top+14*d}",
        "${d*6},${top+13*d}","${d*6},${top+12*d}","${d*6},${top+11*d}","${d*6},${top+10*d}","${d*6},${top+9*d}",
        "${d*5},${top+8*d}","${d*4},${top+8*d}","${d*3},${top+8*d}","${d*2},${top+8*d}","${d},${top+8*d}","0,${top+8*d}",
        "0,${top+7*d}","0,${top+6*d}",
        "${d},${top+6*d}","${d*2},${top+6*d}","${d*3},${top+6*d}","${d*4},${top+6*d}","${d*5},${top+6*d}",
        "${d*6},${top+5*d}","${d*6},${top+4*d}","${d*6},${top+3*d}","${d*6},${top+2*d}","${d*6},${top+d}","${d*6},${top}",
        "${d*7},${top}","${d*7},${top+d}","${d*7},${top+2*d}","${d*7},${top+3*d}","${d*7},${top+4*d}","${d*7},${top+5*d}",
        "${d*7},${top+6*d}")
    private var bluePath: Array<String> = arrayOf(
        "${d*13},${top+8*d}","${d*12},${top+8*d}","${d*11},${top+8*d}","${d*10},${top+8*d}","${d*9},${top+8*d}",
        "${d*8},${top+9*d}","${d*8},${top+10*d}","${d*8},${top+11*d}","${d*8},${top+12*d}","${d*8},${top+13*d}","${d*8},${top+14*d}",
        "${d*7},${top+14*d}","${d*6},${top+14*d}",
        "${d*6},${top+13*d}","${d*6},${top+12*d}","${d*6},${top+11*d}","${d*6},${top+10*d}","${d*6},${top+9*d}",
        "${d*5},${top+8*d}","${d*4},${top+8*d}","${d*3},${top+8*d}","${d*2},${top+8*d}","${d},${top+8*d}","0,${top+8*d}",
        "0,${top+7*d}","0,${top+6*d}",
        "${d},${top+6*d}","${d*2},${top+6*d}","${d*3},${top+6*d}","${d*4},${top+6*d}","${d*5},${top+6*d}",
        "${d*6},${top+5*d}","${d*6},${top+4*d}","${d*6},${top+3*d}","${d*6},${top+2*d}","${d*6},${top+d}","${d*6},${top}",
        "${d*7},${top}","${d*8},${top}",
        "${d*8},${top+d}","${d*8},${top+2*d}","${d*8},${top+3*d}","${d*8},${top+4*d}","${d*8},${top+5*d}",
        "${d*9},${top+6*d}","${d*10},${top+6*d}","${d*11},${top+6*d}","${d*12},${top+6*d}","${d*13},${top+6*d}","${d*14},${top+6*d}",
        "${d*14},${top+7*d}","${d*13},${top+7*d}","${d*12},${top+7*d}","${d*11},${top+7*d}","${d*10},${top+7*d}","${d*9},${top+7*d}",
        "${d*8},${top+7*d}",
    )
    private var yellowPath: Array<String> = arrayOf(
        "${d*6},${top+13*d}","${d*6},${top+12*d}","${d*6},${top+11*d}","${d*6},${top+10*d}","${d*6},${top+9*d}",
        "${d*5},${top+8*d}","${d*4},${top+8*d}","${d*3},${top+8*d}","${d*2},${top+8*d}","${d},${top+8*d}","0,${top+8*d}",
        "0,${top+7*d}","0,${top+6*d}",
        "${d},${top+6*d}","${d*2},${top+6*d}","${d*3},${top+6*d}","${d*4},${top+6*d}","${d*5},${top+6*d}",
        "${d*6},${top+5*d}","${d*6},${top+4*d}","${d*6},${top+3*d}","${d*6},${top+2*d}","${d*6},${top+d}","${d*6},${top}",
        "${d*7},${top}","${d*8},${top}",
        "${d*8},${top+d}","${d*8},${top+2*d}","${d*8},${top+3*d}","${d*8},${top+4*d}","${d*8},${top+5*d}",
        "${d*9},${top+6*d}","${d*10},${top+6*d}","${d*11},${top+6*d}","${d*12},${top+6*d}","${d*13},${top+6*d}","${d*14},${top+6*d}",
        "${d*14},${top+7*d}","${d*14},${top+8*d}",
        "${d*13},${top+8*d}","${d*12},${top+8*d}","${d*11},${top+8*d}","${d*10},${top+8*d}","${d*9},${top+8*d}",
        "${d*8},${top+9*d}","${d*8},${top+10*d}","${d*8},${top+11*d}","${d*8},${top+12*d}","${d*8},${top+13*d}","${d*8},${top+14*d}",
        "${d*7},${top+14*d}","${d*7},${top+13*d}","${d*7},${top+12*d}","${d*7},${top+11*d}","${d*7},${top+10*d}","${d*7},${top+9*d}",
        "${d*7},${top+8*d}"
    )
    private val starsPath: Array<String> = arrayOf("${d*6},${top+13*d}","${d*2},${top+8*d}","${d},${top+6*d}","${d*6},${top+2*d}","${d*8},${top+d}","${d*12},${top+6*d}","${d*13},${top+8*d}","${d*8},${top+12*d}")

    fun redPath(): Array<String> {
        return redPath
    }
    fun greenPath(): Array<String> {
        return greenPath
    }
    fun bluePath(): Array<String> {
        return bluePath
    }
    fun yellowPath(): Array<String> {
        return yellowPath
    }
    fun starPath(): Array<String> {
        return starsPath
    }
}