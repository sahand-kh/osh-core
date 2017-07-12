/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.processing;

import java.io.File;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.processing.ProcessConfig;


/**
 * <p>
 * Configuration class for SensorML based processors.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 20, 2015
 */
public class SMLProcessConfig extends ProcessConfig
{
    @DisplayInfo(label="SensorML File", desc="Path of SensorML description of the process")
    public String sensorML;
    
    

    
    
    public String getSensorMLPath()
    {
        if (sensorML == null)
            return null;        
        return new File(sensorML).getAbsolutePath();
    }
}
