# StopmotionCamera

## Simple Android camera app with onion skinning.

![](https://gitlab.com/nobbymilkshakes/StopmotionCamera/badges/master/pipeline.svg)

[Latest apk!]([https://gitlab.com/nobbymilkshakes/StopmotionCamera/-/jobs/artifacts/master/raw/build/outputs/apk/debug/StopmotionCamera-debug.apk?job=assembleDebug](https://gitlab.com/nobbymilkshakes/StopmotionCamera/-/jobs/artifacts/master/raw/build/outputs/apk/debug/StopmotionCamera-debug.apk?job=assembleDebug))

Start StopmotionCamera and camera preview is seen.
As pictures are taken the previous appears semi-transparently.

Before you begin consider a frame size for your images and preview window,
these are seperate and, are set seperately.
Pressing the menu button should bring up the options to set these sizes.

Storage location defaults to ```Pictures/StopmotionCamera-{date}```.
There is created a ```thumbs``` directory beneath.

You may consider changing the date format for the storage folder, always saved under your device's Pictures folder.
The date format is set in the "settings" option on the menu, which also contains a control for the opacity of the onion skin panel.
Put text in _single quotes to mask it from being interpreted_. This is a java program so uses the rules [here](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).
Pictures are stored in your phones Pictures directory with "Stopmotion-" at the start:

```
albumName = "Stopmotion-" + (new SimpleDateFormat(dateFormat).format(new Date()));
```

In the settings screen the number of skins can
be changed, as can whether the preview is
stretched to fit the screen (if smaller than screen).

The "preview" command loads all the pictre in the current ```thumbs``` folder.
The seeking slider can be used to check the frames.

Note: this is not a finished video.

Creating a video is beyond the scope of this app, however,
An encoding command line for ffmpeg can be copied to the clipboard
for termux.

