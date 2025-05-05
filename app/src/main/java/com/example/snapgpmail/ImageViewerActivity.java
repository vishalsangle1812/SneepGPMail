package com.example.snapgpmail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ImageViewerActivity extends AppCompatActivity {
    private static final String TAG = "ImageViewerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView imageView = findViewById(R.id.fullImageView);
        String imagePath = getIntent().getStringExtra("image_path");

        if (imagePath != null) {
            loadImageWithScaling(imageView, imagePath);
        } else {
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void loadImageWithScaling(ImageView imageView, String imagePath) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            // Calculate inSampleSize (power of 2)
            options.inSampleSize = calculateInSampleSize(options,
                    imageView.getWidth(),
                    imageView.getHeight());

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                showErrorAndFinish("Failed to load image");
            }
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory loading image", e);
            showErrorAndFinish("Image too large to display");
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            showErrorAndFinish("Error loading image");
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that's a power of 2 and
            // keeps both height and width larger than the requested dimensions
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        // Clear image view to free memory
        ImageView imageView = findViewById(R.id.fullImageView);
        imageView.setImageDrawable(null);
        super.onDestroy();
    }
}