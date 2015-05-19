package me.mziccard.audio;

import scala.collection.mutable;

/** 
 * The <code>Waveform</code> class is able to estract the waveform given 
 * and AudioFile.Given an audio track as an instance of the <code>Waveform</code> class 
 * the associated waveform can be extracted using the method <code>getWaveform</code>.
 * The waveform is normalized in [0,1] so that it can be used with rendering libraries 
 * as waveform.js (http://www.waveformjs.org/)
 * 
 * When generating a waveform the user is asked to provide the number of desired 
 * points per minute.
 * 
 * A companion object method is provided to convert the waveform into a json 
 * array. The user can specify how many decimal digits should be used for 
 * each point.
 * 
 * @todo provide more flexible methods to get a waveform 
 * (e.g. pointsPerSecond, totalPoints)
 * 
 **/
class Waveform private (private val audioFile : AudioFile) {

  val lengthInSeconds = audioFile.lengthInSeconds
  val numChannels = audioFile.numChannels
  val numFrames = audioFile.numFrames

  /**
   * Returns and array of doubles representing the waveform of the track
   * as average of its channels
   * @param pointsPerMinute how many waveform samples we want to keep per minute
   * @return An array of float each of which is a normalized point of the waveform [0,1]
   **/
  def getWaveform(pointsPerMinute: Int) : Array[Float] = {

    var waveform = Array[Float]()

    val pointsPerSecond = pointsPerMinute.toFloat/60.toFloat
    // Points of the waveform we will collect
    val totalPoints = Math.ceil(pointsPerSecond*lengthInSeconds).toInt
    val sampleSize  = Math.floor(numFrames/totalPoints).toInt

    var framesRead  : Int = 0
    val buffer : Array[Double]  = new Array[Double](sampleSize * numChannels);

    do {
      // Read frames into buffer
      framesRead = audioFile.readNormalizedFrames(buffer, sampleSize);
      var frameMax: Float = Float.MinValue
      var frameIndex = 0
      while (frameIndex < sampleSize * numChannels) {
        // we select the maximum in the Frame
        //println(buffer(frameIndex))
        if (buffer(frameIndex) > frameMax) 
          frameMax = buffer(frameIndex).toFloat

        frameIndex = frameIndex + 1
      }
      waveform = waveform :+ frameMax
    } while (framesRead != 0);

    return waveform
  }
  
  /**
   * Close the audio file associated with the waveform
   **/
  def close() {
      audioFile.close();
  }
}

object Waveform {

  /**
   * Factory method to create a Waveform object
   * @param filename Name of the audio track
   **/
  def apply(filename: String) : Waveform = {
    val file = new java.io.File(filename);
    val audioFile = WavFile(file);
    new Waveform(audioFile);
  }

  /**
   * Factory method to create a Waveform object
   * @param file Audio track file
   **/
  def apply(file: java.io.File) : Waveform = {
    val audioFile = WavFile(file);
    new Waveform(audioFile);
  }

  /**
   * Factory method to create a Waveform object
   * @param audioFile Audio file for the track
   **/
  def apply(audioFile: AudioFile) : Waveform = {
    new Waveform(audioFile);
  }
  
  /**
   * Format a waveform to a json array
   * @param waveform Array of Float values representing the normalized waveform
   * @param precision Number of decimal digits for each waveform point
   * @return The waveform as a json array
   **/
  def formatToJson(waveform: Array[Float], precision: Int): String = {
    var waveformString = "[";
    val format : String = "%." + precision + "f"
    for (value <- waveform.slice(0, waveform.length - 1 )) {
      waveformString = waveformString + String.format(format, value: java.lang.Float) + ","
    }
    waveformString = waveformString + String.format(format, waveform(waveform.length-1): java.lang.Float) + "]"
    return waveformString
  }
}