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

import java.util.Map;
import org.sensorhub.api.common.IEntity;


/**
 * <p>
 * Interface for all receivers of command data (e.g. actuator, process)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 23, 2017
 */
public interface ICommandReceiver extends IEntity
{
    
    /**
     * @return true if ready to accept commands, false otherwise
     */
    public boolean isEnabled();
    
    
    /**
     * Retrieves the list of data inputs
     * @return map of output names -> data interface objects
     */
    public Map<String, ? extends IStreamingControlInterface> getCommandInputs();
}
