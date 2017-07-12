/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.Iterator;
import net.opengis.gml.v32.AbstractFeature;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.utils.MsgUtils;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Factory for streaming data providers with storage.<br/>
 * Most of the logic is inherited from {@link StorageDataProviderFactory}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ProducerType> Type of producer handled by this provider
 * @since Feb 28, 2015
 */
public class StreamWithStorageProviderFactory<ProducerType extends IDataProducer> extends StorageDataProviderFactory
{
    final ProducerType producer;
    final StreamDataProviderConfig altConfig;
    final StreamDataProviderFactory<ProducerType> altProvider;
    long liveDataTimeOut;
    
    
    public StreamWithStorageProviderFactory(SOSServlet servlet, StreamDataProviderConfig config, ProducerType producer) throws SensorHubException
    {
        super(servlet, new StorageDataProviderConfig(config));
        this.producer = producer;
        this.altConfig = config;
        this.liveDataTimeOut = (long)(config.liveDataTimeout * 1000);
        
        // build alt provider to generate capabilities in case storage is disabled
        this.altProvider = new StreamDataProviderFactory<>(config, producer, "Stream");
        
        // listen to producer lifecycle events
        disableEvents = true; // disable events on startup
        producer.registerListener(this);
        disableEvents = false;
    }


    @Override
    public SOSOfferingCapabilities generateCapabilities() throws SensorHubException
    {
        SOSOfferingCapabilities capabilities;
        
        if (storage.isStarted())
        {
            capabilities = super.generateCapabilities();
            
            // if storage does support FOIs, list the current ones
            if (!(storage instanceof IObsStorage))
                FoiUtils.updateFois(caps, producer, config.maxFois);
        }
        else
        {
            capabilities = altProvider.generateCapabilities();
        }
        
        // enable real-time requests only if streaming data source is enabled
        if (producer.isEnabled())
        {
            // replace description
            if (config.description == null && storage.isStarted())
                capabilities.setDescription("Live and archive data from " + producer.getName());
            
            // enable live by setting end time to now
            TimeExtent timeExtent = capabilities.getPhenomenonTime();
            if (timeExtent.isNull())
                timeExtent.setBeginNow(true);
            timeExtent.setEndNow(true);     
        }
        
        return capabilities;
    }
    
    
    @Override
    public void updateCapabilities() throws SensorHubException
    {
        if (caps == null)
            return;
        
        if (storage.isStarted())
            super.updateCapabilities();
        
        // enable real-time requests if streaming data source is enabled
        if (producer.isEnabled())
        {
            // if latest record is not too old, enable real-time
            if (altProvider.hasNewRecords(liveDataTimeOut))
                caps.getPhenomenonTime().setEndNow(true);
            
            // if storage does support FOIs, list the current ones
            if (!(storage instanceof IObsStorage))
                FoiUtils.updateFois(caps, producer, config.maxFois);
        }
    }


    @Override
    public Iterator<AbstractFeature> getFoiIterator(IFoiFilter filter) throws SensorHubException
    {
        Iterator<AbstractFeature> foiIt = super.getFoiIterator(filter);
        if (!foiIt.hasNext())
            foiIt = FoiUtils.getFilteredFoiIterator(producer, filter);
        return foiIt;
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        // we can receive events before producer is even set because we first
        // register only to storage, so just ignore those
        if (disableEvents || producer == null)
            return;
        
        if (e instanceof ModuleEvent)
        {
            switch (((ModuleEvent)e).getType())
            {
                // show/hide offering when producer or storage is enabled/disabled
                case STATE_CHANGED:
                    if (e.getSource() == producer || e.getSource() == storage)
                    {
                        ModuleState state = ((ModuleEvent)e).getNewState();
                        if (state == ModuleState.STARTED || state == ModuleState.STOPPING)
                        {
                            if (isEnabled())
                                servlet.showProviderCaps(this);
                            else
                                servlet.hideProviderCaps(this);
                        }
                    }
                    break;
                
                // cleanly transmute provider when producer or storage is deleted
                case DELETED:
                    if (e.getSource() == producer)
                        servlet.onSensorDeleted(config.offeringID);
                    else if (e.getSource() == storage)
                        servlet.onStorageDeleted(config.offeringID);
                    break;
                    
                default:
                    return;
            }
        }
    }
    
    
    @Override
    protected void checkEnabled() throws SensorHubException
    {
        if (!config.enabled)
            throw new ServiceException("Offering " + config.offeringID + " is disabled");
        
        if (!storage.isStarted() && !producer.isEnabled())
            throw new ServiceException("Storage " + MsgUtils.moduleString(storage) + " is disabled");
    }
    
    
    @Override
    public boolean isEnabled()
    {
        if (config.enabled)
        {
            if (storage != null && storage.isStarted())
                return true;
            
            if (producer != null && producer.isEnabled())
                return true;
        }
        
        return false;
    }
    
    
    @Override
    public void cleanup()
    {
        super.cleanup();
        if (producer != null)
            producer.unregisterListener(this);
    }
    
    
    @Override
    public StreamDataProviderConfig getConfig()
    {
        return this.altConfig;
    }
}
