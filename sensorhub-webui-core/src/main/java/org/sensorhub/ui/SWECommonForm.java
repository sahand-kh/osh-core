/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import org.vast.data.DataValue;
import org.vast.swe.SWEDataTypeUtils;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.HasRefFrames;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.SimpleComponent;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.UnitReference;
import net.opengis.swe.v20.Vector;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;


@SuppressWarnings("serial")
public class SWECommonForm extends VerticalLayout
{
    transient SWEDataTypeUtils sweUtils = new SWEDataTypeUtils();
    boolean showArrayTable;
    boolean addSpacing = false;
    
    
    protected SWECommonForm() {}
    
    
    public SWECommonForm(DataComponent dataComponent)
    {
        addComponent(buildWidget(dataComponent, true));
    }
    
    
    protected Component buildWidget(DataComponent dataComponent, boolean showValues)
    {
        if (dataComponent instanceof DataRecord || dataComponent instanceof Vector)
        {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(addSpacing);
            
            Label l = new Label();
            l.setContentMode(ContentMode.HTML);
            l.setValue(getCaption(dataComponent, false));
            l.setDescription(getTooltip(dataComponent));
            layout.addComponent(l);
            
            VerticalLayout form = new VerticalLayout();
            form.setSpacing(addSpacing);
            form.setMargin(new MarginInfo(false, false, false, true));
            for (int i = 0; i < dataComponent.getComponentCount(); i++)
            {
                DataComponent c = dataComponent.getComponent(i);
                Component w = buildWidget(c, showValues);
                if (w != null)
                    form.addComponent(w);
            }
            layout.addComponent(form);
            
            return layout;
        }
        
        else if (dataComponent instanceof DataArray)
        {
            DataArray dataArray = (DataArray)dataComponent;
            VerticalLayout layout = new VerticalLayout();
            
            Label l = new Label();
            l.setContentMode(ContentMode.HTML);
            l.setValue(getCaption(dataComponent, false));
            l.setDescription(getTooltip(dataComponent));
            layout.addComponent(l);
            
            VerticalLayout form = new VerticalLayout();
            form.setMargin(new MarginInfo(false, false, false, true));
            form.setSpacing(false);
            form.addComponent(buildWidget(dataArray.getElementType(), false));
            layout.addComponent(form);
            
            return layout;
        }
        
        else if (dataComponent instanceof DataChoice)
        {
            DataChoice dataChoice = (DataChoice)dataComponent;
            VerticalLayout layout = new VerticalLayout();
            
            Label l = new Label();
            l.setContentMode(ContentMode.HTML);
            l.setValue(getCaption(dataChoice, false));
            l.setDescription(getTooltip(dataChoice));
            layout.addComponent(l);
            
            VerticalLayout form = new VerticalLayout();
            form.setMargin(new MarginInfo(false, false, false, true));
            form.setSpacing(false);
            for (int i = 0; i < dataComponent.getComponentCount(); i++)
            {
                DataComponent c = dataComponent.getComponent(i);
                Component w = buildWidget(c, showValues);
                if (w != null)
                    form.addComponent(w);
            }
            layout.addComponent(form);
            
            return layout;
        }
        
        else if (dataComponent instanceof SimpleComponent)
        {
            Label l = new Label();
            l.setContentMode(ContentMode.HTML);
            l.setValue(getCaption(dataComponent, showValues));
            l.setDescription(getTooltip(dataComponent));
            return l;
        }
        
        return null;
    }
    
    
    protected String getCaption(DataComponent dataComponent, boolean showValues)
    {
        StringBuffer caption = new StringBuffer();
        caption.append("<b>").append(getPrettyName(dataComponent)).append("</b>");
        
        if (dataComponent instanceof SimpleComponent)
        {
            // uom code
            String uom = null;
            if (dataComponent instanceof HasUom)
            {
                UnitReference unit = ((HasUom) dataComponent).getUom();
                if (unit.isSetCode())
                    uom = unit.getCode();
                if (uom != null && uom.equals("1"))
                    uom = null;
            }
            
            DataBlock data = dataComponent.getData();
            if (showValues && data != null)
            {
                caption.append(" = ");
                caption.append(sweUtils.getStringValue((DataValue)dataComponent));
                caption.append(' ');
                if (uom != null)
                    caption.append(uom);
            }
            else
            {
                if (uom != null)
                    caption.append(" (").append(uom).append(')');
            }
        }
        
        // array size
        else if (dataComponent instanceof DataArray)
        {
            caption.append(" [");
            caption.append(((DataArray)dataComponent).getComponentCount());
            caption.append(']');
        }
        
        // choice
        else if (dataComponent instanceof DataChoice)
        {
            caption.append(" - choice of");
        }
        
        return caption.toString();
    }
    
    
    protected String getTooltip(DataComponent dataComponent)
    {
        StringBuffer tooltip = new StringBuffer();
        
        if (dataComponent.getDescription() != null)
            tooltip.append("<p><b>Description: </b>").append(dataComponent.getDescription()).append("</p>");
        
        String def = dataComponent.getDefinition();
        if (def != null)
            tooltip.append("<p><b>Definition: </b><a target='_blank' href='").append(def).append("'/>").append(def).append("</a></p>");
        
        if (dataComponent instanceof HasRefFrames)
        {
            String refFrame = ((HasRefFrames) dataComponent).getReferenceFrame();
            if (refFrame != null)
                tooltip.append("<p><b>Ref Frame: </b><a target='_blank' href='").append(refFrame).append("'/>").append(refFrame).append("</a></p>");
            
            String localFrame = ((HasRefFrames) dataComponent).getLocalFrame();
            if (localFrame != null)
                tooltip.append("<p><b>Local Frame: </b><a target='_blank' href='").append(localFrame).append("'/>").append(localFrame).append("</a></p>");
        }
        
        if (dataComponent instanceof HasUom)
        {
            UnitReference unit = ((HasUom) dataComponent).getUom();
            String uom = null;
            
            if (unit.isSetCode())
                uom = unit.getCode();
            else if (unit.hasHref())
            {
                if (unit.getHref().equals(Time.ISO_TIME_UNIT))
                    uom = "ISO 8601";
                else
                    uom = unit.getHref();
            }  
                
            if (uom != null)
                tooltip.append("<p><b>Unit: </b>").append(uom).append("</p>");
        }
        
        return tooltip.toString();
    }
    
    
    protected String getPrettyName(DataComponent dataComponent)
    {
        String label = dataComponent.getLabel();
        if (label == null)
            label = DisplayUtils.getPrettyName(dataComponent.getName());
        return label;
    }
    
}
