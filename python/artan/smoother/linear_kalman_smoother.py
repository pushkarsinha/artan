#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at

#  http://www.apache.org/licenses/LICENSE-2.0

#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

from artan.state import StatefulTransformer
from artan.filter.linear_kalman_filter import LinearKalmanFilterParams
from artan.filter.filter_params import *


class HasFixedLag(Params):
    """
    Mixin for param for fixed lag
    """

    fixedLag = Param(
        Params._dummy(),
        "fixedLag", "Fixed lag", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasFixedLag, self).__init__()

    def getFixedLag(self):
        """
        Gets the value of fixed lag or its default value.
        """
        return self.getOrDefault(self.fixedLag)


class LinearKalmanSmoother(StatefulTransformer, LinearKalmanFilterParams, HasFixedLag):
    """
    Fixed lag linear kalman smoother implemented with Rauch-Tung-Striebel method.
    """
    def __init__(self, stateSize, measurementSize):
        super(LinearKalmanSmoother, self).__init__()
        self._java_obj = self._new_java_obj("com.ozancicek.artan.ml.smoother.LinearKalmanSmoother",
                                            stateSize, measurementSize, self.uid)

    def setFixedLag(self, value):
        """
        Set fixed lag

        :param value: Int
        :return: LinearKalmanSmoother
        """
        return self._set(fixedLag=value)