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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouterChainUtilsTest {

    /**
     * Test router name validation - valid and invalid router names
     */
    @Test
    public void testIsValidRouterName() {
        assertTrue(RouterChainUtils.isValidRouterName("region-router"));
        assertTrue(RouterChainUtils.isValidRouterName("metadata-router"));
        assertTrue(RouterChainUtils.isValidRouterName("metadata-router-1"));
        assertTrue(RouterChainUtils.isValidRouterName("metadata-router-2"));
        assertTrue(RouterChainUtils.isValidRouterName("custom-router"));
        assertTrue(RouterChainUtils.isValidRouterName("custom-anything"));

        assertFalse(RouterChainUtils.isValidRouterName("invalid-router"));
        assertFalse(RouterChainUtils.isValidRouterName(""));
        assertFalse(RouterChainUtils.isValidRouterName(null));
        assertFalse(RouterChainUtils.isValidRouterName("router"));
        assertFalse(RouterChainUtils.isValidRouterName("metadata"));
    }
}
