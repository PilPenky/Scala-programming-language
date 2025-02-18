import ExercisesChapter6.CardSuits.{CardSuits, Clubs, Diamonds, Hearts}

object ExercisesChapter6 extends App {
  /*
  1. Напишите объект Conversions с методами inchesToCentimeters, gallonsToLiters и milesToKilometers.
  */
  println("Задание 1:")

  object Conversions {
    def inchesToCentimeters(a: Double): Double = a * 2.54

    def gallonsToLiters(a: Double): Double = a * 3.785

    def milesToKilometers(a: Double): Double = a * 1.60934
  }

  val inches = 4.6
  println("%.2f inches = %.2f centimeters.".format(inches, Conversions.inchesToCentimeters(inches)))
  val gallons = 6.5
  println("%.2f gallons = %.2f liters.".format(gallons, Conversions.gallonsToLiters(gallons)))
  val miles = 425.0
  println("%.2f miles = %.2f kilometers.".format(miles, Conversions.milesToKilometers(miles)))

  /*
  2. Предыдущую задачу трудно назвать объектно-ориентированной. Реализуйте общий суперкласс UnitConversion
  и определите объекты InchesToCentimeters, GallonsToLiters и MilesToKilometers, наследующие его.
  */
  println()
  println("Задание 2:")

  abstract private class UnitConversion() {
    def conversion(a: Double): Double
  }

  private object inchesToCentimeters extends UnitConversion() {
    override def conversion(a: Double): Double = a * 2.54
  }

  private object gallonsToLiters extends UnitConversion() {
    override def conversion(a: Double): Double = a * 3.785
  }

  private object milesToKilometers extends UnitConversion() {
    override def conversion(a: Double): Double = a * 1.60934
  }

  private val inches2 = 4.6
  private val gallons2 = 4.6
  private val miles2 = 4.6
  println("%.2f".format(inchesToCentimeters.conversion(inches2)))
  println("%.2f".format(gallonsToLiters.conversion(gallons2)))
  println("%.2f".format(milesToKilometers.conversion(miles2)))

  /*
  3. Определите объект Origin, наследующий класс java.awt.Point. Почему это не самая лучшая идея? (Рассмотрите поближе методы класса Point.)
  */
  println()
  println("Задание 3:")

  object Origin extends java.awt.Point {
    /* Класс Point имеет множество методов, которые могут не иметь смысла для объекта Origin.
    Например, методы, которые изменяют координаты точки, могут быть неуместны для объекта, который должен всегда оставаться в начале координат.*/
  }

  //Вместо наследования от Point, можно создать класс Origin, который будет представлять начало координат, и использовать его по мере необходимости, не полагаясь на методы и свойства Point.
  class Origin {
    val x: Int = 0
    val y: Int = 0

    def toPoint: java.awt.Point = new java.awt.Point(x, y)
  }

  /*
  4. Определите класс Point с объектом-компаньоном, чтобы можно было конструировать экземпляры Point, как Point(3, 4), без ключевого слова new.
  */
  println()
  println("Задание 4:")

  class Point(val x: Int, val y: Int)

  object Point {
    def apply(x: Int, y: Int): Point = new Point(x, y)
  }

  val p1 = Point(3, 4)
  println(s"Point coordinates: (${p1.x}, ${p1.y})")

  /*
  5. Напишите приложение на языке Scala, используя трейт App, которое выводит аргументы командной строки в обратном порядке, разделяя их пробелами.
  Например, команда scala Reverse Hello World должна вывести World Hello.
  */
  println()
  println("Задание 5:")

  object Reverse extends App {
    val argsReversed = args.reverse
    val result = argsReversed.mkString(" ")
    println(result)
  }

  /*
  6. Напишите перечисление, описывающее четыре масти игральных карт так, чтобы метод toString возвращал «♣», «♦», «♥» или «♠».
  */
  println()
  println("Задание 6:")

  object CardSuits extends Enumeration {
    type CardSuits = Value
    val Clubs = Value("♣")
    val Diamonds = Value("♦")
    val Hearts = Value("♥")
    val Spades = Value("♠")
  }

  print(CardSuits.Clubs.toString)
  print(CardSuits.Diamonds.toString)
  print(CardSuits.Hearts.toString)
  println(CardSuits.Spades.toString)

  /*
  7. Напишите функцию для проверки масти карты, реализованной в предыдущем упражнении, которая проверяла бы принадлежность карты к красной масти.
  */
  println()
  println("Задание 7:")

  private def cardSuitChecks(cart: CardSuits): Unit = {
    if (cart == Hearts || cart == Diamonds) println("This card is red.") else println("This card is not red.")
  }

  cardSuitChecks(Clubs)
  cardSuitChecks(Hearts)

  /*
  8. Напишите перечисление, описывающее восемь углов цветового куба RGB.
  В качестве числовых идентификаторов должны использоваться значения цвета (например, 0xff0000 – для Red).
  */
  println()
  println("Задание 8:")

  object CubeRGB extends Enumeration {
    type CubeRGB = Value
    val Red = Value(0xff0000, "Red")
    val Purple = Value(0x800080, "Purple")
    val Navy = Value(0x000080, "Navy")
    val Black = Value(0x000000, "Black")
    val Yellow = Value(0xFFFF00, "Yellow")
    val White = Value(0xFFFFFF, "White")
    val Blue = Value(0x0000FF, "Blue")
    val Green = Value(0x008000, "Green")
  }

  for (c <- CubeRGB.values) println(s"${c.id}: $c")
}