/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.data;

import java.util.Map;
import org.sensorhub.api.common.IEntity;
import org.sensorhub.api.common.IEventProducer;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Interface for all producers of streaming data
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 23, 2017
 */
public interface IDataProducer extends IEntity, IEventProducer
{
    
    /**
     * @return true if generating data, false otherwise
     */
    public boolean isEnabled();
    
    
    /**
     * Retrieves the list of data outputs
     * @return read-only map of output names -> data interface objects
     */
    public Map<String, ? extends IStreamingDataInterface> getOutputs();
    
    
    /**
     * Retrieves the feature of interest for which this producer is 
     * currently generating data.
     * @return Feature object
     */
    public AbstractFeature getCurrentFeatureOfInterest();
    
}
