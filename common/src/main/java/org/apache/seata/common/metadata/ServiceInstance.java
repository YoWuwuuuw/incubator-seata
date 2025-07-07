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
package org.apache.seata.common.metadata;

import org.apache.seata.common.util.NetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * entity for packaging inetSocketAddress and metadata for loadBalance
 */
public class ServiceInstance {
    private InetSocketAddress address;
    private Map<String, String> metadata;

    public ServiceInstance(InetSocketAddress address, Map<String, String> metadata) {
        this.address = address;
        this.metadata = metadata;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    /**
     * Converts a list of InetSocketAddress to a list of ServiceInstance.
     * @param addresses list of InetSocketAddress
     * @return list of ServiceInstance
     */
    public static List<ServiceInstance> convertToServiceInstanceList(List<InetSocketAddress> addresses) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        if (addresses != null && !addresses.isEmpty()) {
            for (InetSocketAddress address : addresses) {
                NetUtil.validAddress(address);
                serviceInstances.add(new ServiceInstance(address, null));
            }
        }
        return serviceInstances;
    }

    /**
     * Converts a list of ServiceInstance to a list of InetSocketAddress.
     * @param serviceInstances list of ServiceInstance
     * @return list of InetSocketAddress
     */
    public static List<InetSocketAddress> convertToInetSocketAddressList(List<ServiceInstance> serviceInstances) {
        List<InetSocketAddress> addresses = new ArrayList<>();
        if (serviceInstances != null && !serviceInstances.isEmpty()) {
            for (ServiceInstance serviceInstance : serviceInstances) {
                if (serviceInstance != null && serviceInstance.getAddress() != null) {
                    InetSocketAddress address = serviceInstance.getAddress();
                    NetUtil.validAddress(address);
                    addresses.add(address);
                }
            }
        }
        return addresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return Objects.equals(address, that.address) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, metadata);
    }

    @Override
    public String toString() {
        return "ServiceInstance{" + "address=" + address + ", metadata=" + metadata + '}';
    }
}
