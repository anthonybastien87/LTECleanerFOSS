package theredspy15.ltecleanerfoss;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;

import theredspy15.ltecleanerfoss.controllers.MainActivity;

public class ScheduledService extends JobIntentService {
    private static final int UNIQUE_JOB_ID = 1337;

    public static void enqueueWork(Context ctxt) {
        enqueueWork(ctxt.getApplicationContext(), ScheduledService.class, UNIQUE_JOB_ID,
                new Intent(ctxt, ScheduledService.class));
    }

    @Override
    public void onHandleWork(@NonNull Intent i) {
        try {
            File path = Environment.getExternalStorageDirectory();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            // scanner setup
            FileScanner fs = new FileScanner(path,getApplicationContext())
                    .setEmptyDir(prefs.getBoolean("empty", false))
                    .setAutoWhite(prefs.getBoolean("auto_white", true))
                    .setDelete(true)
                    .setCorpse(prefs.getBoolean("corpse", false))
                    .setGUI(null)
                    .setContext(getApplicationContext())
                    .setUpFilters(
                            prefs.getBoolean("generic", true),
                            prefs.getBoolean("aggressive", false),
                            prefs.getBoolean("apk", false));

            // kilobytes found/freed text
            long kilobytesTotal = fs.startScan();
            String title = getApplicationContext().getString(R.string.clean_notification)+" "+MainActivity.convertSize(kilobytesTotal);
            makeStatusNotification(title,getApplicationContext());
        } catch (Exception e) {
            makeStatusNotification(e.toString(),getApplicationContext());
        }
    }

    static void makeStatusNotification(String message, Context context) {

        // Name of Notification Channel for verbose notifications of background work
        final CharSequence VERBOSE_NOTIFICATION_CHANNEL_NAME = "Verbose WorkManager Notifications";
        final String VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows notifications whenever work finishes";
        final CharSequence NOTIFICATION_TITLE = context.getString(R.string.notification_title);
        final String CHANNEL_ID = "VERBOSE_NOTIFICATION";
        final int NOTIFICATION_ID = 1;

        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, VERBOSE_NOTIFICATION_CHANNEL_NAME, importance);
            channel.setDescription(VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION);

            // Add the channel
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_cleaning_services_24)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[0]);

        // Show the notification
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
}
