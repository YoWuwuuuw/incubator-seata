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

import static org.junit.jupiter.api.Assertions.*;

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
        assertNotNull(properties.getMetadataRouters());
        assertTrue(properties.getMetadataRouters().isEmpty());
    }

    /**
     * Test dynamic metadata router configuration
     */
    @Test
    public void testDynamicMetadataRouters() {
        RoutingProperties properties = new RoutingProperties();

        // Create metadata router configurations
        MetadataRouterConfig config1 = new MetadataRouterConfig();
        config1.setEnabled(true);
        config1.setExpression("env == 'prod'");

        MetadataRouterConfig config2 = new MetadataRouterConfig();
        config2.setEnabled(true);
        config2.setExpression("version == '1.0.0'");

        // Add configurations
        properties.addMetadataRouter("metadata-router-1", config1);
        properties.addMetadataRouter("metadata-router-2", config2);

        // Verify configurations
        assertEquals(2, properties.getMetadataRouters().size());
        assertEquals(config1, properties.getMetadataRouter("metadata-router-1"));
        assertEquals(config2, properties.getMetadataRouter("metadata-router-2"));
        assertNull(properties.getMetadataRouter("non-existent"));
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

        // Verify properties
        assertTrue(properties.isEnabled());
        assertTrue(properties.isDebug());
        assertFalse(properties.isFallback());
    }

    /**
     * Test fluent API
     */
    @Test
    public void testFluentApi() {
        RoutingProperties properties =
                new RoutingProperties().setEnabled(true).setDebug(true).setFallback(false);

        // Verify fluent API result
        assertTrue(properties.isEnabled());
        assertTrue(properties.isDebug());
        assertFalse(properties.isFallback());
    }
}
