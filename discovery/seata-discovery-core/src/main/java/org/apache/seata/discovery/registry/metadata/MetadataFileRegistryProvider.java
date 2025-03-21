package org.apache.seata.discovery.registry.metadata;

import org.apache.seata.common.loader.LoadLevel;

@LoadLevel(name = "File", order = 1)
public class MetadataFileRegistryProvider implements MetadataRegistryProvider {

    @Override
    public MetadataRegistryService provide() {
        // TODO:add impl return
        return null;
    }
}
