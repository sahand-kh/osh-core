/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.List;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Class providing default implementation of common sensor data interface
 * API methods. By default, storage is unsupported.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ProducerType> Type of parent sensor
 * @since Nov 2, 2014
 */
public abstract class AbstractSensorOutput<ProducerType extends IDataProducer> implements ISensorDataInterface
{
    protected static final String ERROR_NO_STORAGE = "Data storage is not supported by driver ";
    protected ProducerType parentSensor;
    protected Logger log;
    protected String name;
    protected IEventHandler eventHandler;
    protected DataBlock latestRecord;
    protected long latestRecordTime = Long.MIN_VALUE;
    
    
    public AbstractSensorOutput(ISensorModule<?> parentSensor)
    {
        this(null, parentSensor);
    }
    
    
    public AbstractSensorOutput(String name, ISensorModule<?> parentSensor)
    {
        this(name, (ProducerType)parentSensor, parentSensor.getParentHub());
    }
    
    
    public AbstractSensorOutput(String name, ProducerType parentSensor, ISensorHub hub)
    {
        this.name = name;
        this.parentSensor = parentSensor;
        
        // setup log
        if (parentSensor instanceof AbstractSensorModule)
            this.log = ((AbstractSensorModule<?>)parentSensor).getLogger();
        else
            this.log = LoggerFactory.getLogger(getClass().getCanonicalName());        

        // obtain an event handler for this output
        String moduleID = parentSensor.getUniqueIdentifier();
        eventHandler = hub.getEventBus().registerProducer(moduleID, getName()); 
    }
    
    
    protected void stop()
    {
        // do nothing by default
    }
    
    
    @Override
    public ProducerType getProducer()
    {
        return parentSensor;
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return latestRecord;
    }
    
    
    @Override
    public long getLatestRecordTime()
    {
        return latestRecordTime;
    }
    
    
    @Override
    public boolean isStorageSupported()
    {
        return false;
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        return 0;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.entityString(parentSensor));
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.entityString(parentSensor));
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.entityString(parentSensor));
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.entityString(parentSensor));
    }
    
    
    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }

}
