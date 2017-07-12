/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor;

import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.PointImpl;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


public class FakeSensor extends AbstractSensorModule<SensorConfig>
{
   
    
    public FakeSensor()
    {
        this.uniqueID = "urn:sensors:mysensor:001";
        this.xmlID = "SENSOR1";
    }
    
    
    public void setSensorUID(String sensorUID)
    {
        this.uniqueID = sensorUID;
    }
    
    
    public void setDataInterfaces(ISensorDataInterface... outputs) throws SensorException
    {
        for (ISensorDataInterface o: outputs)
            addOutput(o, false);
    }
    
    
    public void setControlInterfaces(ISensorControlInterface... inputs) throws SensorException
    {
        for (ISensorControlInterface i: inputs)
            addControlInput(i);
    }
    

    @Override
    public void updateConfig(SensorConfig config) throws SensorHubException
    {
    }
    
    
    @Override
    public void init() throws SensorHubException
    {        
    }


    @Override
    public void start() throws SensorHubException
    {        
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        for (ISensorDataInterface o: getObservationOutputs().values())
            ((IFakeSensorOutput)o).stop();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            Point pos = new PointImpl(3);
            pos.setId("P01");
            pos.setSrsName("http://www.opengis.net/def/crs/EPSG/0/4979");
            pos.setPos(new double[] {45.6, 2.3, 193.2});
            ((PhysicalSystem)sensorDescription).addPositionAsPoint(pos);
        }
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
        for (ISensorDataInterface o: getObservationOutputs().values())
        {
            o.getLatestRecord();
            ((IFakeSensorOutput)o).start(waitForListeners);
        }            
    }
    
    
    public boolean hasMoreData()
    {
        for (IStreamingDataInterface o: getOutputs().values())
            if (o.isEnabled())
                return true;            
        
        return false;
    }
}
