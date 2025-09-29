package android.iocl.dac_collector.Receivers;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.util.Log;
import android.widget.RemoteViews;

public class TransparentWidget extends AppWidgetProvider {
    String TAG  = "AppWidgetServiceAEMM";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        WakeupHelper.wakeupAppService(context);
        Log.d(TAG, "onUpdate: Received Request");

    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {

            Log.d(TAG, "Widget ID: " + appWidgetId);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_transparent);

            // Example: Update widget text (optional)
            views.setTextViewText(R.id.widget_text, "Hello, Transparent!");

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
