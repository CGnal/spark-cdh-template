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

package com.cloudera.ps.examples.spark

import java.io.File
import java.net.{ URL, URLClassLoader }

import com.databricks.spark.avro.AvroSaver
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.AvroInputFormat
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }

object Main extends App {

  val yarn = true

  //Simple function for adding a directory to the system classpath
  def addPath(dir: String) = {
    val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    method.setAccessible(true)
    method.invoke(ClassLoader.getSystemClassLoader(), new File(dir).toURI().toURL())
  }

  //given a class it returns the jar (in the classpath) containing that class
  def getJar(klass: Class[_]): String = {
    val codeSource = klass.getProtectionDomain.getCodeSource
    codeSource.getLocation.getPath
  }

  addPath(args(0)) //You can pass the HADOOP config directory as an option

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
  val data = 1 to 100000
  val rdd: RDD[Int] = sparkContext.parallelize[Int](data)
  println(rdd.count())

  sparkContext.stop()

}
