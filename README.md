# StopmotionCamera

## Simple Android camera app with onion skinning.

![build status](https://gitlab.com/robin.t.potter/stopmotioncamera-the-revenge/badges/master/pipeline.svg "Build")

Start StopmotionCamera and camera preview is seen behind a frosty window, this is an overlay with no pictures loaded.
Before you begin snapping away you should consider (and stick to!) a frame size for your images and preview window, these are seperate and, in release 1-4 at least, are set seperately.

Pressing the menu button should bring up the options to set these sizes (check "more" on the end if not visible).

You may consider changing the date format for the storage folder. The being under your device's Pictures folder. The date format is set in the "settings" option on the menu, which also contains a control for the opacity of the onion skin panel. Put text in single quotes to mask it from being interpreted. This is a java program so uses the rules [here](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html). Picture are stored in your phones Pictures directory with "Stopmotion" at the start:
```
albumName = "Stopmotion-" + (new SimpleDateFormat(dateFormat).format(new Date()));
```

In the menu the number of skins can be changed, as can whether the preview is stretched to fit the screen (if smaller than screen).

The "preview" command loads all the pictre in the current folder at a smaller scale to conserve memory. the seeking slider can be used to check the frames.


