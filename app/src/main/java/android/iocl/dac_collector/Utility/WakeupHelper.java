package android.iocl.dac_collector.Utility;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.os.Build;

public class WakeupHelper {


    public static void wakeupAppService(Context context) {
        
        try {
            boolean runningTimer = Utility.isServiceRunning(context, FixOppoAutoKill.class);

            if (!runningTimer) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, FixOppoAutoKill.class));
                } else {
                    context.startService(new Intent(context, FixOppoAutoKill.class));
                }
            }

        } catch (Exception e) {


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
