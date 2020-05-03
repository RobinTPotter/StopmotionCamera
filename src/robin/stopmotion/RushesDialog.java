package robin.stopmotion;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class RushesDialog extends Dialog {

    private int playbackSpeed;
    PlaybackThread playbackThread;
    Camera.Size pictureSize;

    public File getAlbumStorageDir() {
        return albumStorageDir;
    }

    public void setAlbumStorageDir(File albumStorageDir) {
        this.albumStorageDir = albumStorageDir;
    }

    File albumStorageDir;

    public Camera.Size getPictureSize() {
        return pictureSize;
    }

    public void setPictureSize(Camera.Size pictureSize) {
        this.pictureSize = pictureSize;
    }


    public OnionSkinView getOnionSkinView() {
        return onionSkinView;
    }

    public void setOnionSkinView(OnionSkinView onionSkinView) {
        this.onionSkinView = onionSkinView;
    }

    OnionSkinView onionSkinView;

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    File currentDirectory;

    public int getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlayBackSpeed(int p) {
        playbackSpeed = p;
    }

    public RushesDialog(Context context) {
        super(context);
    }

    public RushesDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    protected RushesDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.stopmotion_rushes_panel);

    }

    protected void onStart() {

        final TextView fpsText = (TextView) findViewById(R.id.fps);
        fpsText.setText(String.valueOf(playbackSpeed));

        final SquashedPreview squashedPreview = (SquashedPreview) findViewById(R.id.view);

        final SeekBar playbackSpeedBar = (SeekBar) findViewById(R.id.playbackSpeed);
        playbackSpeedBar.setProgress(playbackSpeed);
        playbackSpeedBar.setMax(StopmotionCamera.MAX_FPS);
        playbackSpeedBar.setMin(1);

        playbackSpeedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                playbackSpeed = progress;
                fpsText.setText(String.valueOf(playbackSpeed));
                if (playbackSpeed == 0) playbackSpeed = 1;
                if (playbackThread != null)
                    playbackThread.setPlayBackSpeed(playbackSpeed);

                fpsText.setText(String.valueOf(playbackSpeed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final SeekBar previewSeekBar = (SeekBar) findViewById(R.id.previewSeekBar);
        squashedPreview.setSeekbar(previewSeekBar);

        squashedPreview.setDirectory(currentDirectory.getPath(), StopmotionCamera.THUMBNAIL_SUBFOLDER);

        previewSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                squashedPreview.setImageNumber(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button btnFrameLeft = (Button) findViewById(R.id.frameLeft);
        btnFrameLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int current = squashedPreview.getImageNumber();
                if (current == 0) {
                    current = squashedPreview.getNumberImages();
                }
                current--;
                squashedPreview.setImageNumber(current);
            }
        });

        Button btnFrameRight = (Button) findViewById(R.id.frameRight);
        btnFrameRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int current = squashedPreview.getImageNumber();
                current++;

                if (current == squashedPreview.getNumberImages()) {
                    current = 0;
                }
                squashedPreview.setImageNumber(current);
            }
        });


        Button buttonSetSkins = (Button) findViewById(R.id.setSkins);
        buttonSetSkins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (squashedPreview.previewImages.length == 0) return;
                for (int nn = previewSeekBar.getProgress(); nn < previewSeekBar.getProgress() + onionSkinView.getNumSkins(); nn++) {
                    onionSkinView.setBmp(squashedPreview.previewImages[nn]);
                }
            }
        });

        final Button buttonPlay = (Button) findViewById(R.id.play);

        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (buttonPlay.getText().equals(StopmotionCamera.PLAY)) {
                    buttonPlay.setText(StopmotionCamera.STOP);
                    playbackThread = new PlaybackThread(previewSeekBar, playbackSpeed);
                    playbackThread.setRunning(true);
                    playbackThread.start();

                    try {
                    } catch (Exception ex) {
                        Log.d(StopmotionCamera.LOGTAG, "except..." + ex.getMessage());
                    }
                } else {
                    buttonPlay.setText(StopmotionCamera.PLAY);
                    playbackThread.setRunning(false);

                }

            }
        });

        final Button buttonEncode = (Button) findViewById(R.id.encode);

        buttonEncode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "encode pressed", Toast.LENGTH_SHORT).show();
                //StopmotionCamera.this.ffmpegCommandTest();
                //StopmotionCamera.this.encodeCurrent();
                //StopmotionCamera.this.justDoThis();

                if (getPictureSize() == null) {
                    Toast.makeText(getContext(), "no picture size set! (perhaps no pictures?)", Toast.LENGTH_SHORT).show();
                } else {
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("ffmpeg string for termux", "ffmpeg -r 15 -f image2 -s "
                            + getPictureSize().width + "x" + getPictureSize().height + " -i "
                            + getAlbumStorageDir() + "/" + StopmotionCamera.IMAGE_NUMBER_FORMAT
                            + ".jpg -vcodec libx264 -crf 25 -pix_fmt yuv420p "
                            + getAlbumStorageDir() + "/out.mp4");
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "copied", Toast.LENGTH_SHORT).show();
                }
            }
        });


        Button btnDeleteCurrent = findViewById(R.id.deleteCurrent);
        btnDeleteCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int im = squashedPreview.getImageNumber();
                Toast.makeText(getContext(), "delete " + squashedPreview.deleteImage(im), Toast.LENGTH_SHORT).show();
                squashedPreview.setDirectory();
                squashedPreview.setImageNumber(im);
                previewSeekBar.setProgress(im);
            }
        });

        Button btnDeleteAll = findViewById(R.id.deleteAll);
        btnDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int im = squashedPreview.getImageNumber();
                if (squashedPreview.deleteAll()) {
                    Toast.makeText(getContext(), "deleted all", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                }
                squashedPreview.setDirectory();
                squashedPreview.setImageNumber(im);
                previewSeekBar.setProgress(im);

            }
        });


    }

}
