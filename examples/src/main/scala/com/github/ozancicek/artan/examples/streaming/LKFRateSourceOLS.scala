/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ozancicek.artan.examples.streaming

import com.github.ozancicek.artan.ml.filter.{LinearKalmanFilter}
import org.apache.spark.ml.linalg._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._


/**
 * Recursive Least Squares with linear kalman filter & streaming rate source.
 *
 * To run the sample from source, build the assembly jar for artan-examples project and run:
 *
 * `spark-submit --class com.github.ozancicek.artan.examples.streaming.LKFRateSourceOLS artan-examples-assembly-VERSION.jar 10 10`
 */
object LKFRateSourceOLS {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: LKFRateSourceOLS <numStates> <measurementsPerSecond>")
      System.exit(1)
    }
    val spark = SparkSession
      .builder
      .appName("LKFRateSourceOLS")
      .getOrCreate
    spark.sparkContext.setLogLevel("WARN")

    import spark.implicits._

    val numStates = args(0).toInt
    val rowsPerSecond = args(1).toInt

    // OLS problem, states to be estimated are a, b and c
    // z = a*x + b * y + c + w, where w ~ N(0, 1)

    val a = 0.5
    val b = 0.2
    val c = 1.2
    val stateSize = 3
    val measurementsSize = 1
    val noiseParam = 1.0

    val featuresUDF = udf((x: Double, y: Double) => {
      new DenseMatrix(measurementsSize, stateSize, Array(x, y, 1.0))
    })

    val labelUDF = udf((x: Double, y: Double, r: Double) => {
      new DenseVector(Array(a*x + b*y + c + r))
    })

    val features = spark.readStream.format("rate")
      .option("rowsPerSecond", rowsPerSecond)
      .load()
      .withColumn("mod", $"value" % numStates)
      .withColumn("stateKey", $"mod".cast("String"))
      .withColumn("x", ($"value"/numStates).cast("Integer").cast("Double"))
      .withColumn("y", sqrt($"x"))
      .withColumn("label", labelUDF($"x", $"y", randn() * noiseParam))
      .withColumn("features", featuresUDF($"x", $"y"))

    val filter = new LinearKalmanFilter(stateSize, measurementsSize)
      .setInitialCovariance(
        new DenseMatrix(3, 3, Array(10.0, 0.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 10.0)))
      .setStateKeyCol("stateKey")
      .setMeasurementCol("label")
      .setMeasurementModelCol("features")
      .setProcessModel(DenseMatrix.eye(stateSize))
      .setProcessNoise(DenseMatrix.zeros(stateSize, stateSize))
      .setMeasurementNoise(DenseMatrix.eye(measurementsSize))

    val truncate = udf((state: DenseVector) => state.values.map(t => (math floor t * 100)/100))

    val query = filter.transform(features)
      .select($"stateKey", $"stateIndex", truncate($"state").alias("modelParameters"))
      .writeStream
      .queryName("LKFRateSourceOLS")
      .outputMode("append")
      .format("console")
      .start()

    query.awaitTermination()
  }
}