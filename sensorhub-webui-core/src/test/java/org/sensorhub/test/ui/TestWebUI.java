/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.InMemoryStorageConfig;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.test.module.DummyModule;
import org.sensorhub.ui.AdminUIConfig;
import org.sensorhub.ui.AdminUIModule;
import org.sensorhub.ui.CustomUIConfig;
import org.sensorhub.ui.HttpServerConfigForm;


public class TestWebUI
{
    static ModuleRegistry registry;
    
    
    static public void setup() throws Exception
    {
        // start sensorhub and load modules
        registry = new SensorHub().getModuleRegistry();
        setupConfig();
        
        // connect to servlet and check response
        HttpServerConfig httpConfig = HttpServer.getInstance().getConfiguration();
        URL url = new URL("http://localhost:" + httpConfig.httpPort + httpConfig.servletsRootUrl + "/test");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String resp = reader.readLine();
        System.out.println(resp);
        reader.close();
    }
    
    
    static protected void setupConfig() throws SensorHubException
    {
        // HTTP server
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.autoStart = true;
        httpConfig.moduleClass = HttpServer.class.getCanonicalName();
        httpConfig.id = UUID.randomUUID().toString();
        registry.loadModule(httpConfig);
        
        // Admin UI
        AdminUIConfig adminConfig = new AdminUIConfig();
        adminConfig.autoStart = true;
        adminConfig.moduleClass = AdminUIModule.class.getCanonicalName();
        adminConfig.id = UUID.randomUUID().toString();
        adminConfig.customForms.add(new CustomUIConfig(HttpServerConfig.class.getCanonicalName(), HttpServerConfigForm.class.getCanonicalName()));
        registry.loadModule(adminConfig);
        
        // Dummy modules
        String[] moduleNames = new String[] {"SOS Service", "SPS Service", "Storage1", "Storage2", "Sensor1", "Sensor2"};
        
        for (String name: moduleNames)
        {
            ModuleConfig config;
            
            if (name.contains("Service"))
                config = new TestServiceConfig();
            else if (name.contains("Storage"))
                config = new InMemoryStorageConfig();
            else if (name.contains("Process"))
                config = new ProcessConfig();
            else
                config = new SensorConfig();
                
            config.id = UUID.randomUUID().toString();
            config.name = name;
            config.autoStart = true;
            config.moduleClass = DummyModule.class.getCanonicalName();
            registry.loadModule(config);   
        }
    }
    
    
    public static void main(String[] args) throws Exception
    {
        setup();
        Thread.sleep(1000*3600);
    }
}
