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

import java.util.Map;


/**
 * <p>
 * Interface for groups of entities (e.g. sensor networks, sensor systems).
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <EntityType> Type of entity composing this group
 * @since Jun 9, 2017
 */
public interface IEntityGroup<EntityType extends IEntity> extends IEntity
{
    
    /**
     * @return map of member entities (entity ID -> IEntity object)
     */
    public Map<String, ? extends EntityType> getEntities();
}
