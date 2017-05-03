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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.ui.api.UIConstants;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;


/**
 * <p>
 * Popup window showing a list of selectable OSH modules
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 24, 2017
 */
@SuppressWarnings("serial")
public class ModuleTypeSelectionPopup extends Window implements UIConstants
{
    static final String PROP_NAME = "name";
    static final String PROP_VERSION = "version";
    static final String PROP_DESC = "desc";
    static final String PROP_AUTHOR = "author";
    
    
    protected interface ModuleTypeSelectionCallback
    {
        public void onSelected(ModuleConfig config);
    }
    
    
    protected interface ModuleTypeSelectionWithClearCallback extends ModuleTypeSelectionCallback
    {
        public void onClearSelection();
    }
    
    
    public ModuleTypeSelectionPopup(final Class<?> moduleType, final ModuleTypeSelectionCallback callback)
    {
        super("Select Module Type");
        
        ModuleRegistry registry = SensorHub.getInstance().getModuleRegistry();
        Collection<IModuleProvider> providers = new ArrayList<IModuleProvider>();
        for (IModuleProvider provider: registry.getInstalledModuleTypes())
        {
            Class<?> configClass = provider.getModuleConfigClass();
            Class<?> moduleClass = provider.getModuleClass();
            if (moduleType.isAssignableFrom(configClass) || moduleType.isAssignableFrom(moduleClass))
                providers.add(provider);                
        }
        
        buildDialog(providers, callback);
    }
    
    
    public ModuleTypeSelectionPopup(Collection<IModuleProvider> moduleProviders, final ModuleTypeSelectionCallback callback)
    {
        super("Select Module Type");
        buildDialog(moduleProviders, callback);
    }
    
    
    protected void buildDialog(Collection<IModuleProvider> moduleProviders, final ModuleTypeSelectionCallback callback)
    {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        
        // generate table with module list
        final Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);       
        table.addContainerProperty(PROP_NAME, String.class, null);
        table.addContainerProperty(PROP_VERSION, String.class, null);
        table.addContainerProperty(PROP_DESC, String.class, null);
        table.addContainerProperty(PROP_AUTHOR, String.class, null);
        table.setColumnHeaders(new String[] {"Module Type", "Version", "Description", "Author"});
        table.setPageLength(10);
        table.setMultiSelect(false);
        
        final Map<Object, IModuleProvider> providerMap = new HashMap<Object, IModuleProvider>();
        for (IModuleProvider provider: moduleProviders)
        {
            Object id = table.addItem(new Object[] {
                    provider.getModuleName(),
                    provider.getModuleVersion(),
                    provider.getModuleDescription(),
                    provider.getProviderName()}, null);
            providerMap.put(id, provider);
        }
        layout.addComponent(table);
        
        // link to more modules
        Button installNew = new Button("Install More Modules...");
        installNew.setStyleName(STYLE_LINK);
        layout.addComponent(installNew);
        layout.setComponentAlignment(installNew, Alignment.MIDDLE_RIGHT);
        installNew.addClickListener(new ClickListener()
        {
            private static final long serialVersionUID = 1L;
            
            @Override
            public void buttonClick(ClickEvent event)
            {
                //close();
                getUI().addWindow(new DownloadModulesPopup());
            }
        });
        
        // buttons bar
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        layout.addComponent(buttons);
        layout.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
        
        // OK button
        final ModuleRegistry registry = SensorHub.getInstance().getModuleRegistry();
        Button okButton = new Button("OK");
        okButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event)
            {
                Object selectedItemId = table.getValue();
                IModuleProvider provider = providerMap.get(selectedItemId);
                
                try
                {
                    if (selectedItemId != null)
                    {
                        // send back new config object
                        ModuleConfig config = registry.createModuleConfig(provider);
                        callback.onSelected(config); 
                    }
                    
                    close();
                }
                catch (NoClassDefFoundError e)
                {
                    DisplayUtils.showDependencyError(provider.getClass(), e);
                    return;
                }
                catch (SensorHubException e)
                {
                    DisplayUtils.showErrorPopup("Cannot add new module", e);
                }
            }
        });
        buttons.addComponent(okButton);
        
        // also add clear button if callback allows for clearing
        if (callback instanceof ModuleTypeSelectionWithClearCallback)
        {
            // add clear button
            Button clearButton = new Button("Select None");
            clearButton.addClickListener(new Button.ClickListener() {
                private static final long serialVersionUID = 1L;
                
                @Override
                public void buttonClick(ClickEvent event)
                {
                    try
                    {
                        ((ModuleTypeSelectionWithClearCallback)callback).onClearSelection();
                    }
                    finally
                    {
                        close();
                    }                    
                }
            });
            buttons.addComponent(clearButton);
        }
        
        setContent(layout);
        center();
    }
}
