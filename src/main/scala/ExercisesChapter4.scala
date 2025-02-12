import java.util.{Calendar, LinkedHashMap, Locale, Properties}
import scala.collection.immutable.SortedMap
import scala.collection.mutable

object ExercisesChapter4 {
  def main(args: Array[String]): Unit = {
    /*
    1. Создайте ассоциативный массив с ценами на вещи, которые вы хотели бы приобрести.
    Затем создайте второй ассоциативный массив с теми же ключами и ценами с 10%-ной скидкой.
    */
    println("Задание 1:")
    val fruits = Map("Oranges" -> 139, "Bananas" -> 149, "Apples" -> 109, "Pineapple" -> 159)
    val newFruits = Map("Oranges" -> (((fruits("Oranges") * 1.10 - fruits("Oranges")) - fruits("Oranges")) * -1),
      "Bananas" -> (((fruits("Bananas") * 1.10 - fruits("Bananas")) - fruits("Bananas")) * -1),
      "Apples" -> (((fruits("Apples") * 1.10 - fruits("Apples")) - fruits("Apples")) * -1),
      "Pineapple" -> (((fruits("Pineapple") * 1.10 - fruits("Pineapple")) - fruits("Pineapple")) * -1))
    println(fruits)
    println(newFruits)

    /*
    2. Напишите программу, читающую слова из файла. Используйте изменяемый ассоциативный массив для подсчета вхождений каждого слова.
    Для чтения слов используйте java.util.Scanner:
      val in = new java.util.Scanner(new java.io.File("myfile.txt"))
      while (in.hasNext()) обработка in.next()
    В конце выведите все слова и их счетчики.
    */
    println()
    println("Задание 2:")
    var count = 0;
    //var scores2: Map[String, Int] = Map()
    var scores2: Map[String, Int] = Map()
    val in = new java.util.Scanner(new java.io.File("src/main/scala/myfile.txt"))
    while (in.hasNext()) {
      val word = in.next()
      count += 1
      scores2 += (word -> count)
    }
    println(scores2)

    /*
    3. Выполните предыдущее упражнение, используя неизменяемый ассоциативный массив.
    */
    println()
    println("Задание 3:")
    var count3 = 0;
    var scores3: Map[String, Int] = Map()
    val in3 = new java.util.Scanner(new java.io.File("src/main/scala/myfile.txt"))
    while (in3.hasNext()) {
      val word3 = in3.next()
      count3 += 1
      val newScores3 = scores3 + (word3 -> count3)
      println(newScores3)
    }

    /*
    4. Выполните предыдущее упражнение, используя сортированный ассоциативный массив, чтобы слова выводились в отсортированном порядке.
    */
    println()
    println("Задание 4:")
    var count4 = 0;
    var scores4: SortedMap[String, Int] = SortedMap()
    val in4 = new java.util.Scanner(new java.io.File("src/main/scala/myfile.txt"))
    while (in4.hasNext()) {
      val word4 = in4.next()
      count4 += 1
      scores4 += (word4 -> count4)
    }
    println(scores4)

    /*
    5. Выполните предыдущее упражнение, используя java.util.TreeMap, адаптировав его для работы со Scala API.
    */
    println()
    println("Задание 5:")
    var count5 = 0;
    var scores5: mutable.TreeMap[String, Int] = new mutable.TreeMap[String, Int]()
    val in5 = new java.util.Scanner(new java.io.File("src/main/scala/myfile.txt"))
    while (in5.hasNext()) {
      val word5 = in5.next()
      count5 += 1
      scores5.put(word5, count5)
    }
    println(scores5)

    /*
    6. Определите связанную хеш-таблицу, отображающую "Monday" в java.util.Calendar.MONDAY, и так далее для других дней недели.
    Продемонстрируйте обход элементов в порядке их добавления.
    */
    println()
    println("Задание 6:")
    val days = new LinkedHashMap[String, Int]()
    days.put("Sunday", Calendar.SUNDAY)
    days.put("Monday", Calendar.MONDAY)
    days.put("Tuesday", Calendar.TUESDAY)
    days.put("Wednesday", Calendar.WEDNESDAY)
    days.put("Thursday", Calendar.THURSDAY)
    days.put("Friday", Calendar.FRIDAY)
    days.put("Saturday", Calendar.SATURDAY)

    days.forEach((dayName, dayValue) => {
      println(s"$dayName: $dayValue")
    })

    /*
    7. Выведите таблицу всех Java-свойств, таких как:
    java.runtime.name       | Java(TM) SE Runtime Environment
    sun.boot.library.path   | /home/apps/jdk1.6.0_21/jre/lib/i386
    java.vm.version         | 17.0-b16
    java.vm.vendor          | Sun Microsystems Inc.
    java.vendor.url         | http://java.sun.com/
    path.separator          | :
    java.vm.name            | Java HotSpot(TM) Server VM
    */
    println()
    println("Задание 7:")
    val propertyHashMap = new LinkedHashMap[Properties, String]()
    val runtimeName = System.getProperties.getProperty("java.runtime.name")
    val sunBootLibraryPath = System.getProperties.getProperty("sun.boot.library.path")
    val javaVmVersion = System.getProperties.getProperty("java.vm.version ")
    val javaVmVendor = System.getProperties.getProperty("java.vm.vendor")
    val javaVendorUrl = System.getProperties.getProperty("java.vendor.url")
    val pathSeparator = System.getProperties.getProperty("path.separator")
    val javaVmName = System.getProperties.getProperty("java.vm.name")

    println(propertyHashMap.put(System.getProperties, runtimeName))
    println(propertyHashMap.put(System.getProperties, sunBootLibraryPath))
    println(propertyHashMap.put(System.getProperties, javaVmVersion))
    println(propertyHashMap.put(System.getProperties, javaVmVendor))
    println(propertyHashMap.put(System.getProperties, javaVendorUrl))
    println(propertyHashMap.put(System.getProperties, pathSeparator))
    println(propertyHashMap.put(System.getProperties, javaVmName))


    /*
    8. Напишите функцию minmax(values: Array[Int]), возвращающую пару, содержащую наименьшее и наибольшее значения.
    */
    println()
    println("Задание 8:")
    def minmax(values: Array[Int]) = {
      val min = values.min
      val max = values.max
      val tuple = (min, max)
      println(tuple)
    }
    minmax(Array(1, 2, 3, 4, 5))

    /*
    9. Напишите функцию lteqgt(values: Array[Int], v: Int), возвращающую тройку, содержащую счетчик значений, меньших v, равных v и больших v.
    */
    println()
    println("Задание 9:")
    def lteqgt(values: Array[Int], v: Int) = {
      var less = 0
      var more = 0
      var equal = 0
      for (value <- values) {
        if (value < v) {
          less += 1
        } else if (value == v) {
          equal += 1
        } else {
          more += 1
        }
      }
      (less, more, equal)
    }
    val res = lteqgt(Array(1, 2, 3, 4, 5, 3, 2, 1), 3)
    println(s"Меньше: ${res._1} шт., Больше: ${res._2} шт., Равно: ${res._3} шт.")

    /*
    10. Что произойдет, если попытаться упаковать две строки, такие как "Hello".zip("World")? Придумайте достаточно реалистичный случай использования.
    */
    println()
    println("Задание 10:")
    println("Hello".zip("World"))
    // Можно преобразовать в ассоциативный массив:
    println("Hello".zip("World").toMap)
  }
}