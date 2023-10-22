/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.role.v2.mgt.core.model;

/**
 * Represents the Role Audience.
 */
public class RoleAudience {

    private String audience;
    private String audienceId;
    private String audienceName;


    public RoleAudience(String audience, String audienceId) {

        this.audience = audience;
        this.audienceId = audienceId;
    }

    /**
     * Get the audience.
     *
     * @return the audience.
     */
    public String getAudience() {

        return audience;
    }

    /**
     * Set the audience.
     *
     * @param audience the audience.
     */
    public void setAudience(String audience) {

        this.audience = audience;
    }

    /**
     * Get the audience id.
     *
     * @return the audience id.
     */
    public String getAudienceId() {

        return audienceId;
    }

    /**
     * Set the audience id.
     *
     * @param audienceId the audience id.
     */
    public void setAudienceId(String audienceId) {

        this.audienceId = audienceId;
    }

    /**
     * Get the audience name.
     *
     * @return the audience name.
     */
    public String getAudienceName() {

        return audienceName;
    }

    /**
     * Set the audience name.
     *
     * @param audienceName the audience name.
     */
    public void setAudienceName(String audienceName) {

        this.audienceName = audienceName;
    }
}
