package robin.stopmotion;

import android.widget.SeekBar;

public class PlaybackThread  extends Thread {

    boolean running = false;


    SeekBar seekBar;
    int playBackSpeed;



    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }



    PlaybackThread(SeekBar seekbar, int playBackSpeed) {
        this.seekBar = seekbar;
        this.playBackSpeed=playBackSpeed;
    }




    public void run() {

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

        }
    }

}