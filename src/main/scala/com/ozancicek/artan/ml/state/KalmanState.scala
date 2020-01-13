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

package com.ozancicek.artan.ml.state

import org.apache.spark.ml.linalg.{Vector, Matrix, DenseVector}
import com.ozancicek.artan.ml.linalg.LinalgUtils
import com.ozancicek.artan.ml.stats.MultivariateGaussian

/**
 * Case class for inputs of a kalman filter. Let state at step k be denoted with x_k, measurement with
 * z_k, where x_k and z_k are vectors of lenght n_state and n_obs
 *
 * State evolution:
 * x_k = F_k * x_k-1 + B_k * u_k + w_k
 *
 * Measurement:
 * z_k = H_k * x_k + v_k
 *
 * @param stateKey Key of the filter state.
 * @param measurement z_k, Vector with length n_obs
 * @param measurementModel H_k, Matrix with dimensions (n_state, n_obs)
 * @param measurementNoise v_k, Matrix with dimensions (n_obs, n_obs)
 * @param processModel F_k, Matrix with dimensions (n_state, n_state)
 * @param processNoise w_k, Matrix with dimensions (n_state, n_state)
 * @param control u_k, Vector with length n_state
 * @param controlFunction B_k, Matrix with dimensions (n_state, n_state)
 */
private[ml] case class KalmanInput(
    stateKey: String,
    measurement: Option[Vector],
    measurementModel: Option[Matrix],
    measurementNoise: Option[Matrix],
    processModel: Option[Matrix],
    processNoise: Option[Matrix],
    control: Option[Vector],
    controlFunction: Option[Matrix])



/**
 * Case class for representing the output state of a kalman filter.
 * Let state at step k be denoted with x_k, measurement with
 * z_k, where x_k and z_k are vectors of lenght n_state and n_obs
 *
 *
 * @param stateKey Key of the state
 * @param stateIndex index of the filter, incremented only on state evolution
 * @param state x_k, the state vector with length n_state
 * @param stateCovariance state covariance matrix with dimensions n_state, n_stae
 * @param residual residual of x_k and z_k, vector with length n_obs
 * @param residualCovariance covariance of residual, matrix with dimensions n_obs, n_obs
 */
case class KalmanOutput(
    stateKey: String,
    stateIndex: Long,
    state: Vector,
    stateCovariance: Matrix,
    residual: Vector,
    residualCovariance: Matrix) {

  def loglikelihood: Double = {
    val zeroMean = new DenseVector(Array.fill(residual.size) {0.0})
    MultivariateGaussian.logpdf(residual.toDense, zeroMean, residualCovariance.toDense)
  }

  def mahalanobis: Double = {
    val zeroMean = new DenseVector(Array.fill(residual.size) {0.0})
    LinalgUtils.mahalanobis(residual.toDense, zeroMean, residualCovariance.toDense)
  }
}

/**
 * Internal representation of the state of a kalman filter.
 */
private[ml] case class KalmanState(
    stateKey: String,
    stateIndex: Long,
    state: Vector,
    stateCovariance: Matrix,
    residual: Vector,
    residualCovariance: Matrix) extends KeyedState[String, KalmanOutput] {

  def asOut: KalmanOutput = {
    KalmanOutput(
      stateKey,
      stateIndex,
      state,
      stateCovariance,
      residual,
      residualCovariance
    )
  }
}
