package robin.stopmotion;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import android.annotation.SuppressLint;
import android.app.Dialog;

import android.content.SharedPreferences;
import android.net.*;
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
    private String currentPreviewSize;
    private String currentPictureSize;


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

        Button testButton = findViewById(R.id.testButton);
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
                    /// TODO Auto-generated method stub
                    Log.d(LOGTAG, "on click listener");

                    if (takingPicture) return;

                    takingPicture = true;

                    if (justfocussed) {
                        justfocussed = false;
                    } else {
                        try {
                            Log.d(LOGTAG, "going to take picture");
                            camera.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
                            Toast.makeText(StopmotionCamera.this, "ding!", Toast.LENGTH_SHORT).show();

                        } catch (Exception ex) {
                            Log.d(LOGTAG, "failed to take picture!");
                        }

                    }
                    takingPicture = false;
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

            currentDirectory = getAlbumStorageDir();

            int imgs = (currentDirectory.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return (filename.endsWith("jpg"));
                }
            })).length;

            String stamp = String.format(StopmotionCamera.IMAGE_NUMBER_FORMAT, imgs); // + "_" + String.valueOf((new Date()).getTime());
            Uri uriTarget = android.net.Uri.fromFile(new File(currentDirectory, stamp + ".jpg"));
            Uri uriTarget_thumb = android.net.Uri.fromFile(new File(currentDirectory.getPath() + THUMBNAIL_SUBFOLDER + '/', stamp + ".thumb.jpg"));

            //lastPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
            Bitmap smallerPicture = Bitmap.createScaledBitmap(lastPicture, THUMB_HEIGHT * lastPicture.getWidth() / lastPicture.getHeight(), THUMB_HEIGHT, false);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            smallerPicture.compress(Bitmap.CompressFormat.JPEG, 30, stream);
            byte[] byteArray = stream.toByteArray();

            OutputStream imageFileOS;
            try {
                imageFileOS = getContentResolver().openOutputStream(uriTarget);
                imageFileOS.write(arg0);
                imageFileOS.flush();
                imageFileOS.close();

                imageFileOS = getContentResolver().openOutputStream(uriTarget_thumb);
                imageFileOS.write(byteArray);
                imageFileOS.flush();
                imageFileOS.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            onionSkinView.setBmp(lastPicture);

            onionSkinView.updateBackground();
            camera.startPreview();
            previewing = true;

            Log.d(LOGTAG, "picture " + uriTarget.toString());

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
        testButton.invalidate();
        //testButton.bringToFront();

    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Log.d(LOGTAG, "onSaveInstanceState");

        bundle.putString("dateFormat", dateFormat);

        //bundle.putString("lastBmp", lastPictureFile);
        bundle.putInt("buttonlignment", alignment);
        bundle.putInt("opacity", onionSkinView.getOpacity());
        bundle.putBoolean("stretch", stretch);
        bundle.putInt("previewSizeWhich", previewSizeWhich);
        bundle.putInt("pictureSizeWhich", pictureSizeWhich);
        bundle.putInt("numSkins", numSkins);
        bundle.putInt("playbackSpeed", playbackSpeed);
        onionSkinView.invalidate();
        testButton.invalidate();
        //testButton.bringToFront();

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

    private void initOnionskin(LinearLayout layoutOnionSkin, int skins) {

        layoutOnionSkin.removeAllViews();
        onionSkinView = new OnionSkinView(this, skins);

        //onionSkinView.setLayoutParams(new LinearLayout.LayoutParams(
        //       LinearLayout.LayoutParams.MATCH_PARENT,
        //       LinearLayout.LayoutParams.MATCH_PARENT));


        onionSkinView.setOnClickListener(buttonClickListener);

        onionSkinView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        justfocussed = true;
                        Toast.makeText(StopmotionCamera.this, "focus", Toast.LENGTH_SHORT).show();
                    }
                });
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
        testButton.invalidate();
        //testButton.bringToFront();
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
        // Commit the edits!
        editor.commit();

        Log.d(LOGTAG, "committed and logged");
        process = launchLogcat();
        // Toast.makeText(StopmotionCamera.this, "properties saved and logged", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");

        load();
        onionSkinView.updateBackground();
        onionSkinView.invalidate();
        testButton.invalidate();
        //testButton.bringToFront();
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

        idPreviewSize("bollocks", previewSizeWhich);
        idPictureSize("bollocks", pictureSizeWhich);

        //Toast.makeText(StopmotionCamera.this,"properties loaded", Toast.LENGTH_SHORT).show();
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
                testButton.invalidate();
                //testButton.bringToFront();
                onionSkinView.setActivated(true);
            }

            save();
        }

        if (camera != null) {
            if (previewing) camera.startPreview();
        }
        onionSkinView.invalidate();
        testButton.invalidate();
        //testButton.bringToFront();
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
                        testButton.invalidate();
                        //testButton.bringToFront();
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
                defbutton.setOnClickListener(new View.OnClickListener() {
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
                currentPreviewSize = thing;
                Log.d(LOGTAG,"currentPreviewSize set to "+thing);
                save();
            }
            enumc++;
        }

        return success;
    }

    private boolean idPictureSize(String thing, int idnum) {

        Log.d(LOGTAG, "idPictureSize");
        boolean success = false;
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();

        int enumc = 0;

        for (Camera.Size size : pictureSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            if (thing.startsWith(text) || enumc == idnum) {
                Camera.Parameters params = camera.getParameters();
                params.setPictureSize(size.width, size.height);
                pictureSize = size;
                camera.setParameters(params);
                success = true;
                currentPictureSize = thing;
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

        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();

        int order = 0;

        menu.add(GROUPID_OTHER, Menu.NONE, order++, BUTTON_TOGGLE_STRETCH);
        //    menu.add(2, Menu.NONE, order++, CHANGE_OPACITY_DEC);
        //   menu.add(2, Menu.NONE, order++, CHANGE_OPACITY_INC);
        // menu.add(2, Menu.NONE, order++, ONION_LEAF_DEC);
        // menu.add(2, Menu.NONE, order++, ONION_LEAF_INC);
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
                if (currentPreviewSize.equals(text)) { mi.setChecked(true);}
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
                if (currentPictureSize.equals(text)) { mi.setChecked(true);}
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
        setSize(lastGoodWidth, lastGoodHeight);
    }

    public void setSize(int width, int height) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int measuredHeight = displayMetrics.heightPixels;
        int measuredWidth = displayMetrics.widthPixels;

        if (width == 0 && height == 0) {
            width = measuredWidth;
            height = measuredHeight;
        }

        float asp = (float) width / height;

        //int measuredHeight = surfaceView.getMeasuredHeight();
        //int measuredWidth = surfaceView.getMeasuredWidth();

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

        //Toast.makeText(StopmotionCamera.this,"Size " + width +"x" + height  + " Screen " + measuredWidth + "x" + measuredHeight  , Toast.LENGTH_LONG).show();
        surfaceView.getHolder().setFixedSize(width, height);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

        int l = (measuredWidth - width) / 2;
        int t = (measuredHeight - height) / 2;

        //surfaceView.layout(l, t, l + width, t + height);
        surfaceView.setLayoutParams(layoutParams);
        surfaceView.invalidate();

        //onionSkinView.layout(l, t, l + width, t + height);
        layoutOnionSkin.setLayoutParams(layoutParams);
        onionSkinView.updateBackground();
        onionSkinView.layout(0,0,0,0);
        onionSkinView.invalidate();
        layoutOnionSkin.invalidate();

        //if (alignmentGroup != null) {

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(150, 150);

        switch (alignment) {

            case R.id.rdoBL:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

                //testButton.layout(50, measuredHeight - 200, 200, measuredHeight - 50);
                //  Toast.makeText(this,"alignment set bl"  ,Toast.LENGTH_SHORT).show();
                break;

            case R.id.rdoTL:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                //testButton.layout(50, 50, 200, 200);
                // Toast.makeText(this,"alignment set tl"  ,Toast.LENGTH_SHORT).show();
                break;

            case R.id.rdoBR:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                // testButton.layout(measuredWidth - 200, measuredHeight - 200, measuredWidth - 50, measuredHeight - 50);
                //Toast.makeText(this,"alignment set br"  ,Toast.LENGTH_SHORT).show();
                break;

            case R.id.rdoTR:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                // testButton.layout(measuredWidth - 200, 50, measuredWidth - 50, measuredHeight - 200);
                // Toast.makeText(this,"alignment set tr "  ,Toast.LENGTH_SHORT).show();
                break;
            default:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                //testButton.layout(50, 50, 200, 200);
                //Toast.makeText(this,"alignment set default "  ,Toast.LENGTH_SHORT).show();
                break;
        }

        testButton.setLayoutParams(params);
        // }

        // testButton.layout(measuredWidth - 200,50,measuredWidth - 50,200);

        testButton.invalidate();
        testButton.bringToFront();

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
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.d(LOGTAG, "couldn't create " + albumName);
        }

        // Get the directory for the user's public pictures directory.
        File file_subfolder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName + THUMBNAIL_SUBFOLDER);
        if (!file_subfolder.mkdirs()) {
            Log.d(LOGTAG, "couldn't create " + albumName + THUMBNAIL_SUBFOLDER);
        }

        Log.d(LOGTAG, "getAlbumStorageDir " + file.toString());
        if (termux)
            return new File("/data/data/com.termux/files/home/storage/pictures/" + albumName);
        else return file;
    }

    @Override
    public void onStart() {
        super.onStart();

        camera = Camera.open();
        final StopmotionCamera me = StopmotionCamera.this;

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

