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
import net.opengis.OgcPropertyList;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.Envelope;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.impl.persistence.FilterUtils;
import org.sensorhub.impl.persistence.FilteredIterator;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockString;
import org.vast.data.DataBlockTuple;
import org.vast.ogc.gml.GMLUtils;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.util.Bbox;


public class FoiUtils
{
    static final String FOI_ID_LABEL = "FOI ID";
    
    
    public static void updateFois(SOSOfferingCapabilities caps, IDataProducerModule<?> producer, int maxFois)
    {
        caps.getRelatedFeatures().clear();
        caps.getObservedAreas().clear();        
        
        if (producer instanceof IMultiSourceDataProducer)
        {
            Collection<? extends AbstractFeature> fois = ((IMultiSourceDataProducer)producer).getFeaturesOfInterest();
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
    
    
    public static Iterator<AbstractFeature> getFilteredFoiIterator(IDataProducerModule<?> producer, final IFoiFilter filter)
    {
        // get all fois from producer
        Iterator<? extends AbstractFeature> allFois;
        if (producer instanceof IMultiSourceDataProducer)
            allFois = ((IMultiSourceDataProducer)producer).getFeaturesOfInterest().iterator();
        else if (producer.getCurrentFeatureOfInterest() != null)
            allFois = Arrays.asList(producer.getCurrentFeatureOfInterest()).iterator();
        else
            allFois = Collections.EMPTY_LIST.iterator();
        
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
    
    
    public final static DataComponent buildFoiIDComponent(IMultiSourceDataProducer multiProducer)
    {
        // try to detect common prefix
        StringBuilder prefix = null;
        for (String uid: multiProducer.getEntityIDs())
        {
            if (prefix == null)
                prefix = new StringBuilder();
            else
            {
                // prefix cannot be longer than ID
                if (prefix.length() > uid.length())
                    prefix.setLength(uid.length());
                
                // keep only common chars
                for (int i = 0; i < prefix.length(); i++)
                {
                    if (uid.charAt(i) != prefix.charAt(i))
                    {
                        prefix.setLength(i);
                        break;
                    }
                }
            }
        }
        
        // use category component if prefix was detected
        // or text component otherwise
        SWEHelper fac = new SWEHelper();
        if (prefix.length() > 2)
            return fac.newCategory(SWEConstants.DEF_FOI_ID, FOI_ID_LABEL, null, prefix.toString());
        else
            return fac.newText(SWEConstants.DEF_FOI_ID, FOI_ID_LABEL, null);
    }

    
    public final static void addFoiID(DataComponent dataStruct, DataComponent producerIDField)
    {
        String foiCompName = "foiID";
        
        if (dataStruct instanceof DataRecord)
        {
            OgcPropertyList<DataComponent> fields = ((DataRecord) dataStruct).getFieldList();
            producerIDField.setName(foiCompName);
            fields.add(0, producerIDField);
        }
        else
        {
            SWEHelper fac = new SWEHelper();
            DataRecord rec = fac.newDataRecord();
            rec.addField(foiCompName, producerIDField);
            rec.addField("data", dataStruct);
        }
    }
    
    
    public final static DataBlock addFoiID(DataBlock dataBlk)
    {
        if (dataBlk instanceof DataBlockMixed || dataBlk instanceof DataBlockTuple)
        {
            AbstractDataBlock[] blockArray = (AbstractDataBlock[])dataBlk.getUnderlyingObject();
            AbstractDataBlock[] newArray = new AbstractDataBlock[blockArray.length+1];
            newArray[0] = new DataBlockString(1);
            System.arraycopy(blockArray, 0, newArray, 1, blockArray.length);
            dataBlk.setUnderlyingObject(blockArray);
        }
        
        return dataBlk;
    }
}
