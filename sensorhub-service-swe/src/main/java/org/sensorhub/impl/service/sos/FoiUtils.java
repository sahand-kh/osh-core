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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.Envelope;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.impl.persistence.FilterUtils;
import org.sensorhub.impl.persistence.FilteredIterator;
import org.vast.ogc.gml.GMLUtils;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.util.Bbox;


public class FoiUtils
{
        
    public static void updateFois(SOSOfferingCapabilities caps, IDataProducer producer, int maxFois)
    {
        caps.getRelatedFeatures().clear();
        caps.getObservedAreas().clear();        
        
        if (producer instanceof IMultiSourceDataProducer)
        {
            Collection<? extends AbstractFeature> fois = ((IMultiSourceDataProducer)producer).getFeaturesOfInterest().values();
            int numFois = fois.size();
            
            Bbox boundingRect = new Bbox();
            for (AbstractFeature foi: fois)
            {
                if (numFois <= maxFois)
                    caps.getRelatedFeatures().add(foi.getUniqueIdentifier());
                
                AbstractGeometry geom = foi.getLocation();
                if (geom != null)
                {
                    Envelope env = geom.getGeomEnvelope();
                    boundingRect.add(GMLUtils.envelopeToBbox(env));
                }
            }
            
            if (!boundingRect.isNull())
                caps.getObservedAreas().add(boundingRect);
        }
        else
        {
            AbstractFeature foi = producer.getCurrentFeatureOfInterest();
            if (foi != null)
            {
                caps.getRelatedFeatures().add(foi.getUniqueIdentifier());
                
                AbstractGeometry geom = foi.getLocation();
                if (geom != null)
                {
                    Envelope env = geom.getGeomEnvelope();
                    Bbox bbox = GMLUtils.envelopeToBbox(env);
                    caps.getObservedAreas().add(bbox);
                }
            }
        }
    }
    
    
    public static Iterator<AbstractFeature> getFilteredFoiIterator(IDataProducer producer, final IFoiFilter filter)
    {
        // get all fois from producer
        Iterator<? extends AbstractFeature> allFois;
        if (producer instanceof IMultiSourceDataProducer)
            allFois = ((IMultiSourceDataProducer)producer).getFeaturesOfInterest().values().iterator();
        else if (producer.getCurrentFeatureOfInterest() != null)
            allFois = Arrays.asList(producer.getCurrentFeatureOfInterest()).iterator();
        else
            allFois = Collections.emptyIterator();
        
        // return all features if no filter is used
        if ((filter.getFeatureIDs() == null || filter.getFeatureIDs().isEmpty()) && filter.getRoi() == null)
            return (Iterator<AbstractFeature>)allFois;
        
        return new FilteredIterator<AbstractFeature>((Iterator<AbstractFeature>)allFois)
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
}
