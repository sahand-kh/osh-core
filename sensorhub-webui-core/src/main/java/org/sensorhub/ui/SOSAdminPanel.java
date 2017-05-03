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

import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.service.sos.SOSService;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.MyBeanItem;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import com.vaadin.data.Property;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;


/**
 * <p>
 * Admin panel for SOS service modules.<br/>
 * This adds a section with example links to SOS operations
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 1.0
 */
@SuppressWarnings("serial")
public class SOSAdminPanel extends DefaultModulePanel<SOSService> implements IModuleAdminPanel<SOSService>
{
    private static final String PROP_ENDPOINT = "endPoint";
    private static final String LINK_TARGET = "osh-sos";
    
    
    static class LinkItem extends HorizontalLayout
    {
        public LinkItem(String label, String linkText, String href)
        {
            setSpacing(true);
            addComponent(new Label(label + ":"));
            addComponent(new Link(linkText, new ExternalResource(href), LINK_TARGET, 0, 0, null));
        }
        
        public void addLinkItem(String linkText, String href)
        {
            addComponent(new Link(linkText, new ExternalResource(href), LINK_TARGET, 0, 0, null));
        }
    }    
    
    
    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final SOSService module)
    {
        super.build(beanItem, module);       
        
        // get capabilities
        SOSServiceCapabilities caps = module.getCapabilities();
        
        // get base URL
        String baseUrl = null;
        Property<?> endPointProp = beanItem.getItemProperty(PROP_ENDPOINT);
        if (endPointProp != null)
        {
            baseUrl = (String)endPointProp.getValue();
            if (baseUrl != null)
                baseUrl = baseUrl.substring(1);
        }
        
        if (module.isStarted() && caps != null && baseUrl != null)
        {
            // section label
            Label sectionLabel = new Label("Test Links");
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            addComponent(sectionLabel);

            // link to capabilities
            baseUrl += "?service=SOS&version=2.0&request=";
            String href = baseUrl + "GetCapabilities";
            Link link = new Link("Service Capabilities", new ExternalResource(href), LINK_TARGET, 0, 0, null);
            link.setWidthUndefined();
            addComponent(link);
            
            // offering links in tabs
            TabSheet linkTabs = new TabSheet();
            addComponent(linkTabs);
                        
            for (SOSOfferingCapabilities offering: caps.getLayers())
            {
                VerticalLayout tabLayout = new VerticalLayout();
                tabLayout.setMargin(true);
                Tab tab = linkTabs.addTab(tabLayout, offering.getTitle());
                tab.setDescription(offering.getDescription());
                LinkItem linkItem;
                
                // sensor description
                href = baseUrl + "DescribeSensor&procedure=" + offering.getMainProcedure();
                linkItem = new LinkItem("Sensor Description", "XML", href);
                href += "&procedureDescriptionFormat=" + SOSOfferingCapabilities.FORMAT_SML2_JSON;
                linkItem.addLinkItem("JSON", href);
                tabLayout.addComponent(linkItem);
                
                // fois
                href = baseUrl + "GetFeatureOfInterest&procedure=" + offering.getMainProcedure();
                linkItem = new LinkItem("Features of Interest", "XML", href);
                //href += "&procedureDescriptionFormat=" + SOSOfferingCapabilities.FORMAT_SML2_JSON;
                //linkItem.addLinkItem("JSON", href);
                tabLayout.addComponent(linkItem);
                
                for (String obs: offering.getObservableProperties())
                {                
                    // spacer
                    Label spacer = new Label();
                    spacer.setStyleName(STYLE_SPACER);
                    spacer.setHeight(10, Unit.PIXELS);
                    tabLayout.addComponent(spacer);
                    
                    // observable name
                    String label = obs.substring(obs.lastIndexOf('/')+1);
                    tabLayout.addComponent(new Label("<b>"+label+"</b>", ContentMode.HTML));
                    
                    // result template
                    href = baseUrl + "GetResultTemplate&offering=" + offering.getIdentifier() + "&observedProperty=" + obs;
                    linkItem = new LinkItem("Record Description", "XML", href);
                    href += "&responseFormat=" + OWSUtils.JSON_MIME_TYPE;
                    linkItem.addLinkItem("JSON", href);
                    tabLayout.addComponent(linkItem);
                    
                    // latest obs
                    href = baseUrl + "GetResult&offering=" + offering.getIdentifier() + "&observedProperty=" + obs +
                                     "&temporalFilter=phenomenonTime,now";
                    linkItem = new LinkItem("Latest Measurements", "RAW", href);
                    href += "&responseFormat=" + OWSUtils.JSON_MIME_TYPE;
                    linkItem.addLinkItem("JSON", href);
                    tabLayout.addComponent(linkItem);
                }
            }
        }
    }
}
