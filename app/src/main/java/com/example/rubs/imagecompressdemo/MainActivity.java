package com.example.rubs.imagecompressdemo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    protected ImageView captureImage;
    private Uri picUri;
    private File picFile;
    public static String imagePath;
    protected int LOAD_IMAGE_FROM_CAMERA = 102,LOAD_IMAGE_FROM_GALLERY = 103;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imagePath = CompressImageHandler.getFilename();
        captureImage = (ImageView)findViewById(R.id.captureImage);
        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(MainActivity.this,captureImage);
                popupMenu.getMenuInflater().inflate(R.menu.menu_main,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.takePhoto) {
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            picFile = new File(imagePath);
                            picUri = Uri.fromFile(picFile);
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
                            startActivityForResult(cameraIntent, LOAD_IMAGE_FROM_CAMERA);
                        }else if (id == R.id.chooseFromGallery){
                            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            galleryIntent.setType("image/*");
                            galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
                            startActivityForResult(Intent.createChooser(galleryIntent, "Select an image"), LOAD_IMAGE_FROM_GALLERY);
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == LOAD_IMAGE_FROM_CAMERA) {
            new ImageCompression().execute(imagePath);
        } else if (requestCode == LOAD_IMAGE_FROM_GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            final String picturePath = cursor.getString(columnIndex);
            cursor.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CompressImageHandler.copyFile(picturePath, imagePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            new ImageCompression().execute(imagePath);

        }
    }
    public class ImageCompression extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            if (strings.length == 0 || strings[0] == null)
                return null;
            return CompressImageHandler.compressImage(strings[0]);
        }
        protected void onPostExecute(String imagePath) {
            captureImage.setImageBitmap(BitmapFactory.decodeFile(new File(imagePath).getAbsolutePath()));
            Toast.makeText(MainActivity.this,"Compress image is saved in your sd card(/ImagecompressDemo/Images)",Toast.LENGTH_LONG).show();
        }
    }
}
