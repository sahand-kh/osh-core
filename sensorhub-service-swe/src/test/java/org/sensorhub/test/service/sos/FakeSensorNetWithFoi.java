/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalComponent;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.IEntity;
import org.sensorhub.api.common.IEntityGroup;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;
import org.sensorhub.test.sensor.IFakeSensorOutput;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.sensorML.SMLFactory;


public class FakeSensorNetWithFoi extends FakeSensor implements IMultiSourceDataProducer
{
    static int MAX_FOIS = 3;
    static String FOI_UID_PREFIX = "urn:blabla:myfois:";
    static String SENSOR_UID_PREFIX = "urn:blabla:sensors:";
    GMLFactory gmlFac = new GMLFactory(true);
    Map<String, IDataProducer> sensors;
    Map<String, AbstractFeature> fois;
    
    
    class FakeDataProducer implements IDataProducer
    {
        PhysicalComponent sensor;
        AbstractFeature foi;
        Map<String, IStreamingDataInterface> outputs = new HashMap<>();
        
        public FakeDataProducer(int entityIndex)
        {
            Point p = gmlFac.newPoint();
            p.setPos(new double[] {entityIndex, entityIndex, 0.0});
            
            // create sensor description
            String entityID = SENSOR_UID_PREFIX + entityIndex;
            SMLFactory fac = new SMLFactory();
            sensor = fac.newPhysicalComponent();
            sensor.setUniqueIdentifier(entityID);
            sensor.setName("Networked sensor " + entityID.substring(entityID.lastIndexOf(':')+1));
            sensor.addPositionAsPoint(p);
            
            // create FOI
            QName fType = new QName("http://myNsUri", "MyFeature");
            String foiID = FOI_UID_PREFIX + entityIndex;
            foi = new GenericFeatureImpl(fType);
            foi.setId("F" + entityIndex);
            foi.setUniqueIdentifier(foiID);
            foi.setName("FOI" + entityIndex);
            foi.setDescription("This is feature of interest #" + entityIndex);           
            foi.setLocation(p);            
            fois.put(foiID, foi);
            
            // create output
            ISensorHub hub = FakeSensorNetWithFoi.this.getParentHub();
            outputs.put(TestSOSService.NAME_OUTPUT1,
                    new FakeSensorData(this, TestSOSService.NAME_OUTPUT1, 1, TestSOSService.SAMPLING_PERIOD, TestSOSService.NUM_GEN_SAMPLES, hub));
            outputs.put(TestSOSService.NAME_OUTPUT2,
                    new FakeSensorData2(this, TestSOSService.NAME_OUTPUT2, TestSOSService.SAMPLING_PERIOD, TestSOSService.NUM_GEN_SAMPLES, null, hub));
        }

        @Override
        public IEntityGroup<? extends IEntity> getParentGroup()
        {
            return FakeSensorNetWithFoi.this;
        }
        
        @Override
        public String getUniqueIdentifier()
        {
            return sensor.getUniqueIdentifier();
        }

        @Override
        public AbstractProcess getCurrentDescription()
        {
            return sensor;
        }

        @Override
        public long getLastDescriptionUpdate()
        {
            return 0;
        }

        @Override
        public Map<String, ? extends IStreamingDataInterface> getOutputs()
        {
            return Collections.unmodifiableMap(outputs);
        }

        @Override
        public AbstractFeature getCurrentFeatureOfInterest()
        {
            return foi;
        }

        @Override
        public void registerListener(IEventListener listener)
        {                
        }

        @Override
        public void unregisterListener(IEventListener listener)
        {                
        }

        @Override
        public String getName()
        {
            return sensor.getName();
        }

        @Override
        public boolean isEnabled()
        {
            return FakeSensorNetWithFoi.this.isStarted();
        }            
    };
    
    
    public FakeSensorNetWithFoi()
    {
        this.uniqueID = "urn:sensors:mysensornetwork:001";
        this.xmlID = "SENSORNET";
    }
    
    
    @Override
    public void init()
    {
        sensors = new LinkedHashMap<>();
        fois = new LinkedHashMap<>();
        
        for (int foiNum = 1; foiNum <= MAX_FOIS; foiNum++)
        {
            FakeDataProducer producer = new FakeDataProducer(foiNum);
            sensors.put(producer.getUniqueIdentifier(), producer);
        }
    }


    @Override
    public Map<String, IDataProducer> getEntities()
    {
        return Collections.unmodifiableMap(sensors);
    }


    @Override
    public Map<String, AbstractFeature> getFeaturesOfInterest()
    {
        return Collections.unmodifiableMap(fois);
    }


    @Override
    public Collection<String> getEntitiesWithFoi(String foiID)
    {
        if (!fois.containsKey(foiID))
            return Collections.EMPTY_SET;
        
        String entityID = foiID.replace(FOI_UID_PREFIX, SENSOR_UID_PREFIX);
        return Arrays.asList(entityID);
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            int index = 0;
            for (IDataProducer sensor: sensors.values())
            {
                index++;
                ((PhysicalSystem)sensorDescription).getComponentList().add("sensor"+index, sensor.getUniqueIdentifier(), null);
            }
        }
    }


    @Override
    public void start() throws SensorHubException
    {        
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        for (IDataProducer sensor: sensors.values())
            for (IStreamingDataInterface o: sensor.getOutputs().values())
                ((IFakeSensorOutput)o).stop();
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
    }


    public void startSendingData(boolean waitForListeners)
    {
        for (IDataProducer sensor: sensors.values())
            for (IStreamingDataInterface o: sensor.getOutputs().values())
                ((IFakeSensorOutput)o).start(waitForListeners);        
    }
    
    
    public boolean hasMoreData()
    {
        for (IDataProducer sensor: sensors.values())
            for (IStreamingDataInterface o: sensor.getOutputs().values())
                if (o.isEnabled())
                    return true;
        
        return false;
    }
    
}
