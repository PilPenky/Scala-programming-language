import java.time.LocalDate
import java.time.format.{DateTimeFormatter, DateTimeParseException}

/*
11. Определите интерполятор строк date, чтобы значение типа
    java.time.LocalDate можно было определить как date"$year-$month-$day".
Для этого вам потребуется определить «неявный» класс с методом date, например:
    implicit class DateInterpolator(val sc: StringContext) extends AnyVal {
    def date(args: Any*): LocalDate = . . .

args(i) – это значение i-го выражения.
Преобразуйте каждый элемент в строку и затем в целое число и передайте их в вызов метода LocalDate.of.
Если вы уже немного знакомы с языком Scala, добавьте обработку ошибок.
Возбудите исключение, если количество аргументов больше трех, или если они не являются целыми числами,
или если не отделяются друг от друга дефисами. (Строки, разделяющие выражения, доступны в sc.parts.)
*/

object ExercisesChapter2ex11 {
  def main(args: Array[String]): Unit = {
    val dat = dater"2025-02-07"
    println(dat)
  }

  implicit class DateInterpolator(val sc: StringContext) extends AnyVal {
    def dater(args: Any*): LocalDate = {
      // Получаем массив частей строки
      val parts = sc.parts.toArray

      // Проверяю, что есть ровно одна часть (дата)
      if (parts.length != 1) {
        throw new IllegalArgumentException("Неверный формат даты.")
      }

      val dateString = parts(0) // Получаем строку даты
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // Указываем формат

      // Проверяю, что аргументы не отделяются друг от друга дефисами
      val a = dateString.split("")
      if(a(4) != "-" || a(7) != "-") {
          throw new IllegalArgumentException("Аргументы не отделяются друг от друга дефисами.")
      }

      // Проверяю, что количество аргументов больше трех
      val dateParts = dateString.split("-")
      if (dateParts.length > 3) {
        throw new IllegalArgumentException("Количество аргументов больше трех.")
      }
      // Проверяю, что каждая часть даты является целым числом
      else if (dateParts.exists(part => !part.forall(_.isDigit))) {
        throw new IllegalArgumentException("Каждая часть даты должна быть целым числом.")
      }

      // Проверяю соответствует ли строка ожидаемому формату
      val datePattern = """^\d{4}-\d{2}-\d{2}$""".r
      dateString match {
        case datePattern() => // Формат корректный
        case _ => throw new IllegalArgumentException("Неверный формат даты. Ожидается 'yyyy-MM-dd'.")
      }

      try {
        LocalDate.parse(dateString, formatter)
      } catch {
        case e: DateTimeParseException =>
          throw new IllegalArgumentException(s"Ошибка при парсинге даты: ${e.getMessage}")
      }
    }
  }
}