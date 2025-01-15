package android.iocl.dac_collector.Utility;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.iocl.dac_collector.Services.FloatingBallService;
import android.os.Build;
import android.util.Log;

public class WakeupHelper {


    public static void wakeupAppService(Context context) {

        Log.d(Utility.TAG, "Called wakeupAppService....");
        boolean runningFloating = Utility.isServiceRunning(context, FloatingBallService.class);
        boolean runningTimer = Utility.isServiceRunning(context, FixOppoAutoKill.class);

        if (!runningFloating) {
            Log.d(Utility.TAG, "Not Running FloatingBallService. Starting....");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, FloatingBallService.class));
            } else {
                context.startService(new Intent(context, FloatingBallService.class));
            }
        }
        if (!runningTimer) {
            Log.d(Utility.TAG, "Not Running FixOppoKill. Starting....");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, FixOppoAutoKill.class));
            } else {
                context.startService(new Intent(context, FixOppoAutoKill.class));
            }
        }

    }


    public static void scheduleAlarm(Context context, Class<?> receiverClass) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiverClass);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = System.currentTimeMillis() + (2 * 60 * 1000); // 2 minutes in milliseconds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

}
