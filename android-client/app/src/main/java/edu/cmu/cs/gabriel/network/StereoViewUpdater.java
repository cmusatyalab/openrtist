package edu.cmu.cs.gabriel.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.google.protobuf.ByteString;

import java.util.function.Consumer;

public class StereoViewUpdater implements Consumer<ByteString> {
    private static final String TAG = "StereoViewUpdater";
    private final ImageView imageView1;
    private final ImageView imageView2;

    public StereoViewUpdater(ImageView imageView1, ImageView imageView2) {
        this.imageView1 = imageView1;
        this.imageView2 = imageView2;
    }

    @Override
    public void accept(ByteString jpegByteString) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                jpegByteString.toByteArray(), 0, jpegByteString.size());
        if (bitmap == null) {
            Log.e(TAG, "decodeByteArray returned null!");
            return;
        }

        this.imageView1.post(() -> this.imageView1.setImageBitmap(bitmap));
        this.imageView2.post(() -> this.imageView2.setImageBitmap(bitmap));
    }
}
