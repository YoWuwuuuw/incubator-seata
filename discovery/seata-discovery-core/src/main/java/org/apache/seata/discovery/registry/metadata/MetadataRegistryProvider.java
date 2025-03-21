package org.apache.seata.discovery.registry.metadata;

/**
 * the interface registry provider for metadata mode
 */
public interface MetadataRegistryProvider {

    /**
     * provide a registry implementation instance for metadata mode
     * @return MetadataRegistryService
     */
    MetadataRegistryService provide();
}
