/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.processing;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.process.ProcessInfo;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Text;


public class StreamDataSource extends ExecutableProcessImpl implements IEventListener, ISensorHubProcess
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("datasource:stream", "Stream Data Source", null, StreamDataSource.class);
    public static final String PRODUCER_URI_PARAM = "producerURI";
    
    ISensorHub hub;
    Text producerURI;
    WeakReference<IDataProducer> dataSourceRef;
    boolean started;
    boolean paused;
    
    
    public StreamDataSource()
    {
        super(INFO);
        SWEHelper fac = new SWEHelper();
        
        // param
        producerURI = fac.newText(SWEHelper.getPropertyUri("ProducerUID"), "Producer Unique ID", null);
        paramData.add(PRODUCER_URI_PARAM, producerURI);
        
        // output cannot be created until source URI param is set
    }


    @Override
    public void notifyParamChange()
    {
        String producerUri = producerURI.getData().getStringValue();
        
        if (producerUri != null)
        {
            IDataProducer producer = (IDataProducer)hub.getEntityManager().getEntity(producerUri);
            
            // set process info
            ProcessInfo instanceInfo = new ProcessInfo(
                    processInfo.getUri(),
                    producer.getName(),
                    processInfo.getDescription(),
                    processInfo.getImplementationClass());
            this.processInfo = instanceInfo;
            
            // add outputs
            outputData.clear();
            for (IStreamingDataInterface output: producer.getOutputs().values())
                outputData.add(output.getName(), output.getRecordDescription().copy());
            
            dataSourceRef = new WeakReference<>(producer);
        }
    }
    
    
    @Override
    public synchronized void start() throws ProcessException
    {
        IDataProducer producer = dataSourceRef.get();
        if (producer != null && !started)
        {
            started = true;
            
            // start listening to events on all connected outputs
            for (String outputName: outputConnections.keySet())
                producer.getOutputs().get(outputName).registerListener(this);
            
            getLogger().debug("Connected to data source '{}'", producer.getUniqueIdentifier());
        }
    }
    
    
    @Override
    public void start(ExecutorService threadPool) throws ProcessException
    {
        start();
    }


    @Override
    public synchronized void stop()
    {
        IDataProducer producer = dataSourceRef.get();
        if (producer != null && started)
        {
            started = false;
            
            // unregister listeners from all outputs
            for (IStreamingDataInterface output: producer.getOutputs().values())
                output.unregisterListener(this);        
            
            getLogger().debug("Disconnected from data source '{}'", producer.getUniqueIdentifier());
        }
    }


    public void pause()
    {
        this.paused = true;
    }


    public void resume()
    {
        this.paused = false;
    }


    @Override
    public void handleEvent(Event<?> e)
    {
        if (paused)
            return;
        
        if (e instanceof DataEvent)
        {
            // process each data block
            for (DataBlock dataBlk: ((DataEvent) e).getRecords())
            {
                String outputName = ((DataEvent) e).getChannelID();
                outputData.getComponent(outputName).setData(dataBlk);            
            
                try
                {
                    publishData(outputName);
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    @Override
    public void execute() throws ProcessException
    {
        // nothing to do, data is just pushed to outputs
        // when events are received
    }


    @Override
    public boolean needSync()
    {
        return false;
    }


    @Override
    public void setParentHub(ISensorHub hub)
    {
        this.hub = hub;        
    }

}
