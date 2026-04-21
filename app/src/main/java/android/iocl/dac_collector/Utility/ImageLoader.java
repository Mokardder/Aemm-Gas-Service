package android.iocl.dac_collector.Utility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static LruCache<String, Bitmap> cache;

    static {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8; // use 1/8th memory

        cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static void load(String url, ImageView imageView, int placeholder) {

        imageView.setImageResource(placeholder);
        imageView.setTag(url);


        Log.d(TAG, "load: loading " + url);

        Bitmap cached = cache.get(url);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            Log.d(TAG, "load: used from cached ");
            return;
        }



        new Thread(() -> {
            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.connect();

                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                if (bitmap != null) {
                    cache.put(url, bitmap);

                    imageView.post(() -> {
                        // Prevent wrong image in recycled views
                        if (imageView.getTag().equals(url)) {
                            imageView.setImageBitmap(bitmap);
                            Log.d(TAG, "load: used from url ");
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
