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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataRouterConfigTest {

    @Test
    public void testDefaultValues() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        assertTrue(config.isEnabled());
        assertEquals("", config.getExpression());
    }

    @Test
    public void testSetAndGetProperties() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        config.setEnabled(false);
        config.setExpression("version >= 2.0");

        assertFalse(config.isEnabled());
        assertEquals("version >= 2.0", config.getExpression());
    }

    @Test
    public void testFluentApi() {
        MetadataRouterConfig config =
                new MetadataRouterConfig().setEnabled(true).setExpression("env = prod");

        assertTrue(config.isEnabled());
        assertEquals("env = prod", config.getExpression());
    }

    @Test
    public void testComplexExpression() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        String complexExpression = "(version >= 2.0) | (env = dev) | (region = cn-bj)";
        config.setExpression(complexExpression);

        assertEquals(complexExpression, config.getExpression());
    }

    @Test
    public void testEmptyExpression() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        config.setExpression("");
        assertEquals("", config.getExpression());

        config.setExpression(null);
        assertNull(config.getExpression());
    }
}
