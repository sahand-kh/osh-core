/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


public class ModuleUtils
{
    public static final String MODULE_NAME = "Bundle-Name";
    public static final String MODULE_DESC = "Bundle-Description";
    public static final String MODULE_VERSION = "Bundle-Version";
    public static final String MODULE_VENDOR = "Bundle-Vendor";
    public static final String MODULE_BUILD = "Bundle-BuildNumber";
    public static final String MODULE_DEPS = "OSH-Dependencies";
    
    
    public static Manifest getManifest(Class<?> clazz)
    {
        try
        {
            URL srcUrl = clazz.getProtectionDomain().getCodeSource().getLocation();
            if (srcUrl != null && "jar".equalsIgnoreCase(srcUrl.getProtocol()))
            {
                String jarName = URLDecoder.decode(srcUrl.getFile(), StandardCharsets.UTF_8.name());
                try ( JarFile jar = new JarFile(new File(jarName)) ) {
                    return jar.getManifest();
                }
            }
            
            return null;            
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot access JAR manifest", e);
        }
    }
    
    
    public static IModuleProvider getModuleInfo(Class<?> clazz)
    {
        Manifest manifest = getManifest(clazz);
        final String name, desc, version, vendor;
        
        if (manifest == null)
        {
            name = MODULE_NAME;
            desc = MODULE_DESC;
            version = MODULE_VERSION;
            vendor = MODULE_VENDOR;
        }
        else
        {
            Attributes attributes = manifest.getMainAttributes();
            name = attributes.getValue(MODULE_NAME);
            desc = attributes.getValue(MODULE_DESC);
            version = attributes.getValue(MODULE_VERSION);
            vendor = attributes.getValue(MODULE_VENDOR);
        }
        
        return new IModuleProvider()
        {
            @Override
            public String getModuleName()
            {
                return name;
            }

            @Override
            public String getModuleDescription()
            {
                return desc;
            }

            @Override
            public String getModuleVersion()
            {
                return version;
            }

            @Override
            public String getProviderName()
            {
                return vendor;
            }

            @Override
            public Class<? extends IModule<?>> getModuleClass()
            {
                return null;
            }

            @Override
            public Class<? extends ModuleConfig> getModuleConfigClass()
            {
                return null;
            }    
        };
    }
    
    
    public static String[] getBundleDependencies(Class<?> clazz)
    {
        Manifest manifest = getManifest(clazz);
        if (manifest == null)
            return new String[0];
            
        String packages = manifest.getMainAttributes().getValue(MODULE_DEPS);
        if (packages == null)
            return new String[0];
        else
            return packages.split(",");
    }
    
    
    public static String getBuildNumber(Class<?> clazz)
    {
        Manifest manifest = getManifest(clazz);
        if (manifest == null)
            return null;
        return manifest.getMainAttributes().getValue(MODULE_BUILD);
    }
}
