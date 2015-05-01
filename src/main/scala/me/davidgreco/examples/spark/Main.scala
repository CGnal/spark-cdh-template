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

import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{ FileSystem, Path }
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{ Seconds, StreamingContext }
import org.apache.spark.{ SparkConf, SparkContext }

object Main extends App {

  val yarn = true

  //addPath(args(0))

  val conf = new SparkConf().setAppName("spark-cdh5-template-yarn")

  val master = conf.getOption("spark.master")

  val uberJarLocation = {
    val location = getJar(Main.getClass)
    if (new File(location).isDirectory) s"${System.getProperty("user.dir")}/assembly/target/scala-2.10/spark-cdh-template-assembly-1.0.jar" else location
  }

  if (master.isEmpty) {
    //it means that we are NOT using spark-submit
    if (yarn) {
      //If we are not using spark-submit we assume that we are running this application on a machine
      //which is not a spark gateway, in this case we have to deploy the spark-assembly by ourselves.

      //The spark assembly is on the resources, so I copy it in tmp
      val inputUrl = getClass.getClassLoader.getResource("spark-assembly-1.3.0-cdh5.4.0-hadoop2.6.0-cdh5.4.0.jar")
      val dest = new File("/tmp/spark-assembly-1.3.0-cdh5.4.0-hadoop2.6.0-cdh5.4.0.jar")
      FileUtils.copyURLToFile(inputUrl, dest)
      //
      val fs = FileSystem.get(new Configuration())
      val assembly = s"hdfs:///user/${System.getProperty("user.name")}/.sparkStaging/spark-assembly.jar"
      if (fs.exists(new Path(assembly)))
        fs.delete(new Path(assembly), true)
      fs.copyFromLocalFile(
        false,
        true,
        new Path("file:///tmp/spark-assembly-1.3.0-cdh5.4.0-hadoop2.6.0-cdh5.4.0.jar"),
        new Path(assembly)
      )
      conf.
        setMaster("yarn-client").
        setAppName("spark-cdh5-template-yarn").
        set("executor-memory", "128m").
        setJars(List(uberJarLocation)).
        set("spark.yarn.jar", assembly).
        setIfMissing("spark.serializer", "org.apache.spark.serializer.KryoSerializer").
        setIfMissing("spark.io.compression.codec", "lzf").
        setIfMissing("spark.speculation", "true").
        setIfMissing("spark.shuffle.manager", "sort").
        setIfMissing("spark.shuffle.service.enabled", "true").
        setIfMissing("spark.dynamicAllocation.enabled", "true").
        setIfMissing("spark.dynamicAllocation.minExecutors", Integer.toString(1)).
        setIfMissing("spark.dynamicAllocation.maxExecutors", Integer.toString(4)).
        setIfMissing("spark.dynamicAllocation.executorIdleTimeout", "60").
        setIfMissing("spark.executor.cores", Integer.toString(1)).
        setIfMissing("spark.executor.memory", "256m").
        setIfMissing("spark.ui.port", Integer.toString(4040)).
        setIfMissing("spark.ui.showConsoleProgress", "false")
    } else
      conf.
        setAppName("spark-cdh5-template-local").
        setMaster("local[16]")
  }

  val sparkContext = new SparkContext(conf)

  val streamingContext = new StreamingContext(sparkContext, Seconds(1))

  val kafkaStream = KafkaUtils.createStream(streamingContext, "cdh-docker:2181", "id", Map("topic" -> 1))

  kafkaStream.foreachRDD(rdd => rdd.collect().foreach(x => println(x)))

  streamingContext.start()
  streamingContext.awaitTermination()

  sparkContext.stop()

}
