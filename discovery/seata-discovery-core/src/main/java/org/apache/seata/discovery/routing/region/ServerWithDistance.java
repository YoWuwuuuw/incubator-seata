/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.discovery.routing.region;

/**
 * Server wrapper with distance
 * Used to store server instance and its distance information during routing calculation
 *
 * @param <T> server instance type
 */
public class ServerWithDistance<T> {
    private final T server;
    private final double distance;

    public ServerWithDistance(T server, double distance) {
        this.server = server;
        this.distance = distance;
    }

    public T getServer() {
        return server;
    }

    public double getDistance() {
        return distance;
    }
}
