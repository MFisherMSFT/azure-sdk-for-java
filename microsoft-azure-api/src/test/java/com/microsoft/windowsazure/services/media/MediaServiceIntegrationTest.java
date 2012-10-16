/**
 * Copyright 2012 Microsoft Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.windowsazure.services.media;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.microsoft.windowsazure.services.core.ServiceException;
import com.microsoft.windowsazure.services.media.models.AccessPolicyInfo;
import com.microsoft.windowsazure.services.media.models.AccessPolicyPermission;
import com.microsoft.windowsazure.services.media.models.AssetInfo;
import com.microsoft.windowsazure.services.media.models.AssetState;
import com.microsoft.windowsazure.services.media.models.CreateAccessPolicyOptions;
import com.microsoft.windowsazure.services.media.models.CreateAssetOptions;
import com.microsoft.windowsazure.services.media.models.CreateLocatorOptions;
import com.microsoft.windowsazure.services.media.models.EncryptionOption;
import com.microsoft.windowsazure.services.media.models.ListLocatorsResult;
import com.microsoft.windowsazure.services.media.models.ListMediaProcessorsOptions;
import com.microsoft.windowsazure.services.media.models.ListMediaProcessorsResult;
import com.microsoft.windowsazure.services.media.models.LocatorInfo;
import com.microsoft.windowsazure.services.media.models.LocatorType;
import com.microsoft.windowsazure.services.media.models.UpdateAssetOptions;
import com.microsoft.windowsazure.services.media.models.UpdateLocatorOptions;

public class MediaServiceIntegrationTest extends IntegrationTestBase {
    private static MediaContract service;

    private static final String testAssetPrefix = "testAsset";
    private static String fakeAssetId = "nb:cid:UUID:00000000-0000-4a00-0000-000000000000";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setup() throws Exception {
        service = MediaService.create(createConfig());
        cleanupEnvironment();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        cleanupEnvironment();
    }

    private static void removeAllAssets() throws ServiceException {
        List<AssetInfo> listAssetsResult = service.listAssets(null);
        for (AssetInfo assetInfo : listAssetsResult) {
            try {
                service.deleteAsset(assetInfo.getId());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void removeAllLocators() throws ServiceException {
        ListLocatorsResult listLocatorsResult = service.listLocators(null);
        for (LocatorInfo locatorInfo : listLocatorsResult.getLocatorInfos()) {
            try {
                service.deleteLocator(locatorInfo.getId());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void cleanupEnvironment() throws ServiceException {
        config = createConfig();
        removeAllAssets();
        removeAllLocators();
    }

    private void verifyAssetProperties(String message, String testName, String altId,
            EncryptionOption encryptionOption, AssetState assetState, AssetInfo actualAsset) {
        assertNotNull(message, actualAsset);
        assertEquals(message + " Name", testName, actualAsset.getName());
        assertEquals(message + " AlternateId", altId, actualAsset.getAlternateId());
        assertEquals(message + " Options", encryptionOption, actualAsset.getOptions());
        assertEquals(message + " State", assetState, actualAsset.getState());
    }

    @Before
    public void setupInstance() throws Exception {
        service = MediaService.create(config);
    }

    @Test
    public void createAssetSuccess() throws Exception {
        // Arrange
        String testName = testAssetPrefix + "Name";
        CreateAssetOptions options = new CreateAssetOptions().setName(testName);

        // Act
        AssetInfo actualAsset = service.createAsset(options);

        // Assert
        verifyAssetProperties("actualAsset", testName, "", EncryptionOption.None, AssetState.Initialized, actualAsset);
    }

    @Test
    public void createAssetOptionsSuccess() throws Exception {
        // Arrange
        String testName = testAssetPrefix + "createAssetOptionsSuccess";
        String altId = "altId";
        EncryptionOption encryptionOption = EncryptionOption.StorageEncrypted;
        AssetState assetState = AssetState.Published;
        CreateAssetOptions options = new CreateAssetOptions().setAlternateId(altId).setOptions(encryptionOption)
                .setState(assetState).setName(testName);

        // Act
        AssetInfo actualAsset = service.createAsset(options);

        // Assert
        verifyAssetProperties("actualAsset", testName, altId, encryptionOption, assetState, actualAsset);
    }

    @Test
    public void createAssetMeanString() throws Exception {
        // Arrange
        String meanString = "'\"(?++\\+&==/&?''$@://   +ne <some><XML></stuff>"
                + "{\"jsonLike\":\"Created\":\"\\/Date(1336368841597)\\/\",\"Name\":null,cksum value\"}}"
                + "Some unicode: \uB2E4\uB974\uB2E4\uB294\u0625 \u064A\u062F\u064A\u0648\u0009\r\n";

        String testName = testAssetPrefix + "createAssetMeanString" + meanString;
        CreateAssetOptions createAssetOptions = new CreateAssetOptions().setName(testName);

        // Act
        AssetInfo actualAsset = service.createAsset(createAssetOptions);

        // Assert
        assertEquals("actualAsset Name", testName, actualAsset.getName());
    }

    @Test
    public void createAssetNullNameSuccess() throws Exception {
        // Arrange

        // Act
        AssetInfo actualAsset = null;
        try {
            actualAsset = service.createAsset(null);
            // Assert
            assertNotNull("actualAsset", actualAsset);
            assertEquals("actualAsset.getName() should be the service default value, the empty string", "",
                    actualAsset.getName());
        }
        finally {
            // Clean up the anonymous asset now while we have the id, because we
            // do not want to delete all anonymous assets in the bulk-cleanup code.
            try {
                if (actualAsset != null) {
                    service.deleteAsset(actualAsset.getId());
                }
            }
            catch (ServiceException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void getAssetSuccess() throws Exception {
        // Arrange
        String testName = testAssetPrefix + "GetAssetSuccess";
        String altId = "altId";
        EncryptionOption encryptionOption = EncryptionOption.StorageEncrypted;
        AssetState assetState = AssetState.Published;
        CreateAssetOptions options = new CreateAssetOptions().setAlternateId(altId).setOptions(encryptionOption)
                .setState(assetState).setName(testName);
        AssetInfo assetInfo = service.createAsset(options);

        // Act
        AssetInfo actualAsset = service.getAsset(assetInfo.getId());

        assertEquals("Id", assetInfo.getId(), actualAsset.getId());
        verifyAssetProperties("actualAsset", testName, altId, encryptionOption, assetState, actualAsset);
    }

    @Test
    public void getAssetFailedWithInvalidId() throws ServiceException {
        thrown.expect(ServiceException.class);
        thrown.expect(new ServiceExceptionMatcher(404));
        service.getAsset(fakeAssetId);
    }

    @Test
    public void listAssetSuccess() throws ServiceException {
        // Arrange
        Collection<AssetInfo> listAssetResultBaseLine = service.listAssets();
        CreateAssetOptions createAssetOptions = new CreateAssetOptions();
        service.createAsset(createAssetOptions.setName(testAssetPrefix + "assetA"));
        service.createAsset(createAssetOptions.setName(testAssetPrefix + "assetB"));

        // Act
        Collection<AssetInfo> listAssetResult = service.listAssets();

        // Assert
        assertNotNull("listAssetResult", listAssetResult);
        assertEquals("listAssetResult.size", listAssetResultBaseLine.size() + 2, listAssetResult.size());

    }

    @Ignore
    // Bug https://github.com/WindowsAzure/azure-sdk-for-java-pr/issues/364
    @Test
    public void updateAssetSuccess() throws Exception {
        // Arrange
        String originalTestName = testAssetPrefix + "updateAssetSuccessOriginal";
        CreateAssetOptions originalOptions = new CreateAssetOptions().setAlternateId("altId")
                .setOptions(EncryptionOption.StorageEncrypted).setState(AssetState.Published).setName(originalTestName);
        AssetInfo originalAsset = service.createAsset(originalOptions);

        String updatedTestName = testAssetPrefix + "updateAssetSuccessUpdated";
        String altId = "otherAltId";
        EncryptionOption encryptionOption = EncryptionOption.None;
        AssetState assetState = AssetState.Initialized;
        UpdateAssetOptions updateAssetOptions = new UpdateAssetOptions().setName(updatedTestName).setAlternateId(altId)
                .setOptions(encryptionOption).setState(assetState);

        // Act
        service.updateAsset(originalAsset.getId(), updateAssetOptions);
        AssetInfo updatedAsset = service.getAsset(originalAsset.getId());

        // Assert
        verifyAssetProperties("updatedAsset", updatedTestName, altId, encryptionOption, assetState, updatedAsset);
    }

    @Test
    public void updateAssetNoChangesSuccess() throws Exception {
        // Arrange
        String originalTestName = testAssetPrefix + "updateAssetNoChangesSuccess";
        String altId = "altId";
        EncryptionOption encryptionOption = EncryptionOption.StorageEncrypted;
        AssetState assetState = AssetState.Published;
        CreateAssetOptions options = new CreateAssetOptions().setAlternateId(altId).setOptions(encryptionOption)
                .setState(assetState).setName(originalTestName);
        AssetInfo originalAsset = service.createAsset(options);

        UpdateAssetOptions updateAssetOptions = new UpdateAssetOptions();

        // Act
        service.updateAsset(originalAsset.getId(), updateAssetOptions);
        AssetInfo updatedAsset = service.getAsset(originalAsset.getId());

        // Assert
        verifyAssetProperties("updatedAsset", originalTestName, altId, encryptionOption, assetState, updatedAsset);
    }

    @Test
    public void updateAssetFailedWithInvalidId() throws ServiceException {
        // Arrange
        UpdateAssetOptions updateAssetOptions = new UpdateAssetOptions();

        // Act
        thrown.expect(ServiceException.class);
        thrown.expect(new ServiceExceptionMatcher(404));
        service.updateAsset(fakeAssetId, updateAssetOptions);
    }

    @Test
    public void deleteAssetSuccess() throws Exception {
        // Arrange
        String assetName = "deleteAssetSuccess";
        CreateAssetOptions createAssetOptions = new CreateAssetOptions().setName(assetName);
        AssetInfo assetInfo = service.createAsset(createAssetOptions);
        List<AssetInfo> listAssetsResult = service.listAssets();
        int assetCountBaseline = listAssetsResult.size();

        // Act
        service.deleteAsset(assetInfo.getId());

        // Assert
        listAssetsResult = service.listAssets();
        assertEquals("listAssetsResult.size", assetCountBaseline - 1, listAssetsResult.size());

        thrown.expect(ServiceException.class);
        thrown.expect(new ServiceExceptionMatcher(404));
        service.getAsset(assetInfo.getId());
    }

    @Test
    public void deleteAssetFailedWithInvalidId() throws ServiceException {
        thrown.expect(ServiceException.class);
        thrown.expect(new ServiceExceptionMatcher(404));
        service.deleteAsset(fakeAssetId);
    }

    @Test
    public void createLocatorSuccess() throws ServiceException {
        // Arrange 
        CreateLocatorOptions createLocatorOptions = new CreateLocatorOptions();
        AssetInfo assetInfo = service.createAsset(null);
        AccessPolicyInfo accessPolicyInfo = service.createAccessPolicy("createLocatorSuccess", 5);
        LocatorType locatorType = LocatorType.SAS;

        // Act
        LocatorInfo locatorInfo = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(), locatorType,
                createLocatorOptions);

        // Assert 
        assertNotNull(locatorInfo);
        assertNotNull(locatorInfo.getId());

    }

    @Ignore("due to media service bug 596240")
    @Test
    public void createLocatorSetExpirationDateTimeSuccess() throws ServiceException {
        // Arrange 
        CreateLocatorOptions createLocatorOptions = new CreateLocatorOptions();
        Date expectedExpirationDateTime = new Date();
        expectedExpirationDateTime.setTime(expectedExpirationDateTime.getTime() + 1000);
        String accessPolicyName = "createLocatorSetExpirationDateTimeSuccess";

        createLocatorOptions.setExpirationDateTime(expectedExpirationDateTime);
        AccessPolicyInfo accessPolicyInfo = service.createAccessPolicy(accessPolicyName, 10);
        AssetInfo assetInfo = service.createAsset(null);
        LocatorType locatorType = LocatorType.SAS;

        // Act
        LocatorInfo locatorInfo = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(), locatorType,
                createLocatorOptions);

        // Assert 
        assertNotNull(locatorInfo);
        assertNotNull(locatorInfo.getId());
        assertEquals(expectedExpirationDateTime, locatorInfo.getExpirationDateTime());

    }

    @Test
    public void createLocatorSetStartTimeSuccess() throws ServiceException {
        // Arrange 
        CreateLocatorOptions createLocatorOptions = new CreateLocatorOptions();
        Date expectedStartDateTime = new Date();
        expectedStartDateTime.setTime(expectedStartDateTime.getTime() + 1000);
        String accessPolicyName = "createLocatorSetStartDateTimeSuccess";

        createLocatorOptions.setStartTime(expectedStartDateTime);
        AccessPolicyInfo accessPolicyInfo = service.createAccessPolicy(accessPolicyName, 10);
        AssetInfo assetInfo = service.createAsset(null);
        LocatorType locatorType = LocatorType.SAS;

        // Act
        LocatorInfo locatorInfo = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(), locatorType,
                createLocatorOptions);

        // Assert 
        assertNotNull(locatorInfo);
        assertNotNull(locatorInfo.getId());
        assertEquals(expectedStartDateTime, locatorInfo.getStartTime());

    }

    @Ignore("due to media service bug 596238")
    @Test
    public void getLocatorSuccess() throws ServiceException {
        // Arrange
        CreateLocatorOptions createLocatorOptions = new CreateLocatorOptions();
        AssetInfo assetInfo = service.createAsset(null);
        AccessPolicyInfo accessPolicyInfo = service.createAccessPolicy("getLocatorSuccess", 5);
        LocatorType locatorType = LocatorType.SAS;
        LocatorInfo expectedLocatorInfo = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(),
                locatorType, createLocatorOptions);

        // Act
        LocatorInfo actualLocatorInfo = service.getLocator(expectedLocatorInfo.getId());

        // Assert
        assertNotNull(actualLocatorInfo);
        assertEquals(expectedLocatorInfo.getAccessPolicyId(), actualLocatorInfo.getAccessPolicyId());
        assertEquals(expectedLocatorInfo.getAssetId(), actualLocatorInfo.getAssetId());
        assertEquals(expectedLocatorInfo.getExpirationDateTime(), actualLocatorInfo.getExpirationDateTime());
        assertEquals(expectedLocatorInfo.getId(), actualLocatorInfo.getId());
        assertEquals(expectedLocatorInfo.getLocatorType(), actualLocatorInfo.getLocatorType());
        assertEquals(expectedLocatorInfo.getPath(), actualLocatorInfo.getPath());
        assertEquals(expectedLocatorInfo.getStartTime(), actualLocatorInfo.getStartTime());

    }

    @Test
    public void listLocatorsSuccess() throws ServiceException {
        // Arrange
        CreateLocatorOptions createLocatorOptions = new CreateLocatorOptions();
        AssetInfo assetInfo = service.createAsset(null);
        AccessPolicyInfo accessPolicyInfo = service.createAccessPolicy("listLocatorsSuccess", 5);
        LocatorType locatorType = LocatorType.SAS;
        LocatorInfo locatorInfoA = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(), locatorType,
                createLocatorOptions);
        LocatorInfo locatorInfoB = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(), locatorType,
                createLocatorOptions);

        // Act
        ListLocatorsResult listLocatorsResult = service.listLocators();

        // Assert
        assertNotNull(listLocatorsResult);
        service.deleteLocator(locatorInfoA.getId());
        service.deleteLocator(locatorInfoB.getId());

    }

    @Ignore("due to media service bug 596264")
    @Test
    public void updateLocatorSuccess() throws ServiceException {
        // Arrange
        LocatorType locatorTypeExepcted = LocatorType.Origin;
        AssetInfo assetInfo = service.createAsset(null);
        CreateAccessPolicyOptions createAccessPolicyOptions = new CreateAccessPolicyOptions();
        createAccessPolicyOptions.removePermissions(AccessPolicyPermission.DELETE);
        createAccessPolicyOptions.removePermissions(AccessPolicyPermission.WRITE);
        createAccessPolicyOptions.addPermissions(AccessPolicyPermission.READ);
        AccessPolicyInfo accessPolicyInfo = service.createAccessPolicy("listLocatorsSuccess", 5,
                createAccessPolicyOptions);
        LocatorType locatorType = LocatorType.Origin;

        CreateLocatorOptions createLocatorOptions = new CreateLocatorOptions();
        LocatorInfo locatorInfo = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(), locatorType,
                createLocatorOptions);
        Date expirationDateTime = new Date();
        expirationDateTime.setTime(expirationDateTime.getTime() + 1000);

        // Act
        UpdateLocatorOptions updateLocatorOptions = new UpdateLocatorOptions()
                .setExpirationDateTime(expirationDateTime);
        service.updateLocator(locatorInfo.getId(), updateLocatorOptions);

        // Assert
        LocatorInfo locatorInfoActual = service.getLocator(locatorInfo.getId());
        assertEquals(locatorTypeExepcted, locatorInfoActual.getLocatorType());
        assertEquals(expirationDateTime, locatorInfoActual.getExpirationDateTime());

    }

    @Test
    public void deleteLocatorSuccess() throws ServiceException {
        // Arrange
        AssetInfo assetInfo = service.createAsset(null);
        AccessPolicyInfo accessPolicyInfo = service.createAccessPolicy("deleteLocatorsSuccess", 5);
        LocatorType locatorType = LocatorType.SAS;
        CreateLocatorOptions createLocatorOptions = new CreateLocatorOptions();
        LocatorInfo locatorInfo = service.createLocator(accessPolicyInfo.getId(), assetInfo.getId(), locatorType,
                createLocatorOptions);

        // Act
        service.deleteLocator(locatorInfo.getId());

        // Assert
        LocatorInfo locatorInfoResult = null;
        try {
            locatorInfoResult = service.getLocator(locatorInfo.getId());
        }
        catch (ServiceException e) {
            // swallow
        }
        assertNull(locatorInfoResult);
    }

    @Test(expected = ServiceException.class)
    public void deleteLocatorInvalidIdFailed() throws ServiceException {
        // Arrange 

        // Act
        service.deleteLocator("invalidLocatorId");

        // Assert
        assertTrue(false);
    }

    @Test
    public void listMediaProcessorsSuccess() throws ServiceException {
        // Arrange 

        // Act
        ListMediaProcessorsResult listMediaProcessorsResult = service.listMediaProcessors();

        // Assert
        assertNotNull(listMediaProcessorsResult);
        assertTrue(listMediaProcessorsResult.getMediaProcessorInfos().size() > 0);
    }

    @Test
    public void listMediaProcessorWithOptionSuccess() throws ServiceException {
        // Arrange
        ListMediaProcessorsOptions listMediaProcessorsOptions = new ListMediaProcessorsOptions();

        // Act
        ListMediaProcessorsResult listMediaProcessorsResult = service.listMediaProcessors(listMediaProcessorsOptions);

        // Assert
        assertNotNull(listMediaProcessorsResult);
        assertTrue(listMediaProcessorsResult.getMediaProcessorInfos().size() > 0);
    }

}
