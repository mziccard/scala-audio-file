package me.mziccard.audio

import org.scalatest.{FlatSpec, MustMatchers}
import org.scalamock.scalatest.MockFactory

class WaveformSpec extends FlatSpec with MockFactory {

  "Waveform" should "get track info" in {
      val mockFile = mock[AudioFile]
      (mockFile.lengthInSeconds _).expects().returning(60L)
      (mockFile.numFrames _).expects().returning(3600)
      (mockFile.numChannels _).expects().returning(2)
      Waveform(mockFile)
  }
  
  "Waveform" should "read track frames" in {
      val mockFile = mock[AudioFile]
      (mockFile.lengthInSeconds _).expects().returning(60L)
      (mockFile.numFrames _).expects().returning(1024)
      (mockFile.numChannels _).expects().returning(2)
      (mockFile.readNormalizedFrames(_ : Array[Double], _ : Int)).expects(*, 2)
      Waveform(mockFile).getWaveform(512)
      
  }
  
  "Waveform" should "read enough frames" in {
      val mockFile = mock[AudioFile]
      (mockFile.lengthInSeconds _).expects().returning(60L)
      (mockFile.numFrames _).expects().returning(1024)
      (mockFile.numChannels _).expects().returning(2)
      var readFrames = 0
      (mockFile.readNormalizedFrames(_ : Array[Double], _ : Int)) expects(*, 2) onCall { 
        (buffer : Array[Double], numFramesToRead : Int) => {
          if (readFrames < 1024) {
            for(i <- 0 until numFramesToRead)
              buffer(i) = i.toFloat/numFramesToRead
            readFrames = readFrames + numFramesToRead
            2
          } else {
            0
          }
        }
      } repeat(513)
      Waveform(mockFile).getWaveform(512)
  }
  
  "Waveform" should "be of requested size" in {
      val mockFile = mock[AudioFile]
      (mockFile.lengthInSeconds _).expects().returning(60L)
      (mockFile.numFrames _).expects().returning(1024)
      (mockFile.numChannels _).expects().returning(2)
      var readFrames = 0
      (mockFile.readNormalizedFrames(_ : Array[Double], _ : Int)) expects(*, 2) onCall { 
        (buffer : Array[Double], numFramesToRead : Int) => {
          if (readFrames < 1024) {
            for(i <- 0 until numFramesToRead)
              buffer(i) = i.toFloat/numFramesToRead
            readFrames = readFrames + numFramesToRead
            2
          } else {
            0
          }
        }
      } repeat(513)
      val waveform = Waveform(mockFile).getWaveform(512)
      assert(waveform.length >= 512)
  }
  
  "Waveform" should "be the maximum of each frame block" in {
      val mockFile = mock[AudioFile]
      (mockFile.lengthInSeconds _).expects().returning(60L)
      (mockFile.numFrames _).expects().returning(1024)
      (mockFile.numChannels _).expects().returning(2)
      var readFrames = 0
      (mockFile.readNormalizedFrames(_ : Array[Double], _ : Int)) expects(*, 2) onCall { 
        (buffer : Array[Double], numFramesToRead : Int) => {
          if (readFrames < 1024) {
            for(i <- 0 until numFramesToRead)
              buffer(i) = i.toFloat/numFramesToRead
            readFrames = readFrames + numFramesToRead
            2
          } else {
            0
          }
        }
      } repeat(513)
      val waveform = Waveform(mockFile).getWaveform(512)
      waveform.foreach(x => assert(x == 1.toFloat/2))
  }
}