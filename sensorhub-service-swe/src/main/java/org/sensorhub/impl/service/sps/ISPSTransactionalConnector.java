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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.swe.ITaskingCallback;
import org.sensorhub.impl.service.swe.Template;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Extension to SPS connector interface to allow registering new command types
 * and receive corresponding command data.<br/>
 * This is typically used to forward commands to a remote sensor or process
 * connected to SPS via a persistent tasking stream
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 20, 2016
 */
public interface ISPSTransactionalConnector extends ISPSConnector
{
    
    /**
     * Requests connector to prepare for receiving new command type with given
     * data structure and encoding
     * @param component
     * @param encoding
     * @return
     * @throws SensorHubException
     */
    public String newTaskingTemplate(DataComponent component, DataEncoding encoding) throws SensorHubException;
    
    
    /**
     * Retrieve previously registered tasking template information
     * @param templateID
     * @return command message description (structure + encoding)
     * @throws SensorHubException 
     */
    public Template getTemplate(String templateID) throws SensorHubException;
    
    
    /**
     * Registers the tasking callback for the given template ID
     * @param templateID
     * @param callback
     */
    public void registerCallback(String templateID, ITaskingCallback callback);
    
    
    /**
     * Called when the connector is disposed
     */    
    public void cleanup();
    
}
