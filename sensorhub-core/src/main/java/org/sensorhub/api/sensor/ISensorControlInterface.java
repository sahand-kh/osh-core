/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.List;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.data.IStreamingControlInterface;
import org.vast.util.DateTime;


/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system
 * Commands can be sent to each sensor controllable input via this interface.
 * Commands can be executed synchronously or asynchronously by sensors.
 * If asynchronous mode is supported, implementations of this class MUST produce
 * events of type SensorControlEvent.
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface ISensorControlInterface extends IStreamingControlInterface, IEventProducer
{

    /**
     * Checks asynchronous execution capability 
     * @return true if asynchronous command execution is supported, false otherwise
     */
    public boolean isAsyncExecSupported();


    /**
     * Checks scheduled execution capability 
     * @return true if scheduled command execution is supported, false otherwise
     */
    public boolean isSchedulingSupported();    


    /**
     * Sends a command that will be executed asynchronously
     * @see #isAsyncExecSupported()
     * @param command command message data
     * @return initial status of the command (can change during the command life cycle)
     * @throws SensorException
     */
    public CommandStatus sendCommand(DataBlock command) throws SensorException;


    /**
     * Sends a group of commands for asynchronous execution.
     * Order is guaranteed but not atomicity
     * @see #isAsyncExecSupported()
     * @param commands list of command messages data
     * @return a single status object for the command group
     * @throws SensorException
     */
    public CommandStatus sendCommandGroup(List<DataBlock> commands) throws SensorException;


    /**
     * Schedules a command to be executed asynchronously at the specified time
     * @see #isSchedulingSupported()
     * @param command command message data
     * @param execTime desired time of execution
     * @return initial status of the command (can change during the command life cycle)
     * @throws SensorException
     */
    public CommandStatus scheduleCommand(DataBlock command, DateTime execTime) throws SensorException;


    /**
     * Schedules a group of commands to be executed asynchronously at the specified time.
     * Order is guaranteed but not atomicity
     * @see #isSchedulingSupported()
     * @param commands
     * @param execTime
     * @return a single status object for the command group
     * @throws SensorException
     */
    public CommandStatus scheduleCommandGroup(List<DataBlock> commands, DateTime execTime) throws SensorException;


    /**
     * Cancels a command before it is executed (for async or scheduled commands)
     * @see #isAsyncExecSupported()
     * @param commandID id of command to be canceled
     * @return status of the cancelled command
     * @throws SensorException
     */
    public CommandStatus cancelCommand(String commandID) throws SensorException;


    /**
     * Retrieves command status
     * @param commandID id of command to get status for
     * @see #isAsyncExecSupported()
     * @return current status of the command with the specified ID
     * @throws SensorException
     */
    public CommandStatus getCommandStatus(String commandID) throws SensorException;

}
