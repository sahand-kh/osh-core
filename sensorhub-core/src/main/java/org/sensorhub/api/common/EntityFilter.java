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
import java.util.Collections;
import com.vividsolutions.jts.geom.Polygon;


public class EntityFilter implements IEntityFilter
{

    @Override
    public int getSkipCount()
    {
        return 0;
    }


    @Override
    public int getMaxDepth()
    {
        return 0;
    }
    
    
    @Override
    public String getGroupID()
    {
        return null;
    }


    @Override
    public Collection<String> getKeywords()
    {
        return Collections.emptyList();
    }


    @Override
    public Polygon getEntityRoi()
    {
        return null;
    }


    @Override
    public Collection<String> getFoiIDs()
    {
        return null;
    }


    @Override
    public Polygon getFoiRoi()
    {
        return null;
    }

}
