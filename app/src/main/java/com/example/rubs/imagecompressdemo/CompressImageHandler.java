package com.example.rubs.imagecompressdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CompressImageHandler {
    private static final float maxHeight = 920.0f;
    private static final float maxWidth = 920.0f;

    public static int calculateSampleSize(BitmapFactory.Options options, int requireWidth, int requireHeight) {
        final int actualHeight = options.outHeight;
        final int actualWidth = options.outWidth;
        int sampleSize = 1;

        if (actualHeight > requireHeight || actualWidth > requireWidth) {
            final int heightRatio = Math.round((float) actualHeight / (float) requireHeight);
            final int widthRatio = Math.round((float) actualWidth / (float) requireWidth);
            sampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = actualWidth * actualHeight;
        final float totalReqPixelsCap = requireWidth * requireHeight * 2;
        while (totalPixels / (sampleSize * sampleSize) > totalReqPixelsCap) {
            sampleSize++;
        }
        return sampleSize;
    }
    public static String compressImage(String imagePath) {
        Bitmap scaledBitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        float imgRatio = (float) actualWidth / (float) actualHeight;
        float maxRatio = maxWidth / maxHeight;

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;
            }
        }
        options.inSampleSize = calculateSampleSize(options, actualWidth, actualHeight);
        options.inJustDecodeBounds = false;
        options.inDither = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];
        try {
            bitmap = BitmapFactory.decodeFile(imagePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.RGB_565);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        if (bitmap != null) {
            bitmap.recycle();
        }
        ExifInterface exif;
        try {
            exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            } else if (orientation == 3) {
                matrix.postRotate(180);
            } else if (orientation == 8) {
                matrix.postRotate(270);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        String filepath = MainActivity.imagePath;
        try {
            out = new FileOutputStream(filepath);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return filepath;
    }
    public static String createFile() {
        File imageFile = new File(Environment.getExternalStorageDirectory()
                + "/ImagecompressDemo/Images");
        if (!imageFile.exists()) {
            imageFile.mkdirs();
        }
        String imageName = "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
        String uri = (imageFile.getAbsolutePath() + "/" + imageName);
        return uri;
    }
    public static void copyFile(String selectedImagePath, String mdestinationPath) throws IOException {
        InputStream inputStream = new FileInputStream(selectedImagePath);
        OutputStream outputStream = new FileOutputStream(mdestinationPath);
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, len);
        }
        inputStream.close();
        outputStream.close();
    }
}
