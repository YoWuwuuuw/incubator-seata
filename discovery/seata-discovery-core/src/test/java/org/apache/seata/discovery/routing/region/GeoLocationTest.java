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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeoLocationTest {

    /**
     * Test constructor and getter methods - verify coordinate setting and retrieval
     */
    @Test
    public void testConstructorAndGetters() {
        GeoLocation location = new GeoLocation(39.9042, 116.4074);
        assertEquals(39.9042, location.getLatitude(), 0.0001);
        assertEquals(116.4074, location.getLongitude(), 0.0001);
    }

    /**
     * Test toString method - verify formatted output
     */
    @Test
    public void testToString() {
        GeoLocation location = new GeoLocation(39.9042, 116.4074);
        String result = location.toString();
        assertTrue(result.contains("39.904200"));
        assertTrue(result.contains("116.407400"));
        assertTrue(result.contains("Location"));
    }

    /**
     * Test negative coordinates - verify negative value handling
     */
    @Test
    public void testNegativeCoordinates() {
        GeoLocation location = new GeoLocation(-39.9042, -116.4074);
        assertEquals(-39.9042, location.getLatitude(), 0.0001);
        assertEquals(-116.4074, location.getLongitude(), 0.0001);
    }
}
