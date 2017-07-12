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

import java.util.Collection;
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p>
 * Filter interface used to search for entities.<br/>
 * There is an implicit logical AND between all criteria.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 13, 2017
 */
public interface IEntityFilter
{

    /**
     * Gets filter criteria for selecting entities by group ID.<br/>
     * Only entities belonging to the group with the given ID will be selected.<br/>
     * If the list is null or empty, no filtering on group ID will be applied.
     * @return Group ID
     */
    public String getGroupID();
    
    
    /**
     * Maximum depth of the search.<br/>
     * Only children at levels less than or equals to the specified depth will
     * be selected. A value of 0 indicates to return only direct children of
     * the specified group.<br/>
     * When the group ID criteria is provided the search start in the corresponding
     * group, otherwise it starts in the root group (i.e. the group containing
     * entities with no parent)
     * @return
     */
    public int getMaxDepth();
    
    
    /**
     * Gets filter criteria for selecting entities by keywords.<br/>
     * Entities with the listed keywords in their name, description, keywords,
     * identifiers, classifiers will be selected.<br/>
     * There is an implicit OR between keywords.<br/>
     * If the list is null or empty, no filtering on ID will be applied.
     * @return List of keywords
     */
    public Collection<String> getKeywords();
    
    
    /**
     * Gets filter criteria for selecting entities based on their location.<br/>
     * Only entities located within the polygon will be selected.<br/>
     * If the polygon is null, no filtering on location will be applied.<br/>
     * @return Polygonal Region of Interest (ROI)
     */
    public Polygon getEntityRoi();
    
    
    /**
     * Gets filter criteria for selecting entities by their features of interest.<br/>
     * Only entities associated to one of the listed FoI IDs will be selected.<br/>
     * If the list is null or empty, no filtering on FoI ID will be applied.
     * @return List of desired feature of interest IDs
     */
    public Collection<String> getFoiIDs();
    
    
    /**
     * Gets filter criteria for selecting entities based on the geometry of the 
     * associated feature of interest or sampling area.<br/>
     * Only entities whose FOI or sampling geometry intersects with the polygon will be selected.<br/>
     * If the polygon is null, no filtering on location will be applied.<br/>
     * The polygon must be expressed in EPSG 4326 coordinates.
     * @return Polygonal Region of Interest (ROI)
     */
    public Polygon getFoiRoi();
    
    
    /**
     * @return Number of matching items to skip (used for paging)
     */
    public int getSkipCount();
    
}
