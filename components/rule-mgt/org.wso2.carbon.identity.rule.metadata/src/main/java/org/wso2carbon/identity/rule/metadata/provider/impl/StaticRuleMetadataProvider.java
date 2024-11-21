/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2carbon.identity.rule.metadata.provider.impl;

import org.wso2carbon.identity.rule.metadata.config.RuleMetadataConfigFactory;
import org.wso2carbon.identity.rule.metadata.exception.RuleMetadataConfigException;
import org.wso2carbon.identity.rule.metadata.exception.RuleMetadataException;
import org.wso2carbon.identity.rule.metadata.exception.RuleMetadataServerException;
import org.wso2carbon.identity.rule.metadata.model.FieldDefinition;
import org.wso2carbon.identity.rule.metadata.model.FlowType;
import org.wso2carbon.identity.rule.metadata.provider.RuleMetadataProvider;

import java.util.List;

public class StaticRuleMetadataProvider implements RuleMetadataProvider {

    private static final String OPERATOR_CONFIG_PATH = "operators.json";
    private static final String FIELD_DEFINITION_CONFIG_PATH = "fields.json";
    private static final String FLOW_CONFIG_PATH = "flows.json";

    private StaticRuleMetadataProvider() {

    }

    public static StaticRuleMetadataProvider loadStaticMetadata() throws RuleMetadataServerException {

        String operatorConfigPath =
                StaticRuleMetadataProvider.class.getClassLoader().getResource("operators.json").getFile();
        String fieldDefinitionConfigPath =
                StaticRuleMetadataProvider.class.getClassLoader().getResource("fields.json").getFile();
        String flowConfigPath = StaticRuleMetadataProvider.class.getClassLoader().getResource("flows.json").getFile();

        try {
            RuleMetadataConfigFactory.load(operatorConfigPath, fieldDefinitionConfigPath, flowConfigPath);
        } catch (RuleMetadataConfigException e) {
            throw new RuleMetadataServerException("Error while loading static rule metadata.", e);
        }

        return new StaticRuleMetadataProvider();
    }

    @Override
    public List<FieldDefinition> getExpressionMeta(FlowType flowType, String tenantDomain)
            throws RuleMetadataException {

        return RuleMetadataConfigFactory.getFlowConfig().getFieldDefinitionsForFlow(flowType);
    }
}
