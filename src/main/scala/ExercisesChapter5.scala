import scala.beans.BeanProperty


object ExercisesChapter5 {

  def main(args: Array[String]): Unit = {
    /*
    1. Усовершенствуйте класс Counter в разделе 5.1 «Простые классы и методы без параметров»,
    чтобы значение счетчика не превращалось в отрицательное число по достижении Int.MaxValue.
    */
    println("Задание 1:")
    class Counter {
      private var value = 0

      def increment() {
        if (value < Int.MaxValue) {
          value += 1
        } else {
          println("Counter has reached its maximum value.")
        }
      }

      def current() = println(value)
    }
    val counter1 = new Counter()
    counter1.current()

    /*
    2. Напишите класс BankAccount с методами deposit и withdraw и свойством balance, доступным только для чтения.
    */
    println()
    println("Задание 2:")
    class BankAccount(private var balance: Double) {

      def deposit(amount: Double) = {
        balance += amount
      }

      def withdraw(amount: Double) = {
        balance -= amount
      }

      def printBalance(): Unit = {
        println(balance)
      }
    }
    val bankAccount2 = new BankAccount(0)
    bankAccount2.printBalance()
    bankAccount2.deposit(21.57)
    bankAccount2.printBalance()

    /*
    3. Напишите класс Time со свойствами hours и minutes, доступными только для чтения,
    и методом before(other: Time): Boolean, который проверяет, предшествует ли время this времени other.
    Объект Time должен конструироваться как new Time(hrs, min), где hrs – время в 24-часовом формате.
    */
    println()
    println("Задание 3:")
    class Time(private var _hours: Int, private var _minutes: Int) {
      def hours: Int = _hours

      def minutes: Int = _minutes

      def before(other: Time): Boolean = {
        (hours < other.hours) || (hours == other.hours && minutes < other.minutes)
      }
    }
    val time1 = new Time(10, 30)
    val time2 = new Time(12, 15)
    val isBefore = time1.before(time2)
    println(isBefore)

    /*
    4. Перепишите класс Time из предыдущего упражнения, чтобы внутри время было представлено количеством минут, прошедших с начала суток (между 0 и 24 × 60 – 1).
    Общедоступный интерфейс при этом не должен измениться. То есть эти изменения не должны оказывать влияния на клиентский код.
    */
    println()
    println("Задание 4:")
    class Time2(private var _hours: Int, private var _minutes: Int) {
      def allMinutes: Int = (_hours * 60 - 1) + _minutes

      def before(other: Time2): Boolean = {
        (allMinutes < other.allMinutes) || (allMinutes == other.allMinutes && allMinutes < other.allMinutes)
      }
    }
    val time1_1 = new Time2(13, 30)
    val time2_2 = new Time2(12, 15)
    val isBefore2 = time1_1.before(time2_2)
    println(isBefore2)

    /*
    5. Создайте класс Student со свойствами в формате JavaBeans name (типа String) и id (типа Long), доступными для чтения/записи.
    Какие методы будут сгенерированы? Сможете ли вы вызывать методы доступа в формате JavaBeans из программного кода на языке Scala? Необходимо ли это?
    */
    println()
    println("Задание 5:")
    class Student(@BeanProperty var name: String, @BeanProperty var id: Long) {
      val newName = name
      val newId = id
    }
    val student = new Student("Fred", 27)
    println(student.getName)
    println(student.getId)
    student.setName("Bob")
    println(student.getName)
    student.setId(28)
    println(student.getId)

    /*
    6. В классе Person из раздела 5.1 «Простые классы и методы без параметров» реализуйте главный конструктор, преобразующий отрицательное значение возраста в 0.
    */
    println()
    println("Задание 6:")
    class Person(val name: String, age: Int) {
      val validAge = if (age < 0) 0 else age

      override def toString: String = s"Person(name: $name, age: $validAge)"
    }
    val person1 = new Person("Alice", 25)
    val person2 = new Person("Bob", -5)
    println(person1)
    println(person2)

    /*
    7. Напишите класс Person с главным конструктором, принимающим строку, которая содержит имя, пробел и фамилию, например:
    new Person("Fred Smith"). Сделайте свойства firstName и lastName доступными только для чтения.
    Должен ли параметр главного конструктора объявляться как var, val или как обычный параметр? Почему?
    */
    println()
    println("Задание 7:")
    class Person7(fullName: String) {
      /*
      Объявил параметр главного конструктора как обычный параметр, поскольку принимаемое значение нигде не хранится
      и не изменяется в классе, а просто используется только извлечения имен.
      */
      val firstName = divideFullName(0)
      val lastName = divideFullName(1)
      println(firstName)
      println(lastName)

      private def divideFullName(n: Int): String = {
        val validFullName = fullName.split(" ")
        val name = validFullName(n)
        name
      }
    }
    val person7 = new Person7("Fred Smith")

    /*
    8. Создайте класс Car со свойствами, определяющими производителя, название модели и год производства, которые доступны только для чтения,
    и свойство с регистрационным номером автомобиля, доступным для чтения/записи.
    Добавьте четыре конструктора. Все они должны принимать название производителя и название модели.
    При необходимости в вызове конструктора могут также указываться год и регистрационный номер.
    Если год не указан, он должен устанавливаться равным -1, а при отсутствии регистрационного номера должна устанавливаться пустая строка.
    Какой конструктор вы выберете в качестве главного? Почему?
    */
    println()
    println("Задание 8:")
    class Car(private var carManufacturer: String, private var carModelName: String) {
      private var carYearProduction: Int = -1
      var carRegistrationNumber: String = ""

      def this(carManufacturer: String, carModelName: String, carYearProduction: Int) = {
        this(carManufacturer, carModelName)
        this.carYearProduction = carYearProduction
      }

      def this(carManufacturer: String, carModelName: String, carRegistrationNumber: String) = {
        this(carManufacturer, carModelName)
        this.carRegistrationNumber = carRegistrationNumber
      }

      def this(carManufacturer: String, carModelName: String, carYearProduction: Int, carRegistrationNumber: String) = {
        this(carManufacturer, carModelName)
        this.carYearProduction = carYearProduction
        this.carRegistrationNumber = carRegistrationNumber
      }

      override def toString: String = s"Car(carManufacturer: $carManufacturer, carModelName: $carModelName, carYearProduction: $carYearProduction, carRegistrationNumber: $carRegistrationNumber)"
    }
    var car = new Car("Volvo", "XC60", "Н000ЕТ")
    println(car)
    car.carRegistrationNumber = "Х000ХХ"
    println(car)

    /*
    9. Повторно реализуйте класс из предыдущего упражнения на Java, C# или C++ (по выбору). Насколько короче получился класс на языке Scala?
    */
    println()
    println("Задание 9:")
    /*
    public class Main {
        public static void main(String[] args) {
            Car car = new Car("Volvo", "XC60", 2005, "Н000ЕТ");
            System.out.println("car = " + car);
        }
    }
    public class Car {
        private String carManufacturer = "";
        private String carModelName = "";
        private int carYearProduction = -1;
        public String carRegistrationNumber = "";

        public Car(String carManufacturer, String carModelName) {
            this.carManufacturer = carManufacturer;
            this.carModelName = carModelName;
        }

        public Car(String carManufacturer, String carModelName, int carYearProduction) {
            this.carManufacturer = carManufacturer;
            this.carModelName = carModelName;
            this.carYearProduction = carYearProduction;
        }

        public Car(String carManufacturer, String carModelName, String carRegistrationNumber) {
            this.carManufacturer = carManufacturer;
            this.carModelName = carModelName;
            this.carRegistrationNumber = carRegistrationNumber;
        }

        public Car(String carManufacturer, String carModelName, int carYearProduction, String carRegistrationNumber) {
            this.carManufacturer = carManufacturer;
            this.carModelName = carModelName;
            this.carYearProduction = carYearProduction;
            this.carRegistrationNumber = carRegistrationNumber;
        }

        public String getCarManufacturer() {
            return carManufacturer;
        }

        public void setCarManufacturer(String carManufacturer) {
            this.carManufacturer = carManufacturer;
        }

        public String getCarModelName() {
            return carModelName;
        }

        public void setCarModelName(String carModelName) {
            this.carModelName = carModelName;
        }

        public int getCarYearProduction() {
            return carYearProduction;
        }

        public void setCarYearProduction(int carYearProduction) {
            this.carYearProduction = carYearProduction;
        }

        public String getCarRegistrationNumber() {
            return carRegistrationNumber;
        }

        public void setCarRegistrationNumber(String carRegistrationNumber) {
            this.carRegistrationNumber = carRegistrationNumber;
        }

        @Override
        public String toString() {
            return "Car{" +
                    "carManufacturer='" + carManufacturer + '\'' +
                    ", carModelName='" + carModelName + '\'' +
                    ", carYearProduction=" + carYearProduction +
                    ", carRegistrationNumber='" + carRegistrationNumber + '\'' +
                    '}';
        }
    }
    */

    /*
    10. Взгляните на следующее определение класса:
        class Employee(val name: String, var salary: Double) {
          def this() { this("John Q. Public", 0.0) }
        }
    Перепишите его так, чтобы он содержал явные определения полей и имел главный конструктор по умолчанию.
    */
    println()
    println("Задание 10:")
    class Employee() {
      var name: String = "John Q. Public"
      var salary: Double = 0.0

      def this(name: String, salary: Double) = {
        this()
        this.name = name
        this.salary = salary
      }
      override def toString: String = s"Employee(name: $name, salary: $salary)"
    }
    val employee = new Employee()
    println(employee)

  }
}