package org.apache.seata.discovery.registry.metadata;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * pojo for MetadataRegistryService cache
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
}
