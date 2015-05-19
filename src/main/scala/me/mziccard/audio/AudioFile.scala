package me.mziccard.audio

trait AudioFile {

  /**
   * The number of bytes for each sample
   **/ 
  def bytesPerSample : Int;

  /**
   * The number of frames in the audio file
   **/
  def numFrames : Long;

  /**
   * The number of channels in the audio file
   **/
  def numChannels : Int;

  /**
   * The sampling rante of the audio file
   **/
  def sampleRate : Long;

  /**
   * Number of bytes in a frame
   **/
  def blockAlign : Int;

  /**
   * The number of bits for each sample
   **/
  def validBits : Int;

  /**
   * Length of the track in seconds
   **/
  def lengthInSeconds : Long;

  /**
   * Track seconds ss in the format hh:mm:ss
   **/
  def trackSeconds : Long;

  /**
   * Track minutes mm in the format hh:mm:ss
   **/
  def trackMinutes : Long;

  /**
   * Track hours hh in the format hh:mm:ss
   **/
  def trackHours : Long;

  /**
   * Read frames values, normalized between 0 and 1
   * @param sampleBuffer Buffer where to put normalized frames
   * @param numFramesToRead Number of frames to read
   * @return The number of frames read
   **/
  def readNormalizedFrames(sampleBuffer : Array[Double], offset : Int, numFramesToRead : Int) : Int;

  /**
   * Read frames values, normalized between 0 and 1
   * @param sampleBuffer Buffer where to put normalized frames
   * @param offset Offset inside sampleBuffer where to start writing frames
   * @param numFramesToRead Number of frames to read
   * @return The number of frames read
   **/
  def readNormalizedFrames(sampleBuffer : Array[Double], numFramesToRead : Int) : Int;
  
  /**
   * Close the audio file
   **/
   def close();
}
