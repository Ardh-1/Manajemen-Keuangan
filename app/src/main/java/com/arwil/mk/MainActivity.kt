package com.arwil.mk

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.arwil.mk.ui.addTransaction.AddTransactionFragment
import com.arwil.mk.ui.home.HomeFragment
import com.arwil.mk.ui.charts.ChartsFragment
import com.arwil.mk.ui.home.AppDatabase
import kotlinx.coroutines.launch
import com.arwil.mk.ui.reports.ReportsFragment


class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val chartsFragment = ChartsFragment()
    private val reportsFragment = ReportsFragment()

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        //

        //



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            contentFrame.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right
            )


            bottomAppBar.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = systemBars.bottom
            }


            bottomNavView.updatePadding(bottom = systemBars.bottom)

            fab.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = systemBars.bottom + resources.getDimensionPixelSize(R.dimen.fab_margin_bottom)
            }

            insets
        }

        // Tampilkan HomeFragment sebagai default
        setCurrentFragment(homeFragment)

        fab.setOnClickListener {
            // Sekarang MainActivity yang menampilkan form
            val addTransactionFragment = AddTransactionFragment()
            addTransactionFragment.onTransactionAddedListener = { newTransaction ->
                // MainActivity yang menyimpan ke database
                lifecycleScope.launch {
                    db.transactionDao().insertTransaction(newTransaction)
                }
            }
            addTransactionFragment.show(supportFragmentManager, "AddTransactionFragment")
        }

        bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> setCurrentFragment(homeFragment)
                R.id.nav_reports -> setCurrentFragment(reportsFragment)
                R.id.nav_charts -> setCurrentFragment(chartsFragment)
                // R.id.nav_me -> setCurrentFragment(meFragment)
            }
            true
        }
    }
    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.content_frame, fragment)
            commit()
        }
    }
}