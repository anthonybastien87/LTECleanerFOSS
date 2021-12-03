package theredspy15.ltecleanerfosspro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import theredspy15.ltecleanerfosspro.ScheduledService.Companion.enqueueWork

class CleanReceiver : BroadcastReceiver() {
    override fun onReceive(ctxt: Context, i: Intent) {
        if (i.action == null) {
            enqueueWork(ctxt)
        } else {
            scheduleAlarm(ctxt)
        }
    }

    companion object {
        private const val PERIOD = 86400000
        private const val INITIAL_DELAY = 3600000 // 60 seconds
        @JvmStatic
        fun scheduleAlarm(context: Context) {
            val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, CleanReceiver::class.java)
            val pi: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            mgr.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INITIAL_DELAY,
                PERIOD.toLong(), pi
            )
        }

        @JvmStatic
        fun cancelAlarm(context: Context) {
            val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, CleanReceiver::class.java)
            val pi: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT)
            }
            mgr.cancel(pi)
        }
    }
}