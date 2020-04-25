package robin.stopmotion;
import android.widget.SeekBar;



import java.util.TimerTask;

final class MakeTask {

    public static TimerTask makeTask(SeekBar _seekBar) {
        final SeekBar seekBar = _seekBar;
        return new TimerTask() {
            public void run() {
                try {
                    int progress = seekBar.getProgress();
                    progress++;
                    if (progress > seekBar.getMax()) {
                        progress = 0;
                    }
                    seekBar.setProgress(progress);
                    //onionSkinView.setBmp(squashedPreview.previewImages[progress]);
                } catch (Exception e) {
                    //Toast.makeText(StopmotionCamera.this, "TimerTask " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }
}