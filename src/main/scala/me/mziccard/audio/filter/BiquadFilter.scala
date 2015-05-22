package me.mziccard.audio.filter

class BiquadFilter private (
  private val samplingFreq : Long,
  private val channels : Int,
  private val centerFreq : Int,
  private val q : Double,
  private val filterType : FilterType) extends AudioFilter{

  private val piOverSamplingFreq : Double = 
    Math.PI * 2 / samplingFreq;
  private val TwoPiOverSamplingFreq : Double = 
    Math.PI / samplingFreq;

  private var b0 : Double = _;

  private var b1 : Double = _;

  private var b2 : Double = _;

  private var a0 : Double = _;

  private var a1 : Double = _;

  private var a2 : Double = _;

  private val bi1m = new Array[Double](this.channels);

  private val bi2m = new Array[Double](this.channels);

  private val bo1m = new Array[Double](this.channels);

  private val bo2m = new Array[Double](this.channels);

  filterType match {
    case FilterType.LowPass => {
      val w = TwoPiOverSamplingFreq * centerFreq;
      val cosw = Math.cos(w).toDouble;
      val a = Math.sin(w).toDouble / (2*q);
      b0 = (1 - cosw)/2;
      b1 = (1 - cosw);
      b2 = b0;
      a0 = 1 + a;
      a1 = -2 * cosw;
      a2 = 1 - a;
    }
    case FilterType.HighPass => {
      // TODO
    }
  }

  println(toString)

  override def toString = {
    "a0: %f\na1: %f\na2: %f\nb0: %f\nb1: %f\nb2: %f"
      .format(a0, a1, a2, b0, b1, b2)
  }

  override def apply(
    inputBuffer : Array[Double], 
    outputBuffer : Array[Double]) {

    val bufferSize = inputBuffer.size;

    for (i <- 0 until channels) {
      // First two samples
      outputBuffer(0 + i) = (
        b0 * inputBuffer(0 + i) + 
        b1 * bi1m(i) + 
        b2 * bi2m(i) - 
        a1 * bo1m(i) - 
        a2 * bo2m(i)) / a0;
      outputBuffer(channels + i) = (
        b0 * inputBuffer(channels + i) + 
        b1 * inputBuffer(0 + i) + 
        b2 * bi1m(i) - 
        a1 * outputBuffer(0 + i) - 
        a2 * bo1m(i)) / a0;

      var j : Int = 2*channels;
      while (j < bufferSize) {
        outputBuffer(j + i) = (
          b0 * inputBuffer(j + i) + 
          b1 * inputBuffer(j - channels + i) + 
          b2 * inputBuffer(j - 2*channels + i) - 
          a1 * outputBuffer(j - channels + i) -
          a2 * outputBuffer(j - 2*channels + i)) / a0;
        j = j + channels;
      }
      
      bi2m(i) = inputBuffer(bufferSize - 2*channels + i);
      bi1m(i) = inputBuffer(bufferSize - channels + i);
      bo2m(i) = outputBuffer(bufferSize - 2*channels + i);
      bo1m(i) = outputBuffer(bufferSize - channels + i);

    }
  }
}

object BiquadFilter {

  val DefaultCenterFreq: Int = 350;
  val DefaultQ : Double = 1;
  val ButterworthQ : Double = 0.7071;

  def apply(
    samplingFreq : Long,
    channels : Int,
    filterType : FilterType) : BiquadFilter = 
    new BiquadFilter(
      samplingFreq, 
      channels, 
      DefaultCenterFreq, 
      DefaultQ, 
      filterType)

  def apply(
    samplingFreq : Long,
    channels : Int,
    centerFreq : Int,
    q : Double,
    filterType : FilterType) : BiquadFilter = {
    return new BiquadFilter(
      samplingFreq,
      channels,
      centerFreq,
      q,
      filterType)
  }
}

sealed trait FilterType
object FilterType {
  case object LowPass extends FilterType
  case object HighPass extends FilterType
}