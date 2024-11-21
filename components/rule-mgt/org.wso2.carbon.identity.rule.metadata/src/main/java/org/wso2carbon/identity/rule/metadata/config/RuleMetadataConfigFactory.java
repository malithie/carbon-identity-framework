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

package org.wso2carbon.identity.rule.metadata.config;

import org.wso2carbon.identity.rule.metadata.exception.RuleMetadataConfigException;

public class RuleMetadataConfigFactory {

    private static OperatorConfig operatorConfig;
    private static FieldDefinitionConfig fieldDefinitionConfig;

    private static FlowConfig flowConfig;

    public static void load(String operatorConfigPath, String fieldDefinitionConfigPath, String flowConfigPath)
            throws RuleMetadataConfigException {

        operatorConfig = OperatorConfig.load(operatorConfigPath);
        fieldDefinitionConfig = FieldDefinitionConfig.load(fieldDefinitionConfigPath, operatorConfig);
        flowConfig = FlowConfig.load(flowConfigPath, fieldDefinitionConfig);
    }

    public static OperatorConfig getOperatorConfig() {

        return operatorConfig;
    }

    public static FieldDefinitionConfig getFieldDefinitionConfig() {

        return fieldDefinitionConfig;
    }

    public static FlowConfig getFlowConfig() {

        return flowConfig;
    }
}
