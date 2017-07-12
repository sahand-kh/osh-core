/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import java.util.Map;
import org.sensorhub.api.data.ICommandReceiver;
import org.sensorhub.api.data.IDataProducer;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Interface for data processing components run by OSH.<br/>
 * Depending on the type of data sources, the process can be a streaming
 * process (i.e. always running to process incoming data streams) or an
 * on-demand process that is triggered externally.<br/> The process becomes
 * an on-demand process if one or more inputs are exposed through the
 * {@link org.sensorhub.api.data.ICommandReceiver} interface.<br/>
 * In both cases, data is produced on output interface(s) and can be either
 * polled or pushed to registered listeners.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2017
 */
public interface IDataProcess extends IDataProducer, ICommandReceiver
{
    
    /**
     * Gets the list of all inputs exposed by this process.<br/>
     * Note that only inputs that are not connected to data sources will be
     * available for external trigger via the command interface
     * @return map of input descriptors
     */
    public Map<String, DataComponent> getInputs();
    
    
    /**
     * Gets the list of parameters for this process.<br/>
     * Values should be changed directly inside the objects returned in the map.<br/>
     * For stream processing, parameters that can be changed during processing
     * must be marked as 'updatable'. Changing the value during processing of
     * parameters that are not updatable is either silently ignored or can result
     * in a processing exception.
     * @return map of parameter descriptors
     */
    public Map<String, DataComponent> getParameters();
    
    
    /**
     * Commit new values of parameters<br/>
     * New values are taken into account ASAP by the running process after the call
     * to this method.
     */
    public void commit();
    
    
    /**
     * Pauses process execution.<br/>
     * Incoming events are simply discarded and won't be processed when the process is resumed.
     */
    public void pause();
    
    
    /**
     * Resumes normal process execution.<br/>
     * Processing may actually resume only when the next event is received.
     */
    public void resume();
}
