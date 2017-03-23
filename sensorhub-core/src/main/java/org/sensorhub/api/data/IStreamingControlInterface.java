/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.data;

import java.util.List;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.sensor.SensorException;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

/**
 * <p>
 * Interface for all taskable components using the SWE model to describe
 * structure and encoding of commands they accept (e.g. actuators, processes...)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 23, 2017
 */
public interface IStreamingControlInterface
{

    /**
     * Allows by-reference access to parent module
     * @return parent module instance
     */
    public ICommandReceiver getParentModule();
    
    
    /**
     * Gets the interface name.
     * @return name of this control interface
     */
    public String getName();


    /**
     * Checks if this interface is enabled
     * @return true if interface is enabled, false otherwise
     */
    public boolean isEnabled();
    
    
    /**
     * Retrieves description of command message
     * Note that this can be a choice of multiple messages
     * @return Data component containing message structure
     */
    public DataComponent getCommandDescription();
    
    
    /**
     * Executes the command synchronously, blocking until completion of command
     * @param command command message data
     * @return status after execution of command
     * @throws SensorException
     */
    public CommandStatus execCommand(DataBlock command) throws SensorException;


    /**
     * Executes multiple commands synchronously and in the order specified.
     * This method will block until all commands are completed
     * @param commands list of command messages data
     * @return a single status message for the command group
     * @throws SensorException
     */
    public CommandStatus execCommandGroup(List<DataBlock> commands) throws SensorException;
}
