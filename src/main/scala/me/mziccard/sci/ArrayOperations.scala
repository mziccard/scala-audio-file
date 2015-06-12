package me.mziccard.sci

import scala.reflect.ClassTag

object ArrayOperations {
  implicit class SciArray[A : ClassTag](array : Array[A])(implicit num : Numeric[A]) {
    import num._

    /**
     * Subtract a value to each item in the array
     * @param A scalar value
     * @return An array <code>a</code> such that <code>a[i] = this[i] - value</code>
     **/
    def - (value : A) : Array[A] = {
      array.map(_ - value).toArray
    }

    /**
     * Sum a value to each item in the array
     * @param A scalar value
     * @return An array <code>a</code> such that <code>a[i] = this[i] + value</code>
     **/
    def + (value : A) : Array[A] = {
      array.map(_ + value).toArray
    }

    /**
     * Multiply a value to each item in the array
     * @param A scalar value
     * @return An array <code>a</code> such that <code>a[i] = this[i] * value</code>
     **/
    def * (value : A) : Array[A] = {
      array.map(_ * value).toArray
    }

    /**
     * Compute the mean of an array. Returned value has the type of the array
     * @return The mean of the array <code>array.sum/array.length<code>
     **/
    def mean() = array.sum match {
      case x:Int => (x / array.length).asInstanceOf[A]
      case x:Long => (x / array.length).asInstanceOf[A]
      case x:Float => (x / array.length).asInstanceOf[A]
      case x:Double => (x / array.length).asInstanceOf[A]
    }
    /**
     * Compute the absolute value for each item in the array
     * @return An array <code>a</code> such that <code>a[i] = Math.abs(this[i])</code>
     **/
    def abs() : Array[A] = {
      array.map(
        _ match {
          case x:Int => Math.abs(x).asInstanceOf[A]
          case x:Long => Math.abs(x).asInstanceOf[A]
          case x:Float => Math.abs(x).asInstanceOf[A]
          case x:Double => Math.abs(x).asInstanceOf[A]
        }
      ).toArray
    }

    /**
     * Compute the median of an array. Returned value has the type of the array
     * @return The median of the array. If the array has even length returns the last 
     * element of the first half of the array
     **/
    def median() : A = {
      val (lower, upper) = array.sortWith(_<_).splitAt(array.size / 2)
      lower.last
    }

    /**
     * Compute an array according to a given pace
     * @param The frequency to sample the array with
     * @return An array of <code>length/pace</code> elements corresponding to
     * <code>this(0), this(pace), this(2*pace), this(3*pace)</code> ...
     **/
    def undersample(pace : Int) : Array[A] = {
      array.zipWithIndex.filter(_._2 % pace == 0).map(_._1).toArray
    }


    /**
     * Subtract two arrays item by item
     * @param An array at least as long as the first operand
     * @return An array <code>a</code> such that <code>a[i] = this[i] - value[i]</code>
     **/
    def |-| (values : Array[A]) : Array[A] = {
      array.zipWithIndex.map(
        (p : (A, Int)) => p._1 - values(p._2)).toArray
    }

    /**
     * Sum two arrays item by item
     * @param An array at least as long as the first operand
     * @return An array <code>a</code> such that <code>a[i] = this[i] + value[i]</code>
     **/
    def |+| (values : Array[A]) : Array[A] = {
      array.zipWithIndex.map(
       (p : (A, Int)) => p._1 + values(p._2)).toArray
    }

    /**
     * Multiply two arrays item by item
     * @param An array at least as long as the first operand
     * @return An array a such that <code>a[i] = this[i] * value[i]</code>
     **/
    def |*| (values : Array[A]) : Array[A] = {
      array.zipWithIndex.map(
       (p : (A, Int)) => p._1 * values(p._2)).toArray
    }

    /**
     * Compute the autocorretion of an array with the slow O(n*n) 
     * brute force approach
     * @return The autorcorrelated array
     **/
    def correlate() : Array[A] = {
      val n = array.length
      var correlation = new Array[A](n)
      for (k <- 0 until n) {
        for (i <- 0 until n) {
          if (k + i < n)
            correlation(k) = correlation(k) + array(i) * array(k+i)
        }
      }
      return correlation
    }
  }
}