import scala.collection.mutable

object Exam3 extends App with RichTippedTrait with ImplementingTrait {
  //  val s = First("Четыре")
  //  println(colul(s))

  println()
  println("Реализация в текущем формате:")
  // currentMatch: оба значения присутствуют
  println(currentMethod(Some("Hello"), Some(77)))
  // currentMatch: оба значения отсутствуют
  println(currentMethod(None, None))
  // currentMatch: строка присутствует, число отсутствует
  println(currentMethod(Some("Hello"), None))
  // currentMatch: строка отсутствует, число присутствует
  println(currentMethod(None, Some(77)))

  println("========================")
  println("Реализация используя метод zip:")
  // zipMethod: оба значения присутствуют
  println(zipMethod(Some("Hello"), Some(77)))
  // zipMethod: оба значения отсутствуют
  println(zipMethod(None, None))
  // zipMethod: строка присутствует, число отсутствует
  println(zipMethod(Some("Hello"), None))
  // zipMethod: строка отсутствует, число присутствует
  println(zipMethod(None, Some(77)))


  println("========================")
  println("Реализация используя инструкцию for-yield:")
  // forYieldMethod: оба значения присутствуют
  println(forYieldMethod(Some("Hello"), Some(77)))
  // forYieldMethod: оба значения отсутствуют
  println(forYieldMethod(None, None))
  // forYieldMethod: строка присутствует, число отсутствует
  println(forYieldMethod(Some("Hello"), None))
  // forYieldMethod: строка отсутствует, число присутствует
  println(forYieldMethod(None, Some(77)))

  println("========================")
  println("Реализация используя метод map:")
  // mapMethod: оба значения присутствуют
  println(mapMethod(Some("Hello"), Some(77)))
  // mapMethod: оба значения отсутствуют
  println(mapMethod(None, None))
  // mapMethod: строка присутствует, число отсутствует
  println(mapMethod(Some("Hello"), None))
  // mapMethod: строка отсутствует, число присутствует
  println(forYieldMethod(None, Some(77)))

  println("========================")
  println("Реализация используя запечатанный трейт и case-классы:")
  val first = FirstExample(Some("Values passed"), Some(77))
  println(sealedTraitMethod(first))

  val second = SecondExample()
  println(sealedTraitMethod(second))

  val third = ThirdExample(Some("Values passed"))
  println(sealedTraitMethod(third))

  val fourth = FourthExample(Some(77))
  println(sealedTraitMethod(fourth))

}


sealed trait SealedTrait

case class FirstExample(optStr: Option[String], optNum: Option[Int]) extends SealedTrait

case class SecondExample() extends SealedTrait

case class ThirdExample(optStr: Option[String]) extends SealedTrait

case class FourthExample(optNum: Option[Int]) extends SealedTrait

trait ImplementingTrait {
  def sealedTraitMethod(opt: SealedTrait): String = {
    opt match {
      case FirstExample(optStr, optNum) => optStr match {
        case Some(valueStr) => optNum match {
          case Some(valueInt) => s"$valueStr ${valueInt.toString}"
        }
      }
      case SecondExample() => "Values no passed"
      case ThirdExample(optStr) => optStr match {
        case Some(valueStr) => s"$valueStr"
      }
      case FourthExample(optNum) => optNum match {
        case Some(valueInt) => s"${valueInt.toString}"
      }
    }
  }
}


sealed trait TippedTrait

case class First(mess: String) extends TippedTrait

case class Two(mess: String, status: Int) extends TippedTrait

case class Three(mess: String, status: Int) extends TippedTrait

trait RichTippedTrait {
  def colul(str: TippedTrait): String = {
    str match {
      case First(mess) => mess match {
        case "Четыре" => "Четыре"
        case _ => "Это что-то другое"
      }
      case Two(mess, status) => "Это двойка"
    }
  }

  /*
   Реализовать метод в случаях, когда:
     1. Передан String, передан Int. Возвратить интерполяцию двух значений;
     2. Не передан String, не передан Int. Возвратить строку - Не передано ни одно значение;
     3. Передан String, не передан Int. Возвратить строку (интерполяция строки) + строка не передано число;
     4. Не передан String, передан Int. Возвратить строку (интерполяция строки) + строка, не передана строка.

   Реализовать метод в текущем формате.
   Реализовать метод используя zip.
   Реализовать метод используя for yield.
   Реализовать метод используя map.
   Реализовать метод используя запечатанный трейт. Нужно завести кейс классы для каждого значения. Возвращать сообщение из кейс класса, которое установлено при объявлении кейс класса.
  */

  private val redStart = "\u001B[31m"
  private val redEnd = "\u001B[0m"

  def currentMethod(optStr: Option[String], optNum: Option[Int]): String = {
    optStr match {
      case Some(valueStr) => optNum match {
        case Some(valueInt) => s"$valueStr ${valueInt.toString}"
        case None => s"$valueStr. ${redStart}Not found integer.$redEnd"
      }
      case None => optNum match {
        case Some(valueInt) => s"${redStart}Not found string.$redEnd $valueInt."
        case None => s"${redStart}No values passed.$redEnd"
      }
    }
  }

  def zipMethod(optStr: Option[String], optNum: Option[Int]): String = {
    (optStr zip optNum) match {
      case Some((valueStr, valueInt)) => s"$valueStr $valueInt"
      case None =>
        (optStr, optNum) match {
          case (Some(valueStr), None) => s"$valueStr. ${redStart}Not found integer.$redEnd"
          case (None, Some(valueInt)) => s"${redStart}Not found string.$redEnd $valueInt."
          case (None, None) => s"${redStart}No values passed.$redEnd"
        }
    }
  }

  def forYieldMethod(optStr: Option[String], optNum: Option[Int]): String = {
    (for (valueStr <- optStr; valueInt <- optNum) yield s"$valueStr $valueInt").getOrElse {
      (optStr, optNum) match {
        case (Some(valueStr), None) => s"$valueStr. ${redStart}Not found integer.$redEnd"
        case (None, Some(valueInt)) => s"${redStart}Not found string.$redEnd $valueInt."
        case (None, None) => s"${redStart}No values passed.$redEnd"
      }
    }
  }

  def mapMethod(optStr: Option[String], optNum: Option[Int]): String = {
    (optStr.map(s => optNum.map(n => s"$s $n")).getOrElse(None), optStr, optNum) match {
      case (Some(result), _, _) => result
      case (None, Some(valueStr), None) => s"$valueStr. ${redStart}Not found integer.$redEnd"
      case (None, None, Some(valueInt)) => s"${redStart}Not found string.$redEnd $valueInt."
      case (None, None, None) => s"${redStart}No values passed.$redEnd"
    }
  }

  /*
  Дан список строк: List("Валентина", "Петр", "Николай", "Александр").
  Отфильтровать (сделать выборку) List таким образом, в котором каждая вторая буква "а", "о", "е".
  Для каждого из переданного параметра в лист, в листе необходимо подсчитать символы и количество их вхождения.
  Сформировать кейс клас, который будет содержать информацию о результате.
  */

  def filterWord(lst: List[String]): ResultList = {
    var res = List.empty[mutable.Map[Char, Int]]
    for (elem <- lst) {
      if (elem(1) == 'а' || elem(1) == 'о' || elem(1) == 'е') {
        res = freqDef(elem) :: res
      }
    }
    ResultList(res)
  }

  def freqDef(str: String): mutable.Map[Char, Int] = {
    val freq = scala.collection.mutable.Map[Char, Int]()
    for (c <- str.toLowerCase()) {
      freq(c) = freq.getOrElse(c, 0) + 1
    }
    freq
  }

  case class ResultList(freqMap: List[mutable.Map[Char, Int]])

  val names = List("Валентина", "Петр", "Николай", "Александр")
  val result = filterWord(names)
  println(result)
}