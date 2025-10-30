package com.arwil.mk // Pastikan ini package utama Anda

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.arwil.mk.ui.reports.DebtReminderWorker
import com.arwil.mk.ui.home.AppDatabase // <-- Import
import com.arwil.mk.ui.wallet.Account // <-- Import
import kotlinx.coroutines.CoroutineScope // <-- Import
import kotlinx.coroutines.Dispatchers // <-- Import
import kotlinx.coroutines.launch // <-- Import
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        scheduleDailyReminder()
        createDefaultCashAccount()
    }

    private fun createNotificationChannel() {
        // Hanya untuk Android 8.0 (Oreo) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pengingat Hutang"
            val descriptionText = "Notifikasi untuk jatuh tempo pembayaran hutang"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Daftarkan channel ke sistem
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleDailyReminder() {
        // Buat work request untuk berjalan 1 kali setiap 24 jam
        val reminderWorkRequest = PeriodicWorkRequest.Builder(
            DebtReminderWorker::class.java,
            1, TimeUnit.DAYS
        ).build()

        // Enqueue pekerjaan ini secara unik
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DebtReminderWork", // Nama unik untuk pekerjaan ini
            ExistingPeriodicWorkPolicy.KEEP, // Jika sudah ada, jangan buat baru
            reminderWorkRequest
        )
    }

    private fun createDefaultCashAccount() {
        applicationScope.launch {
            val database = AppDatabase.getDatabase(this@MyApplication)
            val accountDao = database.accountDao()

            // Cek apakah akun "Cash" sudah ada (berdasarkan nama)
            val existingCash = accountDao.getAllAccountsAsList().find {
                it.name.equals("Cash", ignoreCase = true)
            }

            // Jika belum ada, buat baru
            if (existingCash == null) {
                val cashAccount = Account(
                    name = "Cash",
                    initialBalance = 0.0, // Saldo awal bisa 0
                    accountType = "Tunai", // Tipe default
                    iconResId = R.drawable.ic_wallet, // Ikon default
                    isDebtAccount = false, // Bukan hutang
                    dueDate = null,
                    reminderDaysBefore = null
                )
                accountDao.insertAccount(cashAccount)
            }
        }
    }
    companion object {
        // ID unik untuk channel notifikasi
        const val CHANNEL_ID = "DEBT_REMINDER_CHANNEL"
    }
}