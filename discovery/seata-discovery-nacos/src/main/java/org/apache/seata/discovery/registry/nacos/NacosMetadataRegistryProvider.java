package org.apache.seata.discovery.registry.nacos;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.discovery.registry.metadata.MetadataRegistryProvider;

@LoadLevel(name = "Nacos", order = 1)
public class NacosMetadataRegistryProvider implements MetadataRegistryProvider {
    @Override
    public NacosMetadataRegistryServiceImpl provide() {
        return NacosMetadataRegistryServiceImpl.getInstance();
    }
}
