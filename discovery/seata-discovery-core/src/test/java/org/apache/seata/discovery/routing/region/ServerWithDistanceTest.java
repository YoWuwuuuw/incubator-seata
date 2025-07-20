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
package org.apache.seata.discovery.routing.region;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ServerWithDistance test
 */
public class ServerWithDistanceTest {

    /**
     * Test constructor and getter methods - verify server and distance setting/getting
     */
    @Test
    public void testConstructorAndGetters() {
        String server = "test-server";
        double distance = 100.5;
        ServerWithDistance<String> serverWithDistance = new ServerWithDistance<>(server, distance);

        assertEquals(server, serverWithDistance.getServer());
        assertEquals(distance, serverWithDistance.getDistance(), 0.001);
    }

    /**
     * Test with null server - verify null value handling
     */
    @Test
    public void testWithNullServer() {
        ServerWithDistance<String> serverWithDistance = new ServerWithDistance<>(null, 100.0);
        assertNull(serverWithDistance.getServer());
        assertEquals(100.0, serverWithDistance.getDistance(), 0.001);
    }

    /**
     * Test with zero distance - verify zero value handling
     */
    @Test
    public void testWithZeroDistance() {
        String server = "test-server";
        ServerWithDistance<String> serverWithDistance = new ServerWithDistance<>(server, 0.0);
        assertEquals(server, serverWithDistance.getServer());
        assertEquals(0.0, serverWithDistance.getDistance(), 0.001);
    }

    /**
     * Test with negative distance - verify negative value handling
     */
    @Test
    public void testWithNegativeDistance() {
        String server = "test-server";
        ServerWithDistance<String> serverWithDistance = new ServerWithDistance<>(server, -50.0);
        assertEquals(server, serverWithDistance.getServer());
        assertEquals(-50.0, serverWithDistance.getDistance(), 0.001);
    }
}
