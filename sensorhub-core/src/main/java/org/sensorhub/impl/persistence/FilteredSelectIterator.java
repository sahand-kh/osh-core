/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * <p>
 * Helper class to write iterators that filter elements returned by another
 * iterator on-the-fly
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 15, 2017
 */
public abstract class FilteredSelectIterator<InputE, E> implements Iterator<E>
{
    E next;
    Iterator<InputE> it;
    
    
    public FilteredSelectIterator(Iterator<InputE> it)
    {
        this.it = it;
        preloadNext();
    }
    
    
    @Override
    public boolean hasNext()
    {
        return (next != null);
    }
    

    @Override
    public E next()
    {
        if (!hasNext())
            throw new NoSuchElementException();
        return preloadNext();
    }
    
    
    /**
     * Preload next element (filter is applied)
     * @return the current element (i.e. the one that should be returned by next())
     */
    protected E preloadNext()
    {
        E current = next;        
        next = null;
        
        // loop until we find the next acceptable item
        // or end of iteration
        while (next == null && it.hasNext())
        {
            InputE elt = it.next();
            next = accept(elt);
        }
        
        return current;
    }
    
    
    protected abstract E accept(InputE elt);

}
