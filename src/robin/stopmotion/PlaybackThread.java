package robin.stopmotion;

import android.util.Log;
import android.widget.SeekBar;

public class PlaybackThread  extends Thread {

    boolean running = false;
String LOGTAG = "PlaybackThread";

    SeekBar seekBar;
    int playBackSpeed;



    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        Log.d(LOGTAG, "set running..." + running);
    }



    PlaybackThread(SeekBar seekbar, int playBackSpeed) {
        Log.d(LOGTAG, "creating...");
        this.seekBar = seekbar;
        this.playBackSpeed=playBackSpeed;
        Log.d(LOGTAG, "created...");
    }




    public void run() {

        Log.d(LOGTAG, "running...");
        try {
            while (running) {
                int progress = seekBar.getProgress();
                progress++;
                if (progress > seekBar.getMax()) {
                    progress = 0;
                }
                seekBar.setProgress(progress);
                this.sleep(playBackSpeed);
            }
            //onionSkinView.setBmp(squashedPreview.previewImages[progress]);
        } catch (Exception e) {
            Log.d(LOGTAG, "except..."+e.getMessage());

        }
    }

}