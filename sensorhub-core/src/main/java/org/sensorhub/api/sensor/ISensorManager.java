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

import java.util.Collection;
import org.sensorhub.api.common.IEntity;
import org.sensorhub.api.common.IEntityFilter;


/**
 * <p>
 * Management interface for sensors connected to the system
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface ISensorManager
{   
    
	/**
     * Gets list of all connected sensors
	 * @param maxLevels max number of recursion levels when retrieving members
	 *                  of entity groups 
     * @return the list of sensors actually connected to the system
     */
    public Collection<ISensor> getConnectedSensors(int maxLevels);
    
    
    
    /**
     * Retrieves all sensors matching the filter
     * @param entityType type of entity to retrieve (subclass of {@link IEntity})
     * @param filter entity filtering criteria
     * @return collection of entities with the given type
     */
    public Collection<ISensor> findSensors(IEntityFilter filter);
    
}
