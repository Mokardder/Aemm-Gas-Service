package android.iocl.dac_collector.Utility;

import android.iocl.dac_collector.BuildConfig;
import android.os.Build;

import java.util.Locale;

public class DeviceCheck {

    public enum DeviceType {
        XIAOMI,
        OTHER
    }

    public static DeviceType getDeviceType() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND.toLowerCase(Locale.ROOT);

        // Xiaomi/Redmi: always, regardless of version
        if (manufacturer.contains("xiaomi") || brand.contains("redmi")) {
            return DeviceType.XIAOMI;
        }


        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P || BuildConfig.DEBUG){
            return DeviceType.OTHER;
        }

        return null;

    }
}


