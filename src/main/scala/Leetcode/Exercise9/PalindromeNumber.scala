package Leetcode.Exercise9

object PalindromeNumber extends App {
  def isPalindrome(x: Int): Boolean = {
    val xSrt = x.toString
    var start = 0
    var end = xSrt.length - 1

    while (start < xSrt.length / 2) {
      if (xSrt(start) != xSrt(end)) return false
      else {
        start += 1
        end -= 1
      }
    }
    true
  }

  println(isPalindrome(121))
  println(isPalindrome(-121))
  println(isPalindrome(10))
}