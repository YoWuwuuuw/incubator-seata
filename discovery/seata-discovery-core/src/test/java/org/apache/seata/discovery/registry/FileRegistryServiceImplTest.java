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
package org.apache.seata.discovery.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FileRegistryServiceImplTest {

    private static final String TEST_GROUP = "testGroup";

    private static FileRegistryServiceImpl fileRegistryService;

    @BeforeAll
    static void setUp() {
        System.setProperty("service.vgroupMapping.testGroup", TEST_GROUP);
        fileRegistryService = FileRegistryServiceImpl.getInstance();
    }

    /**
     * Tests the getServiceGroup method to ensure it retrieves the correct service group name
     */
    @Test
    void testGetServiceGroup() {
        String result = fileRegistryService.getServiceGroup(TEST_GROUP);
        Assertions.assertEquals(TEST_GROUP, result);
        Assertions.assertTrue(RegistryService.SERVICE_GROUP_NAME.contains("service.vgroupMapping.testGroup"));
    }

    /**
     * Tests the aliveLookup and refreshAliveLookup methods.
     */
    @Test
    void testAliveLookupAndRefreshAliveLookup() {
        RegistryService.CURRENT_ADDRESS_MAP.clear();
        List<InetSocketAddress> addresses = Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091));

        // Test empty list
        List<InetSocketAddress> result = fileRegistryService.aliveLookup(TEST_GROUP);
        System.out.println(result);
        Assertions.assertTrue(result.isEmpty());

        // Test data is available
        fileRegistryService.refreshAliveLookup(TEST_GROUP, addresses);
        result = fileRegistryService.aliveLookup(TEST_GROUP);
        Assertions.assertEquals(addresses, result);
    }

    /**
     * Tests the removeOfflineAddressesIfNecessary method when there is no intersection
     */
    @Test
    void testRemoveOfflineAddressesIfNecessaryWithNoIntersection() {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8091);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.2", 8091);
        List<InetSocketAddress> currentAddresses = Collections.singletonList(address1);
        Collection<InetSocketAddress> newAddresses = Collections.singletonList(address2);

        fileRegistryService.refreshAliveLookup(TEST_GROUP, currentAddresses);
        fileRegistryService.removeOfflineAddressesIfNecessary(TEST_GROUP, fileRegistryService.getServiceGroup(TEST_GROUP), newAddresses);

        List<InetSocketAddress> result = fileRegistryService.aliveLookup(TEST_GROUP);
        assertFalse(result.isEmpty());
        assertEquals(currentAddresses, result);
    }

    /**
     * Tests the removeOfflineAddressesIfNecessary method when there is an intersection
     */
    @Test
    void testRemoveOfflineAddressesIfNecessaryWithIntersection() {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8091);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.2", 8091);
        List<InetSocketAddress> currentAddresses = Arrays.asList(address1, address2);
        Collection<InetSocketAddress> newAddresses = Collections.singletonList(address1);

        fileRegistryService.refreshAliveLookup(TEST_GROUP, currentAddresses);
        fileRegistryService.removeOfflineAddressesIfNecessary(TEST_GROUP, fileRegistryService.getServiceGroup(TEST_GROUP), newAddresses);

        List<InetSocketAddress> result = fileRegistryService.aliveLookup(TEST_GROUP);
        assertFalse(result.isEmpty());
        assertEquals(newAddresses.toString(), new HashSet<>(result).toString());
    }
} 