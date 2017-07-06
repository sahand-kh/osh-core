/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.swe;

import org.vast.data.BinaryComponentImpl;
import org.vast.data.DataIterator;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryComponent;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Utility class to compute data component structure hashcode
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 16, 2016
 */
public class DataStructureHash
{
    private int hashcode;


    public DataStructureHash(DataComponent comp, DataEncoding enc)
    {
        hashcode = computeDataStructureHashCode(comp, enc);
    }


    int computeDataStructureHashCode(DataComponent comp, DataEncoding enc)
    {
        StringBuilder buf = new StringBuilder();

        boolean root = true;
        DataIterator it = new DataIterator(comp);
        while (it.hasNext())
        {
            comp = it.next();

            // skip root name because it's not always set
            if (!root)
            {
                buf.append(comp.getName());
                buf.append('|');
            }
            root = false;

            buf.append(comp.getClass().getSimpleName());

            String defUri = comp.getDefinition();
            if (defUri != null)
            {
                buf.append('|');
                buf.append(defUri);
            }

            buf.append('\n');
        }

        if (enc != null)
        {
            buf.append(enc.getClass().getSimpleName());
            if (enc instanceof BinaryEncoding)
            {
                for (BinaryMember opts : ((BinaryEncoding) enc).getMemberList())
                {
                    buf.append('|');
                    buf.append(opts.getRef());
                    buf.append('|');
                    if (opts instanceof BinaryComponent)
                    {
                        buf.append(((BinaryComponentImpl) opts).getCdmDataType());
                    }
                    else if (opts instanceof BinaryBlock)
                    {
                        buf.append(((BinaryBlock) opts).getCompression());
                        buf.append('|');
                        buf.append(((BinaryBlock) opts).getEncryption());
                    }
                }
            }
        }

        return buf.toString().hashCode();
    }


    @Override
    public int hashCode()
    {
        return hashcode;
    }


    @Override
    public boolean equals(Object obj)
    {
        if (hashcode == ((DataStructureHash) obj).hashcode)
            return true;
        return false;
    }
}