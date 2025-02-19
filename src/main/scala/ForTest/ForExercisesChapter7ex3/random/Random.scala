package ForTest.ForExercisesChapter7ex3.random

package object random {
  def nextInt(): Int = {
    val a = 1664525
    a
  }

  def nextDouble(): Double = {
    val b = 1013904223
    b
  }

  def setSeed(seed: Int): Int = {
    previous = seed
    previous
  }
  var previous: Int = setSeed(5)

  val n = 32
}

package random {
  object Random {
    val next = (previous * nextInt + nextDouble) * math.pow(2, n)
    println(next)
  }
}