/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.config.DisplayInfo.ModuleType;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.sensor.swe.SWETransactionalSensor;


/**
 * <p>
 * Configuration class for SPS connectors using the sensor API
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 13, 2014
 */
public class SensorConnectorConfig extends SPSConnectorConfig
{

    @Required
    @DisplayInfo(desc="Local ID of sensor to send commands to")
    @FieldType(Type.MODULE_ID)
    @ModuleType(ISensorModule.class)
    public String sensorID;
    
    
    @DisplayInfo(desc="Names of sensor command interfaces to hide from SPS")
    public List<String> hiddenCommands = new ArrayList<>();


    @Override
    protected ISPSConnector getConnector(SPSServlet servlet) throws SensorHubException
    {
        IModule<?> sensor = servlet.getParentHub().getModuleRegistry().getModuleById(sensorID);
        
        if (sensor instanceof SWETransactionalSensor)
            return new TransactionalSensorConnector(servlet, this);
        else
            return new DirectSensorConnector(servlet, this);
    }
}
