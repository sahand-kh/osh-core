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

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import java.io.IOException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.ModuleRegistry;
import org.vast.ogc.om.IObservation;


/**
 * <p>
 * Wrapper data consumer for updating both sensor and storage appropriately
 * </p>
 *
 * @author Alex Robin>
 * @since Feb 13, 2015
 */
public class SensorWithStorageConsumer extends SensorDataConsumer implements ISOSDataConsumer
{
    IBasicStorage storage;
    
    
    public SensorWithStorageConsumer(SOSServlet servlet, SensorConsumerConfig config) throws SensorHubException
    {
        super(servlet, config);
        ModuleRegistry moduleReg = servlet.getParentHub().getModuleRegistry();
        this.storage = (IBasicStorage)moduleReg.getModuleById(config.storageID);
    }


    @Override
    public String newResultTemplate(DataComponent component, DataEncoding encoding, IObservation obsTemplate) throws IOException
    {
        // call superclass w/o obs template so no FOI is published yet
        // we need to wait until we have a record store to do that
        String templateID = super.newResultTemplate(component, encoding, null);
                
        // add additional datastore if not already there
        String outputName = getOutputNameFromTemplateID(templateID);
        if (!storage.getRecordStores().containsKey(outputName))
            storage.addRecordStore(outputName, component, encoding);
        
        // publish new feature of interest (now that we have a proper record store)
        try
        {
            if (obsTemplate != null)
                sensor.newFeatureOfInterest(outputName, obsTemplate.getFeatureOfInterest());
        }
        catch (SensorException e)
        {
            throw new IOException("Cannot ingest new feature of interest", e);
        }
        
        return templateID;
    }
}
