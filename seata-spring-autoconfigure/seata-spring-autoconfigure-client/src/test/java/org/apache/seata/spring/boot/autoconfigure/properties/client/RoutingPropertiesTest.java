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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for RoutingProperties
 */
public class RoutingPropertiesTest {

    /**
     * Test default values
     */
    @Test
    public void testDefaultValues() {
        RoutingProperties properties = new RoutingProperties();

        // Verify default values
        assertFalse(properties.isEnabled());
        assertFalse(properties.isDebug());
        assertTrue(properties.isFallback());
        assertEquals("", properties.getChainOrder());
        assertTrue(properties.isRegionRouterEnabled());
        assertEquals(5, properties.getRegionRouterTopN());

        assertEquals("region-router", properties.getPrimaryBackupOrder());
        assertTrue(properties.isPrimaryBackupEnabled());
        assertEquals("", properties.getLocationLat());
        assertEquals("", properties.getLocationLng());
        assertNotNull(properties.getMetadataRouters());
        assertTrue(properties.getMetadataRouters().isEmpty());
    }

    /**
     * Test dynamic metadata router configuration
     */
    @Test
    public void testDynamicMetadataRouters() {
        RoutingProperties properties = new RoutingProperties();

        // Add metadata router configuration
        MetadataRouterConfig config1 = new MetadataRouterConfig();
        config1.setEnabled(true);
        config1.setExpression("version >= 2.0");
        properties.addMetadataRouter("metadata-router-1", config1);

        MetadataRouterConfig config2 = new MetadataRouterConfig();
        config2.setEnabled(true);
        config2.setExpression("env = prod");
        properties.addMetadataRouter("metadata-router-2", config2);

        MetadataRouterConfig customConfig = new MetadataRouterConfig();
        customConfig.setEnabled(false);
        customConfig.setExpression("region = cn-bj");
        properties.addMetadataRouter("custom-router", customConfig);

        // Verify configuration
        assertEquals(3, properties.getMetadataRouters().size());

        MetadataRouterConfig retrievedConfig1 = properties.getMetadataRouter("metadata-router-1");
        assertNotNull(retrievedConfig1);
        assertTrue(retrievedConfig1.isEnabled());
        assertEquals("version >= 2.0", retrievedConfig1.getExpression());

        MetadataRouterConfig retrievedConfig2 = properties.getMetadataRouter("metadata-router-2");
        assertNotNull(retrievedConfig2);
        assertTrue(retrievedConfig2.isEnabled());
        assertEquals("env = prod", retrievedConfig2.getExpression());

        MetadataRouterConfig retrievedCustomConfig = properties.getMetadataRouter("custom-router");
        assertNotNull(retrievedCustomConfig);
        assertFalse(retrievedCustomConfig.isEnabled());
        assertEquals("region = cn-bj", retrievedCustomConfig.getExpression());

        // Verify non-existent router returns null
        assertNull(properties.getMetadataRouter("non-existent-router"));
    }

    /**
     * Test setter and getter properties
     */
    @Test
    public void testSetAndGetProperties() {
        RoutingProperties properties = new RoutingProperties();

        // Set properties
        properties.setEnabled(true);
        properties.setDebug(true);
        properties.setFallback(false);
        properties.setChainOrder("region-router,metadata-router-1,metadata-router-2");
        properties.setRegionRouterEnabled(false);
        properties.setRegionRouterTopN(10);

        properties.setPrimaryBackupOrder("metadata-router-1");
        properties.setPrimaryBackupEnabled(false);
        properties.setLocationLat("39.9042");
        properties.setLocationLng("116.4074");

        // Verify properties
        assertTrue(properties.isEnabled());
        assertTrue(properties.isDebug());
        assertFalse(properties.isFallback());
        assertEquals("region-router,metadata-router-1,metadata-router-2", properties.getChainOrder());
        assertFalse(properties.isRegionRouterEnabled());
        assertEquals(10, properties.getRegionRouterTopN());

        assertEquals("metadata-router-1", properties.getPrimaryBackupOrder());
        assertFalse(properties.isPrimaryBackupEnabled());
        assertEquals("39.9042", properties.getLocationLat());
        assertEquals("116.4074", properties.getLocationLng());
    }

    /**
     * Test fluent API
     */
    @Test
    public void testFluentApi() {
        RoutingProperties properties = new RoutingProperties()
                .setEnabled(true)
                .setDebug(true)
                .setFallback(false)
                .setChainOrder("region-router,metadata-router-1")
                .setRegionRouterEnabled(false)
                .setRegionRouterTopN(8)
                .setPrimaryBackupOrder("region-router")
                .setPrimaryBackupEnabled(false)
                .setLocationLat("31.2304")
                .setLocationLng("121.4737");

        // Verify fluent API result
        assertTrue(properties.isEnabled());
        assertTrue(properties.isDebug());
        assertFalse(properties.isFallback());
        assertEquals("region-router,metadata-router-1", properties.getChainOrder());
        assertFalse(properties.isRegionRouterEnabled());
        assertEquals(8, properties.getRegionRouterTopN());

        assertEquals("region-router", properties.getPrimaryBackupOrder());
        assertFalse(properties.isPrimaryBackupEnabled());
        assertEquals("31.2304", properties.getLocationLat());
        assertEquals("121.4737", properties.getLocationLng());
    }
}
