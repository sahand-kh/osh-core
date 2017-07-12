/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;

import net.opengis.gml.v32.Point;


/**
 * <p>
 * Interface for all entities that can be geolocated
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 12, 2017
 */
public interface IEntityWithLocation extends IEntity
{

    /**
     * Retrieves the current geographic location of the entity as a GML point.<br/>
     * Note that the entity location can be different from the feature of
     * interest location/geometry.
     * @return the entity location
     */
    public Point getCurrentLocation();
}
