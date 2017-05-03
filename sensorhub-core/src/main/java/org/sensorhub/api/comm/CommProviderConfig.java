/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.ModuleConfig;


public class CommProviderConfig<ConfigType> extends ModuleConfig
{
    @DisplayInfo(label="Protocol Options")
    public ConfigType protocol;
    
    
    @SuppressWarnings("rawtypes")
    public ICommProvider getProvider()
    {
        try
        {
            Class<?> clazz = Class.forName(moduleClass);
            ICommProvider commProvider = (ICommProvider)clazz.newInstance();
            commProvider.init(this);  
            return commProvider;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot load module " + moduleClass, e);
        }
    }
}
