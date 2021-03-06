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

package com.github.ozancicek.artan.ml.state

import org.apache.spark.ml.linalg.Vector
import java.sql.Timestamp

/**
 * Case class for the inputs of a least mean squares filter.
 * @param stateKey Key of the filter
 * @param label Label corresponding to the features.
 * @param features Features vector.
 * @param eventTime event time of the input
 * @param initialState initial state vector
 */
private[ml] case class LMSInput(
    stateKey: String,
    label: Double,
    features: Vector,
    eventTime: Option[Timestamp],
    initialState: Vector) extends KeyedInput[String]


/**
 * Case class for the output state of a least mean squares filter.
 *
 * @param stateKey   Key of the filter.
 * @param stateIndex Index of state.
 * @param state      State vector.
 * @param eventTime  event time of the of the output
 */
case class LMSOutput(
    stateKey: String,
    stateIndex: Long,
    state: Vector,
    eventTime: Option[Timestamp]) extends KeyedOutput[String]

/**
 * Internal representation of the state of a least mean squares filter.
 */
private[ml] case class LMSState(
    stateIndex: Long,
    state: Vector) extends State
