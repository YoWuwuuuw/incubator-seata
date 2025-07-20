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
package org.apache.seata.discovery.routing;

import java.util.List;

/**
 * State router interface
 * Defines basic behavior of routers, supports chain of responsibility pattern
 *
 * @param <T> service instance type
 */
public interface StateRouter<T> {

    /**
     * Execute routing
     * @param servers service instances list
     * @param ctx routing context
     * @param debugMode whether debug mode is enabled
     * @param snapshots snapshot list, used to collect snapshots from all routers
     * @return routed service instances list
     */
    BitList<T> route(BitList<T> servers, RoutingContext ctx, boolean debugMode, List<RouterSnapshotNode<T>> snapshots);

    /**
     * Whether it's a runtime router
     * Runtime router: needs to calculate dynamically based on each request's context
     * Non-runtime router: configuration-based, only loads configuration once at startup
     * @return whether it's a runtime router
     */
    boolean isRuntime();

    /**
     * Build snapshot
     * @return snapshot string
     */
    String buildSnapshot();

    /**
     * Set next router
     * @param next next router
     */
    void setNext(StateRouter<T> next);

    /**
     * Get next router
     * @return next router
     */
    StateRouter<T> getNext();
}
