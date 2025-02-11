import java.util.TimeZone
import scala.collection.mutable.ArrayBuffer
import java.awt.datatransfer._
import scala.jdk.CollectionConverters.CollectionHasAsScala

object ExercisesChapter3 {
  def main(args: Array[String]): Unit = {
    /*
    1. Напишите фрагмент кода, который записывает в массив "a" целые числа в диапазоне от 0 (включительно) до n (исключая его).
    */
    println("Задание 1:")
    val n = 10
    val a = new Array[Int](n)
    for (i <- a.indices) a(i) = i
    println(a.mkString(", "))

    /*
    2. Напишите цикл, меняющий местами смежные элементы в массиве целых чисел.
    Например, Array(1, 2, 3, 4, 5) должен стать Array(2, 1, 4, 3, 5).
    */
    println()
    println("Задание 2:")
    val b = Array(1, 2, 3, 4, 5)
    for (i <- 0 until(b.length - 1, 2)) {
      val t = b(i)
      b(i) = b(i + 1)
      b(i + 1) = t
    }
    println(b.mkString(", "))

    /*
    3. Повторите предыдущее упражнение, но создайте новый массив с переставленными элементами. Используйте for/yield.
    */
    println()
    println("Задание 3:")
    val c = Array[Int](1, 2, 3, 4, 5)
    val swappedArray = for (i <- c.indices by 2) yield {
      if (i + 1 < c.length) {
        Array(c(i + 1), c(i))
      } else {
        Array(c(i))
      }
    }
    val resultArray = swappedArray.flatten.toArray
    println(resultArray.mkString(", "))

    /*
    4. Дан массив целых чисел, создайте новый массив, в котором сначала будут следовать положительные значения из оригинального массива,
    в оригинальном порядке, а за ними отрицательные и нулевые значения, тоже в оригинальном порядке.
    */
    println()
    println("Задание 4:")
    val d = Array[Int](1, 2, 0, -1, 5, -2, 0, 10)
    val posGreaterZero = for (i <- d.indices if d(i) > 0) yield d(i)
    val posLessOne = for (i <- d.indices if d(i) <= 0) yield d(i)
    val result4 = posGreaterZero ++ posLessOne
    println(result4.mkString(", "))

    /*
    5. Как бы вы вычислили среднее значение элементов массива Array[Double]?
    */
    println()
    println("Задание 5:")
    val numbers = Array(1, 2, 3, 4, 5)
    val average = numbers.sum.toDouble / numbers.length
    println(average)

    /*
    6. Как бы вы переупорядочили элементы массива Array[Int] так, чтобы они следовали в обратном отсортированном порядке?
    Как бы вы сделали то же самое с буфером ArrayBuffer[Int]?
    */
    println()
    println("Задание 6:")
    val e = Array[Int](0, 5, 1, 4, 3)
    val resultE = for (i <- e.indices.reverse) yield e(i)
    println(resultE.mkString(", "))

    val eBuf = ArrayBuffer[Int](0, 5, 1, 4, 3)
    val resultEBuf = eBuf.reverse
    println(resultEBuf.mkString(", "))

    /*
    7. Напишите фрагмент программного кода, выводящий значения всех элементов из массива, кроме повторяющихся.
    */
    println()
    println("Задание 7:")
    val f = Array(1, 2, 2, 3, 4, 4, 5)
    val uniqueF = f.distinct
    println(uniqueF.mkString(", "))

    /*
    8. Представьте, что имеется буфер целых чисел и вам требуется удалить все отрицательные значения, кроме первого.
    Ниже приводится последовательное решение, устанавливающее флаг, когда встречается первое отрицательное число,
    после этого удаляются все отрицательные элементы, находящиеся дальше в буфере.
    var first = true
    var n = a.length
    var i = 0
    while (i < n) {
      if (a(i) >= 0) i += 1
      else {
        if (first) { first = false; i += 1 }
        else { a.remove(i); n -= 1 }
      }
    }
    Это сложное и неэффективное решение. Перепишите его на языке Scala, сначала собрав все позиции отрицательных элементов,
    затем перевернув последовательность и вызвав a.remove(i) для каждого индекса.
    */
    println()
    println("Задание 8:")
    val g = ArrayBuffer[Int](-77, 1, -5, 0, -1, 5, -2, 0, -7)
    val posIndMinus = for (i <- g.indices if g(i) < 0) yield i

    val reversePos = for (i <- posIndMinus.indices.reverse) yield posIndMinus(i)

    for (j <- 0 until reversePos.length - 1) {
      g.remove(reversePos(j))
    }
    println(g.mkString(", "))

    /*
    9. Улучшите решение из предыдущего упражнения. Соберите индексы элементов, подлежащих перемещению и позиции, куда они должны быть помещены.
    Переместите их и укоротите буфер. Не копируйте элементы, находящихся перед первым нежелательным элементом.
    */
    println()
    println("Задание 9:")
    val k = ArrayBuffer[Int](-77, 1, -5, 0, -1, 5, -2, 0, -7)
    val posIndPlus = for (i <- k.indices if k(i) >= 0) yield i
    val posIndMinusK = for (i <- k.indices if k(i) < 0) yield i

    val indexFirstMinus: Int = posIndMinusK(0)
    val valueFirstMinus: Int = k.apply(posIndMinusK(0))

    for (j <- posIndPlus.indices) k(j) = k(posIndPlus(j))
    k.insert(indexFirstMinus, valueFirstMinus)

    k.trimEnd(k.length - posIndMinusK.length)
    println(k.mkString(", "))

    /*
    10. Создайте коллекцию всех часовых поясов, возвращаемых методом java.util.TimeZone.getAvailableIDs для Америки.
    Отбросьте префикс "America/" и отсортируйте результат.
    */
    println()
    println("Задание 10:")
    java.util.TimeZone.getAvailableIDs
    // Получаем все доступные идентификаторы часовых поясов
    val timeZones = TimeZone.getAvailableIDs
    // Фильтруем только те, что начинаются с "America/"
    val americaTimeZones = timeZones.filter(_.startsWith("America/"))
    // Убираем префикс "America/"
    val strippedTimeZones = americaTimeZones.map(_.stripPrefix("America/"))
    // Сортируем результат
    val sortedTimeZones = strippedTimeZones.sorted
    // Выводим результат
    println(sortedTimeZones.mkString(", "))

    /*
    11. Импортируйте java.awt.datatransfer._ и создайте объект типа SystemFlavorMap вызовом val flavors = SystemFlavorMap.getDefaultFlavorMap().
    asInstanceOf[SystemFlavorMap]
    Затем вызовите метод getNativesForFlavor с параметром DataFlavor.imageFlavor и получите возвращаемое значение как буфер Scala.
    (Зачем нужен этот непонятный класс? Довольно сложно найти пример использования java.util.List в стандартной библиотеке Java.)
    */
    println()
    println("Задание 11:")
    val flavors = SystemFlavorMap.getDefaultFlavorMap().asInstanceOf[SystemFlavorMap]
    val imageFlavors = flavors.getNativesForFlavor(DataFlavor.imageFlavor)
    val imageFlavorList = imageFlavors.asScala.toList
    println(imageFlavorList.mkString(", "))
    /*
    Зачем нужен SystemFlavorMap?
    SystemFlavorMap используется для определения, какие форматы данных поддерживаются в системе для операций перетаскивания и буфера обмена.
    Это позволяет приложениям взаимодействовать с данными, которые могут быть скопированы или перетащены между различными приложениями, обеспечивая совместимость форматов.
    Например, если вы копируете изображение из одного приложения и вставляете его в другое, SystemFlavorMap помогает определить, в каком формате это изображение будет передано.
    */
  }
}