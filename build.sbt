import de.heikoseeberger.sbtheader.HeaderPattern
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt._

organization := "me.davidgreco"

name := "spark-cdh5-template"

version in ThisBuild := "1.0"

val assemblyName = "spark-cdh-template-assembly"

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
  "com.databricks" %% "spark-avro" % sparkAvroVersion % "provided" excludeAll ExclusionRule(organization = "org.apache.avro"),
  "org.apache.avro" % "avro" % avroVersion % "provided" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty"),
  "org.apache.avro" % "avro-mapred" % avroVersion % "provided" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty"),
  "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "compile" excludeAll ExclusionRule("javax.servlet")
)

//http://stackoverflow.com/questions/18838944/how-to-add-provided-dependencies-back-to-run-test-tasks-classpath/21803413#21803413
run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in(Compile, run), runner in(Compile, run))

//http://stackoverflow.com/questions/27824281/sparksql-missingrequirementerror-when-registering-table
fork := true

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
    assemblyJarName in assembly := s"$assemblyName-${version.value}.jar",
    libraryDependencies ++= Seq(
      "com.databricks" %% "spark-avro" % sparkAvroVersion % "compile" excludeAll ExclusionRule(organization = "org.apache.avro"),
      "org.apache.avro" % "avro" % avroVersion % "compile" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty"),
      "org.apache.avro" % "avro-mapred" % avroVersion % "compile" classifier "hadoop2" exclude("org.mortbay.jetty", "servlet-api") exclude("io.netty", "netty") exclude("org.apache.avro", "avro-ipc") exclude("org.mortbay.jetty", "jetty")
    )
  ) dependsOn root settings (
  projectDependencies := {
    Seq(
      (projectID in root).value.excludeAll(ExclusionRule(organization = "org.apache.spark"), ExclusionRule(organization = "org.apache.hadoop"))
    )
  })

mappings in Universal := {
  val universalMappings = (mappings in Universal).value
  val filtered = universalMappings filter {
    case (f, n) => ! n.endsWith(s"${organization.value}.${name.value}-${version.value}.jar")
  }
  val fatJar: File = new File(s"${System.getProperty("user.dir")}/assembly/target/scala-2.10/$assemblyName-${version.value}.jar")
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

scriptClasspath ++= Seq(s"$assemblyName-${version.value}.jar")

net.virtualvoid.sbt.graph.Plugin.graphSettings
