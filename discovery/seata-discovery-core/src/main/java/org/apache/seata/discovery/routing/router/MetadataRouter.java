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
package org.apache.seata.discovery.routing.router;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.BitList;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.config.RoutingProperties;
import org.apache.seata.discovery.routing.expression.ConditionMatcher;
import org.apache.seata.discovery.routing.expression.ExpressionParser;

import java.util.List;

/**
 * Metadata router
 *
 * Supports two modes:
 * 1. Single expression: version >= 2.0 (AND logic)
 * 2. OR logic expression: (version >= 2.0) | (env = dev) | (region = cn-bj) (OR logic)
 *
 * Note: AND logic is implemented by configuring multiple MetadataRouters
 */
public class MetadataRouter extends AbstractStateRouter<ServiceInstance> {

    private volatile String expression;

    /**
     * Default constructor
     */
    public MetadataRouter() {
        super("MetadataRouter", false);
        this.expression = RoutingProperties.getMetadataRouterExpression();
    }

    /**
     * Named instance constructor
     * @param routerName router name
     */
    public MetadataRouter(String routerName) {
        super(routerName, false);
        this.expression = RoutingProperties.getMetadataRouterExpression(routerName);
    }

    @Override
    protected BitList<ServiceInstance> doRoute(BitList<ServiceInstance> servers, RoutingContext ctx) {
        // Create a local copy to ensure consistency during method execution
        String currentExpression = this.expression;

        // If expression is empty or contains only spaces, return original server list directly
        if (currentExpression == null || currentExpression.trim().isEmpty()) {
            return servers;
        }

        // Parse expression
        List<ConditionMatcher> matchers = ExpressionParser.parse(currentExpression);

        // Check if it's an OR expression
        if (ExpressionParser.isOrExpression(currentExpression)) {
            // OR logic: any condition satisfied is sufficient
            return servers.filter(server -> matchers.stream().anyMatch(m -> m.match(server, ctx)));
        } else {
            // Single expression: all conditions must be satisfied (though there's only one condition)
            return servers.filter(server -> matchers.stream().allMatch(m -> m.match(server, ctx)));
        }
    }

    /**
     * Set expression
     * @param expression expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Get expression
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    @Override
    public String buildSnapshot() {
        return String.format(
                "MetadataRouter: expression=%s, isOr=%s", expression, ExpressionParser.isOrExpression(expression));
    }
}
