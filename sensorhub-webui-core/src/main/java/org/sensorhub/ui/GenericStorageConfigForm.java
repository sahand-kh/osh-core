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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.persistence.IStorageProvider;
import org.sensorhub.impl.persistence.MaxAgeAutoPurgeConfig;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.data.BaseProperty;
import com.vaadin.data.Property;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.Field;


public class GenericStorageConfigForm extends GenericConfigForm implements IModuleConfigForm
{
    private static final long serialVersionUID = 4462720978742325333L;
    public static final String PROP_STORAGE_PATH = "storagePath";
    public static final String PROP_STORAGE_CONFIG = "storageConfig";
    public static final String PROP_AUTOPURGE = "autoPurgeConfig";
    public static final String PROP_DATASRC_ID = "dataSourceID";
    
    
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop)
    {
        Field<Object> field = (Field<Object>)super.buildAndBindField(label, propId, prop);
        
        if (propId.equals(PROP_STORAGE_PATH))
            return null;
        else if (propId.equals(PROP_STORAGE_CONFIG + PROP_SEP + PROP_NAME))
            return null;
        
        else if (propId.equals(PROP_DATASRC_ID))
        {
            field = makeModuleSelectField(field, IDataProducer.class);
            field.addValidator(new StringLengthValidator(MSG_REQUIRED_FIELD, 1, 256, false));
        }
        
        return field;
    }
    
    
    @Override
    public Map<String, Class<?>> getPossibleTypes(String propId, BaseProperty<?> prop)
    {
        if (propId.equals(PROP_AUTOPURGE))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<String, Class<?>>();
            classList.put("Auto Purge by Maximum Age", MaxAgeAutoPurgeConfig.class);
            return classList;
        }
        
        return super.getPossibleTypes(propId, prop);
    }


    @Override
    public Collection<IModuleProvider> getPossibleModuleTypes(String propId, Class<?> configType)
    {
        Collection<IModuleProvider> storageModules = super.getPossibleModuleTypes(propId, configType);
        
        // remove all stream and read-only storage implementation from list
        Iterator<IModuleProvider> it = storageModules.iterator();
        while (it.hasNext())
        {
            IModuleProvider provider = it.next();
            if (provider.getModuleConfigClass().isAssignableFrom(StreamStorageConfig.class))
                it.remove();
            else if (provider instanceof IStorageProvider && ((IStorageProvider)provider).isReadOnly())
                it.remove();
        }
        
        return storageModules;
    }

}
