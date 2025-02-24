import scala.collection.mutable.ArrayBuffer

object Exam1 extends App {
  // Дано два массива. Необходимо вывести повторяющиеся элементы.
  println("Повторяющиеся элементы:")
  private val arr1: Array[Int] = Array(2, 10, 15, 20)
  private val arr2: Array[Int] = Array(1, 2, 3, 10)
  for (elem <- 0 to arr1.length - 1; elem2 <- 0 to arr2.length - 1 if arr1(elem) == arr2(elem2)) print(arr1(elem) + " ")
  println()
  for (elem <- 0 to arr1.length - 1 if arr2.contains(arr1(elem))) print(arr1(elem) + " ")
  println()
  private val commonElements: Array[Int] = arr1.intersect(arr2)
  println(commonElements.mkString(", "))


  // Дано два массива. Необходимо вывести уникальные элементы.
  println()
  println("Уникальные элементы:")
  private val uniquesElements = scala.collection.mutable.ArrayBuffer[Int]()
  for (elem <- arr1) {
    if (!uniquesElements.contains(elem)) {
      uniquesElements += elem
    }
  }
  for (elem <- arr2) {
    if (!uniquesElements.contains(elem)) {
      uniquesElements += elem
    }
  }
  println(uniquesElements.mkString(" "))


  // Дано два массива. Необходимо вывести уникальные элементы c удалением повторяющихся.
  println()
  println("Уникальные элементы c удалением повторяющихся:")
  private val totalArr = new ArrayBuffer[Int]()
  totalArr ++= arr1
  totalArr ++= arr2

  private var lengthA = 0
  while (lengthA < totalArr.length) {
    var lengthB = lengthA + 1
    while (lengthB < totalArr.length) {
      if (totalArr(lengthA) == totalArr(lengthB)) {
        totalArr.remove(lengthB)
        totalArr.remove(lengthA)
        lengthA -= 1
      }
      lengthB += 1
    }
    lengthA += 1
  }
  println(totalArr.mkString(" "))


}