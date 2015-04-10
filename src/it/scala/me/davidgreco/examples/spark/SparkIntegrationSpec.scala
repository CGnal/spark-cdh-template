package me.davidgreco.examples.spark

//import com.databricks.spark.avro.AvroSaver
//import org.apache.avro.generic.GenericRecord
//import org.apache.avro.mapred.AvroInputFormat
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}

class SparkIntegrationSpec extends WordSpec with MustMatchers with BeforeAndAfterAll {

  var sparkContext: SparkContext = _

  override def beforeAll() = {
    val conf = new SparkConf().
      setAppName("spark-cdh5-template-yarn").
      set("executor-memory", "128m").
      //setJars(List(getJar(AvroSaver.getClass), getJar(classOf[AvroInputFormat[GenericRecord]]))).
      setJars(List(s"${System.getProperty("user.dir")}/assembly/target/scala-2.10/spark-cdh-template-assembly-1.0.jar")).
      set("spark.yarn.jar", "hdfs:///user/spark/share/lib/spark-assembly.jar").
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
