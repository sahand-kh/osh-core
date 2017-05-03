/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.data;

import java.lang.reflect.Field;


@SuppressWarnings("serial")
public class FieldProperty extends BaseProperty<Object>
{
    transient Object instance;


    public FieldProperty(Object instance, Field f)
    {
        this.instance = instance;
        this.f = f;
    }


    @Override
    public Object getValue()
    {
        try
        {
            return f.get(instance);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public void setValue(Object newValue)
    {
        try
        {
            f.set(instance, newValue);
            fireValueChange();
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public Class<?> getType()
    {
        if (getValue() instanceof Enum<?>)
            return getValue().getClass();
        else
            return convertPrimitiveType(f.getType());
    }


    private Class<?> convertPrimitiveType(Class<?> type)
    {
        // Gets the return type from get method
        if (type.isPrimitive())
        {
            if (type.equals(Boolean.TYPE))
            {
                type = Boolean.class;
            }
            else if (type.equals(Integer.TYPE))
            {
                type = Integer.class;
            }
            else if (type.equals(Float.TYPE))
            {
                type = Float.class;
            }
            else if (type.equals(Double.TYPE))
            {
                type = Double.class;
            }
            else if (type.equals(Byte.TYPE))
            {
                type = Byte.class;
            }
            else if (type.equals(Character.TYPE))
            {
                type = Character.class;
            }
            else if (type.equals(Short.TYPE))
            {
                type = Short.class;
            }
            else if (type.equals(Long.TYPE))
            {
                type = Long.class;
            }
        }
        
        return type;
    }
}