/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.idp.mgt;

import org.mockito.Mock;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.ApplicationAuthenticatorService;
import org.wso2.carbon.identity.application.common.ProvisioningConnectorService;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementServiceImpl;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.common.testng.WithAxisConfiguration;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.WithKeyStore;
import org.wso2.carbon.identity.common.testng.WithRealmService;
import org.wso2.carbon.identity.common.testng.WithRegistry;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.idp.mgt.internal.IdpMgtServiceComponentHolder;
import org.wso2.carbon.idp.mgt.util.MetadataConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static java.lang.Boolean.TRUE;

/**
 * Unit tests for IdentityProviderManagementService.
 */
@Test
@WithAxisConfiguration
@WithCarbonHome
@WithRegistry
@WithRealmService(injectToSingletons = {IdpMgtServiceComponentHolder.class}, initUserStoreManager = true)
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/h2.sql"})
@WithKeyStore
public class IdentityProviderManagementServiceTest extends PowerMockTestCase {

    @Mock
    MetadataConverter mockMetadataConverter;

    @BeforeMethod
    public void setUp() throws Exception {

        List<MetadataConverter> metadataConverterList = Arrays.asList(mockMetadataConverter);
        IdpMgtServiceComponentHolder.getInstance().setMetadataConverters(metadataConverterList);
    }

    @AfterMethod
    public void tearDown() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        removeTestIdps(identityProviderManagementService);                  //Clear Database after every test
    }

    @DataProvider
    public Object[][] addIdPData() {

        //Initialize Test Identity Provider 1
        IdentityProvider idp1 = new IdentityProvider();
        idp1.setIdentityProviderName("testIdP1");
        idp1.setEnable(true);
        idp1.setPrimary(true);
        idp1.setFederationHub(true);
        idp1.setCertificate("");

        RoleMapping rm1 = new RoleMapping(new LocalRole("1", "LocalRole1"), "Role1");
        RoleMapping rm2 = new RoleMapping(new LocalRole("2", "LocalRole2"), "Role2");
        PermissionsAndRoleConfig prc = new PermissionsAndRoleConfig();
        prc.setIdpRoles(new String[]{"Role1", "Role2"});
        prc.setRoleMappings(new RoleMapping[]{rm1, rm2});
        idp1.setPermissionAndRoleConfig(prc);

        FederatedAuthenticatorConfig fac = new FederatedAuthenticatorConfig();
        fac.setDisplayName("DisplayName1");
        fac.setName("Name");
        fac.setEnabled(true);
        Property property1 = new Property();
        property1.setName("Property1");
        property1.setValue("value1");
        property1.setConfidential(false);
        Property property2 = new Property();
        property2.setName("Property2");
        property2.setValue("value2");
        property2.setConfidential(true);
        fac.setProperties(new Property[]{property1, property2});
        idp1.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{fac});

        ProvisioningConnectorConfig pcc1 = new ProvisioningConnectorConfig();
        pcc1.setName("ProvisiningConfig1");
        pcc1.setProvisioningProperties(new Property[]{property1});
        ProvisioningConnectorConfig pcc2 = new ProvisioningConnectorConfig();
        pcc2.setName("ProvisiningConfig2");
        pcc2.setProvisioningProperties(new Property[]{property2});
        pcc2.setEnabled(true);
        pcc2.setBlocking(true);
        idp1.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{pcc1, pcc2});

        ClaimConfig claimConfig = new ClaimConfig();
        claimConfig.setLocalClaimDialect(false);
        claimConfig.setRoleClaimURI("Country");
        claimConfig.setUserClaimURI("Country");
        ClaimMapping cm = ClaimMapping.build("http://wso2.org/claims/country", "Country", "", true);
        claimConfig.setClaimMappings(new ClaimMapping[]{cm});
        Claim remoteClaim = new Claim();
        remoteClaim.setClaimId(0);
        remoteClaim.setClaimUri("Country");
        claimConfig.setIdpClaims(new Claim[]{remoteClaim});
        idp1.setClaimConfig(claimConfig);

        //Initialize Test Identity Provider 2
        IdentityProvider idp2 = new IdentityProvider();
        idp2.setIdentityProviderName("testIdP2");

        ClaimConfig claimConfig2 = new ClaimConfig();
        claimConfig2.setLocalClaimDialect(true);
        claimConfig2.setRoleClaimURI("http://wso2.org/claims/role");
        claimConfig2.setUserClaimURI("http://wso2.org/claims/fullname");
        ClaimMapping cm2 = new ClaimMapping();
        Claim localClaim2 = new Claim();
        localClaim2.setClaimId(0);
        localClaim2.setClaimUri("http://wso2.org/claims/fullname");
        cm2.setLocalClaim(localClaim2);
        claimConfig2.setClaimMappings(new ClaimMapping[]{cm2});
        idp2.setClaimConfig(claimConfig2);

        //Initialize Test Identity Provider 3
        IdentityProvider idp3 = new IdentityProvider();
        idp3.setIdentityProviderName("testIdP3");

        return new Object[][]{

                /* IDP with PermissionsAndRoleConfig,FederatedAuthenticatorConfigs,ProvisioningConnectorConfigs,Claims*/
                {idp1},

                /* IDP with Local Cliam Dialect ClaimConfigs */
                {idp2},

                /* IDP with only the name */
                {idp3},
        };
    }

    @Test(dataProvider = "addIdPData")
    public void testAddIdP(Object identityProvider) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        String idpName = ((IdentityProvider) identityProvider).getIdentityProviderName();
        identityProviderManagementService.addIdP(((IdentityProvider) identityProvider));

        IdentityProvider idpFromDb = identityProviderManagementService.getIdPByName(idpName);
        Assert.assertEquals(idpFromDb.getIdentityProviderName(), idpName);
    }

    @DataProvider
    public Object[][] addIdPExceptionData() {

        IdentityProvider idp1 = new IdentityProvider();
        idp1.setIdentityProviderName("testIdP1");

        IdentityProvider idp2 = new IdentityProvider();
        idp2.setIdentityProviderName("SHARED_testIdP");

        IdentityProvider idp3 = new IdentityProvider();

        return new Object[][]{
                {idp1},         //Already existing IDP
                {idp2},         //IDP name with "SHARED_" prefix
                {idp3},         //IDP without a name
        };
    }

    @Test(dataProvider = "addIdPExceptionData")
    public void testAddIdPException(Object identityProvider) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.addIdP((IdentityProvider) identityProvider));
    }

    @DataProvider
    public Object[][] getIdPByNameData() {

        return new Object[][]{
                {"testIdP1"},
                {"testIdP2"},
                {"testIdP3"},
        };
    }

    @Test(dataProvider = "getIdPByNameData")
    public void testGetIdPByName(String idpName) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        IdentityProvider idpFromDb = identityProviderManagementService.getIdPByName(idpName);
        Assert.assertEquals(idpFromDb.getIdentityProviderName(), idpName);
    }

    @DataProvider
    public Object[][] getIdPByNameNullReturnData() {

        return new Object[][]{
                {"NonExistingIdP"},
        };
    }

    @Test(dataProvider = "getIdPByNameNullReturnData")
    public void testGetIdPByNameNullReturn(String idpName) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);
        assertNull(identityProviderManagementService.getIdPByName(idpName));
    }

    @DataProvider
    public Object[][] getIdPByNameIllegalArgumentExceptionData() {

        return new Object[][]{
                {""},
                {null},
        };
    }

    @Test(dataProvider = "getIdPByNameIllegalArgumentExceptionData")
    public void testGetIdPByNameIllegalArgumentException(String idpName) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        assertThrows(IllegalArgumentException.class, () ->
                identityProviderManagementService.getIdPByName(idpName));
    }

    @Test()
    public void testGetAllIdpCount() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        //Without idp data in database
        int idpCount = identityProviderManagementService.getAllIdpCount();
        Assert.assertEquals(idpCount, 0);

        //With 3 idps  in database
        addTestIdps(identityProviderManagementService);
        idpCount = identityProviderManagementService.getAllIdpCount();
        Assert.assertEquals(idpCount, 3);
    }

    @Test()
    public void testGetAllIdps() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        //Without idp data in database
        IdentityProvider[] idpsList = identityProviderManagementService.getAllIdPs();
        Assert.assertEquals(idpsList.length, 0);

        //With 3 idps  in database
        addTestIdps(identityProviderManagementService);
        idpsList = identityProviderManagementService.getAllIdPs();
        Assert.assertEquals(idpsList.length, 3);

        //With 3 idps and Shared idp  in database
        addSharedIdp();
        idpsList = identityProviderManagementService.getAllIdPs();
        Assert.assertEquals(idpsList.length, 3);
    }

    @DataProvider
    public Object[][] getAllPaginatedIdpInfoData() {

        return new Object[][]{
                {1, 3},
                {2, 0},
        };
    }

    @Test(dataProvider = "getAllPaginatedIdpInfoData")
    public void testGetAllPaginatedIdpInfo(int pageNumber, int idpCount) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        IdentityProvider[] idpList = identityProviderManagementService.getAllPaginatedIdpInfo(pageNumber);
        Assert.assertEquals(idpList.length, idpCount);
    }

    @DataProvider
    public Object[][] getAllPaginatedIdpInfoExceptionData() {

        return new Object[][]{
                {0},
                {-1},
        };
    }

    @Test(dataProvider = "getAllPaginatedIdpInfoExceptionData")
    public void testGetAllPaginatedIdpInfoException(int pageNumber) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.getAllPaginatedIdpInfo(pageNumber));
    }

    @DataProvider
    public Object[][] getPaginatedIdpInfoData() {

        return new Object[][]{
                {1, "", 3},
                {1, "name sw test", 3},
                {1, "homeRealmIdentifier eq 1", 1},
                {1, "isEnabled co true", 3},
                {1, "isEnabled eq false", 0},
                {1, "id ew NotExist", 0},
                {2, "name eq testIdP2", 0},
        };
    }

    @Test(dataProvider = "getPaginatedIdpInfoData")
    public void testGetPaginatedIdpInfo(int pageNumber, String filter, int idpCount) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        IdentityProvider[] idpList = identityProviderManagementService.getPaginatedIdpInfo(filter, pageNumber);
        Assert.assertEquals(idpList.length, idpCount);
    }

    @DataProvider
    public Object[][] getPaginatedIdpInfoExceptionData() {

        return new Object[][]{
                {1, "Wrong_Filter"},
                {1, "WrongValue eq 1"},
                {1, "name WrongOperator test"},
                {1, "isEnabled eq Wrong"},
        };
    }

    @Test(dataProvider = "getPaginatedIdpInfoExceptionData")
    public void testGetPaginatedIdpInfoException(int pageNumber, String filter) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.getPaginatedIdpInfo(filter, pageNumber));
    }

    @DataProvider
    public Object[][] getFilteredIdpCountData() {

        return new Object[][]{
                {"", 3},
                {"name ew 1", 1},
                {"name co IdP", 3},
                {"description eq Test Idp 1", 1}
        };
    }

    @Test(dataProvider = "getFilteredIdpCountData")
    public void testGetFilteredIdpCount(String filter, int idpCount) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        int idpCountFromDb = identityProviderManagementService.getFilteredIdpCount(filter);
        Assert.assertEquals(idpCountFromDb, idpCount);
    }

    @DataProvider
    public Object[][] getAllIdPsSearchData() {

        return new Object[][]{
                {"", 3},
                {"test*", 3},
                {"????IdP*", 3},
                {"tes_I*", 3},
                {"*1", 1},
                {"testIdP1", 1},
                {"Notexist", 0},
        };
    }

    @Test(dataProvider = "getAllIdPsSearchData")
    public void testGetAllIdPsSearch(String filter, int idpCount) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);
        IdentityProvider[] idpsList = identityProviderManagementService.getAllIdPsSearch(filter);
        Assert.assertEquals(idpsList.length, idpCount);

        //with shared_idp
        addSharedIdp();
        idpsList = identityProviderManagementService.getAllIdPsSearch(filter);
        Assert.assertEquals(idpsList.length, idpCount);
    }

    @Test()
    public void testGetEnabledAllIdPs() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        IdentityProvider[] idpsList = identityProviderManagementService.getEnabledAllIdPs();
        Assert.assertEquals(idpsList.length, 3);
    }

    @DataProvider
    public Object[][] deleteIdPData() {

        return new Object[][]{
                {"testIdP1"},
                {"testIdP2"},
                {"testIdP3"},
        };
    }

    @Test(dataProvider = "deleteIdPData")
    public void testDeleteIdP(String idpName) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        Assert.assertNotNull(identityProviderManagementService.getIdPByName(idpName));
        identityProviderManagementService.deleteIdP(idpName);
        Assert.assertNull(identityProviderManagementService.getIdPByName(idpName));
    }

    @DataProvider
    public Object[][] deleteIdPExceptionData() {

        return new Object[][]{
                {""},
                {null},
        };
    }

    @Test(dataProvider = "deleteIdPExceptionData")
    public void testDeleteIdPException(String idpName) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.deleteIdP(idpName));
    }

    @DataProvider
    public Object[][] forceDeleteIdPData() {

        return new Object[][]{
                {"testIdP1"},
                {"testIdP2"},
                {"testIdP3"},
        };
    }

    @Test(dataProvider = "forceDeleteIdPData")
    public void testForceDeleteIdP(String idpName) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        Assert.assertNotNull(identityProviderManagementService.getIdPByName(idpName));
        identityProviderManagementService.forceDeleteIdP(idpName);
        Assert.assertNull(identityProviderManagementService.getIdPByName(idpName));
    }

    @DataProvider
    public Object[][] forceDeleteIdPExceptionData() {

        return new Object[][]{
                {""},
                {null},
        };
    }

    @Test(dataProvider = "forceDeleteIdPExceptionData")
    public void testForceDeleteIdPException(String idpName) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.forceDeleteIdP(idpName));
    }

    @DataProvider
    public Object[][] updateIdPData() {

        //Initialize New Test Identity Provider 1
        IdentityProvider idp1New = new IdentityProvider();
        idp1New.setIdentityProviderName("testIdP1New");
        idp1New.setEnable(true);
        idp1New.setPrimary(true);
        idp1New.setFederationHub(true);
        idp1New.setCertificate("");

        RoleMapping rm1New = new RoleMapping();
        rm1New.setRemoteRole("Role1New");
        rm1New.setLocalRole(new LocalRole("1", "LocalRole1"));
        RoleMapping rm2New = new RoleMapping();
        rm2New.setRemoteRole("Role2New");
        rm2New.setLocalRole(new LocalRole("2", "LocalRole2"));

        PermissionsAndRoleConfig prcNew = new PermissionsAndRoleConfig();
        prcNew.setIdpRoles(new String[]{"Role1New", "Role2New"});
        prcNew.setRoleMappings(new RoleMapping[]{rm1New, rm2New});
        idp1New.setPermissionAndRoleConfig(prcNew);

        FederatedAuthenticatorConfig facNew = new FederatedAuthenticatorConfig();
        facNew.setDisplayName("DisplayName1New");
        facNew.setName("Name");
        facNew.setEnabled(true);
        Property property1New = new Property();
        property1New.setName("Property1New");
        property1New.setValue("value1New");
        property1New.setConfidential(false);
        Property property2New = new Property();
        property2New.setName("Property2New");
        property2New.setValue("value2New");
        property2New.setConfidential(false);
        facNew.setProperties(new Property[]{property1New, property2New});
        idp1New.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{facNew});

        ProvisioningConnectorConfig pcc1New = new ProvisioningConnectorConfig();
        pcc1New.setName("ProvisiningConfig1");
        pcc1New.setProvisioningProperties(new Property[]{property1New});
        ProvisioningConnectorConfig pcc2New = new ProvisioningConnectorConfig();
        pcc2New.setName("ProvisiningConfig2");
        pcc2New.setProvisioningProperties(new Property[]{property2New});
        pcc2New.setEnabled(true);
        pcc2New.setBlocking(true);
        idp1New.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{pcc1New, pcc2New});

        ClaimConfig claimConfigNew = new ClaimConfig();
        claimConfigNew.setLocalClaimDialect(false);
        claimConfigNew.setRoleClaimURI("Country");
        claimConfigNew.setUserClaimURI("Country");
        ClaimMapping cm = ClaimMapping.build("http://wso2.org/claims/country", "Country", "", true);
        Claim remoteClaim = new Claim();
        remoteClaim.setClaimId(0);
        remoteClaim.setClaimUri("Country");
        claimConfigNew.setClaimMappings(new ClaimMapping[]{cm});
        claimConfigNew.setIdpClaims(new Claim[]{remoteClaim});
        idp1New.setClaimConfig(claimConfigNew);

        //Initialize New Test Identity Provider 2
        IdentityProvider idp2New = new IdentityProvider();
        idp2New.setIdentityProviderName("testIdP2New");

        //Initialize New Test Identity Provider 3
        IdentityProvider idp3New = new IdentityProvider();
        idp3New.setIdentityProviderName("testIdP3New");

        return new Object[][]{
                {"testIdP1", idp1New},
                {"testIdP2", idp2New},
                {"testIdP3", idp3New},
        };
    }

    @Test(dataProvider = "updateIdPData")
    public void testUpdateIdP(String oldIdpName, Object newIdp) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        addTestIdps(identityProviderManagementService);
        identityProviderManagementService.updateIdP(oldIdpName, (IdentityProvider) newIdp);
        String newIdpName = ((IdentityProvider) newIdp).getIdentityProviderName();

        Assert.assertNull(identityProviderManagementService.getIdPByName(oldIdpName));
        Assert.assertNotNull(identityProviderManagementService.getIdPByName(newIdpName));
    }

    @DataProvider
    public Object[][] updateIdPExceptionData() {

        //Initialize New Test Identity Provider 1
        IdentityProvider idp1New = new IdentityProvider();
        idp1New.setIdentityProviderName("testIdP1New");

        //Initialize New Test Identity Provider 2
        IdentityProvider idp2New = new IdentityProvider();

        return new Object[][]{
                {"NonExist", idp1New},
                {"testIdP2", idp2New},
        };
    }

    @Test(dataProvider = "updateIdPExceptionData")
    public void testUpdateIdPException(String oldIdpName, Object newIdp) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addTestIdps(identityProviderManagementService);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.updateIdP(oldIdpName, (IdentityProvider) newIdp));
    }

    @Test
    public void testGetAllLocalClaimUris() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        ClaimMetadataManagementServiceImpl claimMetadataManagementService =
                mock(ClaimMetadataManagementServiceImpl.class);
        IdpMgtServiceComponentHolder.getInstance().setClaimMetadataManagementService(claimMetadataManagementService);

        LocalClaim localClaim1 = new LocalClaim("http://wso2.org/claims/test1");
        List<LocalClaim> claimList = new ArrayList<>();
        claimList.add(localClaim1);
        when(claimMetadataManagementService.getLocalClaims(anyString())).thenReturn(claimList);

        String[] allLocalClaimUris = identityProviderManagementService.getAllLocalClaimUris();
        Assert.assertEquals(allLocalClaimUris.length, 1);
        Assert.assertEquals(allLocalClaimUris[0], "http://wso2.org/claims/test1");
    }

    @Test
    public void testGetAllLocalClaimUrisException() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        ClaimMetadataManagementServiceImpl claimMetadataManagementService =
                mock(ClaimMetadataManagementServiceImpl.class);
        IdpMgtServiceComponentHolder.getInstance().setClaimMetadataManagementService(claimMetadataManagementService);
        when(claimMetadataManagementService.getLocalClaims(anyString())).thenThrow(ClaimMetadataException.class);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.getAllLocalClaimUris());
    }

    @Test
    public void testGetAllFederatedAuthenticators() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        FederatedAuthenticatorConfig[] allFederatedAuthenticators =
                identityProviderManagementService.getAllFederatedAuthenticators();
        Assert.assertEquals(allFederatedAuthenticators.length, 0);

        FederatedAuthenticatorConfig fac1 = mock(FederatedAuthenticatorConfig.class);
        fac1.setDisplayName("DisplayName1");
        fac1.setName("Name1");
        fac1.setEnabled(true);
        FederatedAuthenticatorConfig fac2 = mock(FederatedAuthenticatorConfig.class);
        fac2.setDisplayName("DisplayName2");
        fac2.setName("Name2");
        fac2.setEnabled(true);

        ApplicationAuthenticatorService.getInstance().addFederatedAuthenticator(fac1);
        allFederatedAuthenticators = identityProviderManagementService.getAllFederatedAuthenticators();
        Assert.assertEquals(allFederatedAuthenticators.length, 1);

        ApplicationAuthenticatorService.getInstance().addFederatedAuthenticator(fac2);
        allFederatedAuthenticators = identityProviderManagementService.getAllFederatedAuthenticators();
        Assert.assertEquals(allFederatedAuthenticators.length, 2);

        //clear after the test
        ApplicationAuthenticatorService.getInstance().removeFederatedAuthenticator(fac1);
        ApplicationAuthenticatorService.getInstance().removeFederatedAuthenticator(fac2);

        allFederatedAuthenticators = identityProviderManagementService.getAllFederatedAuthenticators();
        Assert.assertEquals(allFederatedAuthenticators.length, 0);
    }

    @Test
    public void testGetAllProvisioningConnectors() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        ProvisioningConnectorConfig[] allProvisioningConnectors =
                identityProviderManagementService.getAllProvisioningConnectors();
        Assert.assertNull(allProvisioningConnectors);

        ProvisioningConnectorConfig pcc1 = mock(ProvisioningConnectorConfig.class);
        pcc1.setName("ProvisiningConfig1");
        ProvisioningConnectorConfig pcc2 = mock(ProvisioningConnectorConfig.class);
        pcc2.setName("ProvisiningConfig2");
        pcc2.setEnabled(true);
        pcc2.setBlocking(true);

        ProvisioningConnectorService.getInstance().addProvisioningConnectorConfigs(pcc1);
        allProvisioningConnectors = identityProviderManagementService.getAllProvisioningConnectors();
        Assert.assertEquals(allProvisioningConnectors.length, 1);

        ProvisioningConnectorService.getInstance().addProvisioningConnectorConfigs(pcc2);
        allProvisioningConnectors = identityProviderManagementService.getAllProvisioningConnectors();
        Assert.assertEquals(allProvisioningConnectors.length, 2);

        //clear after the test
        ProvisioningConnectorService.getInstance().removeProvisioningConnectorConfigs(pcc1);
        ProvisioningConnectorService.getInstance().removeProvisioningConnectorConfigs(pcc2);

        allProvisioningConnectors = identityProviderManagementService.getAllProvisioningConnectors();
        Assert.assertNull(allProvisioningConnectors);
    }

    @Test()
    public void testGetResidentIdP() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();

        addResidentIdp(identityProviderManagementService);
        IdentityProvider idpFromDb = identityProviderManagementService.getResidentIdP();

        Assert.assertNotNull(idpFromDb);
        Assert.assertEquals(idpFromDb.getIdentityProviderName(), "LOCAL");
    }

    @Test()
    public void testGetResidentIdPException() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.getResidentIdP());
    }

    @DataProvider
    public Object[][] updateResidentIdPData() {

        //Initialize New Resident Identity Provider 1
        IdentityProvider idp1New = new IdentityProvider();
        idp1New.setIdentityProviderName("LOCAL");

        IdentityProviderProperty idpProperty1 = new IdentityProviderProperty();
        idpProperty1.setName(IdentityApplicationConstants.SESSION_IDLE_TIME_OUT);
        idpProperty1.setValue("100");
        IdentityProviderProperty idpProperty2 = new IdentityProviderProperty();
        idpProperty2.setName(IdentityApplicationConstants.REMEMBER_ME_TIME_OUT);
        idpProperty2.setValue("10000");
        IdentityProviderProperty idpProperty3 = new IdentityProviderProperty();
        idpProperty3.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.SAML_METADATA_VALIDITY_PERIOD);
        idpProperty3.setValue("10000");
        IdentityProviderProperty idpProperty4 = new IdentityProviderProperty();
        idpProperty4.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.SAML_METADATA_SIGNING_ENABLED);
        idpProperty4.setValue("true");
        idp1New.setIdpProperties(new IdentityProviderProperty[]{idpProperty1, idpProperty2, idpProperty3,
                idpProperty4});

        //Initialize New Resident Identity Provider 2
        IdentityProvider idp2New = new IdentityProvider();
        idp2New.setIdentityProviderName("LOCAL");

        FederatedAuthenticatorConfig facNew = new FederatedAuthenticatorConfig();
        facNew.setDisplayName("DisplayName1New");
        facNew.setName("Name");
        facNew.setEnabled(true);
        idp2New.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{facNew});

        //Initialize New Resident Identity Provider 3
        IdentityProvider idp3New = new IdentityProvider();
        idp3New.setIdentityProviderName("LOCAL");

        return new Object[][]{
                {idp1New},          //new Resident IDP with new IDP Properties
                {idp2New},          //new Resident IDP with new FederatedAuthenticatorConfigs
                {idp3New},          //new Resident IDP with no object properties (only contains the name)
        };
    }

    @Test(dataProvider = "updateResidentIdPData")
    public void testUpdateResidentIdP(Object newIdp) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addResidentIdp(identityProviderManagementService);

        identityProviderManagementService.updateResidentIdP((IdentityProvider) newIdp);
        Assert.assertNotNull(identityProviderManagementService.getResidentIdP());
    }

    @DataProvider
    public Object[][] updateResidentIdPExceptionData() {

        //Initialize New Test Resident Identity Provider 1
        IdentityProvider idp1New = new IdentityProvider();
        idp1New.setIdentityProviderName("LOCAL");
        IdentityProviderProperty idpProperty1 = new IdentityProviderProperty();
        idpProperty1.setName(IdentityApplicationConstants.SESSION_IDLE_TIME_OUT);
        idpProperty1.setValue("-1");
        idp1New.setIdpProperties(new IdentityProviderProperty[]{idpProperty1});

        //Initialize New Test Resident Identity Provider 2
        IdentityProvider idp2New = new IdentityProvider();
        idp2New.setIdentityProviderName("LOCAL");
        IdentityProviderProperty idpProperty2 = new IdentityProviderProperty();
        idpProperty2.setName(IdentityApplicationConstants.REMEMBER_ME_TIME_OUT);
        idpProperty2.setValue("Invalid");
        idp2New.setIdpProperties(new IdentityProviderProperty[]{idpProperty2});

        //Initialize New Test Resident Identity Provider 3
        IdentityProvider idp3New = new IdentityProvider();
        idp3New.setIdentityProviderName("LOCAL");
        IdentityProviderProperty idpProperty3 = new IdentityProviderProperty();
        idpProperty3.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.SAML_METADATA_VALIDITY_PERIOD);
        idpProperty3.setValue("");
        idp3New.setIdpProperties(new IdentityProviderProperty[]{idpProperty3});

        //Initialize New Test Resident Identity Provider 4
        IdentityProvider idp4New = new IdentityProvider();
        idp4New.setIdentityProviderName("LOCAL");
        IdentityProviderProperty idpProperty4 = new IdentityProviderProperty();
        idpProperty4.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.SAML_METADATA_SIGNING_ENABLED);
        idpProperty4.setValue("");
        idp4New.setIdpProperties(new IdentityProviderProperty[]{idpProperty4});

        return new Object[][]{
                {idp1New},          //new Resident IDP with Invalid value to the 'SESSION_IDLE_TIME_OUT' idp property
                {idp2New},          //new Resident IDP with Invalid value to the 'REMEMBER_ME_TIME_OUT' idp property
                {idp3New},          //new Resident IDP with Invalid 'SAML_METADATA_VALIDITY_PERIOD' idp property value
                {idp4New},          //new Resident IDP with Invalid 'SAML_METADATA_SIGNING_ENABLED' idp property value
        };
    }

    @Test(dataProvider = "updateResidentIdPExceptionData")
    public void testUpdateResidentIdPException(Object newIdp) throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addResidentIdp(identityProviderManagementService);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.updateResidentIdP((IdentityProvider) newIdp));
    }

    @Test
    public void testGetResidentIDPMetadata() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addResidentIdp(identityProviderManagementService);
        Assert.assertNull(identityProviderManagementService.getResidentIDPMetadata());

        when(mockMetadataConverter.canHandle((FederatedAuthenticatorConfig) anyObject())).thenReturn(TRUE);
        when(mockMetadataConverter.getMetadataString((FederatedAuthenticatorConfig) anyObject())).
                thenReturn("saml2sso");

        IdentityProvider newIdp = new IdentityProvider();
        newIdp.setIdentityProviderName("LOCAL");
        FederatedAuthenticatorConfig facNew = new FederatedAuthenticatorConfig();
        facNew.setDisplayName("SAML2SSO");
        facNew.setName("saml2sso");
        facNew.setEnabled(true);
        newIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{facNew});
        identityProviderManagementService.updateResidentIdP((IdentityProvider) newIdp);

        Assert.assertNotNull(identityProviderManagementService.getResidentIDPMetadata());
        Assert.assertEquals(identityProviderManagementService.getResidentIDPMetadata(), "saml2sso");
    }

    @Test
    public void testGetResidentIDPMetadataException() throws Exception {

        IdentityProviderManagementService identityProviderManagementService = new IdentityProviderManagementService();
        addResidentIdp(identityProviderManagementService);

        when(mockMetadataConverter.canHandle((FederatedAuthenticatorConfig) anyObject())).thenReturn(TRUE);
        when(mockMetadataConverter.getMetadataString((FederatedAuthenticatorConfig) anyObject())).thenThrow
                (IdentityProviderSAMLException.class);

        IdentityProvider newIdp = new IdentityProvider();
        newIdp.setIdentityProviderName("LOCAL");
        FederatedAuthenticatorConfig facNew = new FederatedAuthenticatorConfig();
        facNew.setDisplayName("SAML2SSO");
        facNew.setName("saml2sso");
        facNew.setEnabled(true);
        newIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{facNew});
        identityProviderManagementService.updateResidentIdP((IdentityProvider) newIdp);

        assertThrows(IdentityProviderManagementException.class, () ->
                identityProviderManagementService.getResidentIDPMetadata());
    }

    private void addTestIdps(IdentityProviderManagementService identityProviderManagementService) throws
            IdentityProviderManagementException {

        //Initialize Test Identity Provider 1
        IdentityProvider idp1 = new IdentityProvider();
        idp1.setIdentityProviderName("testIdP1");
        idp1.setIdentityProviderDescription("Test Idp 1");
        idp1.setHomeRealmId("1");
        idp1.setEnable(true);
        idp1.setPrimary(true);
        idp1.setFederationHub(true);
        idp1.setCertificate("");

        RoleMapping rm1 = new RoleMapping();
        rm1.setRemoteRole("Role1");
        rm1.setLocalRole(new LocalRole("1", "LocalRole1"));
        RoleMapping rm2 = new RoleMapping();
        rm2.setRemoteRole("Role2");
        rm2.setLocalRole(new LocalRole("2", "LocalRole2"));

        PermissionsAndRoleConfig prc = new PermissionsAndRoleConfig();
        prc.setIdpRoles(new String[]{"Role1", "Role2"});
        prc.setRoleMappings(new RoleMapping[]{rm1, rm2});
        idp1.setPermissionAndRoleConfig(prc);

        FederatedAuthenticatorConfig fac = new FederatedAuthenticatorConfig();
        fac.setDisplayName("DisplayName1");
        fac.setName("Name");
        fac.setEnabled(true);
        Property property1 = new Property();
        property1.setName("Property1");
        property1.setValue("value1");
        property1.setConfidential(true);
        Property property2 = new Property();
        property2.setName("Property2");
        property2.setValue("value2");
        property2.setConfidential(false);
        fac.setProperties(new Property[]{property1, property2});
        idp1.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{fac});

        ProvisioningConnectorConfig pcc1 = new ProvisioningConnectorConfig();
        pcc1.setName("ProvisiningConfig1");
        pcc1.setProvisioningProperties(new Property[]{property1});
        ProvisioningConnectorConfig pcc2 = new ProvisioningConnectorConfig();
        pcc2.setName("ProvisiningConfig2");
        pcc2.setProvisioningProperties(new Property[]{property2});
        pcc2.setEnabled(true);
        pcc2.setBlocking(true);
        idp1.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{pcc1, pcc2});

        IdentityProviderProperty identityProviderProperty = new IdentityProviderProperty();
        identityProviderProperty.setDisplayName("idpDisplayName");
        identityProviderProperty.setName("idpPropertyName");
        identityProviderProperty.setValue("idpPropertyValue");
        idp1.setIdpProperties(new IdentityProviderProperty[]{identityProviderProperty});

        ClaimConfig claimConfig = new ClaimConfig();
        claimConfig.setLocalClaimDialect(false);
        claimConfig.setRoleClaimURI("Country");
        claimConfig.setUserClaimURI("Country");
        ClaimMapping cm = ClaimMapping.build("http://wso2.org/claims/country", "Country", "", true);
        Claim remoteClaim = new Claim();
        remoteClaim.setClaimId(0);
        remoteClaim.setClaimUri("Country");
        claimConfig.setClaimMappings(new ClaimMapping[]{cm});
        claimConfig.setIdpClaims(new Claim[]{remoteClaim});
        idp1.setClaimConfig(claimConfig);

        //Initialize Test Identity Provider 2
        IdentityProvider idp2 = new IdentityProvider();
        idp2.setIdentityProviderName("testIdP2");
        idp2.setHomeRealmId("2");

        ClaimConfig claimConfig2 = new ClaimConfig();
        claimConfig2.setLocalClaimDialect(true);
        claimConfig2.setRoleClaimURI("http://wso2.org/claims/role");
        claimConfig2.setUserClaimURI("http://wso2.org/claims/fullname");
        ClaimMapping cm2 = new ClaimMapping();
        Claim localClaim2 = new Claim();
        localClaim2.setClaimId(0);
        localClaim2.setClaimUri("http://wso2.org/claims/fullname");
        cm2.setLocalClaim(localClaim2);
        claimConfig2.setClaimMappings(new ClaimMapping[]{cm2});
        idp2.setClaimConfig(claimConfig2);

        //Initialize Test Identity Provider 3
        IdentityProvider idp3 = new IdentityProvider();
        idp3.setIdentityProviderName("testIdP3");
        idp3.setHomeRealmId("3");

        /* IDP with PermissionsAndRoleConfig,FederatedAuthenticatorConfigs, ProvisioningConnectorConfigs,ClaimConfigs */
        identityProviderManagementService.addIdP(idp1);

        /* IDP with Local Cliam Dialect ClaimConfigs */
        identityProviderManagementService.addIdP(idp2);

        /* IDP with Only name */
        identityProviderManagementService.addIdP(idp3);
    }

    private void addResidentIdp(IdentityProviderManagementService identityProviderManagementService) throws
            IdentityProviderManagementException {

        IdentityProvider residentIdp = new IdentityProvider();
        residentIdp.setIdentityProviderName("LOCAL");
        IdentityProviderProperty idpProperty1 = new IdentityProviderProperty();
        idpProperty1.setName(IdentityApplicationConstants.SESSION_IDLE_TIME_OUT);
        idpProperty1.setValue("20");
        residentIdp.setIdpProperties(new IdentityProviderProperty[]{idpProperty1});

        identityProviderManagementService.addIdP(residentIdp);
    }

    private void addSharedIdp() throws SQLException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {

            String sql = "INSERT INTO IDP (TENANT_ID, NAME, UUID, IS_PRIMARY, IS_FEDERATION_HUB, " +
                    "IS_LOCAL_CLAIM_DIALECT, IS_ENABLED) VALUES " +
                    "( '" + SUPER_TENANT_ID + "' , '" + "SHARED_IDP" + "' , '" + "0000" + "' , '" + 1 + "' , '" + 1 +
                    "' , '" + 1 + "' , '" + 1 + "');";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.execute();
            statement.close();
        }
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            String query = "SELECT * FROM IDP WHERE NAME='SHARED_IDP'";
            PreparedStatement statement2 = connection.prepareStatement(query);
            ResultSet resultSet = statement2.executeQuery();
            int result = 0;
            if (resultSet.next()) {
                result = +1;
            }
            Assert.assertEquals(result, 1);
            statement2.close();
        }
    }

    private void removeTestIdps(IdentityProviderManagementService identityProviderManagementService) throws
            IdentityProviderManagementException {

        IdentityProvider[] idpsFromDb = identityProviderManagementService.getAllIdPs();

        for (IdentityProvider idp : idpsFromDb) {
            identityProviderManagementService.deleteIdP(idp.getIdentityProviderName());         //remove current idps
        }
        identityProviderManagementService.deleteIdP("LOCAL");                           //remove resident idp
        identityProviderManagementService.deleteIdP("SHARED_IDP");                      //remove shared idp
    }

}
