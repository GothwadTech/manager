package com.gothwad.manager.misc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

import com.gothwad.manager.DocumentsActivity;
import com.gothwad.manager.R;
import com.gothwad.manager.model.RootInfo;
import com.gothwad.manager.setting.SettingsActivity;

import static com.gothwad.manager.misc.ConnectionUtils.ACTION_STOP_FTPSERVER;
import static com.gothwad.manager.misc.Utils.EXTRA_ROOT;

/**
 * Created by HaKr on 05/09/16.
 */

public class NotificationUtils {

    public static final int FTP_NOTIFICATION_ID = 916;
    public static final String FTP_CHANNEL_ID = "ftp_server_channel";

    public static void createFtpNotification(Context context, Intent intent, int notification_id){
        RootInfo root = intent.getExtras().getParcelable(EXTRA_ROOT);
        if(null == root){
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && notificationManager != null) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    FTP_CHANNEL_ID,
                    "FTP Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        long when = System.currentTimeMillis();

        CharSequence contentTitle = getString(context,R.string.ftp_notif_title);
        CharSequence contentText = String.format(getString(context,R.string.ftp_notif_text),
                ConnectionUtils.getFTPAddress(context));
        CharSequence tickerText = getString(context, R.string.ftp_notif_starting);
        CharSequence stopText = getString(context,R.string.ftp_notif_stop_server);

        Intent notificationIntent = new Intent(context, DocumentsActivity.class);
        notificationIntent.setData(root.getUri());
        notificationIntent.putExtras(intent.getExtras());
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, Utils.getPendingIntentFlags(0));
        Intent stopIntent = new Intent(ACTION_STOP_FTPSERVER);
        stopIntent.putExtras(intent.getExtras());
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0,
                stopIntent, Utils.getPendingIntentFlags(PendingIntent.FLAG_ONE_SHOT));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FTP_CHANNEL_ID)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_server)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true)
                .setColor(SettingsActivity.getPrimaryColor())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_action_stop, stopText, stopPendingIntent)
                .setShowWhen(false);

        Notification notification = builder.build();

        notificationManager.notify(notification_id, notification);
    }

    public static void removeNotification(Context context, int notification_id){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notification_id);
    }

    private static String getString(Context context, int id){
        return  context.getResources().getString(id);
    }
}
