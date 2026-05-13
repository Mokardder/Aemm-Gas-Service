package android.iocl.dac_collector.Utility;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageCompressor {

    public static File compressToSize(
            Context context,
            String inputPath,
            String outputName,
            long maxSizeMB
    ) {

        try {

            long maxBytes = maxSizeMB * 1024 * 1024;

            // Decode bounds
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(inputPath, options);

            // Reduce large image memory usage
            options.inSampleSize =
                    calculateInSampleSize(options, 1920, 1920);

            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(inputPath, options);

            if (bitmap == null) {
                return null;
            }

            Bitmap rotatedBitmap =
                    rotateBitmapIfRequired(bitmap, inputPath);

            // Create output file
            File outputFile = new File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    outputName + ".jpg"
            );

            if (outputFile.exists()) {
                outputFile.delete();
            }

            // Compress loop
            int quality = 100;

            ByteArrayOutputStream stream =
                    new ByteArrayOutputStream();

            do {

                stream.reset();

                rotatedBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        quality,
                        stream
                );

                quality -= 5;

            } while (stream.size() > maxBytes && quality > 5);

            // Save final image
            FileOutputStream fos =
                    new FileOutputStream(outputFile);

            fos.write(stream.toByteArray());

            fos.flush();
            fos.close();

            stream.close();

            // Cleanup
            bitmap.recycle();

            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle();
            }

            return outputFile;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Bitmap rotateBitmapIfRequired(
            Bitmap bitmap,
            String imagePath
    ) {

        try {

            ExifInterface exif =
                    new ExifInterface(imagePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Matrix matrix = new Matrix();

            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;

                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options,
            int reqWidth,
            int reqHeight
    ) {

        int height = options.outHeight;
        int width = options.outWidth;

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            int heightRatio =
                    Math.round((float) height / reqHeight);

            int widthRatio =
                    Math.round((float) width / reqWidth);

            inSampleSize = Math.min(heightRatio, widthRatio);
        }

        return Math.max(inSampleSize, 1);
    }
}