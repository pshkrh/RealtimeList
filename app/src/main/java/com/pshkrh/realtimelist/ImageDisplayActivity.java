package com.pshkrh.realtimelist;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ImageDisplayActivity extends AppCompatActivity {

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mDocsStorageReference;

    Context mContext;
    String imgTitle;
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        imgTitle = "Attached Image";
        setTitle(imgTitle);

        mContext = this;

        mProgressBar = (ProgressBar)findViewById(R.id.img_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);

        mFirebaseStorage = FirebaseStorage.getInstance();
        mDocsStorageReference = mFirebaseStorage.getReference().child("docs/");

        String url = getIntent().getStringExtra("Url");
        ImageView img = (ImageView)findViewById(R.id.img_pic);
        TextView txt = (TextView)findViewById(R.id.img_title);

        Glide.with(mContext)
                .load(url)
                .listener(new RequestListener<Drawable>() {
                              @Override
                              public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {

                                  Toast.makeText(mContext, "Image failed to load, try to reopen", Toast.LENGTH_SHORT).show();
                                  return false;
                              }

                              @Override
                              public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                  mProgressBar.setVisibility(View.INVISIBLE);
                                  //Toast.makeText(mContext, "Image Loaded Successfully", Toast.LENGTH_SHORT).show();
                                  return false;
                              }
                          }
                )
                .into(img);

    }
}
