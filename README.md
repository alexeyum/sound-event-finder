# Sound Event Finder

Mobile app for detecting sound events in ambient audio files using machine learning.

## How it works

Record an audio on the mobile, then open it in the app and run prediction. The app will predict
what is main sound event in the file out of 10 classes:

<img src=https://github.com/alexeyum/sound-event-finder/blob/main/images/screenshot.png/ height="600" />

## What's inside

First, a small 1D convolutional neural network was trained on the [UrbanSound8K](https://urbansounddataset.weebly.com/urbansound8k.html) dataset.
It reads an ~8sec audio file and predicts probabilities of 1-out-10 classes. All the training was done on GoogleColab GPU using PyTorch.
See `./training/` directory for the training source code.

Then, the model was exported using torch-to-mobile library and wrapped into an Android app. The app just loads the model, then applies
it to the arbitrary file with some audio preprocessing. See `./app/` directory for app source code and building the app.

Current app limitations:

- The model only processes the first 8 seconds of audio, everything after that is ignored.
- The app only knows how to work with stereo 16-bit WAV files with sampling rates between 24K and 96K.
- The audio preprocessing is not optimized and may take a lot of time for long files.
  
## Development plans

Main features I want to add in the future:

- Detect multiple events in a single file. Also detect time intervals for the events.
- Add more variety to the detected classes of events.
- Improve audio preprocessing speed and prediction quality.

