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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.processing.IDataProcess;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.utils.MsgUtils;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Factory for streaming data providers with storage.<br/>
 * Most of the logic is inherited from {@link StreamWithStorageProviderFactory}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2015
 */
public class StreamProcessWithStorageProviderFactory extends StreamWithStorageProviderFactory<IDataProcess>
{
    StreamProcessProviderConfig streamProviderConfig;
    
    
    public StreamProcessWithStorageProviderFactory(SOSServlet servlet, StreamProcessProviderConfig config) throws SensorHubException
    {
        super(servlet, config,
              (IDataProcess)servlet.getParentHub().getModuleRegistry().getModuleById(config.processID));
        
        this.streamProviderConfig = config;
    }


    @Override
    public ISOSDataProvider getNewDataProvider(SOSDataFilter filter) throws SensorHubException
    {
        TimeExtent timeRange = filter.getTimeRange();
        
        if (timeRange.isBaseAtNow() || timeRange.isBeginNow())
        {
            if (!producer.isEnabled())
                throw new ServiceException("Process " + MsgUtils.entityString(producer) + " is disabled");
            
            try
            {
                return new StreamProcessDataProvider(producer, streamProviderConfig, filter);
            }
            catch (Exception e)
            {
                throw new ServiceException("Cannot instantiate processing provider", e);
            }
        }
        else
        {            
            return super.getNewDataProvider(filter);
        }
    }
}
