/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.sensor.swe.DataStructureHash;
import org.sensorhub.impl.sensor.swe.SWETransactionalSensor;
import org.sensorhub.impl.sensor.swe.SWETransactionalSensorControl;
import org.sensorhub.impl.service.swe.Template;
import org.sensorhub.impl.sensor.swe.ITaskingCallback;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;


/**
 * <p>
 * Conenctor for receiving commands from a SWETransactionalSensor
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 20, 2016
 */
public class TransactionalSensorConnector extends DirectSensorConnector implements ISPSTransactionalConnector
{
    Map<DataStructureHash, String> structureToTemplateIdMap = new HashMap<DataStructureHash, String>();
    
    
    public TransactionalSensorConnector(SPSServlet service, SensorConnectorConfig config) throws SensorHubException
    {
        super(service, config);
    }
    
    
    @Override
    public String newTaskingTemplate(DataComponent component, DataEncoding encoding) throws SensorHubException
    {
        DataStructureHash templateHashObj = new DataStructureHash(component, encoding);
        String templateID = structureToTemplateIdMap.get(templateHashObj);
        
        try
        {
            // add new sensor control input if needed
            if (templateID == null)
            {
                String inputName = ((SWETransactionalSensor)sensor).newControlInput(component, encoding);
                templateID = generateTemplateID(inputName);
                structureToTemplateIdMap.put(templateHashObj, templateID);
            }
        }
        catch (SensorException e)
        {
            throw new ServiceException("Invalid template", e);
        }
        
        return templateID;
    }


    @Override
    public void registerCallback(String templateID, ITaskingCallback callback)
    {
        String inputName = getInputNameFromTemplateID(templateID);
        SWETransactionalSensorControl input = (SWETransactionalSensorControl)sensor.getCommandInputs().get(inputName);
        input.registerCallback(callback);
    }
    
    
    public Template getTemplate(String templateID) throws SensorHubException
    {
        String paramName = getInputNameFromTemplateID(templateID);
        DataStream param = (DataStream)sensor.getCurrentDescription().getParameter(paramName);
        if (param == null)
            throw new ServiceException("Invalid tasking parameter: " + templateID);
        
        Template template = new Template();
        template.component = param.getElementType();
        template.encoding = param.getEncoding();
        return template;
    }
    
    
    protected final String generateTemplateID(String inputName)
    {
        return sensor.getLocalID() + '#' + inputName;
    }
    
    
    protected final String getInputNameFromTemplateID(String templateID)
    {
        return templateID.substring(templateID.lastIndexOf('#')+1);
    }

}
