package me.mziccard.audio.bpm

import me.mziccard.audio.AudioFile

trait BPMDetector {

  /**
   * Returns the beats per minute of an audio track
   **/
  def bpm() : Int;

}