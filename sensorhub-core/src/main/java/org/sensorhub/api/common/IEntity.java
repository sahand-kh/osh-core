/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;

import org.sensorhub.api.data.IMultiSourceDataProducer;
import net.opengis.sensorml.v20.AbstractProcess;


/**
 * <p>
 * Base interface for all OSH entities that provide a SensorML description
 * (e.g. sensors, actuators, processes, data producers in general)<br/>
 * When the entity represents a real world object such as a hardware device,
 * the entity description should do its best to reflect its current state.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since June 9, 2017
 */
public interface IEntity
{
        
    /**
     * @return entity name
     */
    public String getName();
    
    
    /**
     * @return entity globally unique identifier
     */
    public String getUniqueIdentifier();
    
    
    /**
     * @return the parent entity group or null if this entity is not a member
     * of any group
     */
    public IEntityGroup<? extends IEntity> getParentGroup();
    
    
    /**
     * Retrieves most current SensorML description of the entity.
     * All implementations must return an instance of AbstractProcess with
     * a valid unique identifier.<br/>
     * In the case of a module generating data from multiple entities (e.g. 
     * sensor network), this returns the description of the group as a whole.
     * Descriptions of individual entities within the group are retrived using
     * {@link IMultiSourceDataProducer#getCurrentDescription(String)}
     * @return AbstractProcess SensorML description of the data producer or
     * null if none is available at the time of the call 
     */
    public AbstractProcess getCurrentDescription();


    /**
     * Used to check when SensorML description was last updated.
     * This is useful to avoid requesting the object when it hasn't changed.
     * @return date/time of last description update as unix time (ms since 1970) or
     * {@link Long#MIN_VALUE} if description was never updated.
     */
    public long getLastDescriptionUpdate();

}