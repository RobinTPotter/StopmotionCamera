package robin.stopmotion;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import android.content.SharedPreferences;
import android.net.*;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.view.ViewGroup.LayoutParams;
/// import android.R;

public class StopmotionCamera extends Activity implements SurfaceHolder.Callback {

    private static String PREFS_NAME = "StopmotionCameraPreferences";
    private static String LOGTAG = "StopmotionCameraLog-StopmotionCamera";
    private static String BUTTON_TOGGLE_STRETCH = "ToggleStretch";
    private static String CHANGE_OPACITY_INC = "Opacity+";
    private static String CHANGE_OPACITY_DEC = "Opacity-";

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;

    boolean justfocussed=false;

    Bitmap lastPicture = null;
    String lastPictureFile = "";
    Canvas canvas;

    File currentDirectory;

    Camera.Size previewSize = null;
    Camera.Size pictureSize = null;
    Onionskin onionskin;

    boolean stretch = false;

    LayoutInflater controlInflater = null;
    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback() {

        @Override
        public void onShutter() {
            /// TODO Auto-generated method stub
        }
    };
    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            /// TODO Auto-generated method stub
        }
    };
    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {

            lastPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);

            Uri uriTarget = android.net.Uri.fromFile(new File(currentDirectory, String.valueOf((new Date()).getTime()) + ".jpg"));

            OutputStream imageFileOS;
            try {
                imageFileOS = getContentResolver().openOutputStream(uriTarget);
                imageFileOS.write(arg0);
                imageFileOS.flush();
                imageFileOS.close();
                //  Toast.makeText(StopmotionCamera.this,
                //        "Image saved: " + uriTarget.toString(),
                //        Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            onionskin.setBmp(lastPicture);
            lastPictureFile = uriTarget.getPath();
            onionskin.updateBackgound();
            camera.startPreview();
            previewing = true;

            Log.d(LOGTAG, "picture " + uriTarget.toString());

        }
    };

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        Log.d(LOGTAG, "onRestoreInstanceState");

        lastPictureFile = bundle.getString("lastPictureFile", "");
        if (!lastPictureFile.equals("") && (new File(lastPictureFile).exists())) {
            Log.d(LOGTAG, "picture file from settings " + lastPictureFile);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            lastPicture = BitmapFactory.decodeFile(lastPictureFile, bmOptions);
            onionskin.setBmp(lastPicture);
        }


        stretch = bundle.getBoolean("stretch", false);
        onionskin.setOpacity(bundle.getInt("opacity", 128));
        if (previewSize != null) {
            previewSize.width = bundle.getInt("previewWidth", 100);
            previewSize.height = bundle.getInt("previewHeight", 100);
            Log.d(LOGTAG, "set preview size from restore");
        }
        if (pictureSize != null) {
            pictureSize.width = bundle.getInt("picturewWidth", 100);
            pictureSize.height = bundle.getInt("picturewHeight", 100);
            Log.d(LOGTAG, "set picture size from restore");


        }
        onionskin.setOpacity();
        onionskin.updateBackgound();
        onionskin.invalidate();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Log.d(LOGTAG, "onSaveInstanceState");

        bundle.putString("lastBmp", lastPictureFile);
        bundle.putInt("opacity", onionskin.getOpacity());
        bundle.putBoolean("stretch", stretch);
        bundle.putInt("previewWidth", previewSize.width);
        bundle.putInt("previewHeight", previewSize.height);
        bundle.putInt("picturewWidth", pictureSize.width);
        bundle.putInt("picturewHeight", pictureSize.height);
        onionskin.invalidate();


    }

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_camera_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView) findViewById(R.id.camerapreview);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        /// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        String x = (new Date()).toString().replace(" ", "-");
        currentDirectory = getAlbumStorageDir("Stopmotion-" + x);

        controlInflater = LayoutInflater.from(getBaseContext());
        LinearLayout viewControl = (LinearLayout) (controlInflater.inflate(R.layout.control, null));

        LayoutParams layoutParamsControl
                = new LayoutParams(LayoutParams.MATCH_PARENT,/// FILL_PARENT,
                LayoutParams.MATCH_PARENT);/// FILL_PARENT);

        this.addContentView(viewControl, layoutParamsControl);


        //  onionskin = (Onionskin) findViewById(R.id.takepicture);

        onionskin = new Onionskin(this);

        onionskin.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT));

        viewControl.addView(onionskin);

        onionskin.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                /// TODO Auto-generated method stub

                if (justfocussed) {
                    justfocussed=false;
                }else {

                    camera.takePicture(myShutterCallback,
                            myPictureCallback_RAW, myPictureCallback_JPG);

                }
            }
        });


        onionskin.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        justfocussed=true;
                        Toast.makeText(StopmotionCamera.this,"focus",Toast.LENGTH_LONG).show();
                    }
                });
                return false;
            }
        });

        onionskin.setOpacity();
        onionskin.updateBackgound();

        Log.d(LOGTAG, "created");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
            previewing = false;
        }

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("lastBmp", lastPictureFile);
        editor.putBoolean("stretch", stretch);
        editor.putInt("opacity", onionskin.getOpacity());
        editor.putInt("previewWidth", previewSize.width);
        editor.putInt("previewHeight", previewSize.height);
        editor.putInt("picturewWidth", pictureSize.width);
        editor.putInt("picturewHeight", pictureSize.height);
        // Commit the edits!
        editor.commit();
        Log.d(LOGTAG, "committed");

        onionskin.updateBackgound();
        onionskin.invalidate();
        Log.d(LOGTAG, "paused");

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        stretch = settings.getBoolean("stretch", false);
        onionskin.setOpacity(settings.getInt("opacity", 128));

        Log.d(LOGTAG, "set stretch from resume " + stretch);

        int width = settings.getInt("previewWidth", 100);
        int height = settings.getInt("previewHeight", 100);
        setSize(width, height);

        Log.d(LOGTAG, "set preview size from resume");

        if (previewSize != null) {

            previewSize.width = width;
            previewSize.height = height;
            Log.d(LOGTAG, "set preview size from resume");
        }

        lastPictureFile = settings.getString("lastBmp", "");
        if (!lastPictureFile.equals("") && (new File(lastPictureFile).exists())) {
            Log.d(LOGTAG, "picture file from settings " + lastPictureFile);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            lastPicture = BitmapFactory.decodeFile(lastPictureFile, bmOptions);
            onionskin.setBmp(lastPicture);
        }

        if (pictureSize != null) {
            pictureSize.width = settings.getInt("picturewWidth", 100);
            pictureSize.height = settings.getInt("picturewHeight", 100);
            Log.d(LOGTAG, "set picture size from resume");
        }
        onionskin.updateBackgound();
        onionskin.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (menu.findItem(12) == null || menu.findItem(23) == null) return createMenu(menu);
        else return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /// public boolean onGroupItemClick(MenuItem item) {

        if (camera != null) {

            if (previewing) camera.stopPreview();
        }

        boolean success = false;

        /// Handle item selection
        if (item.getGroupId() == 0) {
            /// preview
            List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();

            for (Camera.Size size : previewSizes) {
                String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
                if (item.getTitle().toString().startsWith(text)) {
                    Camera.Parameters params = camera.getParameters();
                    params.setPreviewSize(size.width, size.height);
                    camera.setParameters(params);
                    success = true;
                    previewSize = size;
                    setSize(size.width, size.height);
                }
            }

        } else if (item.getGroupId() == 1) {

            /// pict
            List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();

            for (Camera.Size size : pictureSizes) {
                String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
                if (item.getTitle().toString().startsWith(text)) {
                    Camera.Parameters params = camera.getParameters();
                    params.setPictureSize(size.width, size.height);
                    pictureSize = size;
                    camera.setParameters(params);
                    success = true;
                }
            }
        } else if (item.getGroupId() == 2) {
            if (item.getTitle().equals(BUTTON_TOGGLE_STRETCH)) {

                setStretch(!stretch);

            } else if (item.getTitle().equals(CHANGE_OPACITY_DEC)) {
                onionskin.decreaseOpacity();

            } else if (item.getTitle().equals(CHANGE_OPACITY_INC)) {
                onionskin.increaseOpacity();

            }
        }

        if (camera != null) {
            if (previewing) camera.startPreview();
        }

        return success;
    }

    public void setStretch(boolean stretch) {

        this.stretch = stretch;
        setSize(previewSize.width, previewSize.height);
        Log.d(LOGTAG, "setStretch to " + this.stretch);
        onionskin.invalidate();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        if (previewing) {
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null) {
            try {
                if (previewSize == null) previewSize = camera.getParameters().getPreviewSize();
                if (pictureSize == null) pictureSize = camera.getParameters().getPictureSize();
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                /// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        camera = Camera.open();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    public boolean createMenu(Menu menu) {

        menu.clear();

        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();

        int order = 0;

        menu.add(2, Menu.NONE, order++, BUTTON_TOGGLE_STRETCH);
        menu.add(2, Menu.NONE, order++, CHANGE_OPACITY_DEC);
        menu.add(2, Menu.NONE, order++, CHANGE_OPACITY_INC);

        SubMenu sm1 = menu.addSubMenu(0, 12, order++, "Preview Size");

        for (Camera.Size size : previewSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height) + " | " + String.format("%.3f",(float) size.width / size.height);
            MenuItem mi = sm1.add(0, Menu.NONE, order++, text);
        }

        SubMenu sm2 = menu.addSubMenu(1, 23, order++, "Picture Size");
        sm2.setGroupCheckable(1, false, true);
        menu.setGroupCheckable(1, false, true);

        for (Camera.Size size : pictureSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height) + " | " + String.format("%.3f", (float)size.width / size.height);
            ;
            MenuItem mi = sm2.add(1, Menu.NONE, order++, text);
        }

        Log.d(LOGTAG, "created Menu for first time");

        return true;
    }


    public void setSize(int width, int height) {

        Log.d(LOGTAG, "setSize " + width + " " + height);

        float asp = (float) width / height;

        int measuredHeight = surfaceView.getMeasuredHeight();
        int measuredWidth = surfaceView.getMeasuredWidth();

        float dev_asp = (float) measuredWidth / measuredHeight;

        if (stretch || width > measuredWidth || height > measuredHeight) {

            if (asp > dev_asp) {
                /// wider, set width to device, change height
                width = measuredWidth;
                height = (int) ((float) measuredHeight / asp);

            } else if (asp < dev_asp) {
                /// narrower, set height to device, change width
                height = measuredHeight;
                width = (int) (asp * measuredHeight);
            } else {
                height = measuredHeight;
                width = measuredWidth;
            }

        } else {
            if (width > measuredWidth) {
                width = measuredWidth;
                height = (int) ((float) measuredHeight / asp);
            } else if (height > measuredHeight) {
                width = (int) (asp * measuredHeight);
                height = measuredHeight;

            }
        }

        int l = (measuredWidth - width) / 2;
        int t = (measuredHeight - height) / 2;

        surfaceView.layout(l, t, l + width, t + height);
        surfaceView.invalidate();

        onionskin.layout(l, t, l + width, t + height);
        onionskin.updateBackgound();
        onionskin.invalidate();


        Log.d(LOGTAG, "setSize done");


    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
        }


        Log.d(LOGTAG, "getAlbumStorageDir " + file.toString());

        return file;
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOGTAG, "START");
        new CountDownTimer(2000, 200) {
            @Override
            public void onFinish() {
                Log.d(LOGTAG, "set stretch with timebombtick");
                setStretch(stretch);
            }

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(LOGTAG, "tick");
            }
        }.start();
    }
}

