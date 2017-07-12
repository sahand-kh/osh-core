/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.swe;

import java.io.IOException;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.swe.SWETransactionalSensor;
import org.sensorhub.impl.sensor.swe.SWETransactionalSensorConfig;
import org.vast.ows.OWSException;
import org.vast.ows.OWSExceptionReport;
import org.vast.util.Asserts;
import net.opengis.sensorml.v20.AbstractProcess;


public class TransactionUtils
{
    private static final String INVALID_SML_MSG = "Invalid SensorML description: ";
    
    
    /*
     * Create a new SWETransactionalSensor module for handling incoming data
     */
    public static IModule<?> createSensorModule(ISensorHub hub, String sensorUID, AbstractProcess sensorDesc) throws IOException
    {
        // create virtual sensor module if needed
        try
        {
            String sensorName = sensorDesc.getName();
            if (sensorName == null)
                sensorName = sensorDesc.getId();
            
            SWETransactionalSensorConfig sensorConfig = new SWETransactionalSensorConfig();
            sensorConfig.autoStart = false;
            sensorConfig.id = sensorUID;
            sensorConfig.name = sensorName;
            
            ModuleRegistry moduleReg = hub.getModuleRegistry();
            SWETransactionalSensor sensorModule = (SWETransactionalSensor)moduleReg.loadModule(sensorConfig);
            sensorConfig.autoStart = true;
            sensorModule.requestInit(false);
            sensorModule.updateSensorDescription(sensorDesc);
            
            // make sure virtual sensor module is started            
            return moduleReg.startModule(sensorUID, 1000);
        }
        catch (SensorHubException e)
        {
            throw new IOException("Cannot register sensor " + sensorUID, e);
        }        
    }
    
    
    /*
     * Checks and update the SensorML description of a SWETransactionalSensor module
     */
    public static void updateSensorDescription(IModule<?> sensor, AbstractProcess newSensorDesc) throws OWSException
    {
        Asserts.checkArgument(sensor instanceof ISensorModule, "Target module is not a sensor!!");                
        ISensorModule<?> sensorModule = (ISensorModule<?>)sensor;
        
        // check that unique ID is same as one being updated
        if (!sensorModule.getUniqueIdentifier().equals(newSensorDesc.getUniqueIdentifier()))
            throw new OWSException("The unique ID in the description must be the same as the UID of the procedure being updated");        
        
        // check that sensor has been previously created by SOS or SPS
        if (!(sensor instanceof SWETransactionalSensor))
            throw new OWSException("A procedure with unique ID " + sensorModule.getUniqueIdentifier() + " is already registered on this server");
        
        // actual update
        ((SWETransactionalSensor)sensor).updateSensorDescription(newSensorDesc);
    }
    
    
    /*
     * Checks that SensorML description contains proper info
     */
    public static void checkSensorML(AbstractProcess smlProcess, OWSExceptionReport report) throws OWSException
    {
        String sensorUID = smlProcess.getUniqueIdentifier();
        
        if (sensorUID == null || sensorUID.length() == 0)
            throw new OWSException(OWSException.invalid_param_code, "procedureDescription", null, INVALID_SML_MSG + "Missing unique ID");
        
        if (sensorUID.length() < 10)
            report.add(new OWSException(OWSException.invalid_param_code, "procedureDescription", sensorUID, INVALID_SML_MSG + "Procedure unique ID is too short"));
    }
}
