/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.config.DisplayInfo.ModuleType;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;
import org.sensorhub.api.persistence.IStorageModule;


/**
 * <p>
 * Configuration class for SOS data providers using the streaming data API.
 * A storage can also be associated to the provider so that archive requests
 * can be handled through the same offering.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 20, 2015
 */
public abstract class StreamDataProviderConfig extends SOSProviderConfig
{
    
    @DisplayInfo(desc="Local ID of storage to use as data source for archive requests")
    @FieldType(Type.MODULE_ID)
    @ModuleType(IStorageModule.class)
    public String storageID;
    
    
    @DisplayInfo(desc="Names of process outputs to hide from SOS")
    public List<String> hiddenOutputs = new ArrayList<>();
    
    
    @DisplayInfo(desc="Time-out after which real-time requests are disabled if no more "
            + "measurements are received (in seconds). Real-time is reactivated as soon as "
            + "new records start being received again")
    public double liveDataTimeout = 10.0;

}
