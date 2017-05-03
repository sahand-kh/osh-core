/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.ArrayList;
import java.util.Collection;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IPersistenceManager;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Default implementation of the persistence manager. 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 15, 2010
 */
public class PersistenceManagerImpl implements IPersistenceManager
{
    private static final Logger log = LoggerFactory.getLogger(PersistenceManagerImpl.class);    
    protected ModuleRegistry moduleRegistry;
    protected String basePath;
    
    
    public PersistenceManagerImpl(ModuleRegistry moduleRegistry, String basePath)
    {
        this.moduleRegistry = moduleRegistry;
        this.basePath = basePath;
    }
    
    
    @Override
    public Collection<IStorageModule<?>> getLoadedModules()
    {
        ArrayList<IStorageModule<?>> enabledStorages = new ArrayList<IStorageModule<?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof IStorageModule)
                enabledStorages.add((IStorageModule<?>)module);
        }
        
        return enabledStorages;
    }
    
    
    @Override
    public boolean isModuleLoaded(String moduleID)
    {
        return moduleRegistry.isModuleLoaded(moduleID);
    }


    @Override
    public Collection<ModuleConfig> getAvailableModules()
    {
        return moduleRegistry.getAvailableModules(IStorageModule.class);
    }


    @Override
    public IStorageModule<?> getModuleById(String moduleID) throws SensorHubException
    {
        IModule<?> module = moduleRegistry.getModuleById(moduleID);
        
        if (module instanceof IStorageModule<?>)
            return (IStorageModule<?>)module;
        else
            throw new SensorHubException("Module " + MsgUtils.moduleString(module) + " is not a storage module");
    }
    
    
    @Override
    public Collection<IRecordStorageModule<?>> findStorageForSensor(String sensorLocalID) throws SensorHubException
    {
        ArrayList<IRecordStorageModule<?>> sensorStorageList = new ArrayList<IRecordStorageModule<?>>();
        
        ISensorModule<?> sensorModule = SensorHub.getInstance().getSensorManager().getModuleById(sensorLocalID);
        String sensorUID = sensorModule.getUniqueIdentifier();
        
        // find all basic storage modules whose data source UID is the same as the sensor UID
        Collection<IStorageModule<?>> storageModules = getLoadedModules();
        for (IStorageModule<?> module: storageModules)
        {
            if (module instanceof IRecordStorageModule<?>)
            {
                String dataSourceUID = ((IRecordStorageModule<?>)module).getLatestDataSourceDescription().getUniqueIdentifier();
                
                if (dataSourceUID != null && dataSourceUID.equals(sensorUID))
                    sensorStorageList.add((IRecordStorageModule<?>)module);
            }
        }
        
        return sensorStorageList;
    }
    
    
    @Override
    public StorageConfig getDefaultStorageConfig(Class<?> storageClass) throws SensorHubException
    {
        for (IModuleProvider provider: moduleRegistry.getInstalledModuleTypes())
        {
            try
            {
                Class<?> moduleClass = provider.getModuleClass();
                if (storageClass.isAssignableFrom(moduleClass))
                {
                    StorageConfig newConfig = (StorageConfig)provider.getModuleConfigClass().newInstance();
                    newConfig.moduleClass = moduleClass.getCanonicalName();
                    return newConfig;
                }
            }
            catch (Exception e)
            {
                log.error("Invalid configuration for module ", e);
            }
        }
        
        throw new StorageException("No persistent storage of type " + storageClass.getSimpleName() + " available");
    }
}
