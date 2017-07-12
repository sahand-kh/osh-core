/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.processing;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.opengis.OgcPropertyList;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.IEntity;
import org.sensorhub.api.common.IEntityGroup;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingControlInterface;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.impl.module.AbstractModule;
import org.vast.process.ProcessException;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.SMLException;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLUtils;


/**
 * <p>
 * Implementation of processing API using the SensorML format for describing
 * the process flow and the SensorML execution engine to run the process.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> Type of process configuration
 * @since Feb 22, 2015
 */
public class SMLProcessImpl extends AbstractModule<SMLProcessConfig> implements IProcessModule<SMLProcessConfig>
{
    public static final String DEFAULT_ID = "PROCESS_DESC";
    protected static final String DATASRC_NAME = "datasource_";
    protected static final String DATASINK_NAME = "datasink_";
    protected static final String PROCESS_NAME = "main_process";
    protected static final int MAX_ERRORS = 10;
        
    protected Map<String, DataComponent> inputs = new LinkedHashMap<>();
    protected Map<String, DataComponent> outputs = new LinkedHashMap<>();
    protected Map<String, DataComponent> parameters = new LinkedHashMap<>();
    protected Map<String, IStreamingDataInterface> outputInterfaces = new LinkedHashMap<>();
    protected Map<String, IStreamingControlInterface> controlInterfaces = new LinkedHashMap<>();
    
    protected SMLUtils smlUtils;
    protected List<StreamDataSource> streamSources;
    protected AggregateProcessImpl wrapperProcess;
    //protected AbstractProcessImpl smlProcess; // actual processing component (can be atomic or a chain itself)
    protected long lastUpdatedProcess = Long.MIN_VALUE;
    protected boolean paused = false;
    protected int errorCount = 0;
    
    
    public SMLProcessImpl()
    {
        wrapperProcess = new AggregateProcessImpl();
        wrapperProcess.setUniqueIdentifier(UUID.randomUUID().toString());
    }
    
    
    @Override
    public void setParentHub(ISensorHub hub)
    {
        super.setParentHub(hub);
        smlUtils = new SMLUtils(SMLUtils.V2_0);
        smlUtils.setProcessFactory(hub.getProcessingManager());
    }


    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // only go further if sensorML file was provided
        // otherwise we'll do it at the next update
        if (config.sensorML != null)
        {
            String smlPath = config.getSensorMLPath();
            
            // parse SensorML file
            try (InputStream is = new BufferedInputStream(new FileInputStream(smlPath)))
            {
                wrapperProcess = (AggregateProcessImpl)smlUtils.readProcess(is);
                //smlProcess = (AbstractProcessImpl)wrapperProcess.getComponent(PROCESS_NAME);
            }
            catch (Exception e)
            {
                throw new ProcessingException(String.format("Cannot read SensorML description from '%s'", smlPath), e);
            }
            
            initChain();
        }
    }
    
    
    protected void initChain() throws SensorHubException
    {
        if (wrapperProcess != null)
        {
            // make process executable
            try
            {
                smlUtils.makeProcessExecutable(wrapperProcess, true);
            }
            catch (SMLException e)
            {
                throw new ProcessingException("Cannot prepare process chain for execution", e);
            }
            
            // advertise process inputs and outputs
            refreshIOList(wrapperProcess.getInputList(), inputs, false);
            refreshIOList(wrapperProcess.getParameterList(), parameters, false);
            refreshIOList(wrapperProcess.getOutputList(), outputs, true);
        }
    }
    
    
    public AggregateProcessImpl getProcessChain()
    {
        return wrapperProcess;
    }
    
    
    protected void refreshIOList(OgcPropertyList<AbstractSWEIdentifiable> ioList, Map<String, DataComponent> ioMap, boolean isOutput) throws ProcessingException
    {
        ioMap.clear();
        if (isOutput)
            outputInterfaces.clear();
                
        int numSignals = ioList.size();
        for (int i=0; i<numSignals; i++)
        {
            String ioName = ioList.getProperty(i).getName();
            AbstractSWEIdentifiable ioDesc = ioList.get(i);
            DataComponent ioComponent = SMLHelper.getIOComponent(ioDesc);
            ioComponent.setName(ioName);
            ioMap.put(ioName, ioComponent.copy());
            
            if (isOutput)
                outputInterfaces.put(ioName, new SMLOutputInterface(this, ioComponent));
        }
    }
    
    
    /**
     * Helper method to make sure derived classes add outputs consistently in the different maps
     * @param outputInterface
     */
    protected void addOutput(IStreamingDataInterface outputInterface)
    {
        String outputName = outputInterface.getName();
        outputs.put(outputName, outputInterface.getRecordDescription());
        outputInterfaces.put(outputName, outputInterface);
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        errorCount = 0; 
        
        if (wrapperProcess == null)
            throw new ProcessingException("No valid processing chain provided");
        
        // start processing thread
        try
        {
            wrapperProcess.setParentLogger(getLogger());
            wrapperProcess.start();
        }
        catch (ProcessException e)
        {
            throw new ProcessingException("Cannot start process chain thread", e);
        }
    }
    
    
    @Override
    public void stop()
    {
        if (wrapperProcess != null && wrapperProcess.isExecutable())
            wrapperProcess.stop();
    }


    @Override
    public void pause()
    {
        
    }


    @Override
    public void resume()
    {
        
    }


    @Override
    public Map<String, DataComponent> getInputs()
    {
        return Collections.unmodifiableMap(inputs);
    }
    
    
    @Override
    public Map<String, DataComponent> getParameters()
    {
        // for parameters we actually maintain a buffer so they
        // can be set during process execution
        return Collections.unmodifiableMap(parameters);
    }
    
    
    @Override
    public Map<String, IStreamingDataInterface> getOutputs()
    {
        return Collections.unmodifiableMap(outputInterfaces);
    }


    @Override
    public Map<String, ? extends IStreamingControlInterface> getCommandInputs()
    {
        return Collections.unmodifiableMap(controlInterfaces);
    }


    @Override
    public synchronized void commit()
    {
        // pause processing
        pause();
        
        // transfer parameters to running process
                
        
        // resume processing
        resume();
    }
    
    
    @Override
    public String getUniqueIdentifier()
    {
        return wrapperProcess.getUniqueIdentifier();
    }


    @Override
    public synchronized AbstractProcess getCurrentDescription()
    {
        return wrapperProcess;
    }


    @Override
    public synchronized long getLastDescriptionUpdate()
    {
        return lastUpdatedProcess;
    }


    @Override
    public IEntityGroup<IEntity> getParentGroup()
    {
        return null;
    }
    
    
    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        return null;
    }


    @Override
    public boolean isEnabled()
    {
        return isStarted();
    }


    @Override
    public void cleanup()
    {
    }
}
