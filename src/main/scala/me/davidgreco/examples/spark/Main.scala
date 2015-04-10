/*
 * Copyright 2015 David Greco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.davidgreco.examples.spark

import com.databricks.spark.avro.AvroSaver
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.AvroInputFormat
import org.apache.spark.{ SparkConf, SparkContext }

object Main extends App {

  val yarn = false

  val conf =
    if (yarn)
      new SparkConf().
        setAppName("spark-cdh5-template-yarn").
        set("executor-memory", "128m").
        setJars(List(getJar(AvroSaver.getClass), getJar(classOf[AvroInputFormat[GenericRecord]]))).
        set("spark.yarn.jar", "hdfs:///user/spark/share/lib/spark-assembly.jar").
        setMaster("yarn-client")
    else
      new SparkConf().
        setAppName("spark-cdh5-template-local").
        setMaster("local[16]")

  val sparkContext = new SparkContext(conf)

  import org.apache.spark.sql._

  val sqlContext = new SQLContext(sparkContext)

  val input = if (conf.get("spark.app.name") == "spark-cdh5-template-yarn")
    s"hdfs:///user/${System.getProperty("user.name")}/test.avro"
  else
    s"file://${System.getProperty("user.dir")}/src/test/resources/test.avro"

  import com.databricks.spark.avro._

  val data = sqlContext.avroFile(input)

  data.registerTempTable("test")

  val res = sqlContext.sql("select * from test where a < 10")

  println(res.collect().toList)

  sparkContext.stop()

}
