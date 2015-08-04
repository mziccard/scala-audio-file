# scala-audio-file
Minimal Scala library to process audio files. 
Only WAVE files are supported now.

## Overview
This library allows to:
- Wrap an audio file and extract metadata as well as audio data
- Compute a normalized waveform 
- Detect audio tempo in beats per minute

## Cloning the library
The library uses [JWave](https://github.com/cscheiblich/jwave)
and adds it as a git submodule in `lib/jwave`. 
To have a working copy of the library, after cloning the repository 
you need to init and update the submodules. 
```bash
git submodule init
git submodule update
```

## Configuring sbt
The scala audio library can be included into you sbt project as:
```scala
resolvers += Resolver.bintrayRepo("mziccard", "maven")
libraryDependencies ++= Seq("me.mziccard" %% "scala-audio-file" % "0.2")
```
All releases are pushed to the maven repository.
Latest release is:
- scala-audio-file v0.2 compatible with Scala 2.10 and Scala 2.11

## Library
A WAVE file can be opened via the `WavFile` class. The class has 
private constructor and cannot be directly istantiated, use the 
companion object instead. `WavFile` provides several functionalites 
to access audio data and metadata.  
Audio samples can be read as floating point values in the interval [0,1].
```scala
val audioFile = WavFile("filename.wav");
val readBuffer = new Array[Double](1024*audioFile.numChannels);
val framesRead = audioFile.readNormalizedFrames(readBuffer, 1024);
```
or as integer values.
```
val audioFile = WavFile("filename.wav");
val readBuffer = new Array[Double](1024*audioFile.numChannels);
val framesRead = audioFile.readFrames(readBuffer, 1024);
```

### Computing waveform
A waveform can be computed for the audio track by wrapping the file 
object inside a `Waveform` object. A waveform is represented as a 
Scala array of Double values in the interval [0,1]. User can specify 
the amount of points per minutes the waveform should be made of. 
```scala
val audioFile = WavFile("filename.wav");
val waveform = Waveform(audioFile);
var waveformJSON = Waveform.formatToJson(waveform.getWaveform(512), 2);
```
`Waveform` companion object provides a method to export the waveform 
as a JSON array with a controlled amount of decimal digits.

### Computing beats per minute
Three classes are available to compute audio file tempo in bpm. Both 
classes implement the `BPMDetector` trait.
```scala
trait BPMDetector {
  def bpm() : Double;
}
```
The `SoundEnergyBPMDetector` applies a simple bpm detection algorithm 
based on identificaiton of energy peaks in the track's audio data. No 
transform is applied to data, the class implementes the algorithm #3 
described [here](http://goo.gl/AmWo1u).
```scala
val audioFile = WavFile("filename.wav");
val tempo = SoundEnergyBPMDetector(audioFile).bpm;
```
The `FilterBPMDetector` applies a more complex algorithm based on 
filters. Data are filtered, for instance low-passed, before 
detecting peaks. Tempo is computed as the most recurring distance 
across identified peaks. The `BiquadFilter` class, implementing 
a [biquad filter](http://www.musicdsp.org/files/Audio-EQ-Cookbook.txt), 
can be used in combination with `FilterBPMDetector`. 
```scala
val audioFile = WavFile("filename.wav");
val filter = BiquadFilter (
  audioFile.sampleRate,
  audioFile.numChannels,
  FilterType.LowPass)
val detector = FilterBPMDetector(audioFile, filter);
val tempo = detector.bpm
```
More complex factory methods are also available in `FilterBPMDetector` 
that allow more fine-grained configuration.
For more details on the algorithm implemented by `FilterBPMDetector` 
you can have a look at Beatport's 
[blog](http://tech.beatport.com/2014/web-audio/beat-detection-using-web-audio/).  

The class `WaveletBPMDetector` applies a more precise algorithm 
(described in this [paper](http://soundlab.cs.princeton.edu/publications/2001_amta_aadwt.pdf))
based on the Discrete Wavelet Transform (DWT). The algorithm operates on windows 
of frames so the class can be istantiated by providing an audio file, the 
size of a window in number of frames and the type of wavelet.
```scala
val audioFile = WavFile("filename.wav")
val tempo = WaveletBPMDetector(
              audioFile, 
              131072, 
              WaveletBPMDetector.Daubechies4).bpm
```
So far only Haar and Daubechies4 wavelets are supported. 
Due to the way DWT is implemented the size of a window must be a power of 2. 
An additional integer parameter can be given to the factory method 
providing the maximum number of windows to process. If not specified 
the entire track is processed.
