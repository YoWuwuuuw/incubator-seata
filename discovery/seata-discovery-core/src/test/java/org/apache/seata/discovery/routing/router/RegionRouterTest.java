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

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.BitList;
import org.apache.seata.discovery.routing.RoutingContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegionRouterTest {

    /**
     * Test default constructor
     */
    @Test
    public void testConstructor() {
        RegionRouter router = new RegionRouter();
        assertNotNull(router);
    }

    /**
     * Test build snapshot - verify snapshot information
     */
    @Test
    public void testBuildSnapshot() {
        RegionRouter router = new RegionRouter();
        String snapshot = router.buildSnapshot();
        assertTrue(snapshot.contains("RegionRouter"));
        assertTrue(snapshot.contains("regionTopN"));
    }

    /**
     * Test routing with client location - location-based routing
     */
    @Test
    public void testDoRouteWithClientLocation() {
        RegionRouter router = new RegionRouter();

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata1.put("lat", "39.9042");
        metadata1.put("lng", "116.4074");
        metadata2.put("lat", "31.2304");
        metadata2.put("lng", "121.4737");
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server1, server2));
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("clientLat", "39.9042");
        ctx.setAttribute("clientLng", "116.4074");

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(2, result.size()); // Should return all servers, sorted by distance

        // Verify that servers are sorted by distance (server1 should be first as it has the same location as client)
        assertEquals(server1, result.toList().get(0));
        assertEquals(server2, result.toList().get(1));
    }

    /**
     * Test routing without client location - return original server list
     */
    @Test
    public void testDoRouteWithoutClientLocation() {
        RegionRouter router = new RegionRouter();

        ServiceInstance server = mock(ServiceInstance.class);
        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server));
        RoutingContext ctx = new RoutingContext();

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(servers, result); // Should return original server list
    }

    /**
     * Test routing with invalid client location - return original server list
     */
    @Test
    public void testDoRouteWithInvalidClientLocation() {
        RegionRouter router = new RegionRouter();

        ServiceInstance server = mock(ServiceInstance.class);
        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server));
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("clientLat", "invalid");
        ctx.setAttribute("clientLng", "invalid");

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(servers, result); // Should return original server list
    }

    /**
     * Test routing with server location information - sort by distance
     */
    @Test
    public void testDoRouteWithServerLocation() {
        RegionRouter router = new RegionRouter();

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata1.put("lat", "39.9042");
        metadata1.put("lng", "116.4074");
        metadata2.put("lat", "31.2304");
        metadata2.put("lng", "121.4737");
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server1, server2));
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("clientLat", "39.9042");
        ctx.setAttribute("clientLng", "116.4074");

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(2, result.size()); // Should return all servers, sorted by distance
        // server1 should be first (closer distance)
        assertEquals(server1, result.toList().get(0));
    }

    /**
     * Test routing with server without location information - prioritize servers with location info
     */
    @Test
    public void testDoRouteWithServerWithoutLocation() {
        RegionRouter router = new RegionRouter();

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata1.put("lat", "39.9042");
        metadata1.put("lng", "116.4074");
        // server2 has no location information
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server1, server2));
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("clientLat", "39.9042");
        ctx.setAttribute("clientLng", "116.4074");

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(2, result.size());
        // server1 should be first (has location information)
        assertEquals(server1, result.toList().get(0));
    }

    /**
     * Test routing with invalid server location information - return original server list
     */
    @Test
    public void testDoRouteWithInvalidServerLocation() {
        RegionRouter router = new RegionRouter();

        ServiceInstance server = mock(ServiceInstance.class);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lat", "invalid");
        metadata.put("lng", "invalid");
        when(server.getMetadata()).thenReturn(metadata);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server));
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("clientLat", "39.9042");
        ctx.setAttribute("clientLng", "116.4074");

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(1, result.size()); // Should return original server list
    }
}
