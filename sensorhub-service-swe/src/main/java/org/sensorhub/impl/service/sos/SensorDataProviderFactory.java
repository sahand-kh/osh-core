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

import net.opengis.sensorml.v20.AbstractProcess;
import java.io.IOException;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensor;
import org.sensorhub.api.service.ServiceException;


/**
 * <p>
 * Factory for sensor data providers.<br/>
 * Most of the logic is inherited from {@link StreamDataProviderFactory}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 15, 2013
 */
public class SensorDataProviderFactory extends StreamDataProviderFactory<ISensor> implements ISOSDataProviderFactory, IEventListener
{
    
    
    protected SensorDataProviderFactory(SOSServlet servlet, SensorDataProviderConfig config) throws SensorHubException
    {
        super(servlet, config,
              (ISensor)servlet.getParentHub().getModuleRegistry().getModuleById(config.sensorID),
              "Sensor");
    }
    
    
    @Override
    public AbstractProcess generateSensorMLDescription(double time) throws SensorHubException
    {
        checkEnabled();
        return producer.getCurrentDescription();
    }

    
    @Override
    public ISOSDataProvider getNewDataProvider(SOSDataFilter filter) throws SensorHubException
    {
        checkEnabled();
        
        try
        {
            return new SensorDataProvider(producer, (SensorDataProviderConfig)config, filter);
        }
        catch (IOException e)
        {
            throw new ServiceException("Cannot instantiate sensor provider", e);
        }
    }
}
