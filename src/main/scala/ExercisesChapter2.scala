import scala.io.StdIn
import scala.math._
import scala.runtime.RichInt
import scala.util.Random

object ExercisesChapter2 {
  def main(args: Array[String]): Unit = {
    /*
    1. Сигнум числа равен 1, если число положительное, -1 – если отрицательное, и 0 – если оно равно нулю.
    Напишите функцию, вычисляющую это значение.
    */
    println("Задание 1:")
    def signum(num: Int): Int = {
      if (num > 0) return 1
      else if (num < 0) return -1
      else 0
    }
    println(signum(55)) //  1
    println(signum(-55)) // -1
    println(signum(0)) //  0

    /*
    2. Какое значение возвращает пустой блок {}? Каков его тип?
    ||
    Не возвращает никакого значимого. Тип Unit.
    */

    /*
    3. Придумайте ситуацию, когда присваивание x = y = 1 будет допустимым в Scala.
    */
    var x: Any = 2
    var y = 0
    x = y = 1

    /*
    4. Напишите на языке Scala цикл, эквивалентный циклу for на
    языке Java:
    for (int i = 10; i >= 0; i-- ) System.out.println(i);
    */
    println()
    println("Задание 4:")
    for (i <- 0 to 10) {
      print(i + " ")
    }

    /*
    5. Напишите процедуру countdown(n: Int), которая выводит числа от n до 0.
    */
    println()
    println()
    println("Задание 5:")
    def countdown(n: Int) {
      var num = n
      while (num >= 0) {
        print(num + " ")
        num -= 1
      }
    }

    countdown(10)

    /*
    6. Напишите цикл for для вычисления произведения кодовых пунктов Юникода всех букв в строке.
    Например, произведение символов в строке "Hello" равно 9415087488L.
    */
    println()
    println()
    println("Задание 6:")
    var res: Long = 1
    for (ch <- "Hello") {
      res *= ch.toInt
    }
    println(res)

    /*
    7. Решите предыдущее упражнение без применения цикла. (Подсказка: загляните в описание класса StringOps в Scaladoc.)
    */
    println()
    println("Задание 7:")
    var str = "Hello"
    val res2 = str.foldLeft(1L) {
      (prod, char) => prod * char.toInt
    }
    println(res2)

    /*
    8. Напишите функцию product(s : String), вычисляющую произведение, как описано в предыдущих упражнениях.
    */
    println()
    println("Задание 8:")
    def product(s : String) = {
      var pr: BigInt = 1
      for (ch <- s) {
        pr *= ch.toInt
      }
      println(pr)
    }
    product("Hello")

    /*
    9. Сделайте функцию из предыдущего упражнения рекурсивной.
    */
    println()
    println("Задание 9:")
    def unicodeProduct(str: String): Long = {
      if (str.isEmpty) 1L
      else str.head.toInt * unicodeProduct(str.tail)
    }
    val prod = unicodeProduct("Hello")
    println(prod)

    /*
    10. Напишите функцию, вычисляющую x в степени n, где n – целое число.
    Используйте следующее рекурсивное определение:
      •• x в степени n = y в степени 2, если n – четное и положительное число, где y = x в степени n/2;
      •• x в степени n = x · x в степени n–1, если n – нечетное и положительное число;
      •• x в степени 0 = 1;
      •• x в степени n = 1/x в степени –n, если n – отрицательное число.
    Не используйте инструкцию return.
    */
    println()
    println("Задание 10:")
    def rec(x: Double, n: Int): Double = {
      if (n == 0) {
        1.0 // Базовый случай: x в степени 0 равно 1
      } else if (n > 0) {
        if (n % 2 == 0) {
          val halfPower = rec(x, n / 2) // Считаем x в степени n/2
          halfPower * halfPower // Возвращаем (x^(n/2))^2
        } else {
          x * rec(x, n - 1) // Если n нечетное, то x * (x^(n-1))
        }
      } else {
        1 / rec(x, -n) // Если n отрицательное, считаем 1/(x^(-n))
      }
    }
    println(rec(2, 10))
    println(rec(2, -3))
    println(rec(3, 3))
    println(rec(5, 0))
  }
}