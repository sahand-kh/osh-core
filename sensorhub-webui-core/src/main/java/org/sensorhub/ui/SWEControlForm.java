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

import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.SensorException;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;


@SuppressWarnings("serial")
public class SWEControlForm extends SWEEditForm
{
    transient ISensorControlInterface controlInput;
    
    
    public SWEControlForm(final ISensorControlInterface controlInput)
    {
        super(controlInput.getCommandDescription().copy());
        this.controlInput = controlInput;
        this.component.assignNewDataBlock();
        buildForm();
    }
    
    
    protected void buildForm()
    {
        super.buildForm();
        
        // send button
        Button sendBtn = new Button("Send Command");
        addComponent(sendBtn);
        setComponentAlignment(sendBtn, Alignment.MIDDLE_LEFT);
        sendBtn.addClickListener(new ClickListener()
        {   
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event)
            {
                try
                {
                    controlInput.execCommand(component.getData());
                }
                catch (SensorException e)
                {
                    DisplayUtils.showErrorPopup("Error while sending command to sensor", e);
                }
            }
        });
    }
}
