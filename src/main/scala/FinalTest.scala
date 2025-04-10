/*
Итоговое задание:
Реализовать приложение, которое будет использовать Akka Actor, Akka HTTP. Приложение должно:
  1. Сделать запрос на сервер и получать весь список данных;
  2. Полученные данные из сервера преобразовать в XML-формат;
  3. Данные, преобразованные в XML, сохранить на жесткий диск;
  4. Удалить все данные с сервера;
  5. Считать раннее сохраненный файл XML и преобразовать его в JSON;
  6. Сохранить данные на сервер методом POST;
  7. Запросить данные с сервера, сравнить с ранее сохраненным XML файлом.
  */

import Color._
import FinalTest.system.log
import RestOperationResult.restCodec
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import io.circe.generic.auto.exportEncoder
import io.circe.generic.semiauto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Json}

import java.nio.file.{Files, Paths}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.xml.{Elem, XML}
import scala.io.StdIn

// Модели данных для работы с REST API
case class Validate(idValid: String, isValid: Option[Boolean])

case class Data(id: String, values: List[String], validate: Option[Validate])

case class Rest(restId: String, data: List[Data])

case class RestOperationResult(status: String, data: Option[List[Rest]], error: Option[String])

object RestOperationResult {
  implicit val validateCodec: Codec[Validate] = deriveCodec
  implicit val dataCodec: Codec[Data] = deriveCodec
  implicit val restCodec: Codec[Rest] = deriveCodec
  implicit val restOperationResultCodec: Codec[RestOperationResult] = deriveCodec
}

// Определение сообщений
sealed trait Message

// Команды - что актор может выполнять
sealed trait Commands extends Message

// События - что актор сообщает о результатах
sealed trait Events extends Message

// Сообщения для актора-клиента
object HttpClientActor {
  case object FetchData extends Commands

  case class DataReceived(json: Json) extends Events

  case class FetchError(reason: String) extends Events


  case class DeleteData(ids: List[String]) extends Commands

  case class DataDeleted(id: String) extends Events

  case class DeleteError(id: String, reason: String) extends Events

  case class PostData(json: Json) extends Commands

  case class DataPosted(response: String) extends Events

  case class PostError(reason: String) extends Events
}

// Сообщения для актора-хранилища
object StorageActor {
  case class SaveXml(xml: Elem, path: String) extends Commands

  case class ConvertXmlToJson(xml: String, json: String) extends Commands

  case class XmlSaved(path: String) extends Events

  case class ConversionSuccessful(path: String) extends Events

  case class SaveError(reason: String) extends Events

  case class ReadXml(path: String) extends Commands

  case class XmlRead(xml: Elem) extends Events

  case class ReadError(reason: String) extends Events
}

// Актор-клиент для выполнения HTTP запросов
class HttpClientActor extends Actor with ActorLogging {

  import HttpClientActor._

  implicit val system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  private val url = "http://localhost:9001"
  private val maxParallelDeletes = 1
  private val retryAttempts = 3
  private val delayBetweenRequests = 200.millis

  private def postDataToServer(json: Json): Future[String] = {
    val urlExtension = "/rest"
    val jsonString = json.noSpaces
    log.debug(s"Sending JSON to $url: $jsonString")

    val entity = HttpEntity(ContentTypes.`application/json`, jsonString)

    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = url + urlExtension,
      entity = entity
    )).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[String].map { response =>
          log.debug(s"Server response: $response")
          response
        }
    }.recoverWith {
      case ex =>
        log.error(s"Request failed: ${ex.getMessage}")
        Future.failed(ex)
    }
  }

  override def receive: Receive = {
    case FetchData =>
      val originalSender = sender()
      log.info(colorStart + "Fetching data from server" + colorEnd)

      fetchDataFromServer().onComplete {
        case scala.util.Success(data) =>
          parse(data) match {
            case Right(json) =>
              self ! DataReceived(json)
              originalSender ! DataReceived(json)
            case Left(error) =>
              self ! FetchError(s"JSON parsing failed: ${error.getMessage}")
              originalSender ! FetchError(s"JSON parsing failed: ${error.getMessage}")
          }
        case scala.util.Failure(ex) =>
          self ! FetchError(ex.getMessage)
          originalSender ! FetchError(ex.getMessage)
      }

    case DeleteData(restIds) =>
      val originalSender = sender()
      log.info(colorStart + s"Starting deletion of ${restIds.size} restIds (max $maxParallelDeletes parallel)" + colorEnd)

      Source(restIds)
        .mapAsync(maxParallelDeletes) { restId =>
          akka.pattern.after(delayBetweenRequests, system.scheduler) {
            deleteDataFromServer(restId, retryAttempts)
              .map(_ => DataDeleted(restId))
              .recover { case ex => DeleteError(restId, ex.getMessage) }
          }
        }
        .runWith(Sink.foreach { result =>
          self ! result
          originalSender ! result
        })

    case DataReceived(json) =>
      log.info(colorStart + "Successfully received and parsed JSON data" + colorEnd)

    case FetchError(reason) =>
      log.error(s"Failed to fetch data: $reason")


    case PostData(json) =>
      val originalSender = sender()
      log.info(colorStart + "Posting data to server" + colorEnd)

      // Логируем входящий JSON
      log.debug(s"Incoming JSON to post: ${json.noSpaces}")

      postDataToServer(json).onComplete {
        case scala.util.Success(response) =>
          log.debug(s"Server response: $response")
          self ! DataPosted(response)
          originalSender ! DataPosted(response)
        case scala.util.Failure(ex) =>
          self ! PostError(ex.getMessage)
          originalSender ! PostError(ex.getMessage)
      }

    case DataPosted(response) =>
      log.info(colorStart + s"Successfully posted data to server. Response: $response" + colorEnd)

    case PostError(reason) =>
      log.error(s"Failed to post data: $reason")

  }

  private def fetchDataFromServer(): Future[String] = {
    val urlExtension = "/rest"
    Http().singleRequest(HttpRequest(uri = url + urlExtension))
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          Unmarshal(entity).to[String]
        case resp =>
          Future.failed(new RuntimeException(s"Unexpected status code: ${resp.status}"))
      }
  }

  private def deleteDataFromServer(restId: String, attempts: Int = 3): Future[Unit] = {
    val urlExtension = s"/rest/$restId"

    def attempt(n: Int): Future[Unit] = {
      if (n <= 0) {
        log.error(s"Failed to delete restId $restId after $attempts attempts")
        Future.failed(new RuntimeException(s"Final delete failed for: $restId"))
      } else {
        Http().singleRequest(HttpRequest(
          method = HttpMethods.DELETE,
          uri = url + urlExtension
        )).flatMap {
          case HttpResponse(StatusCodes.OK, _, _, _) =>
            log.info(s"Successfully deleted restId: $restId (attempt ${attempts - n + 1}/$attempts)")
            Future.successful(())
          case resp =>
            log.warning(s"Delete attempt failed for $restId (${resp.status}), retrying...")
            attempt(n - 1)
        }
      }
    }

    attempt(attempts)
  }
}

// Методы для работы с XML / JSON
object XmlUtils {
  def jsonToXml(json: Json): Elem = {
    json.fold(
      jsonNull = <null/>,
      jsonBoolean = b => <boolean>
        {b.toString}
      </boolean>,
      jsonNumber = n => <number>
        {n.toString}
      </number>,
      jsonString = s => <string>
        {s}
      </string>,
      jsonArray = arr => {
        <array>
          {arr.map(item => jsonToXml(item))}
        </array>
      },
      jsonObject = obj => {
        <object>
          {obj.toList.map { case (key, value) =>
          <field name={key}>
            {jsonToXml(value)}
          </field>
        }}
        </object>
      }
    )
  }

  import io.circe.{Json, JsonObject}

  import scala.xml.{Elem, Node}

  def xmlToJson(xml: Elem): Json = {
    def parseNode(node: Node): Json = {
      node match {
        case root if root.label == "object" && (root \ "field").exists(f => (f \ "@name").text == "data") =>
          // Сначала собираем весь data-объект
          val dataObj = (root \ "field").collectFirst {
            case field if (field \ "@name").text == "data" =>
              Json.obj(
                "data" -> Json.fromValues(
                  (field \ "array" \ "object").map(parseDataItem)
                )
              )
          }.getOrElse(Json.obj())

          // Затем создаем новый объект с правильным порядком полей
          Json.fromJsonObject(
            JsonObject.fromIterable(
              dataObj.asObject.get.toList :+ ("status" -> Json.fromString("success"))
            ))

        case _ => Json.Null
      }
    }

    def parseDataItem(obj: Node): Json = {
      val fields = (obj \ "field").flatMap {
        case f if (f \ "@name").text == "data" =>
          val items = (f \ "array" \ "object").map(parseDataItem)
          Some("data" -> Json.fromValues(items))

        case f if (f \ "@name").text == "restId" =>
          Some("restId" -> Json.fromString(f.child.text.trim))

        case f if (f \ "@name").text == "id" =>
          Some("id" -> Json.fromString(f.child.text.trim))

        case f if (f \ "@name").text == "validate" =>
          Some("validate" -> parseValidate(f))

        case f if (f \ "@name").text == "values" =>
          Some("values" -> parseValues(f))

        case _ => None
      }
      Json.fromJsonObject(JsonObject.fromIterable(fields))
    }

    def parseValidate(field: Node): Json = {
      val fields = (field \ "object" \ "field").collect {
        case f if (f \ "@name").text == "idValid" =>
          "idValid" -> Json.fromString(f.child.text.trim)
        case f if (f \ "@name").text == "isValid" =>
          "isValid" -> Json.fromBoolean(f.child.text.trim.toBoolean)
      }
      Json.fromJsonObject(JsonObject.fromIterable(fields))
    }

    def parseValues(field: Node): Json = {
      Json.fromValues(
        (field \ "array" \ "string").map(s => Json.fromString(s.text.trim))
      )
    }

    parseNode(xml)
  }

  private def handleObject(obj: xml.Elem): Json = {
    val fields = obj.child.collect {
      case field: xml.Elem if field.label == "field" =>
        val name = (field \ "@name").text
        name -> handleFieldContent(field)
    }
    Json.fromJsonObject(JsonObject.fromIterable(fields))
  }

  private def handleFieldContent(field: xml.Elem): Json = {
    field.child.headOption match {
      case Some(content: xml.Elem) => content.label match {
        case "object" => handleObject(content)
        case "array" => handleArray(content)
        case "string" => Json.fromString(content.text.trim)
        case "boolean" => Json.fromBoolean(content.text.trim.toBoolean)
        case _ => Json.Null
      }
      case _ => Json.Null
    }
  }

  private def handleArray(arr: xml.Elem): Json = {
    val items = arr.child.collect {
      case item: xml.Elem => item.label match {
        case "object" => handleObject(item)
        case "string" => Json.fromString(item.text.trim)
        case "boolean" => Json.fromBoolean(item.text.trim.toBoolean)
        case _ => Json.Null
      }
    }
    Json.fromValues(items)
  }

  def saveXmlToFile(xml: Elem, path: String): Unit = {
    val writer = new java.io.PrintWriter(new java.io.File(path))
    try {
      writer.write(new scala.xml.PrettyPrinter(80, 2).format(xml))
    } finally {
      writer.close()
    }
  }

  import io.circe.Printer

  // Новый метод с исправленным форматированием
  def formatJsonStrictly(jsonString: String): String = {
    parse(jsonString) match {
      case Right(json) =>
        val printer = Printer(
          indent = "    ",
          lbraceRight = "\n",
          rbraceLeft = "\n",
          lbracketRight = "\n",
          rbracketLeft = "\n",
          lrbracketsEmpty = "",
          arrayCommaRight = "\n",
          objectCommaRight = "\n",
          colonLeft = "",
          colonRight = " ",
          dropNullValues = false
        )
        printer.print(json)
          .replaceAll("\"values\": \\[\\s*\\]", "\"values\": []")

      case Left(error) =>
        log.error(s"Ошибка парсинга JSON: ${error.getMessage}")
        jsonString
    }
  }
}

// Актор-хранилище для работы с файлами
class StorageActor extends Actor with ActorLogging {

  import StorageActor._
  import XmlUtils._

  override def receive: Receive = {
    case ConvertXmlToJson(xmlPath, jsonPath) =>
      try {
        val xml = XML.loadFile(xmlPath)
        val json = xmlToJson(xml)
        val jsonString = json.spaces2
        val formattedJson = XmlUtils.formatJsonStrictly(jsonString) // <- Здесь замена!
        Files.write(Paths.get(jsonPath), formattedJson.getBytes("UTF-8"))
        sender() ! ConversionSuccessful(jsonPath)
      } catch {
        case ex: Exception =>
          log.error(s"Ошибка конвертации: ${ex.getMessage}")
          sender() ! SaveError(ex.getMessage)
      }

    case SaveXml(xml, path) =>
      val originalSender = sender()
      try {
        saveXmlToFile(xml, path)
        self ! XmlSaved(path)
        originalSender ! XmlSaved(path)
      } catch {
        case ex: Exception =>
          self ! SaveError(ex.getMessage)
          originalSender ! SaveError(ex.getMessage)
      }

    case XmlSaved(path) =>
      log.info(colorStart + s"XML successfully saved to $path" + colorEnd)

    case ConversionSuccessful(json) =>
      log.info(colorStart + s"XML successfully converted to $json" + colorEnd)

    case SaveError(reason) =>
      log.error(s"Failed to save XML: $reason")

    case ReadXml(path) =>
      val originalSender = sender()
      try {
        val xml = scala.xml.XML.loadFile(path)
        self ! XmlRead(xml)
        originalSender ! XmlRead(xml)
      } catch {
        case ex: Exception =>
          self ! ReadError(ex.getMessage)
          originalSender ! ReadError(ex.getMessage)
      }

    case ReadError(reason) =>
      log.error(s"Failed to read XML: $reason")
  }
}


object DataComparator {

  import io.circe.parser.parse

  import scala.xml.XML

  // Модель для сравнения (упрощённая версия RestOperationResult)
  case class ComparableData(restId: String, id: String, values: List[String], isValid: Option[Boolean])

  /**
   * Сравнивает данные из XML-файла с данными, полученными от сервера (JSON).
   *
   * @param xmlPath    путь к XML-файлу
   * @param serverJson JSON-строка с сервера
   * @return Boolean (true, если данные идентичны)
   */
  def compareXmlWithServerJson(xmlPath: String, serverJson: String): Boolean = {
    val xmlData = parseXml(xmlPath)
    val jsonData = parseJson(serverJson)

    xmlData == jsonData
  }

  private def parseXml(xmlPath: String): List[ComparableData] = {
    val xml = XML.loadFile(xmlPath)
    (xml \\ "object").flatMap { obj =>
      for {
        restId <- (obj \ "field" \@ "name").find(_ == "restId").map(_ => (obj \ "field" \ "string").text.trim)
        id <- (obj \ "field" \@ "name").find(_ == "id").map(_ => (obj \ "field" \ "string").text.trim)
        values = (obj \ "field" \ "array" \ "string").map(_.text.trim).toList
        isValid = (obj \ "field" \ "boolean").headOption.map(_.text.trim.toBoolean)
      } yield ComparableData(restId, id, values, isValid)
    }.toList
  }

  private def parseJson(jsonStr: String): List[ComparableData] = {
    parse(jsonStr).flatMap { json =>
      json.hcursor.downField("data").as[List[Rest]].map { rests =>
        rests.flatMap { rest =>
          rest.data.map { data =>
            ComparableData(
              restId = rest.restId,
              id = data.id,
              values = data.values,
              isValid = data.validate.flatMap(_.isValid)
            )
          }
        }
      }
    }.getOrElse(Nil)
  }
}

// Главный актор, который координирует работу
class MainActor extends Actor with ActorLogging {

  import HttpClientActor._
  import StorageActor._
  import XmlUtils._

  private val httpClient = context.actorOf(Props[HttpClientActor], "httpClient")
  private val storageActor = context.actorOf(Props[StorageActor], "storageActor")
  private val outputPathXML = "dataFromServerXML.xml"
  private val outputPathJSON = "convertedJSON.json"

  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher


  var deletedCount = 0
  var totalIds = 0

  var successfulPosts = 0
  var totalPosts = 0 // Будет установлено при отправке данных
  var serverResponses = Vector.empty[Json] // Храним распарсенные JSON

  override def receive: Receive = {
    case "start" =>
      log.info(colorStart + "Starting data processing..." + colorEnd)
      httpClient ! FetchData // <-- ЗДЕСЬ ПРОИСХОДИТ ЗАПРОС НА СЕРВЕР И ПОЛУЧЕНИЕ СПИСОКА ДАННЫХ
      Thread.sleep(1500)
      println(colorBeforeMsgStart + "Запрос на сервер осуществлен и получен весь список данных" + colorEnd)
      StdIn.readLine(colorMsgStart + "Нажмите \"Enter\" для продолжения" + colorEnd)

    case DataReceived(json) =>
      log.info(colorStart + "JSON received, converting to XML..." + colorEnd)
      val xmlData = jsonToXml(json) // <-- ЗДЕСЬ ПРОИСХОДИТ ПРЕОБРАЗОВАНИЕ ПОЛУЧЕННЫХ ДАННЫХ ИЗ СЕРВЕРА В XML-ФОРМАТ
      Thread.sleep(1000)
      println(colorBeforeMsgStart + "Полученные данные из сервера преобразованы в XML-формат" + colorEnd)
      StdIn.readLine(colorMsgStart + "Нажмите \"Enter\" для продолжения" + colorEnd)
      storageActor ! SaveXml(xmlData, outputPathXML) // <-- ЗДЕСЬ ПРОИСХОДИТ СОХРАНЕНИЕ XML-ФАЙЛА НА ЖЕСТКИЙ ДИСК
      Thread.sleep(1000)
      println(colorBeforeMsgStart + "Данные сервера в XML-формате сохранены на жестком диске" + colorEnd)
      StdIn.readLine(colorMsgStart + "Нажмите \"Enter\" для продолжения" + colorEnd)

    case XmlSaved(_) =>
      log.info(colorStart + "XML saved, extracting IDs for deletion..." + colorEnd)
      // Здесь нужно прочитать файл и извлечь IDs
      storageActor ! ReadXml(outputPathXML)


    // Модифицируем case DataDeleted
    case DataDeleted(id) =>
      deletedCount += 1
      log.info(colorStart + s"Successfully deleted data with ID: $id ($deletedCount/$totalIds)" + colorEnd)
      if (deletedCount == totalIds) {
        log.info(colorStart + "All data deleted from server, converting to JSON..." + colorEnd)
        storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)
      }

    case XmlRead(xml) =>
      log.info(colorStart + "Extracting restIds from XML..." + colorEnd)

      // Извлекаем restIds (новый метод)
      val restIds = extractRestIdsFromXml(xml)

      log.info(colorStart + s"Found ${restIds.size} restIds to delete: ${restIds.take(5).mkString(", ")}${if (restIds.size > 5) ", ..." else ""}" + colorEnd)

      if (restIds.nonEmpty) {
        context.become(waitingForDeletion(restIds, restIds.size))
        httpClient ! DeleteData(restIds) // <-- ЗДЕСЬ ПРОИСХОДИТ УДАЛЕНИЕ ВСЕХ ДАННЫХ С СЕРВЕРА
        Thread.sleep(4000)
        println(colorBeforeMsgStart + "Все данные с сервера удалены" + colorEnd)
        StdIn.readLine(colorMsgStart + "Нажмите \"Enter\" для продолжения" + colorEnd)
      } else {
        log.info(colorStart + "No restIds found, skipping deletion" + colorEnd)
        storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)
      }

      def waitingForDeletion(remainingIds: List[String], totalIds: Int): Receive = {
        case DataDeleted(id) =>
          val newRemaining = remainingIds.filterNot(_ == id)
          val deletedCount = totalIds - newRemaining.size
          log.info(colorStart + s"Deleted ID: $id ($deletedCount/$totalIds)" + colorEnd)

          if (newRemaining.isEmpty) {
            log.info(colorStart + "All data deleted from server, converting to JSON..." + colorEnd)
            storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON) // <-- ЗДЕСЬ ПРОИСХОДИТ СЧИТЫВАНИЕ РАННЕЕ СОХРАНЕННОГО XML-ФАЙЛА И ПРЕОБРАЗОВЫВАНИЕ ЕГО В JSON
            context.unbecome()
            Thread.sleep(1000)
            println(colorBeforeMsgStart + "Сохраненный раннее файл XML считан, преобразован в JSON-файл и сохранен на жестком диске " + colorEnd)
            StdIn.readLine(colorMsgStart + "Нажмите \"Enter\" для продолжения" + colorEnd)
          } else {
            context.become(waitingForDeletion(newRemaining, totalIds))
          }

        case DeleteError(id, reason) =>
          log.error(s"Failed to delete ID $id: $reason. Continuing with remaining IDs...")
          val newRemaining = remainingIds.filterNot(_ == id)
          if (newRemaining.isEmpty) {
            log.info(colorStart + "All possible deletions completed, converting to JSON..." + colorEnd)
            storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)
            context.unbecome()
          } else {
            context.become(waitingForDeletion(newRemaining, totalIds))
          }
      }

    case DeleteError(id, reason) =>
      log.error(s"Failed to delete $id: $reason")

    case ConversionSuccessful(jsonPath) =>
      log.info(colorStart + s"JSON successfully saved to $jsonPath" + colorEnd)

      val jsonString = new String(Files.readAllBytes(Paths.get(jsonPath)), "UTF-8")
      parse(jsonString) match {
        case Right(json) =>
          json.hcursor.downField("data").as[List[Rest]] match {
            case Right(rests) =>
              totalPosts = rests.size // Устанавливаем общее количество записей
              rests.zipWithIndex.foreach { case (rest, index) =>
                context.system.scheduler.scheduleOnce((index * 100).millis) {
                  val restJson = Json.obj(
                    "restId" -> Json.fromString(rest.restId),
                    "data" -> rest.data.asJson
                  )
                  httpClient ! PostData(restJson) // <-- ЗДЕСЬ ПРОИСХОДИТ СОХРАНЕНИЕ ДАННЫХ JSON-ФАЙЛА НА СЕРВЕР (Для каждого элемента Rest из JSON формируется отдельный POST-запрос)
                }
              }
              Thread.sleep(1000)
              println(colorBeforeMsgStart + "Данные JSON-файла сохранены на сервер" + colorEnd)
              StdIn.readLine(colorMsgStart + "Нажмите \"Enter\" для продолжения" + colorEnd)

            case Left(error) =>
              log.error(s"Failed to parse data array: ${error.getMessage}")
              context.system.terminate()
          }

        case Left(error) =>
          log.error(s"Failed to parse JSON from file: ${error.getMessage}")
          context.system.terminate()
      }


    // Обновлённый обработчик DataPosted:
    case DataPosted(response) =>
      parse(response) match {
        case Right(json) =>
          successfulPosts += 1
          serverResponses = serverResponses :+ json
          log.info(colorStart + s"Успешно отправлено $successfulPosts/$totalPosts" + colorEnd)

          if (successfulPosts >= totalPosts) {
            log.info(colorStart + "Все данные отправлены. Начинаем сравнение..." + colorEnd)
            compareData() //////// <-- ЗДЕСЬ ЗАПУСКАЕТСЯ СРАВНЕНИЕ ДАННЫХ С СЕРВЕРА И ФАЙЛА XML
            Thread.sleep(1000)
            println(colorBeforeMsgStart + "Сохраненный раннее файл XML считан, данные с сервера (JSON) получены. Данные преобразованы в объектное представление Scala" + colorEnd)
            println(colorBeforeMsgStart + "Данные сравнены" + colorEnd)
          }

        case Left(error) =>
          log.error(s"Ошибка парсинга ответа сервера: ${error.getMessage}")
      }

    case PostError(reason) =>
      log.error(colorStart + s"Ошибка при отправке: $reason" + colorEnd)
      context.system.terminate()


    case FetchError(reason)
    =>
      log.error(s"Failed to fetch data: $reason")
      context.system.terminate()

    case SaveError(reason)
    =>
      log.error(s"Failed to save XML: $reason")
      context.system.terminate()
  }

  private def compareData(): Unit = {
    val xmlPath = "dataFromServerXML.xml"
    val combinedJson = Json.arr(serverResponses: _*) // Создаём валидный JSON-массив

    val isEqual = DataComparator.compareXmlWithServerJson(xmlPath, combinedJson.noSpaces)
    log.info(s"${Color.colorResultStart}Результат сравнения: ${if (isEqual) "ДАННЫЕ ИДЕНТИЧНЫ!" else "ДАННЫЕ РАЗЛИЧАЮТСЯ!"}${Color.colorEnd}")

    context.system.terminate()
  }

  private def extractRestIdsFromXml(xml: Elem): List[String] = {
    (xml \\ "field")
      .filter(f => (f \ "@name").text == "restId")
      .flatMap(_.child.collect {
        case e if e.label == "string" => e.text.trim
      })
      .toList
      .distinct
  }
}

object FinalTest extends App {
  implicit val system: ActorSystem = ActorSystem("AkkaHttpActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mainActor = system.actorOf(Props[MainActor], "mainActor")
  mainActor ! "start"
}

object Color {
  val colorStart = "\u001B[34m"

  val colorBeforeMsgStart = "\u001B[35m"

  val colorMsgStart = "\u001B[47;30m"

  val colorResultStart = "\u001B[32m"

  val colorEnd = "\u001B[0m"
}