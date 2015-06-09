package me.mziccard.audio.bpm

import me.mziccard.audio.AudioFile

trait BPMDetector {

  /**
   * Compute the beats per minute of an audio track
   * @return Track tempo in beats per minute
   **/
  def bpm() : Double;

}