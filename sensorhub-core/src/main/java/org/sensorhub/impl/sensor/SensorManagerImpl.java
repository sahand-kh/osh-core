/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.ArrayList;
import java.util.Collection;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.ISensorManager;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p>
 * Default implementation of the sensor manager interface
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorManagerImpl implements ISensorManager
{
    protected ModuleRegistry moduleRegistry;
    
    
    public SensorManagerImpl(ModuleRegistry moduleRegistry)
    {
        this.moduleRegistry = moduleRegistry;
    }
    
    
    @Override
    public Collection<ISensorModule<?>> getLoadedModules()
    {
        ArrayList<ISensorModule<?>> enabledSensors = new ArrayList<>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof ISensorModule)
                enabledSensors.add((ISensorModule<?>)module);
        }
        
        return enabledSensors;
    }
    
    
    @Override
    public boolean isModuleLoaded(String moduleID)
    {
        return moduleRegistry.isModuleLoaded(moduleID);
    }


    @Override
    public Collection<ModuleConfig> getAvailableModules()
    {
        return moduleRegistry.getAvailableModules(ISensorModule.class);
    }


    @Override
    public ISensorModule<?> getModuleById(String moduleID) throws SensorHubException
    {
        IModule<?> module = moduleRegistry.getModuleById(moduleID);
        
        if (module instanceof ISensorModule<?>)
            return (ISensorModule<?>)module;
        else
            return null;
    }


    @Override
    public ISensorModule<?> findSensor(String uid)
    {
        Collection<ISensorModule<?>> enabledSensors = getLoadedModules();
        for (ISensorModule<?> sensor: enabledSensors)
        {
            if (uid.equals(sensor.getUniqueIdentifier()))
                return sensor;
        }
        
        return null;
    }


    @Override
    public Collection<ISensorModule<?>> getConnectedSensors()
    {
        ArrayList<ISensorModule<?>> connectedSensors = new ArrayList<>();
        
        // scan module list
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof ISensorModule && ((ISensorModule<?>)module).isConnected())
                connectedSensors.add((ISensorModule<?>)module);
        }
        
        return connectedSensors;
    }


    @Override
    public String installDriver(String driverPackageURL, boolean replace)
    {
        // TODO Auto-generated method stub
        // TODO need to implement generic module software loading in ModuleRegistry
        //  + dynamic classloader handling new uploaded jars (scanning directory at startup)
        return null;
    }


    @Override
    public void uninstallDriver(String driverID)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public Collection<IModuleProvider> getInstalledSensorDrivers()
    {
        return moduleRegistry.getInstalledModuleTypes(ISensorModule.class);
    }

}
