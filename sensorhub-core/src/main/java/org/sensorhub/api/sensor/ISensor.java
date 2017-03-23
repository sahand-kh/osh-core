/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.Map;
import org.sensorhub.api.data.ICommandReceiver;
import org.sensorhub.api.data.IDataProducer;
import net.opengis.sensorml.v20.AbstractPhysicalProcess;


/**
 * <p>
 * Interface for sensors combining data and control interfaces
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 23, 2017
 */
public interface ISensor extends IDataProducer, ICommandReceiver
{

    /**
     * Retrieves most current sensor description.
     */
    @Override
    public AbstractPhysicalProcess getCurrentDescription();


    /**
     * Retrieves the list of interfaces to all sensor data outputs
     */
    @Override
    public Map<String, ? extends ISensorDataInterface> getAllOutputs();


    /**
     * Retrieves the list of interface to sensor status outputs
     * @return map of output names -> data interface objects
     */
    public Map<String, ? extends ISensorDataInterface> getStatusOutputs();


    /**
     * Retrieves the list of interface to sensor observation outputs
     * @return map of output names -> data interface objects
     */
    public Map<String, ? extends ISensorDataInterface> getObservationOutputs();


    /**
     * Retrieves the list of interface to sensor command inputs
     * @return map of input names -> control interface objects
     */
    public Map<String, ? extends ISensorControlInterface> getCommandInputs();


    /**
     * Returns the sensor connection status.<br/>
     * This method must do whatever it can to really detect the presence of the sensor.
     * Consequently, this method can block for long periods of time.
     * @return true if sensor is actually connected and can communicate with the driver
     */
    public boolean isConnected();
}
