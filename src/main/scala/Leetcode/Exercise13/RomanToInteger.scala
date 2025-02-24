package Leetcode.Exercise13

object RomanToInteger extends App {
  def romanToInt(s: String): Int = {
    var ans = 0
    var flag = true
    for (elem <- s.indices.reverse by -1) {
      if (elem != 0) {
        if (flag == false) flag = true
        else {
          if ((s(elem) == 'V' || s(elem) == 'X') && s(elem - 1) == 'I') {
            ans += romanNums(s(elem)) - 1
            flag = false
          }
          else if ((s(elem) == 'L' || s(elem) == 'C') && s(elem - 1) == 'X') {
            ans += romanNums(s(elem)) - 10
            flag = false
          }
          else if ((s(elem) == 'D' || s(elem) == 'M') && s(elem - 1) == 'C') {
            ans += romanNums(s(elem)) - 100
            flag = false
          }
          else ans += romanNums(s(elem))
        }
      }
      else if (flag != false) ans += romanNums(s(elem))
    }
    ans
  }

  def romanNums(s: Char): Int = {
    if (s == 'I') return 1
    else if (s == 'V') return 5
    else if (s == 'X') return 10
    else if (s == 'L') return 50
    else if (s == 'C') return 100
    else if (s == 'D') return 500
    else return 1000
  }

  println(romanToInt("IV"))
    println(romanToInt("III"))
    println(romanToInt("LVIII"))
    println(romanToInt("CIV"))
    println(romanToInt("MCMXCIV"))
}