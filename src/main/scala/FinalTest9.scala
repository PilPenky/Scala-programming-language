//// Структура JSON немного отличается. Необходимо убрать лишние пробелы после id
//import Color._
//import FinalTest9.system.log
//import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.unmarshalling.Unmarshal
//import akka.stream.ActorMaterializer
//import akka.stream.scaladsl.{Sink, Source}
//import akka.util.Timeout
//import io.circe.parser.parse
//import io.circe.{Json, JsonObject}
//import org.json4s.Xml.toJson
//import org.json4s.jackson.JsonMethods._
//import org.json4s.{DefaultFormats, Formats}
//
//import java.nio.file.{Files, Paths}
//import scala.concurrent.duration._
//import scala.concurrent.{ExecutionContextExecutor, Future}
//import scala.xml.{Elem, Node, XML}
//
//// Определение сообщений
//sealed trait Message
//
//// Команды - что актор может выполнять
//sealed trait Commands extends Message
//
//// События - что актор сообщает о результатах
//sealed trait Events extends Message
//
//// Сообщения для актора-клиента
//object HttpClientActor {
//  case object FetchData extends Commands
//
//  case class DataReceived(json: Json) extends Events
//
//  case class FetchError(reason: String) extends Events
//
//
//  case class DeleteData(ids: List[String]) extends Commands
//
//  case class DataDeleted(id: String) extends Events
//
//  case class DeleteError(id: String, reason: String) extends Events
//}
//
//// Сообщения для актора-хранилища
//object StorageActor {
//  case class SaveXml(xml: Elem, path: String) extends Commands
//
//  case class ConvertXmlToJson(xml: String, json: String) extends Commands
//
//  case class XmlSaved(path: String) extends Events
//
//  case class ConversionSuccessful(path: String) extends Events
//
//  case class SaveError(reason: String) extends Events
//
//  case class ReadXml(path: String) extends Commands
//
//  case class XmlRead(xml: Elem) extends Events
//
//  case class ReadError(reason: String) extends Events
//}
//
//// Актор-клиент для выполнения HTTP запросов
//class HttpClientActor extends Actor with ActorLogging {
//
//  import HttpClientActor._
//
//  implicit val system: ActorSystem = context.system
//  implicit val materializer: ActorMaterializer = ActorMaterializer()
//  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
//
//  private val url = "http://localhost:9001"
//  private val maxParallelDeletes = 1
//  private val retryAttempts = 3
//  private val delayBetweenRequests = 200.millis
//
//  override def receive: Receive = {
//    case FetchData =>
//      val originalSender = sender()
//      log.info(colorStart + "Fetching data from server" + colorEnd)
//
//      fetchDataFromServer().onComplete {
//        case scala.util.Success(data) =>
//          parse(data) match {
//            case Right(json) =>
//              self ! DataReceived(json)
//              originalSender ! DataReceived(json)
//            case Left(error) =>
//              self ! FetchError(s"JSON parsing failed: ${error.getMessage}")
//              originalSender ! FetchError(s"JSON parsing failed: ${error.getMessage}")
//          }
//        case scala.util.Failure(ex) =>
//          self ! FetchError(ex.getMessage)
//          originalSender ! FetchError(ex.getMessage)
//      }
//
//    case DeleteData(restIds) =>
//      val originalSender = sender()
//      log.info(colorStart + s"Starting deletion of ${restIds.size} restIds (max $maxParallelDeletes parallel)" + colorEnd)
//
//      Source(restIds)
//        .mapAsync(maxParallelDeletes) { restId =>
//          akka.pattern.after(delayBetweenRequests, system.scheduler) {
//            deleteDataFromServer(restId, retryAttempts)
//              .map(_ => DataDeleted(restId))
//              .recover { case ex => DeleteError(restId, ex.getMessage) }
//          }
//        }
//        .runWith(Sink.foreach { result =>
//          self ! result
//          originalSender ! result
//        })
//
//    case DeleteData(ids) =>
//      val originalSender = sender()
//      log.info(colorStart + s"Starting deletion of ${ids.size} items" + colorEnd)
//
//      // Отправляем запросы на удаление для каждого ID
//      ids.foreach { id =>
//        deleteDataFromServer(id).onComplete {
//          case scala.util.Success(_) =>
//            self ! DataDeleted(id)
//            originalSender ! DataDeleted(id)
//          case scala.util.Failure(ex) =>
//            self ! DeleteError(id, ex.getMessage)
//            originalSender ! DeleteError(id, ex.getMessage)
//        }
//      }
//
//    case DataReceived(json) =>
//      log.info(colorStart + "Successfully received and parsed JSON data" + colorEnd)
//
//    case FetchError(reason) =>
//      log.error(s"Failed to fetch data: $reason")
//  }
//
//  private def fetchDataFromServer(): Future[String] = {
//    val urlExtension = "/rest"
//    Http().singleRequest(HttpRequest(uri = url + urlExtension))
//      .flatMap {
//        case HttpResponse(StatusCodes.OK, _, entity, _) =>
//          Unmarshal(entity).to[String]
//        case resp =>
//          Future.failed(new RuntimeException(s"Unexpected status code: ${resp.status}"))
//      }
//  }
//
//  private def deleteDataFromServer(restId: String, attempts: Int = 3): Future[Unit] = {
//    val urlExtension = s"/rest/$restId"
//
//    def attempt(n: Int): Future[Unit] = {
//      if (n <= 0) {
//        log.error(s"Failed to delete restId $restId after $attempts attempts")
//        Future.failed(new RuntimeException(s"Final delete failed for: $restId"))
//      } else {
//        Http().singleRequest(HttpRequest(
//          method = HttpMethods.DELETE,
//          uri = url + urlExtension
//        )).flatMap {
//          case HttpResponse(StatusCodes.OK, _, _, _) =>
//            log.info(s"Successfully deleted restId: $restId (attempt ${attempts - n + 1}/$attempts)")
//            Future.successful(())
//          case resp =>
//            log.warning(s"Delete attempt failed for $restId (${resp.status}), retrying...")
//            attempt(n - 1)
//        }
//      }
//    }
//
//    attempt(attempts)
//  }
//}
//
//// Методы для работы с XML / JSON
//object XmlUtils {
//  def jsonToXml(json: Json): Elem = {
//    json.fold(
//      jsonNull = <null/>,
//      jsonBoolean = b => <boolean>
//        {b.toString}
//      </boolean>,
//      jsonNumber = n => <number>
//        {n.toString}
//      </number>,
//      jsonString = s => <string>
//        {s}
//      </string>,
//      jsonArray = arr => {
//        <array>
//          {arr.map(item => jsonToXml(item))}
//        </array>
//      },
//      jsonObject = obj => {
//        <object>
//          {obj.toList.map { case (key, value) =>
//          <field name={key}>
//            {jsonToXml(value)}
//          </field>
//        }}
//        </object>
//      }
//    )
//  }
//
//  def convertXmlFileToJsonFile(xmlFilePath: String, jsonFilePath: String): Unit = {
//    implicit val formats: Formats = DefaultFormats
//    try {
//      val xmlContent = XML.loadFile(xmlFilePath)
//      val jsonValue = toJson(xmlContent)
//      val jsonString = pretty(render(jsonValue))
//
//      Files.write(Paths.get(jsonFilePath), jsonString.getBytes("UTF-8"))
//      log.info(s"Конвертация успешна: $xmlFilePath -> $jsonFilePath")
//    } catch {
//      case e: Exception =>
//        log.error(s"Ошибка конвертации: ${e.getMessage}")
//        throw e
//    }
//  }
//
//  import io.circe.{Json, JsonObject}
//  import scala.xml.{Elem, Node}
//
//  def xmlToJson(xml: Elem): Json = {
//    def parseNode(node: Node): Json = {
//      node match {
//        case root if root.label == "object" && (root \ "field").exists(f => (f \ "@name").text == "data") =>
//          // Сначала собираем весь data-объект
//          val dataObj = (root \ "field").collectFirst {
//            case field if (field \ "@name").text == "data" =>
//              Json.obj(
//                "data" -> Json.fromValues(
//                  (field \ "array" \ "object").map(parseDataItem)
//                )
//              )
//          }.getOrElse(Json.obj())
//
//          // Затем создаем новый объект с правильным порядком полей
//          Json.fromJsonObject(
//            JsonObject.fromIterable(
//              dataObj.asObject.get.toList :+ ("status" -> Json.fromString("success"))
//            ))
//
//        case _ => Json.Null
//      }
//    }
//
//    def parseDataItem(obj: Node): Json = {
//      val fields = (obj \ "field").flatMap {
//        case f if (f \ "@name").text == "data" =>
//          val items = (f \ "array" \ "object").map(parseDataItem)
//          Some("data" -> Json.fromValues(items))
//
//        case f if (f \ "@name").text == "restId" =>
//          Some("restId" -> Json.fromString(f.child.text.trim))
//
//        case f if (f \ "@name").text == "id" =>
//          Some("id" -> Json.fromString(f.child.text.trim))
//
//        case f if (f \ "@name").text == "validate" =>
//          Some("validate" -> parseValidate(f))
//
//        case f if (f \ "@name").text == "values" =>
//          Some("values" -> parseValues(f))
//
//        case _ => None
//      }
//      Json.fromJsonObject(JsonObject.fromIterable(fields))
//    }
//
//    def parseValidate(field: Node): Json = {
//      val fields = (field \ "object" \ "field").collect {
//        case f if (f \ "@name").text == "idValid" =>
//          "idValid" -> Json.fromString(f.child.text.trim)
//        case f if (f \ "@name").text == "isValid" =>
//          "isValid" -> Json.fromBoolean(f.child.text.trim.toBoolean)
//      }
//      Json.fromJsonObject(JsonObject.fromIterable(fields))
//    }
//
//    def parseValues(field: Node): Json = {
//      Json.fromValues(
//        (field \ "array" \ "string").map(s => Json.fromString(s.text.trim))
//      )
//    }
//
//    parseNode(xml)
//  }
//
//  private def handleObject(obj: xml.Elem): Json = {
//    val fields = obj.child.collect {
//      case field: xml.Elem if field.label == "field" =>
//        val name = (field \ "@name").text
//        name -> handleFieldContent(field)
//    }
//    Json.fromJsonObject(JsonObject.fromIterable(fields))
//  }
//
//  private def handleFieldContent(field: xml.Elem): Json = {
//    field.child.headOption match {
//      case Some(content: xml.Elem) => content.label match {
//        case "object" => handleObject(content)
//        case "array" => handleArray(content)
//        case "string" => Json.fromString(content.text.trim)
//        case "boolean" => Json.fromBoolean(content.text.trim.toBoolean)
//        case _ => Json.Null
//      }
//      case _ => Json.Null
//    }
//  }
//
//  private def handleArray(arr: xml.Elem): Json = {
//    val items = arr.child.collect {
//      case item: xml.Elem => item.label match {
//        case "object" => handleObject(item)
//        case "string" => Json.fromString(item.text.trim)
//        case "boolean" => Json.fromBoolean(item.text.trim.toBoolean)
//        case _ => Json.Null
//      }
//    }
//    Json.fromValues(items)
//  }
//
//  def saveXmlToFile(xml: Elem, path: String): Unit = {
//    val writer = new java.io.PrintWriter(new java.io.File(path))
//    try {
//      writer.write(new scala.xml.PrettyPrinter(80, 2).format(xml))
//    } finally {
//      writer.close()
//    }
//  }
//
//  def testXmlConversion(): Unit = {
//    val testXml =
//      <object>
//        <field name="data">
//          <array>
//            <object>
//              <field name="data">
//                <array>
//                  <object>
//                    <field name="id">47c3ca25-9064-4736-abb4-35f578dfe3bd</field>
//                  </object>
//                </array>
//              </field>
//              <field name="restId">
//                <string>34a496b0-f05e-470c-ad9c-f83b3716ad4d</string>
//              </field>
//            </object>
//          </array>
//        </field>
//      </object>
//
//    val json = xmlToJson(testXml)
//    println("Результат конвертации:\n" + json.spaces2)
//  }
//
//}
//
//// Актор-хранилище для работы с файлами
//class StorageActor extends Actor with ActorLogging {
//
//  import StorageActor._
//  import XmlUtils._
//
//  override def receive: Receive = {
//    case ConvertXmlToJson(xmlPath, jsonPath) =>
//      try {
//        val xml = XML.loadFile(xmlPath)
//        val json = xmlToJson(xml) // Используем новый метод
//        Files.write(Paths.get(jsonPath), json.spaces2.getBytes("UTF-8"))
//        sender() ! ConversionSuccessful(jsonPath)
//      } catch {
//        case ex: Exception =>
//          log.error(s"Conversion failed: ${ex.getMessage}")
//          sender() ! SaveError(ex.getMessage)
//      }
//
//    case SaveXml(xml, path) =>
//      val originalSender = sender()
//      try {
//        saveXmlToFile(xml, path)
//        self ! XmlSaved(path)
//        originalSender ! XmlSaved(path)
//      } catch {
//        case ex: Exception =>
//          self ! SaveError(ex.getMessage)
//          originalSender ! SaveError(ex.getMessage)
//      }
//
//    case XmlSaved(path) =>
//      log.info(colorStart + s"XML successfully saved to $path" + colorEnd)
//
//    case ConversionSuccessful(json) =>
//      log.info(colorStart + s"XML successfully converted to $json" + colorEnd)
//
//    case SaveError(reason) =>
//      log.error(s"Failed to save XML: $reason")
//
//    case ReadXml(path) =>
//      val originalSender = sender()
//      try {
//        val xml = scala.xml.XML.loadFile(path)
//        self ! XmlRead(xml)
//        originalSender ! XmlRead(xml)
//      } catch {
//        case ex: Exception =>
//          self ! ReadError(ex.getMessage)
//          originalSender ! ReadError(ex.getMessage)
//      }
//
//    case XmlRead(xml) =>
//      log.info(colorStart + "Successfully read XML from file" + colorEnd)
//
//    case ReadError(reason) =>
//      log.error(s"Failed to read XML: $reason")
//  }
//}
//
//// Главный актор, который координирует работу
//class MainActor extends Actor with ActorLogging {
//
//  import HttpClientActor._
//  import StorageActor._
//  import XmlUtils._
//
//  private val httpClient = context.actorOf(Props[HttpClientActor], "httpClient")
//  private val storageActor = context.actorOf(Props[StorageActor], "storageActor")
//  private val outputPathXML = "dataFromServerXML.xml"
//  private val outputPathJSON = "convertedJSON.json"
//
//  implicit val timeout: Timeout = Timeout(5.seconds)
//  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
//
//  var deletedCount = 0
//  var totalIds = 0
//
//  override def receive: Receive = {
//    case "start" =>
//      log.info(colorStart + "Starting data processing..." + colorEnd)
//      httpClient ! FetchData
//
//    case DataReceived(json) =>
//      log.info(colorStart + "JSON received, converting to XML..." + colorEnd)
//      val xmlData = jsonToXml(json)
//      storageActor ! SaveXml(xmlData, outputPathXML)
//
//    case XmlSaved(_) =>
//      log.info(colorStart + "XML saved, extracting IDs for deletion..." + colorEnd)
//      // Здесь нужно прочитать файл и извлечь IDs
//      storageActor ! ReadXml(outputPathXML)
//
//
//    // Модифицируем case DataDeleted
//    case DataDeleted(id) =>
//      deletedCount += 1
//      log.info(colorStart + s"Successfully deleted data with ID: $id ($deletedCount/$totalIds)" + colorEnd)
//      if (deletedCount == totalIds) {
//        log.info(colorStart + "All data deleted from server, converting to JSON..." + colorEnd)
//        storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)
//      }
//
//    case XmlRead(xml) =>
//      log.info(colorStart + "Extracting restIds from XML..." + colorEnd)
//
//      // Извлекаем restIds (новый метод)
//      val restIds = extractRestIdsFromXml(xml)
//
//      log.info(colorStart + s"Found ${restIds.size} restIds to delete: ${restIds.take(5).mkString(", ")}${if (restIds.size > 5) ", ..." else ""}" + colorEnd)
//
//      if (restIds.nonEmpty) {
//        context.become(waitingForDeletion(restIds, restIds.size))
//        httpClient ! DeleteData(restIds)
//      } else {
//        log.info(colorStart + "No restIds found, skipping deletion" + colorEnd)
//        storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)
//      }
//
//      def waitingForDeletion(remainingIds: List[String], totalIds: Int): Receive = {
//        case DataDeleted(id) =>
//          val newRemaining = remainingIds.filterNot(_ == id)
//          val deletedCount = totalIds - newRemaining.size
//          log.info(colorStart + s"Deleted ID: $id ($deletedCount/$totalIds)" + colorEnd)
//
//          if (newRemaining.isEmpty) {
//            log.info(colorStart + "All data deleted from server, converting to JSON..." + colorEnd)
//            storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)
//            context.unbecome()
//          } else {
//            context.become(waitingForDeletion(newRemaining, totalIds))
//          }
//
//        case DeleteError(id, reason) =>
//          log.error(s"Failed to delete ID $id: $reason. Continuing with remaining IDs...")
//          val newRemaining = remainingIds.filterNot(_ == id)
//          if (newRemaining.isEmpty) {
//            log.info(colorStart + "All possible deletions completed, converting to JSON..." + colorEnd)
//            storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)
//            context.unbecome()
//          } else {
//            context.become(waitingForDeletion(newRemaining, totalIds))
//          }
//      }
//
//
//    case DeleteError(id, reason) =>
//      log.error(s"Failed to delete $id: $reason")
//
//    // Сохранить преобразованный JSON на сервер методом POST
//
//    // Запросить данные с сервера
//
//    case ConversionSuccessful(jsonPath) =>
//      log.info(colorStart + s"JSON successfully saved to $jsonPath" + colorEnd)
//      context.system.terminate()
//
//    case FetchError(reason) =>
//      log.error(s"Failed to fetch data: $reason")
//      context.system.terminate()
//
//    case SaveError(reason) =>
//      log.error(s"Failed to save XML: $reason")
//      context.system.terminate()
//  }
//
//  private def extractRestIdsFromXml(xml: Elem): List[String] = {
//    (xml \\ "field")
//      .filter(f => (f \ "@name").text == "restId")
//      .flatMap(_.child.collect {
//        case e if e.label == "string" => e.text.trim
//      })
//      .toList
//      .distinct
//  }
//}
//
//object FinalTest9 extends App {
//  implicit val system: ActorSystem = ActorSystem("AkkaHttpActorSystem")
//  implicit val materializer: ActorMaterializer = ActorMaterializer()
//  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
//
//  val mainActor = system.actorOf(Props[MainActor], "mainActor")
//  mainActor ! "start"
//}
//
//object Color {
//  val colorStart = "\u001B[34m"
//  val colorEnd = "\u001B[0m"
//}