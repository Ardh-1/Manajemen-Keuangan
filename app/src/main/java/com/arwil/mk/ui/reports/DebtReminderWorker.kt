package com.arwil.mk.ui.reports // Ganti package jika perlu

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arwil.mk.MainActivity
import com.arwil.mk.MyApplication
import com.arwil.mk.R
import com.arwil.mk.ui.home.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DebtReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val db = AppDatabase.getDatabase(context)
            val accounts = db.accountDao().getAllAccountsAsList()

            val today = Calendar.getInstance()

            accounts.forEach { account ->
                // Cek jika ini akun hutang, punya tanggal, dan punya pengingat
                if (account.isDebtAccount && account.dueDate != null && account.reminderDaysBefore != null) {

                    val dueDateCalendar = Calendar.getInstance().apply {
                        timeInMillis = account.dueDate
                    }

                    // Hitung tanggal pengingat
                    val reminderCalendar = Calendar.getInstance().apply {
                        timeInMillis = account.dueDate
                        add(Calendar.DAY_OF_YEAR, -account.reminderDaysBefore)
                    }

                    // Cek apakah "hari ini" adalah hari pengingat
                    if (today.get(Calendar.YEAR) == reminderCalendar.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == reminderCalendar.get(Calendar.DAY_OF_YEAR)) {

                        // Kirim Notifikasi!
                        sendNotification(account.name, dueDateCalendar)
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun sendNotification(accountName: String, dueDate: Calendar) {
        // Intent untuk membuka aplikasi saat notifikasi diklik
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val title = "Pengingat Pembayaran Hutang"
        val text = "Hutang Anda '$accountName' akan jatuh tempo pada ${sdf.format(dueDate.time)}."

        val builder = NotificationCompat.Builder(context, MyApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wallet) // Ganti dengan ikon notifikasi Anda
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Menutup notifikasi saat diklik

        try {
            // Tampilkan notifikasi
            // ID notifikasi (account.id) harus unik
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            // Ini terjadi jika izin POST_NOTIFICATIONS tidak diberikan
            e.printStackTrace()
        }
    }
}