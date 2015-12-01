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

import org.apache.spark.{ SparkConf, SparkContext }

object Main extends App {

  val yarn = true

  val conf = new SparkConf().setAppName("spark-cdh5-template-yarn")

  val master = conf.getOption("spark.master")

  val uberJarLocation = {
    val location = getJar(Main.getClass)
    if (new File(location).isDirectory) s"${System.getProperty("user.dir")}/assembly/target/scala-2.10/spark-cdh-template-assembly-1.0.jar" else location
  }

  if (master.isEmpty) {
    //it means that we are NOT using spark-submit

    addPath(args(0))

    if (yarn)
      conf.
        setMaster("yarn-client").
        setAppName("spark-cdh5-template-yarn").
        setJars(List(uberJarLocation)).
        set("spark.yarn.jar", "local:/opt/cloudera/parcels/CDH/lib/spark/assembly/lib/spark-assembly.jar").
        set("spark.executor.extraClassPath", "/opt/cloudera/parcels/CDH/jars/*"). //This is an hack I made I'm not sure why it works though
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
    else
      conf.
        setAppName("spark-cdh5-template-local").
        setMaster("local[16]")
  }

  val sparkContext = new SparkContext(conf)

  val rdd = sparkContext.parallelize[Int](1 to 10000)
  println(rdd.count())

  sparkContext.stop()

}
