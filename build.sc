import mill._
import mill.scalalib._

object `import` extends ScalaModule {

  def scalaVersion = "2.13.12"

  override def ivyDeps = Agg(
    ivy"mysql:mysql-connector-java:5.1.49",
    ivy"com.typesafe.slick::slick:3.4.1",
    ivy"com.typesafe.akka::akka-stream:2.8.5",
    ivy"ch.qos.logback:logback-classic:1.4.11"
  )
}
