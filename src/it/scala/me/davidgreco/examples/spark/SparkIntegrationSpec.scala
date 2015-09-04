package me.davidgreco.examples.spark

import java.io.File

import com.databricks.spark.avro.AvroSaver
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.AvroInputFormat
import org.apache.commons.io.FileUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}

class SparkIntegrationSpec extends WordSpec with MustMatchers with BeforeAndAfterAll {

  var sparkContext: SparkContext = _

  def getJar(klass: Class[_]): String = {
    val codeSource = klass.getProtectionDomain.getCodeSource
    codeSource.getLocation.getPath
  }

  override def beforeAll() = {
    val inputUrl = getClass.getClassLoader.getResource("spark-assembly_2.10-1.3.0-cdh5.4.5.jar")
    val dest = new File("/tmp/spark-assembly_2.10-1.3.0-cdh5.4.5.jar")
    FileUtils.copyURLToFile(inputUrl, dest)
    //
    val fs = FileSystem.get(new Configuration())
    val assembly = s"hdfs:///user/${System.getProperty("user.name")}/.sparkStaging/spark-assembly.jar"
    if (fs.exists(new Path(assembly)))
      fs.delete(new Path(assembly), true)
    fs.copyFromLocalFile(
      false,
      true,
      new Path("file:///tmp/spark-assembly_2.10-1.3.0-cdh5.4.5.jar"),
      new Path(assembly)
    )

    val conf = new SparkConf().
      setAppName("spark-cdh5-template-yarn").
      set("executor-memory", "128m").
      setJars(List(getJar(AvroSaver.getClass), getJar(classOf[AvroInputFormat[GenericRecord]]))).
      set("spark.yarn.jar", s"hdfs:///user/${System.getProperty("user.name")}/.sparkStaging/spark-assembly.jar").
      setMaster("yarn-client")
    sparkContext = new SparkContext(conf)
  }

  "Spark" must {
    "load an avro file as a schema rdd correctly" in {
      val fs = FileSystem.get(new Configuration())
      val input = s"/user/${System.getProperty("user.name")}/test.avro"
      if (fs.exists(new Path(input)))
        fs.delete(new Path(input), true)
      fs.copyFromLocalFile(
        false,
        true,
        new Path(s"file://${System.getProperty("user.dir")}/src/test/resources/test.avro"),
        new Path(s"/user/${System.getProperty("user.name")}")
      )

      val sqlContext = new SQLContext(sparkContext)

      import com.databricks.spark.avro._

      val data = sqlContext.avroFile(input)

      data.registerTempTable("test")

      val res = sqlContext.sql("select * from test where a < 10")

      res.collect().toList.toString must be("List([0,CIAO0], [1,CIAO1], [2,CIAO2], [3,CIAO3], [4,CIAO4], [5,CIAO5], [6,CIAO6], [7,CIAO7], [8,CIAO8], [9,CIAO9])")
    }
  }

  override def afterAll() = {
    sparkContext.stop()
  }

}
