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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouterSnapshotNodeTest {

    /**
     * Test constructor - create snapshot node
     */
    @Test
    public void testConstructor() {
        String routerName = "test-router";
        int inputSize = 5;
        int outputSize = 3;
        List<String> selectedServers = Arrays.asList("server1", "server2", "server3");
        String snapshot = "test-snapshot";

        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>(routerName, inputSize, outputSize, selectedServers, snapshot);

        assertNotNull(node);
    }

    /**
     * Test getRouterName
     */
    @Test
    public void testGetRouterName() {
        String routerName = "test-router";
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>(routerName, 5, 3, Arrays.asList("server1"), "snapshot");

        assertEquals(routerName, node.getRouterName());
    }

    /**
     * Test getInputSize
     */
    @Test
    public void testGetInputSize() {
        int inputSize = 10;
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>("router", inputSize, 5, Arrays.asList("server1"), "snapshot");

        assertEquals(inputSize, node.getInputSize());
    }

    /**
     * Test getOutputSize
     */
    @Test
    public void testGetOutputSize() {
        int outputSize = 7;
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>("router", 10, outputSize, Arrays.asList("server1"), "snapshot");

        assertEquals(outputSize, node.getOutputSize());
    }

    /**
     * Test getSelectedServers
     */
    @Test
    public void testGetSelectedServers() {
        List<String> selectedServers = Arrays.asList("server1", "server2", "server3");
        RouterSnapshotNode<String> node = new RouterSnapshotNode<>("router", 5, 3, selectedServers, "snapshot");

        assertEquals(selectedServers, node.getSelectedServers());
        assertEquals(3, node.getSelectedServers().size());
    }

    /**
     * Test getSnapshot
     */
    @Test
    public void testGetSnapshot() {
        String snapshot = "test-snapshot-info";
        RouterSnapshotNode<String> node = new RouterSnapshotNode<>("router", 5, 3, Arrays.asList("server1"), snapshot);

        assertEquals(snapshot, node.getSnapshot());
    }

    /**
     * Test getTimestamp - verify timestamp accuracy
     */
    @Test
    public void testGetTimestamp() {
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>("router", 5, 3, Arrays.asList("server1"), "snapshot");

        long timestamp = node.getTimestamp();
        assertTrue(timestamp > 0);

        // Timestamp should be within a reasonable range (a few seconds from now)
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(timestamp - currentTime) < 1000);
    }

    /**
     * Test toString - verify formatted output
     */
    @Test
    public void testToString() {
        String routerName = "test-router";
        int inputSize = 5;
        int outputSize = 3;
        List<String> selectedServers = Arrays.asList("server1", "server2");
        String snapshot = "test-snapshot";

        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>(routerName, inputSize, outputSize, selectedServers, snapshot);

        String result = node.toString();
        assertTrue(result.contains(routerName));
        assertTrue(result.contains(String.valueOf(inputSize)));
        assertTrue(result.contains(String.valueOf(outputSize)));
        assertTrue(result.contains(snapshot));
        assertTrue(result.contains("server1"));
        assertTrue(result.contains("server2"));
    }

    /**
     * Test with empty selected server list - edge case
     */
    @Test
    public void testWithEmptySelectedServers() {
        List<String> emptyServers = Arrays.asList();
        RouterSnapshotNode<String> node = new RouterSnapshotNode<>("router", 5, 0, emptyServers, "snapshot");

        assertEquals(0, node.getOutputSize());
        assertEquals(0, node.getSelectedServers().size());
    }

    /**
     * Test with null snapshot - edge case
     */
    @Test
    public void testWithNullSnapshot() {
        RouterSnapshotNode<String> node = new RouterSnapshotNode<>("router", 5, 3, Arrays.asList("server1"), null);

        assertNull(node.getSnapshot());
    }

    /**
     * Test with Integer type - generic support
     */
    @Test
    public void testWithIntegerType() {
        List<Integer> selectedServers = Arrays.asList(1, 2, 3);
        RouterSnapshotNode<Integer> node = new RouterSnapshotNode<>("router", 5, 3, selectedServers, "snapshot");

        assertEquals(selectedServers, node.getSelectedServers());
        assertEquals(3, node.getSelectedServers().size());
    }
}
