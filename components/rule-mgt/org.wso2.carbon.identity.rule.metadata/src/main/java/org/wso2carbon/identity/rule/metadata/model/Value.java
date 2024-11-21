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

package org.wso2carbon.identity.rule.metadata.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.wso2carbon.identity.rule.metadata.deserializers.ValueTypeDeserializer;

public abstract class Value {

    private InputType inputType;
    @JsonDeserialize(using = ValueTypeDeserializer.class)
    private ValueType valueType;

    public Value(InputType inputType, ValueType valueType) {

        this.inputType = inputType;
        this.valueType = valueType;
    }

    public InputType getInputType() {

        return inputType;
    }

    public ValueType getValueType() {

        return valueType;
    }

    public enum InputType {
        INPUT, OPTIONS,
    }

    public enum ValueType {
        STRING, NUMBER, BOOLEAN, DATE_TIME, REFERENCE
    }
}
