/*
 * Copyright 2016 David Greco
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

import org.apache.hadoop.hbase.client.{Connection, Put, Result, Scan}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseTestingUtility, TableName}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}

case class Person(name: String, age: Int)

class SparkSpec extends WordSpec with MustMatchers with BeforeAndAfterAll {

  import SparkSpec._

  val hbaseUtil = new HBaseTestingUtility()

  var sparkContext: SparkContext = _

  var hbaseContext: HBaseContext = _

  override def beforeAll(): Unit = {
    hbaseUtil.startMiniCluster(1)
    connection = Some(hbaseUtil.getConnection)
    hbaseUtil.createTable(TableName.valueOf("MyTable"), Array("MYCF"))
    val conf = new SparkConf().
      setAppName("spark-cdh5-template-local-test").
      setMaster("local")
    sparkContext = new SparkContext(conf)
    hbaseContext = new HBaseContext(sparkContext, hbaseUtil.getConfiguration)
    ()
  }

  "Spark" must {
    "load from an HBase table correctly" in {
      val table = hbaseUtil.getConnection.getTable(TableName.valueOf("MyTable"))
      for (i <- 1 to 10) {
        val p = new Put(Bytes.toBytes(i))
        p.addColumn(Bytes.toBytes("MYCF"), Bytes.toBytes("QF1"), Bytes.toBytes(s"CIAO$i"))
        table.put(p)
      }
      val rdd = hbaseContext.hbaseRDD(TableName.valueOf("MyTable"), new Scan()).asInstanceOf[RDD[(ImmutableBytesWritable, Result)]]
      rdd.map(p => Bytes.toInt(p._2.getRow)).collect().toList must be(1 to 10)
    }

  }

  override def afterAll(): Unit = {
    sparkContext.stop()
    hbaseUtil.shutdownMiniCluster()
  }

}

object SparkSpec {
  var connection: Option[Connection] = None
}
