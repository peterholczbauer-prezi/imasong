package com.example.holcz.imasong;

import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.holcz.imasong.songrecognition.AcrCloudSongRecognizerImpl;
import com.example.holcz.imasong.songrecognition.Song;
import com.example.holcz.imasong.songrecognition.SongRecognizer;
import com.example.holcz.imasong.songrecognition.SongRecognizerListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SongRecognizerListener {

    private static final String TAG = "MainActivity";
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private String mCurrentPhotoPath;

    private Song songResult;
    private boolean photoTaken = false;

    private TextView resultTextView;
    private ImageView photoView;

    protected SongRecognizer songRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.restartRecButton).setOnClickListener((l) -> startRecognition());
        findViewById(R.id.takePhotoButton).setOnClickListener((l) -> takePhoto());

        resultTextView = findViewById(R.id.result_text);
        photoView = findViewById(R.id.photoView);

        songRecognizer = initSongRecognizer();
    }

    private SongRecognizer initSongRecognizer() {
        SongRecognizer songRecognizer = new AcrCloudSongRecognizerImpl(this);
        songRecognizer.setSongRecognizerListener(this);
        return songRecognizer;
    }

    @Override
    protected void onRestart() {
        this.songRecognizer.restart();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        this.songRecognizer.stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (this.songResult != null) {
            this.songRecognizer.restart();
        }
        super.onResume();
    }

    private void takePhoto(){
        dispatchTakePictureIntent();
        startRecognition();
    }

    private void startRecognition() {
        this.songResult = null;
        resultTextView.setText(getString(R.string.recognizing));
        this.songRecognizer.start();
    }

    private void dispatchTakePictureIntent() {
        photoTaken = false;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.holcz.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            photoTaken = true;
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            photoView.setImageURI(contentUri);
            addPhotoToGallery(contentUri);
            addSongToBitmap();
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void addPhotoToGallery(Uri contentUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void addSongToBitmap(){
        if (songResult != null && photoTaken) {
            try {
                ExifInterface exifInterface = new ExifInterface(mCurrentPhotoPath);
                exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, songResult.toString());
                exifInterface.saveAttributes();
            } catch (IOException e) {

            }
        }
    }

    private void requestRecordAudioPermission() {
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.RECORD_AUDIO)
//                != PackageManagernager.PERMISSION_GRANTED) {
//
//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.READ_CONTACTS)) {
//
//                // Show an explanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//
//            } else {
//
//                // No explanation needed, we can request the permission.
//
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.READ_CONTACTS},
//                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
//
//                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
//                // app-defined int constant. The callback method gets the
//                // resultTextView of the request.
//            }
//        }
    }

    @Override
    public void onResult(Song song) {
        this.songResult = song;
        this.resultTextView.setText(song.toString());
    }

    @Override
    public void onError(String msg) {
        this.songResult = null;
        this.resultTextView.setText(msg);
    }
}
