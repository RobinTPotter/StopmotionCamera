package robin.stopmotion;

import android.util.Log;
import android.util.TimeUtils;
import android.widget.SeekBar;

public class PlaybackThread extends Thread {

    boolean running = false;
    String LOGTAG = "PlaybackThread";

    int playBackSpeed;


    SeekBar seekBar;

    public int getPlayBackSpeed() {
        return playBackSpeed;
    }

    public void setPlayBackSpeed(int playBackSpeed) {
        this.playBackSpeed = playBackSpeed;
    }

    int holdspeed = 1000;
    int currentSpeed;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (running) currentSpeed=playBackSpeed;
        else currentSpeed=holdspeed;
        Log.d(LOGTAG, "set running..." + running);
    }

    PlaybackThread(SeekBar seekbar, int playBackSpeed) {
        Log.d(LOGTAG, "creating...");
        this.seekBar = seekbar;
        this.playBackSpeed = playBackSpeed;
        Log.d(LOGTAG, "created...");
    }

    public void run() {

        Log.d(LOGTAG, "running...");
        try {
            while (running) {
                long start = System.currentTimeMillis();
                int progress = seekBar.getProgress();
                progress++;
                if (progress > seekBar.getMax()) {
                    progress = 0;
                }
                seekBar.setProgress(progress);

                long stop = System.currentTimeMillis();
                long taken = stop-start;
                long rem = (long)(Math.floor(1000.0/getPlayBackSpeed())) - taken;
                sleep(rem);

            }
            //onionSkinView.setBmp(squashedPreview.previewImages[progress]);
        } catch (Exception e) {
            Log.d(LOGTAG, "except..." + e.getMessage());
        }
    }

}