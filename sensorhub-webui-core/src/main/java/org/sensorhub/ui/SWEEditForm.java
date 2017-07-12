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

import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.SimpleComponent;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;


@SuppressWarnings("serial")
public class SWEEditForm extends SWECommonForm
{
    transient DataComponent component;

    
    public SWEEditForm(DataComponent component)
    {
        this.addSpacing = true;
        this.component = component;
        if (!component.hasData())
            component.assignNewDataBlock();
        buildForm();
    }
    
    
    protected void buildForm()
    {
        setSpacing(true);
        addComponent(buildWidget(component, false));
    }
    
    
    @Override
    protected Component buildWidget(final DataComponent component, boolean showValues)
    {
        if (component instanceof DataChoice)
        {
            final DataChoice dataChoice = (DataChoice)component;
            if (dataChoice.getSelectedItem() == null)
                dataChoice.setSelectedItem(0);
            
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(true);
                        
            // combo to select command type
            HorizontalLayout header = getCaptionLayout(component);
            ListSelect combo = new ListSelect();
            combo.setItemCaptionMode(ItemCaptionMode.ID);
            combo.setNullSelectionAllowed(false);
            combo.setRows(1);
            for (int i = 0; i < component.getComponentCount(); i++)
            {
                DataComponent c = component.getComponent(i);
                combo.addItem(c.getName());
            }
            combo.select(dataChoice.getSelectedItem().getName());
            combo.addValueChangeListener(new ValueChangeListener() {
                private static final long serialVersionUID = 1L;
                @Override
                public void valueChange(ValueChangeEvent event)
                {
                    // select choice item and redraw
                    dataChoice.setSelectedItem((String)event.getProperty().getValue());
                    buildForm();
                }
        
            });            
            header.addComponent(combo);
            layout.addComponent(header);
            
            // display form for selected item
            Component w = buildWidget(dataChoice.getSelectedItem(), showValues);
            layout.addComponent(w);            
                
            return layout;
        }
        else if (component instanceof SimpleComponent)
        {
            HorizontalLayout layout = getCaptionLayout(component);
            final TextField f = new TextField();
            f.setValue(component.getData().getStringValue());
            layout.addComponent(f);
            f.addValueChangeListener(new ValueChangeListener() {
                private static final long serialVersionUID = 1L;
                @Override
                public void valueChange(ValueChangeEvent event)
                {
                    component.getData().setStringValue(f.getValue());
                }
            });
            return layout;            
        }
        else
        {
            Component widget = super.buildWidget(component, showValues);
            ((SpacingHandler)widget).setSpacing(true);
            return widget;
        }
    }
    
    
    protected HorizontalLayout getCaptionLayout(DataComponent component)
    {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        
        Label l = new Label();
        l.setContentMode(ContentMode.HTML);
        l.setValue(getCaption(component, false));
        l.setDescription(getTooltip(component));
        header.addComponent(l);
        header.setComponentAlignment(l, Alignment.MIDDLE_LEFT);
        
        return header;
    }
    
}
