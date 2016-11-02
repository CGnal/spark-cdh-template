/*
 * Copyright 2016 CGnal S.p.A.
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

package com.cgnal.examples.spark

import java.io.File
import java.net.InetAddress

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }

object Main extends App {

  val yarn = true

  val initialExecutors = 4

  val minExecutors = 4

  val conf: SparkConf = new SparkConf().setAppName("spark-cdh5-template-yarn")

  val master: Option[String] = conf.getOption("spark.master")

  val uberJarLocation: String = {
    val location = getJar(Main.getClass)
    if (new File(location).isDirectory) s"${System.getProperty("user.dir")}/assembly/target/scala-2.10/spark-cdh-template-assembly-1.0.jar" else location
  }

  if (master.isEmpty) {
    //it means that we are NOT using spark-submit

    addPath(args(0))

    if (yarn) {
      val _ = conf.
        setMaster("yarn-client").
        setAppName("spark-cdh5-template-yarn").
        setJars(List(uberJarLocation)).
        set("spark.yarn.jar", "local:/opt/cloudera/parcels/CDH/lib/spark/assembly/lib/spark-assembly.jar").
        set("spark.serializer", "org.apache.spark.serializer.KryoSerializer").
        set("spark.io.compression.codec", "lzf").
        set("spark.speculation", "true").
        set("spark.shuffle.manager", "sort").
        set("spark.shuffle.service.enabled", "true").
        set("spark.dynamicAllocation.enabled", "true").
        set("spark.executor.cores", Integer.toString(1)).
        set("spark.executor.memory", "256m")
    } else {
      val _ = conf.
        setAppName("spark-cdh5-template-local").
        setMaster("local[16]")
    }
  }

  val sparkContext = new SparkContext(conf)

  def getSystemRDD(sparkConf: SparkContext): RDD[(Int, (String, String))] = {
    val yarnClient = YarnClient.createYarnClient()
    yarnClient.init(new Configuration())
    yarnClient.start()

    val numNodeManagers: Int = yarnClient.getYarnClusterMetrics.getNumActiveNodeManagers

    val rdd: RDD[Int] = sparkContext.parallelize[Int](1 to numNodeManagers, numNodeManagers)

    rdd.mapPartitionsWithIndex[(Int, (String, String))]((index, iterator) => new Iterator[(Int, (String, String))] {

      @SuppressWarnings(Array("org.wartremover.warts.Var"))
      var firstTime = true

      @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
      override def hasNext: Boolean =
        if (firstTime) {
          firstTime = false
          true
        } else
          firstTime

      override def next(): (Int, (String, String)) = (index, {
        val address: InetAddress = InetAddress.getLocalHost()
        (address.getHostAddress, address.getHostName)
      })
    }, preservesPartitioning = true)
  }

  getSystemRDD(sparkContext).collect().foreach(println(_))

  sparkContext.stop()

}
