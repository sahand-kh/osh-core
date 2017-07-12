/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.data;

import java.util.Collection;
import java.util.Map;
import org.sensorhub.api.common.IEntityGroup;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Interface for multi-source data producers.<br/>
 * This type of producer can be used to model an entire group of data sources
 * (e.g. sensor network) and provides additional methods to filter data records
 * and metadata by entity ID (aka source ID).<br/>
 * In particular, outputs can be of type {@link IMultiSourceDataInterface}
 * allowing data from multiple sources to be multiplexed in a single data stream. 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 31, 2015
 */
public interface IMultiSourceDataProducer extends IDataProducer, IEntityGroup<IDataProducer>
{
        
    /**
     * Retrieves the list of features of interest for which this producer
     * is generating data
     * @return read-only map of all FOI ids -> feature objects
     */
    public Map<String, ? extends AbstractFeature> getFeaturesOfInterest();
    
    
    /**
     * Gets IDs of entities that are observing the specified feature of interest. 
     * @param foiID ID of feature of interest
     * @return collection of entity IDs (can be empty)
     */
    public Collection<String> getEntitiesWithFoi(String foiID);
    
}
