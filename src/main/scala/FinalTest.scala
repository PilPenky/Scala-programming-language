import Color._
import FinalTest.system.log
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.circe.Json
import io.circe.parser.parse
import org.json4s.Xml.toJson
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats}

import java.nio.file.{Files, Paths}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.xml.{Elem, PrettyPrinter, XML}

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

    case DataReceived(json) =>
      log.info(colorStart + "Successfully received and parsed JSON data" + colorEnd)

    case FetchError(reason) =>
      log.error(s"Failed to fetch data: $reason")
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

  def xmlToJson(xmlFilePath: String, jsonFilePath: String): Unit = {
    implicit val formats: Formats = DefaultFormats
    try {
      val xmlContent = XML.loadFile(xmlFilePath)
      val jsonValue = toJson(xmlContent) // Прямое преобразование через json4s-xml
      val jsonString = pretty(render(jsonValue)) // Красивое форматирование

      Files.write(Paths.get(jsonFilePath), jsonString.getBytes("UTF-8"))
      log.info(colorStart + s"Успешно конвертировано $xmlFilePath в $jsonFilePath" + colorEnd)
    } catch {
      case e: Exception =>
        println(s"Ошибка при конвертации: ${e.getMessage}")
        throw e
    }
  }

  def saveXmlToFile(xml: Elem, path: String): Unit = {
    val prettyPrinter = new PrettyPrinter(80, 2)
    val prettyXml = prettyPrinter.format(xml)

    val writer = new java.io.PrintWriter(new java.io.File(path))
    try {
      writer.write(prettyXml)
    } finally {
      writer.close()
    }
  }
}

// Актор-хранилище для работы с файлами
class StorageActor extends Actor with ActorLogging {

  import StorageActor._
  import XmlUtils._

  override def receive: Receive = {
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

    case ConvertXmlToJson(xml, json) =>
      val originalSender = sender()
      try {
        xmlToJson(xml, json)
        self ! ConversionSuccessful(json)
        originalSender ! ConversionSuccessful(json)
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

    case XmlRead(xml) =>
      log.info(colorStart + "Successfully read XML from file" + colorEnd)

    case ReadError(reason) =>
      log.error(s"Failed to read XML: $reason")
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

  override def receive: Receive = {
    case "start" =>
      log.info(colorStart + "Starting data processing..." + colorEnd)
      httpClient ! FetchData

    case DataReceived(json) =>
      log.info(colorStart + "JSON received, converting to XML..." + colorEnd)
      val xmlData = jsonToXml(json)
      storageActor ! SaveXml(xmlData, outputPathXML)

    case XmlSaved(_) =>
      log.info(colorStart + "XML saved, starting conversion to JSON..." + colorEnd)
      // Добавить удаление данных с сервера + обновление сервера, после преобразование
      storageActor ! ReadXml(outputPathXML)

    case XmlRead(xml) =>
      log.info(colorStart + "Converting XML to JSON..." + colorEnd)
      storageActor ! ConvertXmlToJson(outputPathXML, outputPathJSON)

    // Сохранить преобразованный JSON на сервер методом POST

    // Запросить данные с сервера

    case ConversionSuccessful(jsonPath) =>
      log.info(colorStart + s"JSON successfully saved to $jsonPath" + colorEnd)
      context.system.terminate()

    case FetchError(reason) =>
      log.error(s"Failed to fetch data: $reason")
      context.system.terminate()

    case SaveError(reason) =>
      log.error(s"Failed to save XML: $reason")
      context.system.terminate()
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
  val colorEnd = "\u001B[0m"
}