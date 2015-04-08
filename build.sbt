import de.heikoseeberger.sbtheader.HeaderPattern
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt._

name := "spark-cdh5-template"

version in ThisBuild := "1.0"

enablePlugins(JavaAppPackaging)

scalaVersion := "2.10.5"

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

scalariformSettings

scalastyleFailOnError := true

dependencyUpdatesExclusions := moduleFilter(organization = "org.scala-lang")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

wartremoverErrors ++= Seq(
  Wart.Any,
  Wart.Any2StringAdd,
  Wart.EitherProjectionPartial,
  Wart.OptionPartial,
  Wart.Product,
  Wart.Serializable,
  Wart.ListOps,
  Wart.Nothing
)

val sparkVersion = "1.2.0-cdh5.3.3"

val hadoopVersion = "2.5.0-cdh5.3.3"

val sparkAvroVersion = "0.2.0"

val avroVersion = "1.7.6-cdh5.3.3"

val scalaTestVersion = "2.2.4"

resolvers in ThisBuild ++= Seq(
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "compile" excludeAll ExclusionRule(organization = "org.apache.hadoop"),
  "org.apache.spark" %% "spark-sql" % sparkVersion % "compile" excludeAll ExclusionRule(organization = "org.apache.hadoop"),
  "org.apache.spark" %% "spark-yarn" % sparkVersion % "compile" excludeAll ExclusionRule(organization = "org.apache.hadoop"),
  "com.databricks" %% "spark-avro" % sparkAvroVersion % "compile" excludeAll ExclusionRule(organization = "org.apache.avro"),
  "org.apache.avro" % "avro" % avroVersion % "compile" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty"),
  "org.apache.avro" % "avro-mapred" % avroVersion % "compile" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty"),
  "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "compile" excludeAll ExclusionRule("javax.servlet")
)

fork := true //http://stackoverflow.com/questions/27824281/sparksql-missingrequirementerror-when-registering-table

parallelExecution in Test := false

headers := Map(
  "scala" ->(HeaderPattern.cStyleBlockComment, Apache2_0("2015", "David Greco")._2),
  "conf" ->(HeaderPattern.hashLineComment, Apache2_0("2015", "David Greco")._2)
)

lazy val root = (project in file(".")).
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*).
  settings(
    libraryDependencies += "org.scalatest" % "scalatest_2.10" % scalaTestVersion % "it,test"
  ).enablePlugins(AutomateHeaderPlugin).disablePlugins(AssemblyPlugin)

lazy val assembly_ = (project in file("assembly")).
  settings(
    assemblyJarName in assembly := s"spark-cdh-template-assembly-${version.value}.jar", //assembly-assembly-0.1-SNAPSHOT.jar
    libraryDependencies ++= Seq(
      "com.databricks" %% "spark-avro" % sparkAvroVersion % "compile" excludeAll ExclusionRule(organization = "org.apache.avro"),
      "org.apache.avro" % "avro" % avroVersion % "compile" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty"),
      "org.apache.avro" % "avro-mapred" % avroVersion % "compile" classifier "hadoop2" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty")
    )
  ) dependsOn root settings (
  projectDependencies := {
    Seq(
      (projectID in root).value.excludeAll(ExclusionRule(organization = "org.apache.spark"),ExclusionRule(organization = "org.apache.hadoop"))
    )
  })

net.virtualvoid.sbt.graph.Plugin.graphSettings
