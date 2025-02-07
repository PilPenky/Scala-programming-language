package Leetcode.Exercise1

import scala.util.control.Breaks.{break, breakable}

object TwoSum {
  def main(args: Array[String]): Unit = {
    val nums: Array[Int] = Array(3, 3)
    val target = 6
    println(twoSum(nums, target).mkString(", "))
  }

  def twoSum(nums: Array[Int], target: Int): Array[Int] = {
    val ans: Array[Int] = new Array[Int](2)
    breakable {
      for (i <- nums.indices; j <- nums.indices) {
        if (i != j) {
          if (nums(i) + nums(j) == target) {
            ans(0) = i
            ans(1) = j
            break()
          }
        }
      }
    }
    ans
  }
}