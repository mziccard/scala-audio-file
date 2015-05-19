package me.mziccard.audio

import java.io.File;
import java.io.FileInputStream;

/** 
 * The <code>WavFile</code> class allows reading an audio file in WAVE format
 * The class extracts header information and provides it to the user in the 
 * form of getters.
 * The class also allows to read the audio file frame by frame. 
 * 
 * The class supports 8, 16, 24 and 32 bits samples both signed and unsigned
 * A FileInputStream is used to read the audio file. Once read the input 
 * stream is not reset. Any further attempt to reading will cause an error.
 * 
 * Read frames are put in a used defined buffer to serve for further processing.
 * 
 * A WavFile object must be closed when finished reading the file, thus 
 * closing the associated FileInputStream
 * 
 * This class is based on a Java project by A.Greensted and available at 
 * http://www.labbookpages.co.uk/audio/javaWavFiles.html
 **/
class WavFile private () extends AudioFile {

  /**
   * File for the wav track
   **/
  private var _file : File = _;

  /**
   * Setter for the file field
   **/
  private def file_= (value:File):Unit = _file = value

  /**
   * Number of bytes used to save a sample
   **/
  private var _bytesPerSample : Int = _;

  /**
   * Setter for the bytesPerSample field
   **/
  private def bytesPerSample_= (value:Int):Unit = _bytesPerSample = value;

  /**
   * Getter for the bytesPerSample field
   **/
  def bytesPerSample = _bytesPerSample;

  /**
   * NUmber of frames in the data section of the wav file
   **/
  private var _numFrames : Long = _;

  /**
   * Setter for the numFrames field
   **/
  private def numFrames_= (value:Long):Unit = _numFrames = value;

  /**
   * Getter for the num 
   **/
  def numFrames = _numFrames;

  /**
   * Audio file input stream
   **/
  private var _iStream : FileInputStream = _;

  /**
   * Setter for the iStream field
   **/
  private def iStream_= (value:FileInputStream):Unit = _iStream = value;

  /**
   * Getter for the iStream field
   **/
  private def iStream = _iStream;

  /**
   * Scale factor used to convert Ints to floats
   **/
  var floatScale : Double = _;

  /**
   * Offset factor used to covnert Ints to floats
   **/     
  var floatOffset : Double = _; 

  /**
   * Additive factor to convert signed to unsigned data
   **/
  var signedToUnsigned : Double = 0.0F;

  /**
   * Additive factor to convert unsigned to signed data
   **/
  var unsignedToSigned : Double = 0.0F;

  /**
   * Max possible unsigned PCM values for the wav file
   **/
  var maxUnsignedPCMValue : Double = 0.0F;

  /**
   * Max possible signed PCM values for the wav file
   **/
  var maxSignedPCMValue : Double = 0.0F;

  /**
   * Number of channels in the track
   * 2 bytes, unsigned value 
   **/
  private var _numChannels : Int = _;

  /**
   * Setter for the numChannels field
   **/
  private def numChannels_= (value:Int):Unit = _numChannels = value;

  /**
   * Getter for the numChannels field
   **/
  def numChannels = _numChannels;

  /**
   * Sampling rate for the audio track
   * 4 bytes, unsigned value
   **/
  private var _sampleRate : Long = _;

  /**
   * Setter for the sampleRate field
   **/
  private def sampleRate_= (value:Long):Unit = _sampleRate = value;

  /**
   * Getter for the sampleRate field
   **/
  def sampleRate = _sampleRate;

  /**
   * Number of bytes in a frame
   * 2 bytes, unsigned value
   **/
  private var _blockAlign : Int = _;

  /**
   * Setter for the numChannels field
   **/
  private def blockAlign_= (value:Int):Unit = _blockAlign = value;

  /**
   * Getter for the blockAlign field
   **/
  def blockAlign = _blockAlign;

  /**
   * Bits used to store a sample
   * 2 bytes, unsigned value
   * TODO change name
   **/
  private var _validBits : Int = _;

  /**
   * Setter for the numChannels field
   **/
  private def validBits_= (value:Int):Unit = _validBits = value;

  /**
   * Getter for the sampling rate
   **/
  def validBits = _validBits;

  /**
   * Buffer used to read frames
   **/
  var buffer : Array[Byte] = new Array[Byte](WavFile.BUFFER_SIZE);

  /**
   * Points to the current position the local buffer,
   * that is, the next frame to be read
   **/
  var bufferPointer : Int = _;

  /**
   * Bytes read during the last read in the local buffer 
   **/
  var bytesRead : Int = _;

  /**
   * Total number of frames read so far
   **/
  var frameCounter : Long = _;

  /**
   * Track length in seconds
   **/
  private var _lengthInSeconds : Long = _;

  /**
   * Setter for the lengthInSeconds field
   **/
  private def lengthInSeconds_= (value:Long):Unit = _lengthInSeconds = value;

  /**
   * Getter for the lengthInSeconds field
   **/
  def lengthInSeconds = _lengthInSeconds;

  /**
   * Track seconds ss in the format hh:mm:ss
   **/
  private var _trackSeconds : Long = _;

  /**
   * Setter for the trackSeconds field
   **/
  private def trackSeconds_= (value:Long):Unit = _trackSeconds = value;

  /**
   * Getter for the trackSeconds field
   **/
  def trackSeconds = _trackSeconds;

  /**
   * Track minutes mm in the format hh:mm:ss
   **/
  private var _trackMinutes : Long = _;

  /**
   * Setter for the trackMinutes field
   **/
  private def trackMinutes_= (value:Long):Unit = _trackMinutes = value;

  /**
   * Getter for the trackMinutes field
   **/
  def trackMinutes = _trackMinutes;

  /**
   * Track hours hh in the format hh:mm:ss
   **/
  private var _trackHours : Long = _;

  /**
   * Setter for the trackHours field
   **/
  private def trackHours_= (value:Long):Unit = _trackHours = value;

  /**
   * Getter for the trackHours field
   **/
  def trackHours = _trackHours;

  /**
   * Read a single sample value as long
   * @return The next sample
   **/
  private def readSample : Long = {
    var value : Long = 0L;

    for (b <- 0 until bytesPerSample) {
      if (bufferPointer == bytesRead) {
        val read : Int = iStream.read(buffer, 0, WavFile.BUFFER_SIZE);
        if (read == -1) throw new WavFile.WavFileException("Not enough data available");
        bytesRead = read;
        bufferPointer = 0;
      }

      var byteValue : Int = buffer(bufferPointer);
      if (b < bytesPerSample - 1 || bytesPerSample == 1) 
        byteValue = byteValue & 0xFF;
      value = value + (byteValue << (b * 8));

      bufferPointer = bufferPointer + 1;
    }

    return value;
  }

  /**
   * Read frames values, normalized between 0 and 1
   * @param sampleBuffer Buffer where to put normalized frames
   * @param numFramesToRead Number of frames to read
   * @return The number of frames read
   **/
  def readNormalizedFrames(sampleBuffer : Array[Double], numFramesToRead : Int) : Int = {
    return readNormalizedFrames(sampleBuffer, 0, numFramesToRead);
  }

  /**
   * Read frames values, normalized between 0 and 1
   * @param sampleBuffer Buffer where to put normalized frames
   * @param offset Offset inside sampleBuffer where to start writing frames
   * @param numFramesToRead Number of frames to read
   * @return The number of frames read
   **/
  def readNormalizedFrames(sampleBuffer : Array[Double], offset : Int, numFramesToRead : Int) : Int = {
    var index = offset;
    for (f <- 0 until numFramesToRead) {
      if (frameCounter == numFrames) return f;

      for (c <- 0 until numChannels) {
        //System.out.println("(" + signedToUnsigned + " + " + ((double) readSample()) + ")/"+((double) maxUnsignedPCMValue));
        sampleBuffer(index) = math.abs(math.abs(readSample) - unsignedToSigned).toDouble / maxSignedPCMValue.toDouble;
        index = index + 1;
      }

      frameCounter = frameCounter + 1;
    }

    return numFramesToRead;
  }
  
  /**
   * Close the audio file
   **/
  def close() {
    // Close the input stream and set to null
    if (iStream != null) {
      iStream.close();
      iStream = null;
    }
  }

}

object WavFile {
  val BUFFER_SIZE : Int = 4096;
  val FMT_CHUNK_ID : Int = 0x20746D66;
  val DATA_CHUNK_ID : Int = 0x61746164;
  val RIFF_CHUNK_ID : Int = 0x46464952;
  val RIFF_TYPE_ID : Int = 0x45564157;

  class WavFileException(message: String = null, cause: Throwable = null) 
    extends RuntimeException(message, cause);

  def getLE(buffer : Array[Byte], pos : Int, numBytes : Int) : Long = {
    var idx = pos + numBytes - 1;

    var value : Long = buffer(idx) & 0xFF;
    for (b <- 0 until (numBytes - 1)) {
      idx = idx - 1;
      value = (value << 8) + (buffer(idx) & 0xFF);
    }

    return value;
  }

  /**
   * Factory method used to create WavFile objects
   * @param file Track file
   **/
  def apply(file : File) : WavFile = {
    val wavFile = new WavFile;
    wavFile.iStream = new FileInputStream(file);

    // Read the first 12 bytes of the file
    var bytesRead = wavFile.iStream.read(wavFile.buffer, 0, 12);
    if (bytesRead != 12) throw new WavFileException("Not enough wav file bytes for header");

    // Extract parts from the header
    val riffChunkID = getLE(wavFile.buffer, 0, 4);
    var chunkSize = getLE(wavFile.buffer, 4, 4);
    val riffTypeID = getLE(wavFile.buffer, 8, 4);

    // Check the header bytes contains the correct signature
    if (riffChunkID != RIFF_CHUNK_ID) throw new WavFileException("Invalid Wav Header data, incorrect riff chunk ID");
    if (riffTypeID != RIFF_TYPE_ID) throw new WavFileException("Invalid Wav Header data, incorrect riff type ID");

    // Check that the file size matches the number of bytes listed in header
    if (file.length() != chunkSize+8) {
      throw new WavFileException("Header chunk size (" + chunkSize + ") does not match file size (" + file.length() + ")");
    }

    var foundFormat = false;
    var foundData = false;

    // Search for the Format and Data Chunks
    while (!foundData)
    {
      // Read the first 8 bytes of the chunk (ID and chunk size)
      bytesRead = wavFile.iStream.read(wavFile.buffer, 0, 8);
      if (bytesRead == -1) throw new WavFileException("Reached end of file without finding format chunk");
      if (bytesRead != 8) throw new WavFileException("Could not read chunk header");

      // Extract the chunk ID and Size
      val chunkID = getLE(wavFile.buffer, 0, 4);
      chunkSize = getLE(wavFile.buffer, 4, 4);

      // Word align the chunk size
      // chunkSize specifies the number of bytes holding data. However,
      // the data should be word aligned (2 bytes) so we need to calculate
      // the actual number of bytes in the chunk
      var numChunkBytes = (chunkSize%2) match {
        case 1 => chunkSize+1
        case 0 => chunkSize
      }

      if (chunkID == FMT_CHUNK_ID)
      {
        // Flag that the format chunk has been found
        foundFormat = true;

        // Read in the header info
        bytesRead = wavFile.iStream.read(wavFile.buffer, 0, 16);

        // Check this is uncompressed data
        val compressionCode = getLE(wavFile.buffer, 0, 2);
        if (compressionCode != 1) throw new WavFileException("Compression Code " + compressionCode + " not supported");

        // Extract the format information
        wavFile.numChannels = getLE(wavFile.buffer, 2, 2).toInt;
        wavFile.sampleRate = getLE(wavFile.buffer, 4, 4);
        wavFile.blockAlign = getLE(wavFile.buffer, 12, 2).toInt;
        wavFile.validBits = getLE(wavFile.buffer, 14, 2).toInt;

        if (wavFile.numChannels == 0) throw new WavFileException("Number of channels specified in header is equal to zero");
        if (wavFile.blockAlign == 0) throw new WavFileException("Block Align specified in header is equal to zero");
        if (wavFile.validBits < 2) throw new WavFileException("Valid Bits specified in header is less than 2");
        if (wavFile.validBits > 64) throw new WavFileException("Valid Bits specified in header is greater than 64, this is greater than a long can hold");

        // Calculate the number of bytes required to hold 1 sample
        wavFile.bytesPerSample = (wavFile.validBits + 7) / 8;
        if (wavFile.bytesPerSample * wavFile.numChannels != wavFile.blockAlign)
          throw new WavFileException("Block Align does not agree with bytes required for validBits and number of channels");

        // Account for number of format bytes and then skip over
        // any extra format bytes
        numChunkBytes = numChunkBytes - 16;
        if (numChunkBytes > 0) wavFile.iStream.skip(numChunkBytes);
      } else if (chunkID == DATA_CHUNK_ID) {
        // Check if we've found the format chunk,
        // If not, throw an exception as we need the format information
        // before we can read the data chunk
        if (foundFormat == false) throw new WavFileException("Data chunk found before Format chunk");

        // Check that the chunkSize (wav data length) is a multiple of the
        // block align (bytes per frame)
        if (chunkSize % wavFile.blockAlign != 0) throw new WavFileException("Data Chunk size is not multiple of Block Align");

        // Calculate the number of frames
        wavFile.numFrames = chunkSize / wavFile.blockAlign;

        // Calculate the length of the track in seconds
        wavFile.lengthInSeconds = wavFile.numFrames/wavFile.sampleRate;
        
        // Flag that we've found the wave data chunk
        foundData = true;
      } else {
        // If an unknown chunk ID is found, just skip over the chunk data
        wavFile.iStream.skip(numChunkBytes);
      }
    }

    // Throw an exception if no data chunk has been found
    if (foundData == false) throw new WavFileException("Did not find a data chunk");

    // Calculate the scaling factor for converting to a normalised double
    if (wavFile.validBits > 8) {
      // If more than 8 validBits, data is signed
      // Conversion required dividing by magnitude of max negative value
      wavFile.floatOffset = 0;
      wavFile.floatScale = 1 << (wavFile.validBits - 1);
      wavFile.unsignedToSigned = 0x0F;
      wavFile.maxSignedPCMValue = (0x1 << (wavFile.validBits-1));
    }
    else {
      // Else if 8 or less validBits, data is unsigned
      // Conversion required dividing by max positive value
      wavFile.floatOffset = -1;
      wavFile.unsignedToSigned = (0x1 << (wavFile.validBits-1));
      wavFile.floatScale = 0.5 * ((1 << wavFile.validBits) - 1);
      wavFile.maxSignedPCMValue = (0x1 << (wavFile.validBits-1));
    }

    wavFile.bufferPointer = 0;
    wavFile.bytesRead = 0;
    wavFile.frameCounter = 0;
    wavFile.trackMinutes = wavFile.lengthInSeconds / 60;
    wavFile.trackSeconds = wavFile.lengthInSeconds % 60;
    wavFile.trackHours = wavFile.trackMinutes / 60;
    wavFile.trackMinutes = wavFile.trackMinutes % 60;

    return wavFile;
  }

  /**
   * Factory method used to create WavFile objects
   * @param filename Name of the track
   **/
  def apply(filename : String) : WavFile = {
    val file = new File(filename);
    return apply(file);
  }

}


