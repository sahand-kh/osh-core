/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.EntityFilter;
import org.sensorhub.api.common.IEntity;
import org.sensorhub.api.common.IEntityFilter;
import org.sensorhub.api.common.IEntityGroup;
import org.sensorhub.api.common.IEntityManager;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;


/**
 * <p>
 * Default implementation of the entity manager maintaining a map of all
 * registered entities in memory
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 11, 2017
 */
public class EntityManagerImpl implements IEntityManager
{
    public static final String EVENT_PRODUCER_ID = "ENTITY_MANAGER";

    ReadWriteLock lock = new ReentrantReadWriteLock();
    ModuleRegistry moduleRegistry;
    Map<String, IEntity> rootEntities = new LinkedHashMap<>();
    Map<String, IEntity> allEntities = new HashMap<>();
    IEventHandler eventHandler;
    
    
    public EntityManagerImpl(ISensorHub hub)
    {
        this.moduleRegistry = hub.getModuleRegistry();
        this.eventHandler = hub.getEventBus().registerProducer(EVENT_PRODUCER_ID);
    }
    
    
    @Override
    public void registerEntity(IEntity entity)
    {
        lock.writeLock().lock();
        
        try
        {
            // always add to all entities list
            allEntities.put(entity.getUniqueIdentifier(), entity);
            
            // only add to root list if entity has no parent
            if (entity.getParentGroup() == null)
                rootEntities.put(entity.getUniqueIdentifier(), entity);
            
            if (entity instanceof IEntityGroup)
            {
                // also register members recursively
                for (IEntity member: ((IEntityGroup<IEntity>)entity).getEntities().values())
                    registerEntity(member);
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }


    @Override
    public void unregisterEntity(String uid)
    {
        lock.writeLock().lock();
        
        try
        {
            // always remove from all entities list
            IEntity entity = allEntities.remove(uid);
            if (entity == null)
                throw new IllegalArgumentException("Cannot find entity " + uid);
            
            // if entity group, unregister members recursively
            rootEntities.remove(uid);
            if (entity instanceof IEntityGroup)
            {
                for (IEntity member: ((IEntityGroup<IEntity>)entity).getEntities().values())
                    unregisterEntity(member.getUniqueIdentifier());
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }


    @Override
    public IEntity getEntity(String id)
    {
        lock.readLock().lock();
        
        try
        {
            if (moduleRegistry.isModuleLoaded(id))
            {
                IModule<?> module = moduleRegistry.getLoadedModuleById(id);
                if (!(module instanceof IEntity))
                    throw new IllegalArgumentException("Module " + MsgUtils.moduleString(module) + " is not an entity");
                return (IEntity)module;
            }            
            else
            {
                IEntity entity = allEntities.get(id);
                if (entity == null)
                    throw new IllegalArgumentException("Cannot find entity " + id); 
                return entity;
            }
        }
        finally
        {
            lock.readLock().unlock();
        }
    }
    
    
    @Override
    public <EntityType extends IEntity> Collection<EntityType> getEntities(Class<EntityType> entityType)
    {
        return findEntities(entityType, new EntityFilter());                
    }
    
    
    @Override
    public <EntityType extends IEntity> Collection<EntityType> findEntities(Class<EntityType> entityType, IEntityFilter filter)
    {
        lock.readLock().lock();        
        
        try
        {
            ArrayList<EntityType> destList = new ArrayList<>();
            collect(entityType, rootEntities.values(), destList, filter, filter.getMaxDepth());
            return destList;
        }
        finally
        {
            lock.readLock().unlock();
        }
    }
    
    
    /*
     * Called recursively to collect entities until specified level of nesting
     */
    protected <EntityType extends IEntity> void collect(Class<EntityType> entityType, Collection<? extends IEntity> srcList, ArrayList<EntityType> destList, IEntityFilter filter, int maxLevels)
    {
        for (IEntity entity: srcList)
        {
            if (entityType.isAssignableFrom(entity.getClass()) && applyFilter(entity, filter))
                destList.add((EntityType)entity);
            
            if (maxLevels > 0 && entity instanceof IEntityGroup)
            {
                Collection<? extends IEntity> members = ((IEntityGroup<IEntity>)entity).getEntities().values();
                collect(entityType, members, destList, filter, maxLevels-1);
            }            
        }
    }
    
    
    protected boolean applyFilter(IEntity entity, IEntityFilter filter)
    {
        boolean match = true;
        
        // keywords
        if (filter.getKeywords() != null)
        {
            String name = entity.getName();
            String desc = entity.getCurrentDescription().getDescription();
            match = false;
            
            for (String keyword: filter.getKeywords())
            {
                if (name.contains(keyword) || desc.contains(keyword))
                {
                    match = true;
                    break;
                }
            }
        }
        
        // skip
        
        return match;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);        
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);        
    }
}
