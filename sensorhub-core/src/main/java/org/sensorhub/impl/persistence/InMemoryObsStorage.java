/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;
import net.opengis.gml.v32.AbstractFeature;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.vast.ogc.gml.GMLUtils;
import org.vast.util.Bbox;


/**
 * <p>
 * In-memory obs storage implementation.
 * This is used mainly for test purposes but could perhaps be improved to be
 * used as a local memory cache of a remote storage.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since March 25, 2017
 */
public class InMemoryObsStorage extends InMemoryBasicStorage implements IObsStorageModule<InMemoryStorageConfig>
{
    ConcurrentSkipListMap<String, AbstractFeature> foisMap = new ConcurrentSkipListMap<String, AbstractFeature>();
    Bbox bbox = new Bbox();
    
    
    public InMemoryObsStorage()
    {
    }
    

    @Override
    public int getNumFois(IFoiFilter filter)
    {
        int count = 0;
        
        for (AbstractFeature f: foisMap.values())
        {
            if (FilterUtils.isFeatureSelected(filter, f))
                count++;
        }
        
        return count;
    }


    @Override
    public Bbox getFoisSpatialExtent()
    {
        return bbox.copy();
    }


    @Override
    public Iterator<String> getFoiIDs(final IFoiFilter filter)
    {
        return new FilteredSelectIterator<AbstractFeature, String>(foisMap.values().iterator())
        {
            @Override
            protected String accept(AbstractFeature f)
            {
                if (FilterUtils.isFeatureSelected(filter, f))
                    return f.getUniqueIdentifier();
                else
                    return null;
            }    
        };
    }


    @Override
    public Iterator<AbstractFeature> getFois(final IFoiFilter filter)
    {
        return new FilteredIterator<AbstractFeature>(foisMap.values().iterator())
        {
            @Override
            protected boolean accept(AbstractFeature f)
            {
                if (FilterUtils.isFeatureSelected(filter, f))
                    return true;
                else
                    return false;
            }    
        };
    }


    @Override
    public void storeFoi(String producerID, AbstractFeature foi)
    {
        foisMap.put(foi.getId(), foi);
        if (foi.getLocation() != null)
        {
            Bbox foiBbox = GMLUtils.envelopeToBbox(foi.getLocation().getGeomEnvelope());
            bbox.add(foiBbox);
        }
    }
}
