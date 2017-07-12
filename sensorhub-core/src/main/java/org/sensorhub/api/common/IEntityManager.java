/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;

import java.util.Collection;


/**
 * <p>
 * There is one entities manager per sensor hub that is used to retrieve the
 * list of all entities (i.e. sensors, actuators, processes, other data sources)
 * registered on the hub. It also generates events when entities are added and 
 * removed.
 * @see IEntity.class
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 11, 2017
 */
public interface IEntityManager extends IEventProducer
{

    /**
     * Registers an entity with this manager<br/>
     * Entities implemented as modules (typically sensor drivers) are automatically
     * registered when they are loaded by the module registry but all other
     * entities must be registered explicitely.
     * @param entity
     */
    public void registerEntity(IEntity entity);
    
    
    /**
     * Unregisters entity with the given ID
     * @param id entity ID
     */
    public void unregisterEntity(String id);
    
    
    /**
     * Retrieves an entity using its ID
     * @param id entity ID, can be either the entity unique ID or the module
     *        local ID when the entity is implemented as a module
     * @return entity with the given unique ID
     */
    public IEntity getEntity(String id);
    
    
    /**
     * Retrieves all top-level entities of the given type (i.e. entities that are
     * member of a group are not retrieved)
     * @param entityType type of entity to retrieve (subclass of {@link IEntity})
     * @return collection of entities with the given type
     */
    public <EntityType extends IEntity> Collection<EntityType> getEntities(Class<EntityType> entityType);
    
    
    /**
     * Retrieves all entities of the given type matching the filter
     * @param entityType type of entity to retrieve (subclass of {@link IEntity})
     * @param filter filtering criteria
     * @return collection of matching entities
     */
    public <EntityType extends IEntity> Collection<EntityType> findEntities(Class<EntityType> entityType, IEntityFilter filter);
}
