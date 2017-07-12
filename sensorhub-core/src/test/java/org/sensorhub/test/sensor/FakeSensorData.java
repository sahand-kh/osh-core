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

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TimeImpl;
import org.vast.swe.SWEConstants;
import org.vast.util.DateTimeFormat;


/**
 * <p>
 * Fake sensor output implementation for testing sensor data API
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 20, 2013
 */
public class FakeSensorData extends AbstractSensorOutput<IDataProducer> implements IFakeSensorOutput
{
    public static final String URI_OUTPUT1 = "urn:blabla:weatherData";
    public static final String URI_OUTPUT1_FIELD1 = "urn:blabla:temperature";
    public static final String URI_OUTPUT1_FIELD2 = "urn:blabla:windSpeed";
    public static final String URI_OUTPUT1_FIELD3 = "urn:blabla:pressure";
    
    String name;
    DataComponent outputStruct;
    DataEncoding outputEncoding;
    int maxSampleCount;
    int sampleCount;
    int bufferSize;
    double samplingPeriod; // seconds
    Deque<DataBlock> dataQueue;
    Timer timer;
    TimerTask sendTask;
    boolean started;
    boolean hasListeners;
    
    
    public FakeSensorData(FakeSensor sensor, String name)
    {
        this(sensor, name, 1, 1.0, 5);
    }
    
    
    public FakeSensorData(FakeSensor sensor, final String name, final int bufferSize, final double samplingPeriod, final int maxSampleCount)
    {
        super(name, sensor);
        this.name = name;
        this.bufferSize = bufferSize;
        this.samplingPeriod = samplingPeriod;
        this.dataQueue = new LinkedBlockingDeque<DataBlock>(bufferSize);
        this.maxSampleCount = maxSampleCount;
        init();
    }
    
    
    public FakeSensorData(IDataProducer sensor, final String name, final int bufferSize, final double samplingPeriod, final int maxSampleCount, final ISensorHub hub)
    {
        super(name, sensor, hub);
        this.name = name;
        this.bufferSize = bufferSize;
        this.samplingPeriod = samplingPeriod;
        this.dataQueue = new LinkedBlockingDeque<DataBlock>(bufferSize);
        this.maxSampleCount = maxSampleCount;
        init();
    }
    
    
    public void init()
    {
        outputStruct = new DataRecordImpl(3);
        outputStruct.setName(this.name);
        outputStruct.setDefinition(URI_OUTPUT1);
        
        Time time = new TimeImpl();
        time.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        time.getUom().setHref(Time.ISO_TIME_UNIT);
        outputStruct.addComponent("time", time);
        
        Quantity temp = new QuantityImpl();
        temp.setDefinition(URI_OUTPUT1_FIELD1);
        temp.getUom().setCode("Cel");
        outputStruct.addComponent("temp", temp);
        
        Quantity wind = new QuantityImpl();
        wind.setDefinition(URI_OUTPUT1_FIELD2);
        wind.getUom().setCode("m/s");
        outputStruct.addComponent("windSpeed", wind);
        
        Quantity press = new QuantityImpl();
        press.setDefinition(URI_OUTPUT1_FIELD3);
        press.getUom().setCode("hPa");
        outputStruct.addComponent("press", press);
        
        outputEncoding = new TextEncodingImpl(",", "\n");
    }


    public int getMaxSampleCount()
    {
        return maxSampleCount;
    }


    @Override
    public void start(boolean waitForListeners)
    {
        sendTask = new TimerTask()
        {
            @Override
            public void run()
            {
                // safety to make sure we don't output more samples than requested
                // cancel does not seem to be taken into account early enough with high rates
                if (sampleCount >= maxSampleCount)
                    return;
                
                synchronized (dataQueue)
                {
                    // miss random samples 20% of the time
                    if (Math.random() > 0.8)
                        return;
                    
                    double samplingTime = System.currentTimeMillis() / 1000.;
                    DataBlock data = new DataBlockDouble(4);
                    data.setDoubleValue(0, samplingTime);
                    data.setDoubleValue(1, 1.0 + ((int)(Math.random()*100))/1000.);
                    data.setDoubleValue(2, 2.0 + ((int)(Math.random()*100))/1000.);
                    data.setDoubleValue(3, 3.0 + ((int)(Math.random()*100))/1000.);
                               
                    sampleCount++;
                    String isoTime = new DateTimeFormat().formatIso(samplingTime, 0);
                    System.out.println("Weather record #" + sampleCount + " generated @ " + isoTime);
                    if (sampleCount >= maxSampleCount)
                        cancel();
                    
                    if (dataQueue.size() == bufferSize)
                        dataQueue.remove();
                    dataQueue.offer(data);
                    
                    latestRecordTime = System.currentTimeMillis();
                    eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FakeSensorData.this, data));
                }                        
            }                
        };
        
        // we start sending only if we have listeners
        if (hasListeners || !waitForListeners)
            startSending();
        started = true;
    }
    
    
    protected void startSending()
    {
        if (timer == null)
        {
            timer = new Timer(name, true);
            timer.scheduleAtFixedRate(sendTask, 0, (long)(samplingPeriod * 1000));
        }
    }


    @Override
    public DataBlock getLatestRecord()
    {
        synchronized (dataQueue)
        {   
            // make sure the first record is produced
            if (!hasListeners & sendTask != null)
            {
                while (dataQueue.isEmpty())
                    sendTask.run();
            }
            
            return dataQueue.peekLast();
        }
    }
    
    
    @Override
    public void stop()
    {
        if (timer != null)
        {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }
    
    
    @Override
    public boolean isEnabled()
    {
        if (sampleCount >= maxSampleCount)
            return false;
        else
            return true;
    }    
    
    
    @Override
    public boolean isStorageSupported()
    {
        return (bufferSize > 0);
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriod;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return outputStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        return bufferSize;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        synchronized (dataQueue)
        {
            return dataQueue.size();
        }
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        synchronized (dataQueue)
        {
            List<DataBlock> records = new ArrayList<DataBlock>(maxRecords);
            
            if (clear)
            {
                for (int i=0; i<maxRecords; i++)
                    records.add(0, dataQueue.pollLast());
            }
            else
            {
                Iterator<DataBlock> it = dataQueue.descendingIterator();
                for (int i=0; it.hasNext() && i<maxRecords; i++)
                    records.add(0, it.next());
            }
            
            return records;
        }
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        synchronized (dataQueue)
        {
            List<DataBlock> records = new ArrayList<DataBlock>();
            
            if (clear)
            {
                while (!dataQueue.isEmpty())
                    records.add(0, dataQueue.poll());
            }
            else
            {
                for (DataBlock data: dataQueue)
                    records.add(data);
            }
            
            return records;
        } 
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        synchronized (dataQueue)
        {
            int numRecords = dataQueue.size();
            dataQueue.clear();
            return numRecords;
        }
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        super.registerListener(listener);
        
        // we start sending only if start has been called
        if (started)
            startSending();
        
        hasListeners = true;
    }

}
