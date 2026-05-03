package com.makeeasy.ludo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val adView = findViewById<LinearLayout>(R.id.adView)
        LudoActivity.admob.loadBanner(adView, false)
        findViewById<View>(R.id.menu).setOnClickListener {
            MenuDialog(this).show()
        }
        findViewById<View>(R.id.money).setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("You can share your real money app link here, to drive user from free to paid.")
                .setPositiveButton("OK") { dialog, _ -> dialog!!.dismiss() }.create().show()
        }
        findViewById<View>(R.id.p1).setOnClickListener {
            //LudoActivity.playerCount = 1
            //startActivity(Intent(this,LudoActivity::class.java))
        }
        findViewById<View>(R.id.p2).setOnClickListener {
            LudoActivity.playerCount = 2
            startActivity(Intent(this,LudoActivity::class.java))
        }
        findViewById<View>(R.id.p3).setOnClickListener {
            LudoActivity.playerCount = 3
            startActivity(Intent(this,LudoActivity::class.java))
        }
        findViewById<View>(R.id.p4).setOnClickListener {
            LudoActivity.playerCount = 4
            startActivity(Intent(this,LudoActivity::class.java))
        }
    }
}