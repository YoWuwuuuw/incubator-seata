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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractStateRouterTest {

    /**
     * Concrete router implementation for testing
     */
    private static class TestRouter extends AbstractStateRouter<String> {

        private final boolean shouldReturnEmpty;

        public TestRouter(String routerName, boolean runtime, boolean shouldReturnEmpty) {
            super(routerName, runtime);
            this.shouldReturnEmpty = shouldReturnEmpty;
        }

        @Override
        protected BitList<String> doRoute(BitList<String> servers, RoutingContext ctx) {
            if (shouldReturnEmpty) {
                return BitList.fromList(new ArrayList<>());
            }
            return servers;
        }

        @Override
        public String buildSnapshot() {
            return "TestRouter: test-router";
        }
    }

    /**
     * Test constructor
     */
    @Test
    public void testConstructor() {
        TestRouter router = new TestRouter("test-router", false, false);
        assertNotNull(router);
    }

    /**
     * Test isRuntime method - distinguish between runtime and non-runtime routers
     */
    @Test
    public void testIsRuntime() {
        TestRouter runtimeRouter = new TestRouter("runtime-router", true, false);
        TestRouter nonRuntimeRouter = new TestRouter("non-runtime-router", false, false);

        assertTrue(runtimeRouter.isRuntime());
        assertFalse(nonRuntimeRouter.isRuntime());
    }

    /**
     * Test set and get next router - chained routing
     */
    @Test
    public void testSetAndGetNext() {
        TestRouter router1 = new TestRouter("router1", false, false);
        TestRouter router2 = new TestRouter("router2", false, false);

        assertNull(router1.getNext());
        router1.setNext(router2);
        assertEquals(router2, router1.getNext());
    }

    /**
     * Test normal routing flow - standard routing execution
     */
    @Test
    public void testRouteWithNormalFlow() {
        TestRouter router = new TestRouter("test-router", false, false);
        List<String> servers = Arrays.asList("server1", "server2", "server3");
        BitList<String> bitList = BitList.fromList(servers);
        RoutingContext ctx = new RoutingContext();

        BitList<String> result = router.route(bitList, ctx, false, null);
        assertEquals(3, result.size());
        assertTrue(result.toList().containsAll(servers));
    }

    /**
     * Test empty result scenario - trigger fallback strategy
     */
    @Test
    public void testRouteWithEmptyResult() {
        TestRouter router = new TestRouter("test-router", false, true);
        List<String> servers = Arrays.asList("server1", "server2", "server3");
        BitList<String> bitList = BitList.fromList(servers);
        RoutingContext ctx = new RoutingContext();

        BitList<String> result = router.route(bitList, ctx, false, null);
        // Should use fallback strategy, return original list
        assertEquals(3, result.size());
        assertTrue(result.toList().containsAll(servers));
    }

    /**
     * Test debug mode - verify debug logging
     */
    @Test
    public void testRouteWithDebugMode() {
        TestRouter router = new TestRouter("test-router", false, false);
        List<String> servers = Arrays.asList("server1", "server2");
        BitList<String> bitList = BitList.fromList(servers);
        RoutingContext ctx = new RoutingContext();
        List<RouterSnapshotNode<String>> snapshots = new ArrayList<>();

        BitList<String> result = router.route(bitList, ctx, true, snapshots);
        assertEquals(2, result.size());

        // Verify debug logging
        assertEquals(1, snapshots.size());
        RouterSnapshotNode<String> snapshot = snapshots.get(0);
        assertEquals("test-router", snapshot.getRouterName());
        assertEquals(2, snapshot.getInputSize());
        assertEquals(2, snapshot.getOutputSize());
        assertEquals(2, snapshot.getSelectedServers().size());
        assertTrue(snapshot.getSnapshot().contains("TestRouter"));
    }

    /**
     * Test chained routing - multiple routers execute in series
     */
    @Test
    public void testRouteWithNextRouter() {
        TestRouter router1 = new TestRouter("router1", false, false);
        TestRouter router2 = new TestRouter("router2", false, false);
        router1.setNext(router2);

        List<String> servers = Arrays.asList("server1", "server2");
        BitList<String> bitList = BitList.fromList(servers);
        RoutingContext ctx = new RoutingContext();

        BitList<String> result = router1.route(bitList, ctx, false, null);
        assertEquals(2, result.size());
    }

    /**
     * Test build snapshot - verify snapshot information
     */
    @Test
    public void testBuildSnapshot() {
        TestRouter router = new TestRouter("test-router", true, false);
        String snapshot = router.buildSnapshot();
        assertTrue(snapshot.contains("TestRouter"));
        assertTrue(snapshot.contains("test-router"));
    }
}
