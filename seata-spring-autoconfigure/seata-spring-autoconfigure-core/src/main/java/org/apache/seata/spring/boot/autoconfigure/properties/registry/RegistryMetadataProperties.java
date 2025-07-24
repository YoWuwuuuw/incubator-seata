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
package org.apache.seata.spring.boot.autoconfigure.properties.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_METADATA_PREFIX;

@Component
@ConfigurationProperties(prefix = REGISTRY_METADATA_PREFIX)
public class RegistryMetadataProperties {
    private String external;

    /**
     * Dynamic metadata configuration map
     * This allows loading all metadata properties dynamically
     */
    private Map<String, String> metadata = new ConcurrentHashMap<>();

    public String getExternal() {
        return external;
    }

    public RegistryMetadataProperties setExternal(String external) {
        this.external = external;
        return this;
    }

    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }

    public RegistryMetadataProperties setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Set metadata value by key
     * @param key metadata key
     * @param value metadata value
     */
    public void setMetadataValue(String key, String value) {
        metadata.put(key, value);
    }

    /**
     * Get location latitude
     * @return latitude value
     */
    public String getLat() {
        return metadata.get("lat");
    }

    /**
     * Set location latitude
     * @param lat latitude value
     */
    public void setLat(String lat) {
        metadata.put("lat", lat);
    }

    /**
     * Get location longitude
     * @return longitude value
     */
    public String getLng() {
        return metadata.get("lng");
    }

    /**
     * Set location longitude
     * @param lng longitude value
     */
    public void setLng(String lng) {
        metadata.put("lng", lng);
    }
}
