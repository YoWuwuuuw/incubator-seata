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
package org.apache.seata.discovery.routing.router;

import org.apache.seata.discovery.routing.BitList;
import org.apache.seata.discovery.routing.RouterSnapshotNode;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.StateRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Provides basic implementation for routers
 *
 * @param <T> service instance type
 */
public abstract class AbstractStateRouter<T> implements StateRouter<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStateRouter.class);

    private StateRouter<T> next;
    private final String routerName;
    private final boolean runtime;

    /**
     * Constructor
     * @param routerName router name
     * @param runtime whether it's a runtime router
     */
    protected AbstractStateRouter(String routerName, boolean runtime) {
        this.routerName = routerName;
        this.runtime = runtime;
    }

    @Override
    public BitList<T> route(
            BitList<T> servers, RoutingContext ctx, boolean debugMode, List<RouterSnapshotNode<T>> snapshots) {
        // Record input size
        int inputSize = servers.size();

        // Execute specific routing logic
        BitList<T> result = doRoute(servers, ctx);

        // Record output size
        int outputSize = result.size();

        // Record snapshot (debug mode)
        if (debugMode && snapshots != null) {
            RouterSnapshotNode<T> snapshot =
                    new RouterSnapshotNode<>(routerName, inputSize, outputSize, result.toList(), buildSnapshot());
            snapshots.add(snapshot);
        }

        // If result is empty, try fallback
        if (result.isEmpty()) {
            return fallback(servers);
        }

        // If there's a next router, continue execution
        if (next != null) {
            return next.route(result, ctx, debugMode, snapshots);
        }

        return result;
    }

    /**
     * Execute specific routing logic
     * @param servers service instances list
     * @param ctx routing context
     * @return routed service instances list
     */
    protected abstract BitList<T> doRoute(BitList<T> servers, RoutingContext ctx);

    /**
     * Fallback handling
     * @param servers original service instances list
     * @return fallback service instances list
     */
    protected BitList<T> fallback(BitList<T> servers) {
        // Default fallback strategy: return original list
        LOGGER.info("Using fallback strategy for router " + routerName);
        return servers;
    }

    @Override
    public boolean isRuntime() {
        return runtime;
    }

    @Override
    public String buildSnapshot() {
        return String.format("Router: %s, Runtime: %s", routerName, runtime);
    }

    @Override
    public void setNext(StateRouter<T> next) {
        this.next = next;
    }

    @Override
    public StateRouter<T> getNext() {
        return next;
    }
}
