package robin.stopmotion;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class StopmotionCamera extends Activity implements SurfaceHolder.Callback {

    private static final int THUMB_HEIGHT = 480;
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static String PREFS_NAME = "StopmotionCameraPreferences";

    private static int ITEMID_PREVIEW = 12;
    private static int ITEMID_PICTURE = 23;
    private static int ITEMID_ASPECT = 27;
    private static int GROUPID_PREVIEW = 0;
    private static int GROUPID_PICTURE = 1;
    private static int GROUPID_ASPECT = 2;
    private static int GROUPID_OTHER = 3;
    public static int MAX_IMAGES = 1000;
    public static final String PLAY = "Play";
    public static final String STOP = "Stop";
    public static final String IMAGE_NUMBER_FORMAT = "%07d";
    public static int MAX_FPS = 30;
    private String dateFormat = "yyyy-MM-dd-HH";
    private String defaultDateFormat = "yyyy-MM-dd";

    public static String LOGTAG = "StopmotionCameraLog-StopmotionCamera";
    public static String BUTTON_TOGGLE_STRETCH = "Toggle";
    public static String THUMBNAIL_SUBFOLDER = "/thumb";

    //private static String ONION_LEAF_INC = "Skin+";
    //private static String ONION_LEAF_DEC = "Skin-";

    public static String CHANGE_DATE_FORMAT = "Settings";
    public static String SHOW_RUSHES = "Preview";

    private int numSkins = 3;
    private int playbackSpeed = 10;

    private boolean takingPicture = false;

    Process process;

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Button testButton;
    boolean previewing = false;

    boolean justfocussed = false;

    Bitmap lastPicture = null;


    int alignment;

    File currentDirectory;

    Camera.Size previewSize = null;
    Camera.Size pictureSize = null;

    int previewSizeWhich = -1;
    int pictureSizeWhich = -1;

    OnionSkinView onionSkinView;
    LinearLayout layoutOnionSkin;

    boolean stretch = false;

    LayoutInflater controlInflater = null;
    private int lastGoodHeight = 0;
    private int lastGoodWidth = 0;
    private String aspectLock = "None";
    private String currentPreviewSize = "";
    private String currentPictureSize = "";
    private boolean surfaceReady = false;
    private boolean permissionRequested = false;


    @SuppressLint("SourceLockedOrientationActivity")
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.main_camera_activity);

        getWindow().setFormat(PixelFormat.UNKNOWN);


        surfaceView = (SurfaceView) findViewById(R.id.surface_view);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        /// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //controlInflater = LayoutInflater.from(getBaseContext());
        //viewSiteForOnionSkinControl = (RelativeLayout) (controlInflater.inflate(R.layout.camera_control_screen, null));

        //LayoutParams layoutParamsControl
        //        = new LayoutParams(LayoutParams.MATCH_PARENT,/// FILL_PARENT,
        //        LayoutParams.MATCH_PARENT);/// FILL_PARENT);


        //this.addContentView(viewSiteForOnionSkinControl, layoutParamsControl);
        layoutOnionSkin = (LinearLayout) (findViewById(R.id.layoutOnionSkin));
        initOnionskin(layoutOnionSkin, 3);

        testButton = findViewById(R.id.testButton);
        testButton.setText("...");

        testButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                StopmotionCamera.this.openOptionsMenu();
            }
        });

        currentDirectory = getAlbumStorageDir();

        //ffmpegCommandTest();
        //encodeCurrent();

        Log.d(LOGTAG, "created");

    }




    Button.OnClickListener buttonClickListener =
            new Button.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Log.d(LOGTAG, "on click listener");

                    if (takingPicture || camera == null || !previewing) return;

                    takingPicture = true;

                    if (justfocussed) {
                        justfocussed = false;
                        takingPicture = false;
                    } else {
                        try {
                            Log.d(LOGTAG, "going to take picture");
                            camera.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
                            Toast.makeText(StopmotionCamera.this, "ding!", Toast.LENGTH_SHORT).show();
                        } catch (Exception ex) {
                            takingPicture = false;
                            Log.d(LOGTAG, "failed to take picture!", ex);
                            Toast.makeText(StopmotionCamera.this, "Failed to take picture", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };

    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback() {

        @Override
        public void onShutter() {
        }
    };
    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
        }
    };
    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] arg0, Camera arg1) {
            if (arg0 == null || arg0.length == 0) {
                takingPicture = false;
                return;
            }

            final File targetDir = getAlbumStorageDir();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap decoded = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
                    if (decoded == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                takingPicture = false;
                            }
                        });
                        return;
                    }

                    String[] existing = targetDir.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            return (filename.endsWith("jpg"));
                        }
                    });
                    int imgs = (existing != null) ? existing.length : 0;

                    String stamp = String.format(StopmotionCamera.IMAGE_NUMBER_FORMAT, imgs);
                    final File targetFile = new File(targetDir, stamp + ".jpg");
                    File thumbDir = new File(targetDir.getPath() + THUMBNAIL_SUBFOLDER);
                    if (!thumbDir.exists()) {
                        thumbDir.mkdirs();
                    }
                    File thumbFile = new File(thumbDir, stamp + ".thumb.jpg");

                    int thumbWidth = Math.max(1, THUMB_HEIGHT * decoded.getWidth() / Math.max(1, decoded.getHeight()));
                    Bitmap smallerPicture = Bitmap.createScaledBitmap(decoded, thumbWidth, THUMB_HEIGHT, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    smallerPicture.compress(Bitmap.CompressFormat.JPEG, 30, stream);
                    byte[] byteArray = stream.toByteArray();

                    try (OutputStream imageFileOS = new FileOutputStream(targetFile)) {
                        imageFileOS.write(arg0);
                        imageFileOS.flush();
                    } catch (IOException e) {
                        Log.e(LOGTAG, "Failed writing picture", e);
                    }

                    try (OutputStream thumbFileOS = new FileOutputStream(thumbFile)) {
                        thumbFileOS.write(byteArray);
                        thumbFileOS.flush();
                    } catch (IOException e) {
                        Log.e(LOGTAG, "Failed writing thumbnail", e);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                currentDirectory = targetDir;
                                lastPicture = decoded;
                                onionSkinView.setBmp(decoded);
                                onionSkinView.updateBackground();

                                if (camera != null) {
                                    camera.startPreview();
                                    previewing = true;
                                }
                                Log.d(LOGTAG, "picture " + targetFile.getAbsolutePath());
                            } finally {
                                takingPicture = false;
                            }
                        }
                    });
                }
            }).start();
        }
    };

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        Log.d(LOGTAG, "onRestoreInstanceState");

        stretch = bundle.getBoolean("stretch", false);
        onionSkinView.setOpacity(bundle.getInt("opacity", 128));
        previewSizeWhich = bundle.getInt("previewSizeWhich", 100);
        pictureSizeWhich = bundle.getInt("pictureSizeWhich", 100);
        dateFormat = bundle.getString("dateFormat", defaultDateFormat);

        playbackSpeed = bundle.getInt("playbackSpeed", 70);
        numSkins = bundle.getInt("numSkins", 3);

        idPreviewSize("bollocks", previewSizeWhich);
        idPictureSize("bollocks", pictureSizeWhich);

        onionSkinView.setOnionSkins(numSkins);
        onionSkinView.setOpacity();
        onionSkinView.updateBackground();
        onionSkinView.invalidate();
        if (testButton != null) testButton.invalidate();

    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Log.d(LOGTAG, "onSaveInstanceState");

        bundle.putString("dateFormat", dateFormat);

        bundle.putInt("buttonlignment", alignment);
        bundle.putInt("opacity", onionSkinView.getOpacity());
        bundle.putBoolean("stretch", stretch);
        bundle.putInt("previewSizeWhich", previewSizeWhich);
        bundle.putInt("pictureSizeWhich", pictureSizeWhich);
        bundle.putInt("numSkins", numSkins);
        bundle.putInt("playbackSpeed", playbackSpeed);
        onionSkinView.invalidate();
        if (testButton != null) testButton.invalidate();

    }

    private Process launchLogcat() {

        try {
            File baseDir = getExternalFilesDir(null);
            if (baseDir == null) baseDir = getFilesDir();
            File filename = new File(baseDir, "stopmotion-logfile.log");
            filename.createNewFile();
            String cmd = "logcat -d -f " + filename.getAbsolutePath();
            return Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            Log.w(LOGTAG, "Could not write logcat file", e);
            return null;
        }
    }

    private void initOnionskin(LinearLayout layoutOnionSkin, int skins) {

        layoutOnionSkin.removeAllViews();
        onionSkinView = new OnionSkinView(this, skins);

        onionSkinView.setOnClickListener(buttonClickListener);

        onionSkinView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (camera != null) {
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            justfocussed = true;
                            Toast.makeText(StopmotionCamera.this, "focus", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return false;
            }
        });

        layoutOnionSkin.addView(onionSkinView);
        layoutOnionSkin.invalidate();

        onionSkinView.setOpacity();
        onionSkinView.updateBackground();
        onionSkinView.invalidate();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
            previewing = false;
        }

        save();

        onionSkinView.updateBackground();
        onionSkinView.invalidate();
        if (testButton != null) testButton.invalidate();
        Log.d(LOGTAG, "paused");

    }

    private void save() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("stretch", stretch);
        editor.putInt("opacity", onionSkinView.getOpacity());
        editor.putInt("buttonlignment", alignment);
        editor.putInt("previewSizeWhich", previewSizeWhich);
        editor.putInt("pictureSizeWhich", pictureSizeWhich);
        editor.putString("dateFormat", dateFormat);
        editor.putInt("numSkins", numSkins);
        editor.putInt("playbackSpeed", playbackSpeed);
        editor.apply();

        Log.d(LOGTAG, "committed and logged");
        process = launchLogcat();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");

        if (camera == null && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
        load();
        onionSkinView.updateBackground();
        onionSkinView.invalidate();
        if (testButton != null) testButton.invalidate();
    }

    private void load() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        stretch = settings.getBoolean("stretch", false);
        onionSkinView.setOpacity(settings.getInt("opacity", 128));
        previewSizeWhich = settings.getInt("previewSizeWhich", 100);
        pictureSizeWhich = settings.getInt("pictureSizeWhich", 100);
        numSkins = settings.getInt("numSkins", 3);
        playbackSpeed = settings.getInt("playbackSpeed", 10);
        alignment = settings.getInt("buttonlignment", R.id.rdoTL);

        onionSkinView.setOnionSkins(numSkins);

        dateFormat = settings.getString("dateFormat", defaultDateFormat);

        if (camera != null) {
            idPreviewSize("bollocks", previewSizeWhich);
            idPictureSize("bollocks", pictureSizeWhich);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (menu.findItem(ITEMID_PREVIEW) == null || menu.findItem(ITEMID_PICTURE) == null)
            return createMenu(menu);
        else return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /// public boolean onGroupItemClick(MenuItem item) {

        if (camera != null) {
            if (previewing) camera.stopPreview();
        }

        Log.d("item group " + item.getGroupId(), LOGTAG);
        boolean success = false;

        /// Handle item selection
        if (item.getGroupId() == GROUPID_PREVIEW) {

            Log.d(LOGTAG, "GROUPID_PREVIEW");
            /// preview
            success = idPreviewSize(item.getTitle().toString(), -1);
            invalidateOptionsMenu();
            closeOptionsMenu();
            Toast.makeText(StopmotionCamera.this, "SET PREVIEW " + success, Toast.LENGTH_SHORT).show();


        } else if (item.getGroupId() == GROUPID_PICTURE) {

            Log.d(LOGTAG, "GROUPID_PICTURE");
            /// pict
            success = idPictureSize(item.getTitle().toString(), -1);
            invalidateOptionsMenu();
            closeOptionsMenu();
            Toast.makeText(StopmotionCamera.this, "SET PICTURE " + success, Toast.LENGTH_SHORT).show();


        } else if (item.getGroupId() == GROUPID_ASPECT) {

            Log.d(LOGTAG, "GROUP ID ASPECT");
            /// pict
            setAspectLock(item.getTitle().toString());
            invalidateOptionsMenu();
            closeOptionsMenu();
            Toast.makeText(StopmotionCamera.this, aspectLock +" " + success, Toast.LENGTH_SHORT).show();


        } else if (item.getGroupId() == GROUPID_OTHER) {

            Log.d(LOGTAG, "GROUPID_OTHER");
            if (item.getTitle().equals(BUTTON_TOGGLE_STRETCH)) {
                Log.d(LOGTAG, "BUTTON_TOGGLE_STRETCH");
                setStretch(!stretch);

            } else if (item.getTitle().equals(SHOW_RUSHES)) {

                Log.d(LOGTAG, "SHOW_RUSHES");
                onionSkinView.setActivated(false);

                currentDirectory = getAlbumStorageDir();

                RushesDialog dialog = new RushesDialog(this);
                dialog.setPlayBackSpeed(playbackSpeed);
                dialog.setCurrentDirectory(currentDirectory);
                dialog.setOnionSkinView(onionSkinView);
                dialog.setPictureSize(pictureSize);
                dialog.setAlbumStorageDir(getAlbumStorageDir(true));
                dialog.show();
                onionSkinView.setActivated(true);

            } else if (item.getTitle().equals(CHANGE_DATE_FORMAT)) {
                // showEditDialog();
                Log.d(LOGTAG, "CHANGE_DATE_FORMAT");
                onionSkinView.setActivated(false);

                getSettingsDialog().show();

                onionSkinView.invalidate();
                setSize();
                setStretch();
                if (testButton != null) testButton.invalidate();
                onionSkinView.setActivated(true);
            }

            save();
        }

        if (camera != null) {
            if (previewing) camera.startPreview();
        }
        onionSkinView.invalidate();
        if (testButton != null) testButton.invalidate();
        Log.d(LOGTAG, "end of menu " + success);
        return success;
    }

    private Dialog getSettingsDialog() {

        return (new Dialog(this) {
            @Override
            protected void onCreate(Bundle savedInstanceState) {

                super.onCreate(savedInstanceState);

                requestWindowFeature(Window.FEATURE_NO_TITLE);

                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

                setContentView(R.layout.stopmotion_settings_panel);

                //getWindow().setLayout(-1, -1);

                final TextView showSkinNums = (TextView) findViewById(R.id.txtNumSkins);
                showSkinNums.setText(String.valueOf(onionSkinView.getNumSkins()));

                SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                seekBar.setMax(255);
                seekBar.setProgress(onionSkinView.getOpacity());

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        Log.d(LOGTAG, "progress " + progress);
                        onionSkinView.setOpacity(progress);
                        onionSkinView.updateBackground();
                        onionSkinView.invalidate();
                        if (testButton != null) testButton.invalidate();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

                final EditText editText = (EditText) findViewById(R.id.editFormat);
                editText.setClickable(true);
                editText.setEnabled(true);
                editText.setText(dateFormat);

                Button button = (Button) findViewById(R.id.button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dateFormat = editText.getText().toString();

                    }
                });

                Button defbutton = (Button) findViewById(R.id.defaultDateButton);
                defbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editText.setText(defaultDateFormat);
                    }
                });

                Button defbutton2 = (Button) findViewById(R.id.defaultDateButton2);
                defbutton2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editText.setText(defaultDateFormat + "-Scene-001");
                    }
                });

                Button btnSkinPlus = (Button) findViewById(R.id.btnSkinPlus);
                btnSkinPlus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onionSkinView.skinsInc();
                        showSkinNums.setText(String.valueOf(onionSkinView.getNumSkins()));
                    }
                });


                Button btnSkinMinus = (Button) findViewById(R.id.btnSkinMinusButton);
                btnSkinMinus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onionSkinView.skinsDec();
                        showSkinNums.setText(String.valueOf(onionSkinView.getNumSkins()));
                    }
                });

                Button btnSkinClear = (Button) findViewById(R.id.btnClearSkins);
                btnSkinClear.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onionSkinView.skinsClear();
                    }
                });



                final RadioGroup alignmentGroup = (RadioGroup) findViewById(R.id.radiogroup);
                alignmentGroup.check(alignment);

                final RadioButton tr = (RadioButton) findViewById(R.id.rdoTR);

                tr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            alignment = R.id.rdoTR;
                            alignmentGroup.check(R.id.rdoTR);
                            save();
                            setSize();
                        }
                    }
                });

                final RadioButton tl = (RadioButton) findViewById(R.id.rdoTL);
                tl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            alignment = R.id.rdoTL;
                            alignmentGroup.check(R.id.rdoTL);
                            save();
                            setSize();
                        }
                    }
                });

                final RadioButton br = (RadioButton) findViewById(R.id.rdoBR);
                br.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            alignment = R.id.rdoBR;
                            alignmentGroup.check(R.id.rdoBR);
                            save();
                            setSize();
                        }
                    }
                });

                final RadioButton bl = (RadioButton) findViewById(R.id.rdoBL);
                bl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            alignment = R.id.rdoBL;
                            alignmentGroup.check(R.id.rdoBL);
                            save();
                            setSize();
                        }
                    }
                });

            }
        });
    }

    public void setStretch() {
        setStretch(this.stretch);
    }

    public void setStretch(boolean stretch) {

        this.stretch = stretch;
        if (previewSize != null) setSize(previewSize.width, previewSize.height);
        Log.d(LOGTAG, "setStretch to " + this.stretch);
        onionSkinView.invalidate();

    }

    private boolean idPreviewSize(String thing, int idnum) {

        Log.d(LOGTAG, "idPreviewSize");
        if (camera == null) return false;

        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        boolean success = false;
        int enumc = 0;

        for (Camera.Size size : previewSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            if (thing.startsWith(text) || enumc == idnum) {
                try {
                    params.setPreviewSize(size.width, size.height);
                    camera.setParameters(params);
                    success = true;
                    previewSize = size;
                    setSize(size.width, size.height);
                    previewSizeWhich = enumc;
                    currentPreviewSize = text + " | " + String.format("%.3f", (float) size.width / size.height);
                    Log.d(LOGTAG, "currentPreviewSize set to " + currentPreviewSize);
                    save();
                    break;
                } catch (Exception ex) {
                    Log.w(LOGTAG, "Failed to set preview size " + text, ex);
                }
            }
            enumc++;
        }

        if (!success && previewSizes != null && !previewSizes.isEmpty()) {
            Camera.Size current = params.getPreviewSize();
            if (current == null) current = previewSizes.get(0);
            previewSize = current;
            setSize(current.width, current.height);
            currentPreviewSize = current.width + "x" + current.height + " | " + String.format("%.3f", (float) current.width / current.height);
            for (int i = 0; i < previewSizes.size(); i++) {
                Camera.Size s = previewSizes.get(i);
                if (s.width == current.width && s.height == current.height) {
                    previewSizeWhich = i;
                    break;
                }
            }
        }

        return success;
    }

    private boolean idPictureSize(String thing, int idnum) {

        Log.d(LOGTAG, "idPictureSize");
        if (camera == null) return false;

        boolean success = false;
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();

        int enumc = 0;

        for (Camera.Size size : pictureSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            if (thing.startsWith(text) || enumc == idnum) {
                try {
                    params.setPictureSize(size.width, size.height);
                    pictureSize = size;
                    camera.setParameters(params);
                    success = true;
                    currentPictureSize = text + " | " + String.format("%.3f", (float) size.width / size.height);
                    pictureSizeWhich = enumc;
                    save();
                    break;
                } catch (Exception ex) {
                    Log.w(LOGTAG, "Failed to set picture size " + text, ex);
                }
            }
            enumc++;
        }

        if (!success && pictureSizes != null && !pictureSizes.isEmpty()) {
            Camera.Size current = params.getPictureSize();
            if (current == null) current = pictureSizes.get(0);
            pictureSize = current;
            currentPictureSize = current.width + "x" + current.height + " | " + String.format("%.3f", (float) current.width / current.height);
            for (int i = 0; i < pictureSizes.size(); i++) {
                Camera.Size s = pictureSizes.get(i);
                if (s.width == current.width && s.height == current.height) {
                    pictureSizeWhich = i;
                    break;
                }
            }
        }

        return success;

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        surfaceReady = true;
        if (previewing && camera != null) {
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        previewing = false;
    }

    public boolean createMenu(Menu menu) {

        Log.d(LOGTAG,"creating menu");
        menu.clear();
        if (camera == null) {
            return false;
        }

        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();

        int order = 0;

        menu.add(GROUPID_OTHER, Menu.NONE, order++, BUTTON_TOGGLE_STRETCH);
        menu.add(GROUPID_OTHER, Menu.NONE, order++, CHANGE_DATE_FORMAT);
        menu.add(GROUPID_OTHER, Menu.NONE, order++, SHOW_RUSHES);

        SubMenu sm1 = menu.addSubMenu(GROUPID_PREVIEW, ITEMID_PREVIEW, order++, "Preview Size");

        for (Camera.Size size : previewSizes) {
            String aspect = String.format("%.3f", (float) size.width / size.height);
            String sizes = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            String text = sizes + " | " + aspect;
            if (aspectLock.equals("None") || aspect.equals(getAspectLock())) {
                MenuItem mi = sm1.add(GROUPID_PREVIEW, Menu.NONE, order++, text);
                mi.setCheckable(true);

                Log.d(LOGTAG, "currentPreviewSize " + currentPreviewSize +" vs "+text);
                if (text.equals(currentPreviewSize)) { mi.setChecked(true);}
            }

        }

        SubMenu sm2 = menu.addSubMenu(GROUPID_PICTURE, ITEMID_PICTURE, order++, "Picture Size");

        for (Camera.Size size : pictureSizes) {
            String aspect = String.format("%.3f", (float) size.width / size.height);
            String sizes = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            String text = sizes + " | " + aspect;

            if (aspectLock.equals("None") || aspect.equals(getAspectLock())) {
                MenuItem mi = sm2.add(GROUPID_PICTURE, Menu.NONE, order++, text);
                mi.setCheckable(true);
                if (text.equals(currentPictureSize)) { mi.setChecked(true);}
            }
        }

        SubMenu sm3 = menu.addSubMenu(GROUPID_ASPECT, ITEMID_ASPECT, order++, "Aspect Lock");

        MenuItem minn = sm3.add(GROUPID_ASPECT, Menu.NONE, order++, "None");
        ArrayList<String> listAspects = new ArrayList<String>();

        for (Camera.Size size : pictureSizes) {
            String text = String.format("%.3f", (float) size.width / size.height);
            if (!listAspects.contains(text)) {
                listAspects.add(text);
                MenuItem mi = sm3.add(GROUPID_ASPECT, Menu.NONE, order++, text);
                mi.setCheckable(true);
                if (getAspectLock().equals(text)) { mi.setChecked(true);}
            }
        }

        Log.d(LOGTAG, "created Menu for first time");

        return true;
    }

    private String getAspectLock() {
        return aspectLock;
    }

    private void setAspectLock(String val) {
        aspectLock = val;

    }

    public void setSize() {
        if (previewSize != null) {
            setSize(previewSize.width, previewSize.height);
        } else {
            setSize(lastGoodWidth, lastGoodHeight);
        }
    }

    public void setSize(int width, int height) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int measuredHeight = displayMetrics.heightPixels;
        int measuredWidth = displayMetrics.widthPixels;

        if (width <= 0 || height <= 0) {
            width = measuredWidth;
            height = measuredHeight;
        }

        float asp = (float) width / height;
        float dev_asp = (float) measuredWidth / measuredHeight;

        if (stretch || width > measuredWidth || height > measuredHeight) {

            if (asp > dev_asp) {
                /// wider, set width to device, change height
                width = measuredWidth;
                height = (int) ((float) measuredWidth / asp);

            } else if (asp < dev_asp) {
                /// narrower, set height to device, change width
                height = measuredHeight;
                width = (int) (asp * measuredHeight);
            } else {
                height = measuredHeight;
                width = measuredWidth;
            }

        }

        surfaceView.getHolder().setFixedSize(width, height);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

        surfaceView.setLayoutParams(layoutParams);
        surfaceView.invalidate();

        layoutOnionSkin.setLayoutParams(layoutParams);
        onionSkinView.updateBackground();
        onionSkinView.layout(0,0,0,0);
        onionSkinView.invalidate();
        layoutOnionSkin.invalidate();

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(150, 150);

        if (alignment == R.id.rdoBL) {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        } else if (alignment == R.id.rdoTL) {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        } else if (alignment == R.id.rdoBR) {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        } else if (alignment == R.id.rdoTR) {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        }

        if (testButton != null) {
            testButton.setLayoutParams(params);
            testButton.invalidate();
            testButton.bringToFront();
        }

        Log.d(LOGTAG, "setSize " + width + " " + height);

        lastGoodHeight = height;
        lastGoodWidth = width;

    }

    public File getAlbumStorageDir() {
        return getAlbumStorageDir(false);
    }

    public File getAlbumStorageDir(boolean termux) {
        String albumName = "Stopmotion-";
        try {
            albumName = "Stopmotion-" + (new SimpleDateFormat(dateFormat).format(new Date()));
        } catch (Exception ex) {
            getSettingsDialog().show();
            setSize();
            setStretch();
        }

        File picturesBase = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picturesBase == null) {
            picturesBase = new File(getFilesDir(), "Pictures");
        }
        File file = new File(picturesBase, albumName);
        if (!file.exists() && !file.mkdirs()) {
            Log.d(LOGTAG, "couldn't create " + albumName);
        }

        File file_subfolder = new File(picturesBase, albumName + THUMBNAIL_SUBFOLDER);
        if (!file_subfolder.exists() && !file_subfolder.mkdirs()) {
            Log.d(LOGTAG, "couldn't create " + albumName + THUMBNAIL_SUBFOLDER);
        }

        Log.d(LOGTAG, "getAlbumStorageDir " + file.toString());
        if (termux)
            return new File(file.getAbsolutePath());
        else return file;
    }

    private void openCamera() {
        if (camera != null) return;
        try {
            camera = Camera.open();
            if (camera != null && surfaceReady && surfaceHolder != null) {
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.startPreview();
                    previewing = true;
                } catch (IOException e) {
                    Log.e(LOGTAG, "Failed to start camera preview", e);
                }
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to open camera", e);
            Toast.makeText(this, "Could not open camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (!permissionRequested) {
            permissionRequested = true;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        Log.d(LOGTAG, "START");
        new CountDownTimer(2000, 200) {
            @Override
            public void onFinish() {
                if (!isFinishing() && !isDestroyed()) {
                    Log.d(LOGTAG, "set stretch with timebombtick");
                    setStretch(stretch);
                }
            }

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(LOGTAG, "tick");
            }
        }.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
                load();
            } else {
                Toast.makeText(this, "Camera permission is required to use Stopmotion Camera", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

}

