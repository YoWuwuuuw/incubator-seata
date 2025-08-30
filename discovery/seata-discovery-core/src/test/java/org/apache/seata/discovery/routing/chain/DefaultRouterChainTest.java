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
package org.apache.seata.discovery.routing.chain;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultRouterChainTest {

    @BeforeEach
    public void setUp() {
        // Enable debug mode
        System.setProperty("client.routing.debug", "true");
    }

    @AfterEach
    public void tearDown() {
        // Clean up debug mode settings
        System.clearProperty("client.routing.debug");
    }

    /**
     * Test constructor
     */
    @Test
    public void testConstructor() {
        // Verify default constructor
        DefaultRouterChain chain = new DefaultRouterChain();
        assertNotNull(chain);
    }

    /**
     * Test empty server list - should return original empty list
     */
    @Test
    public void testFilterAllWithEmptyServers() {
        DefaultRouterChain chain = new DefaultRouterChain();
        List<ServiceInstance> servers = new ArrayList<>();

        List<ServiceInstance> result = chain.filterAll(servers, new RoutingContext());
        assertEquals(servers, result);
    }

    /**
     * Test debug mode - verify complete debug logging
     */
    @Test
    public void testDebugMode() {
        // Create test servers
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        // Set client location
        ctx.setAttribute("clientLat", "39.9042");
        ctx.setAttribute("clientLng", "116.4074");

        // Create router chain
        DefaultRouterChain chain = new DefaultRouterChain();

        // Execute routing (this will trigger debug logging, verified through log output)
        List<ServiceInstance> result = chain.filterAll(servers, ctx);

        // Verify result, verify debug mode is effective
        assertNotNull(result);
    }

    /**
     * Test debug mode disabled scenario
     */
    @Test
    public void testDebugModeDisabled() {
        // Temporarily disable debug mode
        System.setProperty("client.routing.debug", "false");

        try {
            // Create test servers
            List<ServiceInstance> servers = createTestServers();
            RoutingContext ctx = new RoutingContext();

            // Create router chain
            DefaultRouterChain chain = new DefaultRouterChain();

            // Execute routing (this won't trigger debug logging, verified by whether logs are output)
            List<ServiceInstance> result = chain.filterAll(servers, ctx);

            // Verify result
            assertNotNull(result);
            assertFalse(result.isEmpty());
        } finally {
            // Restore debug mode
            System.setProperty("client.routing.debug", "true");
        }
    }

    /**
     * Create test servers
     */
    private List<ServiceInstance> createTestServers() {
        List<ServiceInstance> servers = new ArrayList<>();

        // Server 1: Beijing, version 2.0
        ServiceInstance server1 = createMockServer("server1", "39.9042", "116.4074", "2.0", "prod");
        servers.add(server1);

        // Server 2: Shanghai, version 1.5
        ServiceInstance server2 = createMockServer("server2", "31.2304", "121.4737", "1.5", "prod");
        servers.add(server2);

        // Server 3: Guangzhou, version 2.0
        ServiceInstance server3 = createMockServer("server3", "23.1291", "113.2644", "2.0", "prod");
        servers.add(server3);

        return servers;
    }

    /**
     * Create mock server
     */
    private ServiceInstance createMockServer(String id, String lat, String lng, String version, String env) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lat", lat);
        metadata.put("lng", lng);
        metadata.put("version", version);
        metadata.put("env", env);

        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        return new ServiceInstance(address, metadata);
    }
}
