/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.persistence;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.FoiFilter;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.GenericStreamStorage;
import org.sensorhub.impl.persistence.InMemoryObsStorage;
import org.sensorhub.impl.persistence.InMemoryStorageConfig;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.test.sensor.FakeSensorData;
import org.sensorhub.test.sensor.FakeSensorWithPos;
import org.sensorhub.test.sensor.SensorConfigWithPos;


public class TestGenericStreamStorage
{
    private final static String OUTPUT_NAME = "out1";
    File configFile;
    FakeSensorWithPos fakeSensor;
    FakeSensorData fakeSensorData;
    GenericStreamStorage storage;
    ModuleRegistry registry;
    
    
    @Before
    public void setup() throws Exception
    {
        // get instance with in-memory DB
        registry = SensorHub.getInstance().getModuleRegistry();
        
        // create test sensor
        SensorConfigWithPos sensorCfg = new SensorConfigWithPos();
        sensorCfg.autoStart = false;
        sensorCfg.moduleClass = FakeSensorWithPos.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        sensorCfg.setLocation(45., 2.5, 325.);
        fakeSensor = (FakeSensorWithPos)registry.loadModule(sensorCfg);
        fakeSensorData = new FakeSensorData(fakeSensor, OUTPUT_NAME, 10, 0.05, 10);
        fakeSensor.setDataInterfaces(fakeSensorData);
        registry.startModule(fakeSensor.getLocalID());
        
        // create test storage
        StreamStorageConfig genericStorageConfig = new StreamStorageConfig();
        genericStorageConfig.moduleClass = GenericStreamStorage.class.getCanonicalName();
        genericStorageConfig.name = "SensorStorageTest";
        genericStorageConfig.autoStart = true;
        genericStorageConfig.dataSourceID = fakeSensor.getLocalID();
        StorageConfig storageConfig = new InMemoryStorageConfig();
        storageConfig.moduleClass = InMemoryObsStorage.class.getCanonicalName();
        genericStorageConfig.storageConfig = storageConfig;
        storage = (GenericStreamStorage)registry.loadModule(genericStorageConfig);
    }
    
    
    @Test
    public void testSaveDescription() throws Exception
    {
        while (fakeSensorData.isEnabled())
            Thread.sleep((long)(fakeSensorData.getAverageSamplingPeriod() * 500));
        
        Thread.sleep(100);
        assertTrue("Description should not be null", storage.getLatestDataSourceDescription() != null);
        assertEquals("Incorrect sensor description", fakeSensor.getCurrentDescription(), storage.getLatestDataSourceDescription());
    }
    
    
    @Test
    public void testSaveRecords() throws Exception
    {
        while (fakeSensorData.isEnabled())
            Thread.sleep((long)(fakeSensorData.getAverageSamplingPeriod() * 500));
        
        Thread.sleep(100);
        assertEquals("Wrong number of records in storage", fakeSensorData.getMaxSampleCount(), storage.getNumRecords(OUTPUT_NAME));
    }
    
    
    @Test
    public void testSaveFoi() throws Exception
    {
        while (fakeSensorData.isEnabled())
            Thread.sleep((long)(fakeSensorData.getAverageSamplingPeriod() * 500));
        
        Thread.sleep(100);
        
        FoiFilter filter = new FoiFilter();
        assertEquals("Wrong number of FOIs in storage", 1, storage.getNumFois(filter));
        assertEquals("Incorrect feature of interest", fakeSensor.getCurrentFeatureOfInterest(), storage.getFois(filter).next());
    }
    
    
    @After
    public void cleanup()
    {
        try
        {
            registry.shutdown(false, false);
            SensorHub.clearInstance();
        }
        catch (SensorHubException e)
        {
        }
    }
}
