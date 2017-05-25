/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.utils.ModuleUtils;


/**
 * <p>
 * Base module provider obtaining module metadata from jar file MANIFEST
 * </p>
 *
 * @author Alex Robin
 * @since Nov 7, 2016
 */
public abstract class JarModuleProvider implements IModuleProvider
{
    IModuleProvider manifestInfo = ModuleUtils.getModuleInfo(getClass());
        
    
    @Override
    public String getModuleName()
    {
        String name = manifestInfo.getModuleName();
        if (ModuleUtils.MODULE_NAME.equals(name))
        {
            if (getModuleClass() != null) 
                return getModuleClass().getSimpleName();
            else
                return this.getClass().getPackage().getName();                
        }
        else
            return name;
    }


    @Override
    public String getModuleDescription()
    {
        return manifestInfo.getModuleDescription();
    }


    @Override
    public String getModuleVersion()
    {
        return manifestInfo.getModuleVersion();
    }


    @Override
    public String getProviderName()
    {
        return manifestInfo.getProviderName();
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return manifestInfo.getModuleClass();
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return manifestInfo.getModuleConfigClass();
    }

}
