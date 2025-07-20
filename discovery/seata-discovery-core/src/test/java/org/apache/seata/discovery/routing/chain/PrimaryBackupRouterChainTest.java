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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrimaryBackupRouterChainTest {

    /**
     * Test default constructor
     */
    @Test
    public void testConstructor() {
        PrimaryBackupRouterChain chain = new PrimaryBackupRouterChain();
        assertNotNull(chain);
    }

    /**
     * Test empty server list - should return original empty list
     */
    @Test
    public void testFilterAllWithEmptyServers() {
        PrimaryBackupRouterChain chain = new PrimaryBackupRouterChain();
        List<ServiceInstance> servers = new ArrayList<>();

        List<ServiceInstance> result = chain.filterAll(servers, new RoutingContext());
        assertEquals(servers, result);
    }

    /**
     * Test null server list - should return null
     */
    @Test
    public void testFilterAllWithNullServers() {
        PrimaryBackupRouterChain chain = new PrimaryBackupRouterChain();

        List<ServiceInstance> result = chain.filterAll(null, new RoutingContext());
        assertNull(result);
    }

    /**
     * Test router order validation - valid and invalid configurations
     */
    @Test
    public void testIsValidRouterOrder() {
        assertTrue(PrimaryBackupRouterChain.isValidRouterOrder("region-router,metadata-router"));
        assertTrue(PrimaryBackupRouterChain.isValidRouterOrder("metadata-router-1,metadata-router-2"));
        assertTrue(PrimaryBackupRouterChain.isValidRouterOrder("custom-router"));

        assertFalse(PrimaryBackupRouterChain.isValidRouterOrder("invalid-router"));
        assertFalse(PrimaryBackupRouterChain.isValidRouterOrder(""));
        assertFalse(PrimaryBackupRouterChain.isValidRouterOrder(null));
    }
}
