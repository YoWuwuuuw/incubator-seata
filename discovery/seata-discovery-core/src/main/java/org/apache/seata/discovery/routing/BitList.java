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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * High-performance routing intermediate structure based on BitSet.
 * Each filter only marks valid bits, does not copy the array.
 *
 * @param <T> element type
 */
public class BitList<T> {

    private final List<T> originalList;
    private final BitSet validBits;
    private final int size;

    /**
     * Constructor
     *
     * @param list original list
     */
    public BitList(List<T> list) {
        this.originalList = list;
        this.size = list.size();
        this.validBits = new BitSet(size);
        this.validBits.set(0, size); // All bits are valid initially
    }

    /**
     * Create BitList from list
     *
     * @param list list
     * @param <T>  element type
     * @return BitList instance
     */
    public static <T> BitList<T> fromList(List<T> list) {
        return new BitList<>(list);
    }

    /**
     * Filter operation
     *
     * @param predicate filter condition
     * @return filtered BitList
     */
    public BitList<T> filter(Predicate<T> predicate) {
        BitList<T> result = new BitList<>(originalList);
        result.validBits.clear();
        result.validBits.or(this.validBits); // Inherit the current valid bit first
        for (int i = 0; i < size; i++) {
            if (validBits.get(i) && !predicate.test(originalList.get(i))) {
                result.validBits.clear(i);
            }
        }
        return result;
    }

    /**
     * Convert to List
     *
     * @return List of valid elements
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (validBits.get(i)) {
                result.add(originalList.get(i));
            }
        }
        return result;
    }

    /**
     * Get the number of valid elements
     *
     * @return number of valid elements
     */
    public int size() {
        return validBits.cardinality();
    }

    /**
     * Check if empty
     *
     * @return whether it is empty
     */
    public boolean isEmpty() {
        return validBits.isEmpty();
    }
}
