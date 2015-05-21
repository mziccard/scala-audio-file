package me.mziccard.audio.bpm

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import me.mziccard.audio.filter.AudioFilter
import me.mziccard.audio.AudioFile

/**
 * Class <code>FilterBPMDetector</code> can be used to 
 * detect the tempo of a track in beats-per-minute.
 * BPM Detection is not applied directly on audio 
 * data but first a filter is applied.
 * Objects of the class can be created given an 
 * <code>AudioFile</code> and an <code>AudioFilter</code>
 * 
 * To detect the tempo, first the audio filter is applied 
 * then peaks are identified on the filtered data.
 * The distance between peaks is identified and used to 
 * predict the tempo. Theoretical tempos are identified 
 * for each recurrent peak distance. Track tempo is assumed 
 * to be in the interval [90, 180]. Any lower theoretical 
 * tempo is doubled until it falls in the interval. Any 
 * higher theoretical tempo is divided by 2 until it 
 * falls in the above interval.
 * The most frequent theoretical tempo is picked as 
 * track's tempo.
 * 
 * Audio track data is buffered so that there's no need 
 * to load the whole track in memory before applying 
 * the detection. The user can provide the number of 
 * frames to buffer.
 * 
 * Class constructor is private, use the companion 
 * object instead.
 **/
class FilterBPMDetector private (
    private val audioFile : AudioFile,
    private var thresholdMultiplier : Double,
    private val skip : Int,
    private val framesToRead : Int, 
    private val audioFilter : AudioFilter) extends BPMDetector {

  /**
   * Size of the buffer to store frames to
   **/
  private val bufferSize = framesToRead*audioFile.numChannels;

  /**
   * Number of times the audio data buffer has been refilled 
   * so far
   **/
  private var bufferRefillCount = 0;

  /**
   * Number of the next frame to process. 
   * Refers to the whole track, is not a buffer pointer.
   **/
  private var nextFrame = 0;

  /**
   * Buffer to store frames yet to be processed. 
   * The buffer is of size <code>bufferSize</code>
   **/
  private val inputBuffer = new Array[Double](bufferSize);

  /**
   * Number of the frames in the track that hold a peak
   **/
  var peaks = ArrayBuffer[Int]();

  /**
   * HashMap of distances across peaks and their frequency 
   * <code>(d -> f)</code>. <code>d</code> is the distance 
   * between two peaks and <code>f</code> is the number of
   * pairs of peaks that have <code>d</code> frames between
   * eachother
   **/
  var distanceHistogram = HashMap[Int,Int]();

  /**
   * HashMap of theoretical tempos in the interval [90, 180]
   * and their frequency
   **/
  var bpmHistogram = HashMap[Int, Int]();

  /**
   * The tempo in beats-per-minute computed for the track
   **/
  var _bpm : Int = -1;

  /**
   * Threshold over which a sampled value is consided a peak.
   * Is computed as the maximum of the filtered sample values 
   * multipled to the <code>thresholdMultiplier</code>.
   **/
  var threshold : Double = _;

  /**
   * Applies a filter to the audio data one buffer at a time. 
   * Filtered data in the processed and oeaks above 
   * <code>threshold</code> are identified. 
   * Populated the <code>peaks</code> array.
   **/
  private def computePeaks() {
    var outputBuffer = new Array[Double](bufferSize);
    audioFile.readNormalizedFrames(
      inputBuffer, 
      framesToRead);
    audioFilter.apply(inputBuffer, outputBuffer)

    var maxVal : Double = 0;
    outputBuffer.foreach(x => if (x> maxVal) maxVal = x)
    threshold = thresholdMultiplier*maxVal
    bufferRefillCount = bufferRefillCount + 1;

    while (bufferRefillCount*framesToRead < audioFile.numFrames) {
      while (nextFrame < bufferRefillCount * framesToRead) {
        val localIndex = nextFrame - (bufferRefillCount-1) * framesToRead;

        var maxValue : Double = 0;
        for (i <- 0 until audioFile.numChannels) {
          val channelValue = outputBuffer(localIndex + i)
          if (channelValue > maxValue) {
            maxValue = channelValue;
          }
        }

/*
        var value : Double = 0;
        for (i <- 0 until audioFile.numChannels) {
          val channelValue = outputBuffer(localIndex + i)
          value = value + channelValue;
        }
        var maxValue = value / audioFile.numChannels;
*/        
        if (maxValue > threshold) {
          peaks += nextFrame;
          nextFrame = nextFrame + skip;
        } else {
          nextFrame = nextFrame + 1;
        }
      }

      outputBuffer = new Array[Double](bufferSize);
      audioFile.readNormalizedFrames(
        inputBuffer, 
        framesToRead);
      audioFilter.apply(inputBuffer, outputBuffer);
      bufferRefillCount = bufferRefillCount + 1;

      var maxVal : Double = 0;
      outputBuffer.foreach(x => if (x> maxVal) maxVal = x)
      threshold = (threshold + thresholdMultiplier*maxVal)/2
    }
  }

  /**
   * Given the peaks are identified, the histogram
   * of peaks distances (in number of frames) and their
   * frequency of occurrence is built. Such histogram
   * in stored in <code>distanceHistogram</code>
   **/
  private def computeDistanceHistogram() {
    peaks.zipWithIndex.foreach { 
      case(x,i) => {
        var j = i + 1;
        while (j < i + 10 && j < peaks.size) {
          val distance = peaks(j) - x;
          j = j + 1

          distanceHistogram.get(distance) match {
            case Some(count) => 
              distanceHistogram += (distance -> (count + 1))
            case None => 
              distanceHistogram += (distance -> 1)
          }
        }
      }
    }  
  }

  /**
   * Once peaks distances and their frequency are indentified 
   * theoretical tempos are computed and projected in the
   * interval [80, 190].
   * Theoretical tempos in [80, 190] are associated with
   * their own frequency.
   * <code>bpmHistogram</code> is populated with this data.
   **/
  private def computeBpmHistogram() {
    distanceHistogram.foreach(p => {
      val distance = p._1
      val distanceCount = p._2
      var theoreticalTempo = 
              (60 / (distance.toDouble / audioFile.sampleRate)).toInt

      if (theoreticalTempo > 1) {
        while (theoreticalTempo < 90) 
          theoreticalTempo = theoreticalTempo * 2;
        while (theoreticalTempo > 180) 
          theoreticalTempo = theoreticalTempo / 2;
        
        bpmHistogram.get(theoreticalTempo) match {
          case Some(count) =>
            bpmHistogram += (theoreticalTempo -> (count + distanceCount))
          case None => 
            bpmHistogram += (theoreticalTempo -> 1)
        }
      }
    });
  }

  /**
   * Browse through <code>bpmHistogram</code> to find
   * the most frequent theoretical tempo in the
   * interval between 90 and 180. <code>_bpm/code> is
   * set.
   **/
  private def computeBpm() = {
    var maxTempo = 0;
    var maxCount = 0;
    bpmHistogram.foreach(p => {
      val tempo = p._1
      val count = p._2
      if (count > maxCount) {
        maxTempo = tempo
        maxCount = count
      }      
    })
    _bpm = maxTempo
  } 

  /**
   * Returns the tempo of the associated audio file in 
   * beats per minute.
   * If called for the first time audio data is analyzed 
   * otherwise the last computed value is returned.
   * @return Detected track tempo in beats per minute
   **/
  def bpm() : Int = {

    if (_bpm == -1) {
      computePeaks();
      computeDistanceHistogram();
      computeBpmHistogram();
      computeBpm();
    }
    
    //bpmHistogram.foreach(p => println(p._1 + "->" +p._2));
    return _bpm
  }

}

object FilterBPMDetector {

  /**
   * Default threshold normalized in [0,1] over which 
   * a sampled value is considered a peak
   **/
  private val DefaultThreshold : Double = 0.95;

  /**
   * Portion of the sampling rate to be skipped 
   * when a peak is identified
   **/
  private val DefaultSkip : Double = 0.25;

  /**
   * Constructs a <code>FilterBPMDetector</code> with 
   * <code>threshold</code> set to 0.9, <code>skip</code> 
   * set to 1/4 of the sampling rate and 
   * <code>framesToRead</code> is set to the sampling
   * rate of the audio file.
   * @param audioFile The audio file to be analyzed
   * @param audioFilter Audio filter used to transform data 
   * before identifying peaks
   * @return A new <code>FilterBPMDetector</code> object 
   **/
  def apply(
    audioFile : AudioFile, 
    audioFilter : AudioFilter) : FilterBPMDetector = {
    return new FilterBPMDetector(
      audioFile,
      DefaultThreshold,
      (DefaultSkip * audioFile.sampleRate).toInt,
      audioFile.sampleRate.toInt, 
      audioFilter);
  }

  /**
   * Constructs a <code>FilterBPMDetector</code>.
   * @param audioFile The audio file to be analyzed
   * @param threshold Threshold normalized in [0,1]
   * over which a sampled value is consided a peak
   * @param skip Number of following frames to be 
   * skipped (not to be considered in peak detection) 
   * after a peak has been identified
   * @param framesToRead Size of the buffer where to store 
   * audio data
   * @param audioFilter Audio filter used to transform data 
   * before identifying peaks
   * @return A new <code>FilterBPMDetector</code> object 
   **/
  def apply(
    audioFile : AudioFile,
    threshold : Double,
    skip : Int, 
    framesToRead : Int,
    audioFilter : AudioFilter) : FilterBPMDetector = {
    return new FilterBPMDetector(
      audioFile,
      threshold,
      skip,
      framesToRead,
      audioFilter);
  }
}