package github.diego.nativeimagefilter;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";

    static {
        System.loadLibrary("native-lib");
    }

    @BindView(R.id.camera)
    CameraView camera;

    @BindView(R.id.imageView)
    ImageView imageView;

    @BindView(R.id.floatingActionButton)
    FloatingActionButton fab;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.seekBar)
    SeekBar sensitivitySeekBar;

    @BindView(R.id.spinner)
    Spinner filterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ArrayAdapter<String> filterNamesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.filterNames)
        );
        filterSpinner.setAdapter(filterNamesAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
    }

    @Override
    protected void onPause() {
        camera.stop();
        super.onPause();
    }


    @OnClick(R.id.floatingActionButton)
    void recordButtonClick() {
        imageView.setImageBitmap(null);
        fab.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        camera.captureImage(new CameraKitEventCallback<CameraKitImage>() {
            @Override
            public void callback(CameraKitImage cameraKitImage) {
                Log.d(TAG, "Reading user definitions");
                double sensitivity = sensitivitySeekBar.getProgress() / 100.0;
                int filterType;
                switch (filterSpinner.getSelectedItemPosition()) {
                    case 0: filterType = FilteredImage.NORMAL_BINARIZATION; break;
                    case 1: filterType = FilteredImage.MEAN_BINARIZATION; break;
                    case 2: filterType = FilteredImage.INVERT_COLORS; break;
                    default:
                        filterType = FilteredImage.INVERT_COLORS; break;
                }
                final Bitmap cameraBitmap = cameraKitImage.getBitmap();
                Log.d(TAG, "Got image from camera");
                final Bitmap resizedBitmap = Bitmap.createScaledBitmap(cameraBitmap, imageView.getWidth(), imageView.getHeight(), false);
                Log.d(TAG, "Resize bitmap");
                final Bitmap filteredImage =
                        FilteredImage.fromBitmap(resizedBitmap)
                        .applyFilter(filterType, sensitivity)
                        .toBitmap();
                Log.d(TAG, "Applied filter");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Setting image");
                        imageView.setImageBitmap(filteredImage);
                        progressBar.setVisibility(View.GONE);
                        fab.setEnabled(true);
                    }
                });
            }
        });

        camera.captureImage();
    }

}
