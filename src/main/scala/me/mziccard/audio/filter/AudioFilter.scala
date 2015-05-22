package me.mziccard.audio.filter

/**
 * Base trait for any audio filter.
 * <code>AudioFilter</code> exposes a method to convert an 
 * input array of Double values to an output array of 
 * Double values generated form the input one and 
 * after applying the filter
 **/
trait AudioFilter {

  /**
   * Apply audio filter to <code>inputBuffer</code>, resulting
   * values are placed in <code>outputBuffer</code>
   * @param inputBuffer Data to apply the filter on
   * @param outputBuffer Data after filter application
   **/
  def apply(
    inputBuffer : Array[Double], 
    outputBuffer : Array[Double]);
}