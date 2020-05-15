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

package com.github.ozancicek.artan.ml.mixture

import com.github.ozancicek.artan.ml.state.{BernoulliMixtureInput, BernoulliMixtureOutput, BernoulliMixtureState}
import com.github.ozancicek.artan.ml.stats.{BernoulliDistribution, BernoulliMixtureDistribution}
import org.apache.spark.ml.param._
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql._
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types._


/**
 * Online bernoulli mixture transformer, based on Cappe (2010) Online Expectation-Maximisation
 *
 * @param mixtureCount number of mixture components
 */
class BernoulliMixture(
    val mixtureCount: Int,
    override val uid: String)
  extends FiniteMixture[
    Boolean,
    BernoulliDistribution,
    BernoulliMixtureDistribution,
    BernoulliMixtureInput,
    BernoulliMixtureState,
    BernoulliMixtureOutput,
    BernoulliMixture]
  with HasInitialProbabilities with HasInitialProbabilitiesCol {

  def this(mixtureCount: Int) = this(mixtureCount, Identifiable.randomUID("BernoulliMixture"))

  protected val defaultStateKey: String = "em.BernoulliMixture.defaultStateKey"

  /**
   * Creates a copy of this instance with the same UID and some extra params.
   */
  override def copy(extra: ParamMap): BernoulliMixture =  {
    val that = new BernoulliMixture(mixtureCount)
    copyValues(that, extra)
  }

  /**
   * Applies the transformation to dataset schemas
   */
  def transformSchema(schema: StructType): StructType = {
    if (!isSet(initialMixtureModelCol)) {
      require(
        isSet(initialProbabilities) | isSet(initialProbabilitiesCol),
        "Initial probabilities or its dataframe column must be set")
      if (isSet(initialProbabilitiesCol)) {
        require(
          schema(getInitialProbabilitiesCol).dataType == ArrayType(DoubleType),
          "Initial probabilities column should be an array of doubles with size mixtureCount")
      }
    }
    asDataFrameTransformSchema(outEncoder.schema)
  }

  def setInitialProbabilities(value: Array[Double]): BernoulliMixture = set(initialProbabilities, value)

  def setInitialProbabilitiesCol(value: String): BernoulliMixture = set(initialProbabilitiesCol, value)

  protected def buildInitialMixtureModel(dataFrame: DataFrame): DataFrame = {
    val mixtureModelFunc = udf(
      (weights: Seq[Double], probabilities: Seq[Double]) =>
        BernoulliMixtureDistribution(weights, probabilities.map(BernoulliDistribution(_))))
    val mixtureModelExpr = mixtureModelFunc(col("initialWeights"), col("initialProbabilities"))
    dataFrame
      .withColumn("initialWeights", getUDFWithDefault(initialWeights, initialWeightsCol))
      .withColumn("initialProbabilities", getUDFWithDefault(initialProbabilities, initialProbabilitiesCol))
      .withColumn("initialMixtureModel", mixtureModelExpr)
  }
}


private[mixture] trait HasInitialProbabilities extends Params {

  final val initialProbabilities: Param[Array[Double]] = new DoubleArrayParam(
    this,
    "initialProbabilities",
    "initialProbabilities")

  final def getInitialProbabilities: Array[Double] = $(initialProbabilities)
}


private[mixture] trait HasInitialProbabilitiesCol extends Params {

  final val initialProbabilitiesCol: Param[String] = new Param[String](
    this,
    "initialProbabilitiesCol",
    "initialProbabilitiesCol"
  )

  final def getInitialProbabilitiesCol: String = $(initialProbabilitiesCol)
}