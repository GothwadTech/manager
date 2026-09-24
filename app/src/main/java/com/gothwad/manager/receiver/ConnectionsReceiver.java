package com.gothwad.manager.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gothwad.manager.misc.ConnectionUtils;
import com.gothwad.manager.misc.NotificationUtils;
import com.gothwad.manager.service.ConnectionsService;

import static com.gothwad.manager.misc.ConnectionUtils.ACTION_FTPSERVER_STARTED;
import static com.gothwad.manager.misc.ConnectionUtils.ACTION_FTPSERVER_STOPPED;
import static com.gothwad.manager.misc.ConnectionUtils.ACTION_START_FTPSERVER;
import static com.gothwad.manager.misc.ConnectionUtils.ACTION_STOP_FTPSERVER;
import static com.gothwad.manager.misc.NotificationUtils.FTP_NOTIFICATION_ID;

public class ConnectionsReceiver extends BroadcastReceiver {

    static final String TAG = ConnectionsReceiver.class.getSimpleName();

    public ConnectionsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (ACTION_START_FTPSERVER.equals(action)) {
            Intent serverService = new Intent(context, ConnectionsService.class);
            serverService.putExtras(intent.getExtras());
            if (!ConnectionUtils.isServerRunning(context)) {
                context.startService(serverService);
            }
        } else if (ACTION_STOP_FTPSERVER.equals(action)) {
            Intent serverService = new Intent(context, ConnectionsService.class);
            serverService.putExtras(intent.getExtras());
            context.stopService(serverService);
        } else if (ACTION_FTPSERVER_STARTED.equals(action)) {
            NotificationUtils.createFtpNotification(context, intent, FTP_NOTIFICATION_ID);
        } else if (ACTION_FTPSERVER_STOPPED.equals(action)) {
            NotificationUtils.removeNotification(context, FTP_NOTIFICATION_ID);
        }

    }
}
