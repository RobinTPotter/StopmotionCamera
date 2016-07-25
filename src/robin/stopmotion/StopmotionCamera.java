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
    private static String BUTTON_TOGGLE_STRETCH = "Toggle";
    private static String CHANGE_OPACITY_INC = "Opac+";
    private static String CHANGE_OPACITY_DEC = "Opac-";

    private static String ONION_LEAF_INC = "Skin+";
    private static String ONION_LEAF_DEC = "Skin-";

    private int numSkins = 3;

    Process process;

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;

    boolean justfocussed = false;

    Bitmap lastPicture = null;
    String lastPictureFile = "";
    Canvas canvas;

    File currentDirectory;

    Camera.Size previewSize = null;
    Camera.Size pictureSize = null;

    int previewSizeWhich = -1;
    int pictureSizeWhich = -1;

    Onionskin onionskin;

    boolean stretch = false;

    LayoutInflater controlInflater = null;
    LinearLayout viewControl;

    Button.OnClickListener buttonClickListener =
            new Button.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    /// TODO Auto-generated method stub

                    if (justfocussed) {
                        justfocussed = false;
                    } else {

                        camera.takePicture(myShutterCallback,
                                myPictureCallback_RAW, myPictureCallback_JPG);

                    }
                }
            };

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
        previewSizeWhich = bundle.getInt("previewSizeWhich", 100);
        pictureSizeWhich = bundle.getInt("pictureSizeWhich", 100);
        numSkins = bundle.getInt("numSkins", 3);


        idPreviewSize("bollocks", previewSizeWhich);
        idPictureSize("bollocks", pictureSizeWhich);

        onionskin.setSkins(numSkins);
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
        bundle.putInt("previewSizeWhich", previewSizeWhich);
        bundle.putInt("pictureSizeWhich", pictureSizeWhich);
        bundle.putInt("numSkins", numSkins);
        onionskin.invalidate();

    }

    private Process launchLogcat() {

        try {
            File filename = new File(Environment.getExternalStorageDirectory() + "/stopmotion-logfile.log");
            filename.createNewFile();
            String cmd = "logcat -d -f " + filename.getAbsolutePath();
            return Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }
    }

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        process = launchLogcat();

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
        viewControl = (LinearLayout) (controlInflater.inflate(R.layout.control, null));

        LayoutParams layoutParamsControl
                = new LayoutParams(LayoutParams.MATCH_PARENT,/// FILL_PARENT,
                LayoutParams.MATCH_PARENT);/// FILL_PARENT);

        this.addContentView(viewControl, layoutParamsControl);

        initOnionskin(viewControl, 3);

        Log.d(LOGTAG, "created");
    }

    private void initOnionskin(LinearLayout viewControl, int skins) {

        viewControl.removeView(onionskin);

        onionskin = new Onionskin(this, skins);

        onionskin.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        viewControl.addView(onionskin);

        onionskin.setOnClickListener(buttonClickListener);

        onionskin.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        justfocussed = true;
                        Toast.makeText(StopmotionCamera.this, "focus", Toast.LENGTH_LONG).show();
                    }
                });
                return false;
            }
        });

        onionskin.setOpacity();
        onionskin.updateBackgound();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
            previewing = false;
        }

        save();

        onionskin.updateBackgound();
        onionskin.invalidate();
        Log.d(LOGTAG, "paused");

    }

    private void save() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("lastBmp", lastPictureFile);
        editor.putBoolean("stretch", stretch);
        editor.putInt("opacity", onionskin.getOpacity());
        editor.putInt("previewSizeWhich", previewSizeWhich);
        editor.putInt("pictureSizeWhich", pictureSizeWhich);
        editor.putInt("numSkins", numSkins);
        // Commit the edits!
        editor.commit();
        Log.d(LOGTAG, "committed");

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");

        load();
        onionskin.updateBackgound();
        onionskin.invalidate();
    }

    private void load() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        stretch = settings.getBoolean("stretch", false);
        onionskin.setOpacity(settings.getInt("opacity", 128));
        previewSizeWhich = settings.getInt("previewSizeWhich", 100);
        pictureSizeWhich = settings.getInt("pictureSizeWhich", 100);
        numSkins = settings.getInt("numSkins", 3);

        onionskin.setSkins(numSkins);

        idPreviewSize("bollocks", previewSizeWhich);
        idPictureSize("bollocks", pictureSizeWhich);

        lastPictureFile = settings.getString("lastBmp", "");
        if (!lastPictureFile.equals("") && (new File(lastPictureFile).exists())) {
            Log.d(LOGTAG, "picture file from settings " + lastPictureFile);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            lastPicture = BitmapFactory.decodeFile(lastPictureFile, bmOptions);

            onionskin.setBmp(lastPicture);
        }

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
            success = idPreviewSize(item.getTitle().toString(), -1);

        } else if (item.getGroupId() == 1) {

            /// pict

            success = idPictureSize(item.getTitle().toString(), -1);

        } else if (item.getGroupId() == 2) {
            if (item.getTitle().equals(BUTTON_TOGGLE_STRETCH)) {

                setStretch(!stretch);

            } else if (item.getTitle().equals(CHANGE_OPACITY_DEC)) {
                onionskin.decreaseOpacity();

            } else if (item.getTitle().equals(CHANGE_OPACITY_INC)) {
                onionskin.increaseOpacity();

            } else if (item.getTitle().equals(ONION_LEAF_INC)) {
                numSkins++;
                onionskin.setSkins(numSkins);

            } else if (item.getTitle().equals(ONION_LEAF_DEC)) {
                if (numSkins > 1) {
                    numSkins--;
                    onionskin.setSkins(numSkins);
                }

            }

            save();
        }

        if (camera != null) {
            if (previewing) camera.startPreview();
        }
        onionskin.invalidate();
        return success;
    }

    public void setStretch(boolean stretch) {

        this.stretch = stretch;
        if (previewSize != null) setSize(previewSize.width, previewSize.height);
        Log.d(LOGTAG, "setStretch to " + this.stretch);
        onionskin.invalidate();

    }

    private boolean idPreviewSize(String thing, int idnum) {

        Log.d(LOGTAG, "idPreviewSize");

        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        boolean success = false;
        int enumc = 0;

        for (Camera.Size size : previewSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            if (thing.startsWith(text) || enumc == idnum) {
                Camera.Parameters params = camera.getParameters();
                params.setPreviewSize(size.width, size.height);
                camera.setParameters(params);
                success = true;
                previewSize = size;
                setSize(size.width, size.height);
                previewSizeWhich = enumc;
                save();
            }
            enumc++;
        }
        return success;
    }

    private boolean idPictureSize(String startswith, int idnum) {

        Log.d(LOGTAG, "idPictureSize");
        boolean success = false;
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();

        int enumc = 0;

        for (Camera.Size size : pictureSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            if (startswith.startsWith(text) || enumc == idnum) {
                Camera.Parameters params = camera.getParameters();
                params.setPictureSize(size.width, size.height);
                pictureSize = size;
                camera.setParameters(params);
                success = true;
                pictureSizeWhich = enumc;
                save();
            }
            enumc++;
        }
        return success;

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
                //   if (previewSize == null) previewSize = camera.getParameters().getPreviewSize();
                //  if (pictureSize == null) pictureSize = camera.getParameters().getPictureSize();
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
        menu.add(2, Menu.NONE, order++, ONION_LEAF_DEC);
        menu.add(2, Menu.NONE, order++, ONION_LEAF_INC);

        SubMenu sm1 = menu.addSubMenu(0, 12, order++, "Preview Size");

        for (Camera.Size size : previewSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height) + " | " + String.format("%.3f", (float) size.width / size.height);
            MenuItem mi = sm1.add(0, Menu.NONE, order++, text);

        }

        SubMenu sm2 = menu.addSubMenu(1, 23, order++, "Picture Size");
        sm2.setGroupCheckable(1, false, true);
        menu.setGroupCheckable(1, false, true);

        for (Camera.Size size : pictureSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height) + " | " + String.format("%.3f", (float) size.width / size.height);
            ;
            MenuItem mi = sm2.add(1, Menu.NONE, order++, text);
        }

        Log.d(LOGTAG, "created Menu for first time");

        return true;
    }

    public void setSize(int width, int height) {

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

        Log.d(LOGTAG, "setSize " + width + " " + height);

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
        camera = Camera.open();
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

