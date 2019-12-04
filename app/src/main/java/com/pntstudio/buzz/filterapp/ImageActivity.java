package com.pntstudio.buzz.filterapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.ajscape.pixatoon.lib.Filter;
import com.ajscape.pixatoon.lib.FilterManager;
import com.ajscape.pixatoon.lib.FilterType;
import com.ajscape.pixatoon.lib.Native;
import com.ajscape.pixatoon.ui.Utils;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pntstudio.buzz.filterapp.fragment.FilterConfigFragment;
import com.pntstudio.buzz.filterapp.fragment.FilterSelectorFragment;
import com.pntstudio.buzz.filterapp.fragment.interfaces.FilterConfigListener;
import com.pntstudio.buzz.filterapp.fragment.interfaces.FilterSelectorListener;
import com.pntstudio.buzz.filterapp.utils.FileUtils;
import com.pntstudio.buzz.filterapp.view.PictureSurfaceView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class ImageActivity extends AppCompatActivity implements View.OnClickListener
        , FilterSelectorListener, FilterConfigListener {
    private static final String TAG = "ImageActivity";

    public static String INTENT_DATA = "image_path";
    private PictureSurfaceView mPictureView;
    private ImageView mSaveImgBtn;
    private ImageView mConfigImgBtn;
    private ImageView mFilterImgBtn;
    private ImageView mShareImgBtn;
    private ImageView reTake;
    private ProgressBar spinner;
    private ImageView mCloseImg;
    private LinearLayout mTopLL;
    private LinearLayout mBottomLl;
    private static String mCurrentSaveUri;
    private String hostUrl = "";
    RequestQueue queue;
    private Bitmap mScaledInputBitmap, mScaledOutputBitmap;
    private Mat mScaledInputMat, mScaledOutputMat;
    ProgressDialog progressDoalog;
    private PictureUpdateThread mUpdateThread;
    private AtomicBoolean mPendingUpdate = new AtomicBoolean(false);
    private boolean mInputRotated = false;
    private FilterManager mFilterManager;
    String imagePath;
    Bitmap originalBitmap;
    private boolean isServerHit = false;
    String name = "",email = "",phoneNumber = "";
    private FilterSelectorFragment mFilterSelectorFragment;
    private FilterConfigFragment mFilterConfigFragment;

    // Statically Load native OpenCV and image filter implementation libraries
    static {
        Log.e(TAG, "load library");
        System.loadLibrary("opencv_java3");
        System.loadLibrary("image_filters");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_image);
        mPictureView = findViewById(R.id.image_img);
       // mSaveImgBtn = findViewById(R.id.img_save);
        mConfigImgBtn = findViewById(R.id.img_config);
        isServerHit = false;
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPrefs", 0);
        spinner = (ProgressBar)findViewById(R.id.simpleProgressBar);
        spinner.setVisibility(View.GONE);
        queue = Volley.newRequestQueue(this);
        mFilterImgBtn = findViewById(R.id.img_filter);
        mShareImgBtn = findViewById(R.id.img_share);
        reTake = findViewById(R.id.img_retake);
        reTake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("CameraActivity", "finish");
                Intent cameraActivityIntent = new Intent(ImageActivity.this,CameraActivity.class);
                startActivity(cameraActivityIntent);
                ImageActivity.this.finish();
            }
        });
     //   mCloseImg  = findViewById(R.id.close_img);
       // mTopLL = findViewById(R.id.top_ll);
//        hostUrl = "http://" + "192.168.70.58" + "/sling_shot";
        hostUrl = "http://" + pref.getString("ip", null) + "/sling_shot";
        name =  pref.getString("name", null);
        email =  pref.getString("email", null);
        phoneNumber =  pref.getString("phone", null);
        mBottomLl = findViewById(R.id.menuBtnPanel);
        mPictureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( mBottomLl.getVisibility()== View.VISIBLE){
                 //   mTopLL.setVisibility(View.INVISIBLE);
                    mBottomLl.setVisibility(View.INVISIBLE);
                }else {
                 //   mTopLL.setVisibility(View.VISIBLE);
                    mBottomLl.setVisibility(View.VISIBLE);
                }
            }
        });
        mFilterManager = FilterManager.getInstance();
        mCurrentSaveUri = "";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        imagePath = getIntent().getStringExtra(INTENT_DATA);
        Log.i("Oatb", imagePath);
        originalBitmap = BitmapFactory.decodeFile(imagePath, options);
        Log.i("ImageHeight", originalBitmap.getHeight() + " " + originalBitmap.getWidth());
//        Bitmap bmImg = BitmapFactory.decodeFile(imagePath);
//        mPictureView.setImageBitmap(bmImg);
        loadPicture(imagePath);
       // mSaveImgBtn.setOnClickListener(this);
        mConfigImgBtn.setOnClickListener(this);
        mFilterImgBtn.setOnClickListener(this);
        mShareImgBtn.setOnClickListener(this);
//        mCloseImg.setOnClickListener(this);

        // Load sketch texture
        // important to use pencil sketch
        loadSketchTexture(getApplicationContext().getResources(),
                com.ajscape.pixatoon.R.drawable.sketch_texture);


        mFilterSelectorFragment = new FilterSelectorFragment();
    }

    /**
     * Returns true if filter configuration panel is opened, else returns false
     *
     * @return
     */
    private boolean isFilterConfigVisible() {
        if (mFilterConfigFragment != null && mFilterConfigFragment.isVisible())
            return true;
        else
            return false;
    }

    /**
     * Open filter configuration panel with current filter specific settings
     */
    private void openCurrentFilterConfig() {
        if (mFilterManager.getCurrentFilter() != null && !isFilterConfigVisible()) {

            mFilterConfigFragment = new FilterConfigFragment();
            mFilterConfigFragment.setFilter(mFilterManager.getCurrentFilter());

//            mConfigFilterBtn.setImageResource(R.drawable.icon_btn_settings_on);
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.filterConfigPanel, mFilterConfigFragment)
                    .commit();
            Log.d(TAG, "filter config opened");
        }
    }

    /**
     * Open filter selector panel, to choose between different image filters
     */
    private void openFilterSelector() {
        if (!mFilterSelectorFragment.isVisible()) {
//            mSelectFilterBtn.setImageResource(R.drawable.icon_btn_filters_on);

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.filterSelectorPanel, mFilterSelectorFragment)
                    .commit();

            Log.d(TAG, "filter selector opened");
        }
    }


    @Override
    public void onClick(View v) {
        // Detect clicked view, and execute actions accordingly
        switch (v.getId()) {
//            case R.id.img_save:
//                mCurrentSaveUri = FileUtils.saveStyleImage(mScaledOutputBitmap, this, mCurrentSaveUri);
//
//                break;
            case R.id.img_config:
                closeFilterSelector();
                if (!isFilterConfigVisible())
                    openCurrentFilterConfig();
                else
                    closeCurrentFilterConfig();

                break;
            case R.id.img_filter:
                closeCurrentFilterConfig();
                if (!mFilterSelectorFragment.isVisible())
                    openFilterSelector();
                else
                    closeFilterSelector();
                break;
            case R.id.img_share:
                spinner.setVisibility(View.VISIBLE);
                new ShareTask(this).execute();
                break;



        }

    }

    /**
     * Close filter configuration panel
     */
    private void closeCurrentFilterConfig() {
        if (isFilterConfigVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(mFilterConfigFragment)
                    .commit();
            Log.d(TAG, "filter config closed");
        }
    }

    /**
     * Close filter selector panel, if opened
     */
    private void closeFilterSelector() {
        if (mFilterSelectorFragment.isVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(mFilterSelectorFragment)
                    .commit();
            Log.d(TAG, "filter selector closed");
        }
    }

    @Override
    public void onFilterConfigChanged() {
        updatePicture();

    }


    @Override
    public void onFilterSelect(FilterType filterType) {


            if (mFilterManager.getCurrentFilter() == null || filterType != mFilterManager.getCurrentFilter().getType()) {
                if (filterType.toString().equals("Origin")) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    originalBitmap = BitmapFactory.decodeFile(imagePath, options);
                    originalBitmap = Utils.rotateBitmap(originalBitmap, 0);
                    mPictureView.setImageBitmap(Bitmap.createScaledBitmap(originalBitmap, 4032, 3024, false));
                    Log.d(TAG, "current filter set to " + filterType.toString() + "Origin");
                }
                else {

                    mFilterManager.setCurrentFilter(filterType);
                    Log.d(TAG, "current filter set to " + filterType.toString());
//            if (mPictureViewerFragment.isVisible()) {
//                mPictureViewerFragment.updatePicture();
//            }
                    // Display selected filter name as Toast
//            displayMessage(filterType.toString());
                    updatePicture();
                }
            }



    }

    public void loadPicture(String pictureFilePath) {
        Log.d(TAG, "load picture from path=" + pictureFilePath);

//        // Load mat from filepath
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap inputBitmap = BitmapFactory.decodeFile(pictureFilePath, options);
        try {
            inputBitmap = FileUtils.modifyOrientation(inputBitmap, pictureFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }


        // Get dimensions of screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        // If input is landscape, rotate for better screen coverage
        if(inputBitmap.getWidth()>inputBitmap.getHeight()) {
            inputBitmap = Utils.rotateBitmap(inputBitmap, 0);
            mInputRotated = true;
            mFilterManager.setSketchFlip(true);
        } else {
            mInputRotated = false;
            mFilterManager.setSketchFlip(false);
        }

        // Get scaled bitmap and mat fit to screen, for preview filter display
        mScaledInputBitmap = Utils.resizeBitmap(inputBitmap, width, height);
        Log.i("Height", height +  " " + width);
        mScaledOutputBitmap = mScaledInputBitmap.copy(mScaledInputBitmap.getConfig(), true);

        if (mScaledInputMat != null)
            mScaledInputMat.release();
        mScaledInputMat = new Mat(mScaledInputBitmap.getHeight(), mScaledInputBitmap.getWidth(), CvType.CV_8UC4);

        if (mScaledOutputMat != null)
            mScaledOutputMat.release();
        mScaledOutputMat = new Mat(mScaledInputBitmap.getHeight(), mScaledInputBitmap.getWidth(), CvType.CV_8UC4);
        org.opencv.android.Utils.bitmapToMat(mScaledInputBitmap, mScaledInputMat);
        mScaledInputMat.copyTo(mScaledOutputMat);
        Log.i("SizeI" ,""+ mScaledInputBitmap.getHeight() +  mScaledInputBitmap.getWidth());
        Log.i("SizeO" ,""+ mScaledOutputBitmap.getHeight() +  mScaledOutputBitmap.getWidth());
        // Set view with scaled bitmap
        updatePicture();
    }

    public void updatePicture() {
        if (mUpdateThread == null || !mUpdateThread.isAlive()) {
            mPendingUpdate.set(false);
            mUpdateThread = new PictureUpdateThread();
            mUpdateThread.start();
        } else {
            mPendingUpdate.set(true);
        }
    }


    class PictureUpdateThread extends Thread {

        @Override
        public void run() {
            do {
                mPendingUpdate.set(false);
                Filter currentFilter = mFilterManager.getCurrentFilter();
                if (currentFilter != null && currentFilter.getType() != FilterType.COLOR_ORIGIN) {
                    if (mFilterManager.getFilterScaleFactor() < 1.0)
                        mFilterManager.setFilterScaleFactor(1.0);
                    currentFilter.process(mScaledInputMat, mScaledOutputMat);
                    org.opencv.android.Utils.matToBitmap(mScaledOutputMat, mScaledOutputBitmap);
                }

              //  mPictureView.setImageBitmap(mScaledOutputBitmap);
                mPictureView.setImageBitmap(Bitmap.createScaledBitmap(mScaledOutputBitmap, 4032, 3024, false));
            } while (mPendingUpdate.get() == true);
        }
    }


    @Override
    protected void onDestroy() {
        mScaledInputBitmap.recycle();
        mScaledInputBitmap = null;
        mScaledOutputBitmap.recycle();
        mScaledOutputBitmap = null;
        mScaledInputMat.release();
        mScaledOutputMat.release();
        mFilterManager.reset();
        Log.d(TAG, "Picture fragment view destroyed");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        // Terminate picture update thread
        if (mUpdateThread != null && mUpdateThread.isAlive()) {
            boolean retry;
            do {
                try {
                    mUpdateThread.join();
                    retry = false;
                    Log.d(TAG, "Update thread terminated");
                } catch (InterruptedException e) {
                    Log.d(TAG, "Error while terminating update thread...retrying");
                    retry = true;
                }
            } while (retry);
        }
        super.onPause();
    }

    private void loadSketchTexture(Resources res, int sketchTexRes) {
        Mat mat, tempMat;
        Bitmap bmp = BitmapFactory.decodeResource(res, sketchTexRes);
        tempMat = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
        org.opencv.android.Utils.bitmapToMat(bmp, tempMat);
        mat = new Mat(tempMat.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(tempMat, mat, Imgproc.COLOR_RGBA2GRAY);
        Native.setSketchTexture(mat.getNativeObjAddr());
    }


    private class ShareTask extends AsyncTask<Void, Void, String> {
       // ProgressDialog dialog = new ProgressDialog(ImageActivity.this);
        private WeakReference<ImageActivity> activityReference;

        // only retain a weak reference to the activity
        ShareTask(ImageActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... params) {

            // do save running task...
           // mCurrentSaveUri = FileUtils.saveStyleImage(mScaledOutputBitmap, activityReference.get(), mCurrentSaveUri);
            Bitmap bm = BitmapFactory.decodeFile(imagePath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mScaledOutputBitmap = Bitmap.createScaledBitmap(mScaledOutputBitmap, 4032, 3024, false);
            mScaledOutputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
            byte[] b = baos.toByteArray();
            String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
            Log.e("Image Add", encodedImage);

            this.SendDataToServer(encodedImage);
            return mCurrentSaveUri;
        }

        private void SendDataToServer(final String base64) {
            if (!isServerHit) {
                final Date currentTime = Calendar.getInstance().getTime();
                Log.i("URL " , hostUrl);
                StringRequest putRequest = new StringRequest(Request.Method.POST, hostUrl + "/saveclick_rest",
                        new Response.Listener<String>()
                        {
                            @Override
                            public void onResponse(String response) {
                                // response
                                Intent registerIntent = new Intent(ImageActivity.this,RegisterActivity.class);
                                startActivity(registerIntent);
                                ImageActivity.this.finish();
                                spinner.setVisibility(View.GONE);
                                Log.d("Response", response);
                            }
                        },
                        new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error
                                spinner.setVisibility(View.GONE);
                                Log.d("Error.Response", error.toString());
                            }
                        }
                ) {

                    @Override
                    protected Map<String, String> getParams()
                    {
                        Map<String, String>  params = new HashMap<String, String>();
                        Log.i("Name " , name);
                        params.put("name", name);
                        params.put("email", email);
                        params.put("photo_base_64", base64);
                        params.put("color", "2");
                        params.put("feedback", "NA");
                        params.put("location", "NA");
                        params.put("mobile", phoneNumber);
                        params.put("clicked_on", "" + currentTime.getTime());
                        params.put("user_id", "rayqube_photobooth");
                        params.put("password", "RP#123");

                        return params;
                    }

                };

                queue.add(putRequest);
                isServerHit = true;
            }

        }

        @Override
        protected void onPostExecute(String result) {
            /* Example of sharing an image */
//            File file = new File(result);
////            Uri uri = Uri.fromFile(file);
//            Uri uri = FileProvider.getUriForFile(
//                    activityReference.get(),
//                    "com.pntstudio.buzz.filterapp.provider", //(use your app signature + ".provider" )
//                    file);
//
//            Intent shareIntent = new Intent(Intent.ACTION_SEND);
//            shareIntent.setType("image/*");
//
//            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
//            shareIntent.putExtra(Intent.EXTRA_TEXT, "Share your sketch");
//            startActivity(shareIntent);

        }
    }

    @Override
    public void onBackPressed() {
        if (mFilterConfigFragment != null && mFilterConfigFragment.isVisible()) {
            closeCurrentFilterConfig();
        } else if (mFilterSelectorFragment != null && mFilterSelectorFragment.isVisible()) {
            closeFilterSelector();
        } else
            super.onBackPressed();
    }


}

