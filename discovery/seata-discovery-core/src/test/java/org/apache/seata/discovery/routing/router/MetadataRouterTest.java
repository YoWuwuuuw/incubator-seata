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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataRouterTest {

    /**
     * Test constructor
     */
    @Test
    public void testConstructor() {
        // Default constructor
        MetadataRouter router = new MetadataRouter();
        assertNotNull(router);

        // Named constructor
        router = new MetadataRouter("metadata-router-1");
        assertNotNull(router);
    }

    /**
     * Test set and get expression
     */
    @Test
    public void testSetAndGetExpression() {
        MetadataRouter router = new MetadataRouter();
        router.setExpression("version >= 2.0");
        assertEquals("version >= 2.0", router.getExpression());
    }

    /**
     * Test build snapshot
     */
    @Test
    public void testBuildSnapshot() {
        MetadataRouter router = new MetadataRouter();
        router.setExpression("version >= 2.0");
        String snapshot = router.buildSnapshot();
        assertTrue(snapshot.contains("MetadataRouter"));
        assertTrue(snapshot.contains("version >= 2.0"));
    }

    /**
     * Test empty expression - should return original server list
     */
    @Test
    public void testDoRouteWithEmptyExpression() {
        MetadataRouter router = new MetadataRouter();
        router.setExpression("");

        ServiceInstance server = mock(ServiceInstance.class);
        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server));
        RoutingContext ctx = new RoutingContext();

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(servers, result);
    }

    /**
     * Test single expression - servers satisfying conditions pass through
     */
    @Test
    public void testDoRouteWithSingleExpression() {
        MetadataRouter router = new MetadataRouter();
        router.setExpression("version >= 2.0");

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata1.put("version", "2.5");
        metadata2.put("version", "1.5");
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server1, server2));
        RoutingContext ctx = new RoutingContext();

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(1, result.size());
        assertTrue(result.toList().contains(server1));
    }

    /**
     * Test OR expression - servers satisfying any condition pass through
     */
    @Test
    public void testDoRouteWithOrExpression() {
        MetadataRouter router = new MetadataRouter();
        router.setExpression("(version >= 2.0) | (env = dev)");

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        ServiceInstance server3 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        Map<String, Object> metadata3 = new HashMap<>();
        metadata1.put("version", "1.5"); // Doesn't satisfy version condition
        metadata1.put("env", "prod"); // Doesn't satisfy env condition
        metadata2.put("version", "2.5"); // Satisfies version condition
        metadata3.put("env", "dev"); // Satisfies env condition
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);
        when(server3.getMetadata()).thenReturn(metadata3);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server1, server2, server3));
        RoutingContext ctx = new RoutingContext();

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(2, result.size());
        assertTrue(result.toList().contains(server2));
        assertTrue(result.toList().contains(server3));
    }

    /**
     * Test string comparison - exact match
     */
    @Test
    public void testDoRouteWithStringComparison() {
        MetadataRouter router = new MetadataRouter();
        router.setExpression("env = prod");

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata1.put("env", "prod");
        metadata2.put("env", "dev");
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server1, server2));
        RoutingContext ctx = new RoutingContext();

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(1, result.size());
        assertTrue(result.toList().contains(server1));
    }

    /**
     * Test missing metadata scenario - servers without version metadata should be filtered out
     */
    @Test
    public void testDoRouteWithMissingMetadata() {
        MetadataRouter router = new MetadataRouter();
        router.setExpression("version >= 2.0");

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        ServiceInstance server3 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata1.put("version", "2.5");
        // server2 has no version metadata
        // server3 has null metadata
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);
        when(server3.getMetadata()).thenReturn(null);

        BitList<ServiceInstance> servers = BitList.fromList(java.util.Arrays.asList(server1, server2, server3));
        RoutingContext ctx = new RoutingContext();

        BitList<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(1, result.size()); // Only server1 should pass through
        assertTrue(result.toList().contains(server1));
        assertFalse(result.toList().contains(server2));
        assertFalse(result.toList().contains(server3));
    }
}
