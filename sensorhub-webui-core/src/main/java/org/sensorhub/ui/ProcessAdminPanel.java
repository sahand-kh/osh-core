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

import net.opengis.gml.v32.impl.ReferenceImpl;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.Link;
import net.opengis.sensorml.v20.Settings;
import net.opengis.sensorml.v20.SimpleProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.processing.SMLProcessConfig;
import org.sensorhub.impl.processing.SMLProcessImpl;
import org.sensorhub.impl.processing.StreamDataSource;
import org.sensorhub.ui.ModuleInstanceSelectionPopup.ModuleInstanceSelectionCallback;
import org.sensorhub.ui.ProcessFlowDiagram.Connection;
import org.sensorhub.ui.ProcessFlowDiagram.ProcessBlock;
import org.sensorhub.ui.ProcessFlowDiagram.ProcessFlowState;
import org.sensorhub.ui.ProcessSelectionPopup.ProcessSelectionCallback;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.MyBeanItem;
import org.sensorhub.utils.FileUtils;
import org.vast.process.ProcessInfo;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEHelper;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;


/**
 * <p>
 * Admin panel for sensor modules.<br/>
 * This adds a section to view structure of inputs and outputs,
 * and allows the user to send commands and view output data values.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 1.0
 */
@SuppressWarnings("serial")
public class ProcessAdminPanel extends DefaultModulePanel<IProcessModule<?>> implements IModuleAdminPanel<IProcessModule<?>>
{
    Panel outputPanel, commandsPanel, processFlowPanel;
    ProcessFlowDiagram diagram;
    SMLProcessConfig config;
    
    
    static class Spacing extends Label
    {
        public Spacing()
        {
            setHeight(0, Unit.PIXELS);
        }
    }
    
    
    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final IProcessModule<?> module)
    {
        super.build(beanItem, module);
        
        if (module instanceof SMLProcessImpl)
        {
            addProcessFlowEditor((SMLProcessImpl)module);
            this.config = (SMLProcessConfig)beanItem.getBean();
        }
        /*// sensor info panel
        if (module.isInitialized())
        {
            Label sectionLabel = new Label("Sensor Info");
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            addComponent(sectionLabel);
            addComponent(new Label("<b>Unique ID:</b> " + module.getUniqueIdentifier(), ContentMode.HTML));
            AbstractFeature foi = module.getCurrentFeatureOfInterest();
            if (foi != null)
                addComponent(new Label("<b>FOI ID:</b> " + foi.getUniqueIdentifier(), ContentMode.HTML));
        
            // inputs section
            if (!module.getCommandInputs().isEmpty())
            {
                // title
                addComponent(new Spacing());
                HorizontalLayout titleBar = new HorizontalLayout();
                titleBar.setSpacing(true);
                sectionLabel = new Label("Inputs");
                sectionLabel.addStyleName(STYLE_H3);
                sectionLabel.addStyleName(STYLE_COLORED);
                titleBar.addComponent(sectionLabel);
                titleBar.setComponentAlignment(sectionLabel, Alignment.MIDDLE_LEFT);
                titleBar.setHeight(31.0f, Unit.PIXELS);
                addComponent(titleBar);
                
                // control panels
                buildControlInputsPanels(module);
            }
            
            // outputs section
            if (!module.getAllOutputs().isEmpty())
            {
                // title
                addComponent(new Spacing());
                HorizontalLayout titleBar = new HorizontalLayout();
                titleBar.setSpacing(true);
                sectionLabel = new Label("Outputs");
                sectionLabel.addStyleName(STYLE_H3);
                sectionLabel.addStyleName(STYLE_COLORED);
                titleBar.addComponent(sectionLabel);
                titleBar.setComponentAlignment(sectionLabel, Alignment.MIDDLE_LEFT);
                
                // refresh button
                final Timer timer = new Timer();
                final Button refreshButton = new Button("Refresh");
                refreshButton.setDescription("Toggle auto-refresh data once per second");
                refreshButton.setIcon(REFRESH_ICON);
                refreshButton.addStyleName(STYLE_SMALL);
                refreshButton.addStyleName(STYLE_QUIET);
                refreshButton.setData(false);
                titleBar.addComponent(refreshButton);
                titleBar.setComponentAlignment(refreshButton, Alignment.MIDDLE_LEFT);
                addComponent(titleBar);
                
                refreshButton.addClickListener(new ClickListener() {
                    transient TimerTask autoRefreshTask;                    
                    @Override
                    public void buttonClick(ClickEvent event)
                    {
                        // toggle button state
                        boolean state = !(boolean)refreshButton.getData();
                        refreshButton.setData(state);
                        
                        if (state)
                        {
                            autoRefreshTask = new TimerTask()
                            {
                                @Override
                                public void run()
                                {
                                    final UI ui = ProcessAdminPanel.this.getUI();
                                    
                                    if (ui != null)
                                    {
                                        ui.access(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                rebuildOutputsPanels(module);
                                                ui.push();
                                            }
                                        });
                                    }
                                    else
                                        cancel(); // if panel was detached
                                }
                            };
                            timer.schedule(autoRefreshTask, 0L, 1000L);                    
                            refreshButton.setIcon(FontAwesome.TIMES);
                            refreshButton.setCaption("Stop");
                        }
                        else
                        {
                            autoRefreshTask.cancel();
                            refreshButton.setIcon(REFRESH_ICON);
                            refreshButton.setCaption("Refresh");
                        }
                    }
                });               
                        
                // output panels
                rebuildOutputsPanels(module);
            }
        }*/
    }
        
        
    protected void addProcessFlowEditor(final SMLProcessImpl module)
    {
        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        addComponent(buttonBar);        
        
        // add data source button
        Button addDatasrcBtn = new Button("Datasource", ADD_ICON);
        addDatasrcBtn.addStyleName(STYLE_SMALL);
        buttonBar.addComponent(addDatasrcBtn);        
        addDatasrcBtn.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                // show popup to select among available module types
                final ModuleInstanceSelectionCallback callback = new ModuleInstanceSelectionCallback() {
                    @Override
                    @SuppressWarnings("rawtypes")
                    public void onSelected(IModule module) throws ProcessingException
                    {
                        diagram.addNewDataSource((IDataProducer)module);
                    }
                };
                
                // popup the list so the user can select what he wants
                ModuleInstanceSelectionPopup popup = new ModuleInstanceSelectionPopup(IDataProducer.class, callback);
                popup.setModal(true);
                getUI().addWindow(popup);
            }
        });
        
        // add process button
        Button addProcessBtn = new Button("Process", ADD_ICON);
        addProcessBtn.addStyleName(STYLE_SMALL);
        buttonBar.addComponent(addProcessBtn);
        addProcessBtn.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                // show popup to select among available module types
                final ProcessSelectionCallback callback = new ProcessSelectionCallback() {
                    @Override
                    public void onSelected(String name, ProcessInfo info) throws ProcessingException
                    {
                        diagram.addNewProcess(name, info);
                    }
                };
                
                // popup the list so the user can select what he wants
                ProcessSelectionPopup popup = new ProcessSelectionPopup(getParentHub().getProcessingManager().getAllProcessingPackages(), callback);
                popup.setModal(true);
                getUI().addWindow(popup);
            }
        });
        
        // add process flow diagram
        diagram = new ProcessFlowDiagram(module.getProcessChain());
        addComponent(diagram);
    }
    
    
    @Override
    protected void beforeUpdateConfig() throws ProcessingException
    {
        if (module instanceof SMLProcessImpl)
            saveProcessChain();
    }
    
    
    protected void saveProcessChain() throws ProcessingException
    {
        AggregateProcessImpl processChain = null;
        
        try
        {
            ProcessFlowState state = diagram.getState();
            
            // do nothing if diagram is empty
            // maybe we just want to read from file
            if (state.processBlocks.isEmpty())
                return;
            
            // convert diagram state to SensorML process chain
            String uid = module.getUniqueIdentifier();
            SMLHelper sml = SMLHelper.createAggregateProcess(uid);
            processChain = (AggregateProcessImpl)sml.getDescription();
            
            // data sources            
            for (ProcessBlock block: state.dataSources.values())
            {
                SimpleProcess p = sml.newSimpleProcess();
                p.setUniqueIdentifier(block.id);
                p.setTypeOf(new ReferenceImpl(block.uri));
                Settings settings = sml.newSettings();
                settings.addSetValue("parameters/"+StreamDataSource.PRODUCER_URI_PARAM, block.id);
                p.setConfiguration(settings);
                saveShape(block, p);
                processChain.addComponent(block.name, p);
            }
            
            // process blocks
            for (ProcessBlock block: state.processBlocks.values())
            {
                SimpleProcess p = sml.newSimpleProcess();
                p.setTypeOf(new ReferenceImpl(block.uri));
                saveShape(block, p);
                processChain.addComponent(block.name, p);                
                processChain.addOutput("test", new SWEHelper().newQuantity());
            }
            
            // connections
            for (Connection c: state.connections.values())
            {
                Link link = sml.newLink();
                link.setSource(c.src);
                link.setDestination(c.dest);
                processChain.addConnection(link);
            }
        }
        catch (Exception e)
        {
            throw new ProcessingException("Cannot create SensorML process chain", e);
        }
        
        // save SensorML file
        String smlPath = config.getSensorMLPath();
        if (smlPath == null || !FileUtils.isSafeFilePath(smlPath))
            throw new ProcessingException("Cannot save process chain: A valid SensorML file path must be provided");
        
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(smlPath)))
        {
            new SMLUtils(SMLUtils.V2_0).writeProcess(os, processChain, true);
        }
        catch (Exception e)
        {
            throw new ProcessingException(String.format("Cannot write SensorML description at '%s'", smlPath), e);
        }        
    }
    
    
    protected void saveShape(ProcessBlock block, AbstractProcess p)
    {
    }
    
    
    @Override
    protected void refreshContent()
    {
        ProcessFlowDiagram oldDiagram = diagram;
        diagram = new ProcessFlowDiagram(((SMLProcessImpl)module).getProcessChain());
        replaceComponent(oldDiagram, diagram);
    }
    
        
    protected void rebuildOutputsPanels(ISensorModule<?> module)
    {
        if (module != null)
        {
            Panel oldPanel;
            
            // measurement outputs
            if (!module.getObservationOutputs().isEmpty())
            {
                oldPanel = outputPanel;
                outputPanel = newPanel("Outputs");
                for (ISensorDataInterface output: module.getObservationOutputs().values())
                {
                    DataComponent dataStruct = output.getRecordDescription().copy();
                    DataBlock latestRecord = output.getLatestRecord();
                    if (latestRecord != null)
                        dataStruct.setData(latestRecord);
                    
                    // data structure
                    Component sweForm = new SWECommonForm(dataStruct);
                    ((Layout)outputPanel.getContent()).addComponent(sweForm);
                }  
                
                if (oldPanel != null)
                    replaceComponent(oldPanel, outputPanel);
                else
                    addComponent(outputPanel);
            }
        }
    }
    
    
    protected void buildControlInputsPanels(ISensorModule<?> module)
    {
        if (module != null)
        {
            Panel oldPanel;
            
            // command inputs
            oldPanel = commandsPanel;
            commandsPanel = newPanel("Command Inputs");
            for (ISensorControlInterface input: module.getCommandInputs().values())
            {
                Component sweForm = new SWEControlForm(input);
                ((Layout)commandsPanel.getContent()).addComponent(sweForm);
            }           

            if (oldPanel != null)
                replaceComponent(oldPanel, commandsPanel);
            else
                addComponent(commandsPanel);
        }
    }
    
    
    protected Panel newPanel(String title)
    {
        Panel panel = new Panel(title);
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setDefaultComponentAlignment(Alignment.TOP_LEFT);
        panel.setContent(layout);
        return panel;
    }
}
