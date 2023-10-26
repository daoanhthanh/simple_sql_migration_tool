package vn.daoanhthanh

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import slick.jdbc.MySQLProfile.api._

import java.io.{File, FilenameFilter}
import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("SQLImporterSystem")
  implicit val materializer: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher

  private val sqlScriptContainerPath: Path = Try {
    val userInput: String = args(0)
    Path.of(userInput)
  }.getOrElse(
    Path.of("./scripts")
  )

  private val db = Database.forConfig("mydb")

  private val executionFlow: Future[Seq[Done]] = Future.sequence(
    listAllSqlScriptFiles(sqlScriptContainerPath).map(sqlFile =>
      importSQLFile(sqlFile)
    )
  )

  private def listAllSqlScriptFiles(path: Path): Seq[File] = {

    val directory = path.toFile

    if (directory.exists && directory.isDirectory) {
      directory
        .listFiles(new FilenameFilter {
          def accept(dir: File, name: String): Boolean =
            name.toLowerCase.endsWith(".sql")
        })
        .filter(_.isFile)
        .toSeq
    } else {
      Seq.empty
    }
  }

  /** Read and execute SQL statements from the file using Akka Streams
    * (determine by semicolon)
    * @param file: SQL file
    */
  private def importSQLFile(file: File): Future[Done] = {
    val source = FileIO.fromPath(file.toPath)
    val delimiter = ByteString(";")
    val sqlStatements: Source[ByteString, Future[IOResult]] =
      source.via(
        Framing.delimiter(
          delimiter,
          maximumFrameLength = 1024,
          allowTruncation = true
        )
      )

    val importResult: Future[Done] =
      sqlStatements
        .map(_.utf8String.trim)
        .mapAsync(parallelism = 4)(executeSQLStatement)
        .runWith(Sink.ignore)

    importResult
  }

  private def executeSQLStatement(sql: String): Future[Int] = {
    db.run(sqlu"#$sql")
  }

  executionFlow.onComplete {
    case Success(_) =>
      println("SQL file imported successfully.")
    case Failure(exception) =>
      println(s"Failed to import SQL file: ${exception.getMessage}")
  }

  executionFlow.onComplete(_ => {
    db.close()
    system.terminate()
  })
}
