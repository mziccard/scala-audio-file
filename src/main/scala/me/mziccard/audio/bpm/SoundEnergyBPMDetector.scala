package me.mziccard.audio.bpm

import scala.collection.mutable.ArrayBuffer

import me.mziccard.audio.AudioFile

class SoundEnergyBPMDetector private (
  private val audioFile : AudioFile,
  private val samplesPerBlock : Int) extends BPMDetector {

  /**
   * Number of blocks considered in each second
   **/
  private val blocksPerSecond : Int = (audioFile.sampleRate / samplesPerBlock).toInt;

  /**
   * Buffer containing the last block read. Made of 
   * <code>samplesPerBlock</code> frames
   **/
  private val blockBuffer : Array[Double] = 
    new Array[Double](audioFile.numChannels * samplesPerBlock);

  /**
   * Circular buffer saving the energy values for the last 
   * blocksPerSecond blocks.
   **/
  private val energyBuffer : Array[Double] =
    new Array[Double](blocksPerSecond)

  /**
   * Index in the <code>energyBuffer</code> array where to 
   * store the next energy value
   **/
  private var energyBufferPointer = 0;

  /**
   * Number of blocks read so far
   **/
  private var blockCounter = 0;

  /**
   * Total number of beats identified in the track
   **/
  private var beatCounter : Int = 0;

  /**
   * The tempo in beats-per-minute computed for the track
   **/
  var _bpm : Double = -1.0;

  /**
   * Array of instantaneous BPM values collected in the track
   * could be used to extract an overall BPM as the median/maximum/minimum
   **/
  private var instantBpm = ArrayBuffer[Double]()

  private def averageLocalEnergy() : Double = {
    var energySum : Double = 0;
    energyBuffer.foreach(
      (e : Double) => { energySum = energySum + e });
    return energySum/blocksPerSecond;
  }

  private def energyVariance(average : Double) : Double = {
    var energyVariance : Double = 0;
    energyBuffer.foreach(
      (e : Double) => { 
        //println("iter " + e + " - " + average)
        energyVariance = energyVariance + (e - average)*(e - average) });
    return energyVariance/blocksPerSecond;
  }

  private def C(variance : Double) : Double = {
    return SoundEnergyBPMDetector.C_ADDER + 
      SoundEnergyBPMDetector.C_MULTIPLIER * variance;
  }

  def bpm() : Double = {
    if (_bpm == -1.0) {
      var localBlockCounter : Int = 0;
      var localPeakCounter : Int = 0;
      var localBeatCounter : Int = 0;
      var readFrames : Int = 
        audioFile.readNormalizedFrames(blockBuffer, samplesPerBlock);
   
      while(readFrames != 0) {
        if (readFrames == samplesPerBlock) {
          var energy : Double = 0;
          var i = 0;
          while (i < samplesPerBlock) {
            energy = energy + 
              blockBuffer(2*i) * blockBuffer(2*i) +
              blockBuffer(2*i+1) * blockBuffer(2*i+1);
            i = i + 1;
          }
          energyBuffer(energyBufferPointer) = energy;
          energyBufferPointer = (energyBufferPointer + 1) % blocksPerSecond;
          blockCounter = blockCounter + 1;
          localBlockCounter = localBlockCounter + 1;

          if (blockCounter > blocksPerSecond) {
            val average = averageLocalEnergy();
            val variance = energyVariance(average);

            val Cparameter = C(variance);
            val soil = Cparameter*average;
            
            if (energy > soil) {
              localPeakCounter = localPeakCounter + 1;
              if (localPeakCounter == 4) {
                localPeakCounter = 0;
                localBeatCounter = localBeatCounter + 1;
                beatCounter = beatCounter + 1;
              }
            } else {
              localPeakCounter = 0;
            }

            if (localBlockCounter > audioFile.sampleRate.toInt * 5 / samplesPerBlock) {
              val beatsPerMinute : Double = 
                (localBeatCounter * audioFile.sampleRate * 60).toDouble / (localBlockCounter * samplesPerBlock);
              
              instantBpm += beatsPerMinute

              localBeatCounter = 0;
              localBlockCounter = 0;
            }

          }
        }
        readFrames =
          audioFile.readNormalizedFrames(blockBuffer, samplesPerBlock);
      }
      _bpm = (beatCounter * audioFile.sampleRate * 60).toDouble / (blockCounter * samplesPerBlock)
    }
    return _bpm
  }

}

object SoundEnergyBPMDetector {

  val SAMPLES_PER_BLOCK = 1024

  val C_MULTIPLIER = -0.0000075;
  val C_ADDER = 1.5142857;

  def apply(audioFile : AudioFile) : SoundEnergyBPMDetector = {
    return new SoundEnergyBPMDetector(audioFile, SAMPLES_PER_BLOCK)
  }
}