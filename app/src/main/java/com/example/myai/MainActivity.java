package com.example.myai;


import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.executors.ConstrainedExecutorService;
import com.facebook.common.executors.DefaultSerialExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImageTranscoderType;
import com.facebook.imagepipeline.core.MemoryChunkType;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import org.w3c.dom.Text;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private EditText editUrl;
    private ImageView btnSearch;
    private ImageView imageViewSource;
    private TextView textViewInfo;
//    private SimpleDraweeView draweeView;

    private Handler mainHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fresco.initialize( getApplicationContext(),
                        ImagePipelineConfig.newBuilder(getApplicationContext())
                        .setMemoryChunkType(MemoryChunkType.BUFFER_MEMORY)
                        .setImageTranscoderType(ImageTranscoderType.JAVA_TRANSCODER)
                        .experiment().setNativeCodeDisabled(true)
                        .build());

        imageViewSource = findViewById(R.id.imageViewSource);
        textViewInfo = findViewById(R.id.textViewInfo);
        editUrl = findViewById(R.id.editUrl);
        btnSearch = findViewById(R.id.imageViewSearch);
        btnSearch.setOnClickListener(this::onSearchButtonClick);
    }

    private void onSearchButtonClick(View view)
    {
        String url = editUrl.getText().toString();
        editUrl.setText("");

        if (url.isEmpty()) {
            Toast toast = Toast.makeText(getApplicationContext(),
                                         "이미지 URL을 입력하세요",
                                         Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Uri imageUri = Uri.parse(url);

//        DraweeController controller = Fresco.newDraweeControllerBuilder()
//                .setUri(imageUri)
//                .setAutoPlayAnimations(true)
//                .setTapToRetryEnabled(true)
//                .setOldController(draweeView.getController())
//                .setControllerListener(listener)
//                .build();

        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(imageUri)
                .build();

        DataSource<CloseableReference<CloseableImage>> dataSource =
                    Fresco.getImagePipeline().fetchDecodedImage(request, getApplicationContext());

        dataSource.subscribe(new BaseBitmapDataSubscriber() {
                                 @Override
                                 public void onNewResultImpl(@Nullable Bitmap bitmap) {
                                     // You can use the bitmap here, but in limited ways.
                                     // No need to do any cleanup.
                                     AnalyzeBitmap(bitmap);
                                     imageViewSource.setImageBitmap(bitmap);
                                 }

                                 @Override
                                 public void onFailureImpl(DataSource dataSource) {
                                     // No cleanup required here.
                                 }
                             },  CallerThreadExecutor.getInstance());

//        DataSource<CloseableReference<CloseableImage>> dataSource =
//                Fresco.getImagePipeline().fetchDecodedImage(request, getApplicationContext());
//
//        CloseableImage closeableImage = null;
//        try {
//            closeableImage = dataSource.getResult().get();
//            if (closeableImage instanceof CloseableStaticBitmap) {
//                Bitmap bitmap = ((CloseableStaticBitmap) closeableImage).getUnderlyingBitmap();
//            }
//        } finally {
//            dataSource.close();
//            CloseableReference.closeSafely((CloseableReference<?>) closeableImage);
//        }
    }


    private void AnalyzeBitmap(Bitmap bitmap)
    {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler();

        labeler.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        // Task completed successfully
                        // ...
                        textViewInfo.setText(String.format("%d", labels.size()));
                        String info = "";
                        for (FirebaseVisionImageLabel label: labels) {
                            String text = label.getText();
                            info += (text + ", ");
                            String entityId = label.getEntityId();
                            float confidence = label.getConfidence();
                        }

                        textViewInfo.setText(info);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                        textViewInfo.setText("정보를 알 수 없음");
                    }
                });
    }
}