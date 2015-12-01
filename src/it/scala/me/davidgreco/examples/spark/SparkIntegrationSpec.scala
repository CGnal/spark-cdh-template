package me.davidgreco.examples.spark

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

  override def beforeAll(): Unit = {

    addPath("/Users/dgreco/Workspace/cdh/hadoop/etc/hadoop")

    val uberJarLocation = s"${System.getProperty("user.dir")}/assembly/target/scala-2.10/spark-cdh-template-assembly-1.0.jar"

    val conf = new SparkConf().
      setMaster("yarn-client").
      setAppName("spark-cdh5-template-yarn").
      setJars(List(uberJarLocation)).
      set("spark.yarn.jar", "local:/opt/cloudera/parcels/CDH/lib/spark/assembly/lib/spark-assembly.jar").
      set("spark.executor.extraClassPath", "/opt/cloudera/parcels/CDH/jars/*").
      set("spark.serializer", "org.apache.spark.serializer.KryoSerializer").
      set("spark.io.compression.codec", "lzf").
      set("spark.speculation", "true").
      set("spark.shuffle.manager", "sort").
      set("spark.shuffle.service.enabled", "true").
      set("spark.dynamicAllocation.enabled", "true").
      set("spark.dynamicAllocation.initialExecutors", Integer.toString(4)).
      set("spark.dynamicAllocation.minExecutors", Integer.toString(4)).
      set("spark.executor.cores", Integer.toString(1)).
      set("spark.executor.memory", "256m")

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

      import com.databricks.spark.avro._

      val sqlContext = new SQLContext(sparkContext)

      val data = sqlContext.read.avro(input)

      data.registerTempTable("test")

      val res = sqlContext.sql("select * from test where a < 10")

      res.collect().toList.toString must
        be("List([0,CIAO0], [1,CIAO1], [2,CIAO2], [3,CIAO3], [4,CIAO4], [5,CIAO5], [6,CIAO6], [7,CIAO7], [8,CIAO8], [9,CIAO9])")
    }
  }

  override def afterAll(): Unit = {
    sparkContext.stop()
  }

}
