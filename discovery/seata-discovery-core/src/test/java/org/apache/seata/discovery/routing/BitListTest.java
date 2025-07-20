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
package org.apache.seata.discovery.routing;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitListTest {

    /**
     * Test creating BitList from list - verify creation and size
     */
    @Test
    public void testFromList() {
        List<String> list = Arrays.asList("a", "b", "c");
        BitList<String> bitList = BitList.fromList(list);
        assertEquals(3, bitList.size());
    }

    /**
     * Test converting to list - verify data integrity
     */
    @Test
    public void testToList() {
        List<String> original = Arrays.asList("a", "b", "c");
        BitList<String> bitList = BitList.fromList(original);
        List<String> result = bitList.toList();
        assertEquals(original, result);
    }

    /**
     * Test filter functionality - verify conditional filtering
     */
    @Test
    public void testFilter() {
        List<String> original = Arrays.asList("a", "b", "c", "d");
        BitList<String> bitList = BitList.fromList(original);

        BitList<String> filtered = bitList.filter(item -> item.equals("a") || item.equals("c"));
        assertEquals(2, filtered.size());
        assertTrue(filtered.toList().contains("a"));
        assertTrue(filtered.toList().contains("c"));
    }

    /**
     * Test empty list - verify empty state check
     */
    @Test
    public void testIsEmpty() {
        BitList<String> emptyList = new BitList<>(Arrays.asList());
        assertTrue(emptyList.isEmpty());

        BitList<String> nonEmptyList = BitList.fromList(Arrays.asList("a"));
        assertFalse(nonEmptyList.isEmpty());
    }

    /**
     * Test size - verify element count
     */
    @Test
    public void testSize() {
        BitList<String> bitList = BitList.fromList(Arrays.asList("a", "b", "c"));
        assertEquals(3, bitList.size());
    }
}
