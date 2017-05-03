/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.impl.persistence.IteratorWrapper;


/**
 * <p>
 * PERST implementation of a time series data store for a single record type
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 7, 2015
 */
class TimeSeriesImpl extends Persistent implements IRecordStoreInfo
{
    static final Key KEY_DATA_START_ALL_TIME = new Key(Double.NEGATIVE_INFINITY);
    static final Key KEY_DATA_END_ALL_TIME = new Key(Double.POSITIVE_INFINITY);
    
    DataComponent recordDescription;
    DataEncoding recommendedEncoding;
    Index<DataBlock> recordIndex;
    protected transient BasicStorageRoot parentStore;
    
    
    /*
     * Implementation of an individual time series record
     */
    class DBRecord extends Persistent implements IDataRecord
    {
        DataKey key;
        DataBlock value;
        
        // default constructor needed for PERST on Android JVM
        DBRecord() {}
        
        protected DBRecord(DataKey key, DataBlock value)
        {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public DataKey getKey()
        {
            return this.key;
        }

        @Override
        public DataBlock getData()
        {
            return this.value;
        }     
    }
    
    /*
     * This class is used to wrap a PERST index entry so we can preload the key and value
     * when necessary. Indeed, using the live entry during concurrent read/write ops doesn't
     * lead the right result because the BTree page may have changed before getKey() or
     * getValue() are called.
     */
    class CachedEntry<T> implements Map.Entry<Object, T>
    {
        Entry<Object, T> liveEntry;
        Object key;
        T value;
        
        CachedEntry(Entry<Object,T> liveEntry)
        {
            this.liveEntry = liveEntry;
        }
        
        public Object getKey()
        {
            if (key == null)
                key = liveEntry.getKey();
            return key;
        }

        public T getValue()
        {
            if (value == null)
                value = liveEntry.getValue();
            return value;
        }

        public T setValue(T value)
        {
            return null;
        }        
    }
    

    // default constructor needed on Android JVM
    TimeSeriesImpl() { }


    TimeSeriesImpl(Storage db, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        super(db);
        this.recordDescription = recordDescription;
        this.recommendedEncoding = recommendedEncoding;
        recordIndex = db.<DataBlock> createIndex(double.class, true);
    }


    @Override
    public String getName()
    {
        return recordDescription.getName();
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return recommendedEncoding;
    }

    
    int getNumRecords()
    {
        try
        {
            recordIndex.sharedLock();
            return recordIndex.size();
        }
        finally
        {
            recordIndex.unlock();
        }
    }


    DataBlock getDataBlock(DataKey key)
    {
        try
        {
            recordIndex.sharedLock();
            return recordIndex.get(new Key(key.timeStamp));
        }
        finally
        {
            recordIndex.unlock();
        }
    }


    Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        final Iterator<Entry<Object, DataBlock>> it = getEntryIterator(filter, true);
        
        return new Iterator<DataBlock>()
        {
            @Override
            public final boolean hasNext()
            {
                return it.hasNext();
            }

            @Override
            public final DataBlock next()
            {
                Entry<Object, DataBlock> entry = it.next();
                return entry.getValue();
            }

            @Override
            public final void remove()
            {
                it.remove();
            }
        };
    }


    int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        // use entry iterator so datablocks are not loaded during scan
        Iterator<Entry<Object, DataBlock>> it = getEntryIterator(filter, false);
        
        int count = 0;
        while (it.hasNext() && count <= maxCount)
        {
            it.next();
            count++;
        }
        
        return count;
    }


    Iterator<DBRecord> getRecordIterator(IDataFilter filter)
    {
        final Iterator<Entry<Object, DataBlock>> it = getEntryIterator(filter, true);
        
        return new Iterator<DBRecord>()
        {
            @Override
            public final boolean hasNext()
            {
                return it.hasNext();
            }

            @Override
            public final DBRecord next()
            {
                Entry<Object, DataBlock> entry = it.next();
                DataKey key = new DataKey(recordDescription.getName(), (double)entry.getKey());
                return new DBRecord(key, entry.getValue());
            }

            @Override
            public final void remove()
            {
                it.remove();
            }
        };
    }
    
    
    Iterator<Entry<Object,DataBlock>> getEntryIterator(IDataFilter filter, boolean preloadValue)
    {
        double[] timeRange = filter.getTimeStampRange();
        Key keyFirst = new Key(timeRange == null ? Double.NEGATIVE_INFINITY : timeRange[0]);
        Key keyLast = new Key(timeRange == null ? Double.POSITIVE_INFINITY : timeRange[1]);
        return getEntryIterator(keyFirst, keyLast, Index.ASCENT_ORDER, preloadValue);
    }
    
        
    /*
     * Gets an entry iterator over the recordIndex protected by a shared lock
     */
    Iterator<Entry<Object,DataBlock>> getEntryIterator(Key keyFirst, Key keyLast, int order, final boolean preloadValue)
    {
        try
        {
            recordIndex.sharedLock();
            
            Iterator<Entry<Object,DataBlock>> indexIt = recordIndex.entryIterator(keyFirst, keyLast, order);
            return new IteratorWrapper<Entry<Object,DataBlock>, Entry<Object,DataBlock>>(indexIt)
            {
                @Override
                protected Entry<Object,DataBlock> preloadNext()
                {
                    try
                    {    
                        recordIndex.sharedLock();
                        return super.preloadNext();
                    }
                    finally
                    {
                        recordIndex.unlock();
                    }
                }
                
                @Override
                protected CachedEntry<DataBlock> process(Entry<Object,DataBlock> elt)
                {
                    // preload key and value if needed
                    CachedEntry<DataBlock> cachedItem = new CachedEntry<>(elt);
                    cachedItem.getKey(); // preload key
                    if (preloadValue)
                        cachedItem.getValue(); // preload value
                    return cachedItem;
                }
            };
        }
        finally
        {
            recordIndex.unlock();
        }
    }


    void store(DataKey key, DataBlock data)
    {
        try
        {
            recordIndex.exclusiveLock();
            recordIndex.put(new Key(key.timeStamp), data);
        }
        finally
        {
            recordIndex.unlock();
        }
    }


    void update(DataKey key, DataBlock data)
    {
        try
        {
            recordIndex.exclusiveLock();
            DataBlock oldData = recordIndex.set(new Key(key.timeStamp), data);
            if (oldData != null && oldData != data)
                getStorage().deallocate(oldData);
        }
        finally
        {
            recordIndex.unlock();
        }
    }


    void remove(DataKey key)
    {
        try
        {
            recordIndex.exclusiveLock();
            DataBlock oldData = recordIndex.remove(new Key(key.timeStamp));
            getStorage().deallocate(oldData);
        }
        finally
        {
            recordIndex.unlock();
        }
    }


    int remove(IDataFilter filter)
    {
        int count = 0;
        
        Key keyFirst = new Key(filter.getTimeStampRange()[0]);
        Key keyLast = new Key(filter.getTimeStampRange()[1]);
        Iterator<Entry<Object,DataBlock>> it = getEntryIterator(keyFirst, keyLast, Index.ASCENT_ORDER, false);
            
        while (it.hasNext())
        {
            Entry<Object,DataBlock> oldData = it.next();            
            getStorage().deallocate(oldData.getValue());
            it.remove();
        }

        return count;
    }


    double[] getDataTimeRange()
    {
        try
        {
            recordIndex.sharedLock();            
            IterableIterator<Entry<Object, DataBlock>> it;
            
            it = recordIndex.entryIterator(KEY_DATA_START_ALL_TIME, KEY_DATA_END_ALL_TIME, Index.ASCENT_ORDER);
            if (!it.hasNext())
                return new double[] { Double.NaN, Double.NaN };
            Entry<Object, DataBlock> first = it.next();
    
            it = recordIndex.entryIterator(KEY_DATA_START_ALL_TIME, KEY_DATA_END_ALL_TIME, Index.DESCENT_ORDER);
            Entry<Object, DataBlock> last = it.next();
    
            return new double[] { (double)first.getKey(), (double)last.getKey() };
        }
        finally
        {
            recordIndex.unlock();
        }
    }
    
    
    public Iterator<double[]> getRecordsTimeClusters()
    {
        final Iterator<Entry<Object, DataBlock>> it;
        it = getEntryIterator(KEY_DATA_START_ALL_TIME, KEY_DATA_END_ALL_TIME, Index.ASCENT_ORDER, false);
        
        return new Iterator<double[]>()
        {
            double lastTime = Double.NaN;
            
            @Override
            public boolean hasNext()
            {
                return it.hasNext();
            }

            @Override
            public double[] next()
            {
                double[] clusterTimeRange = new double[2];
                clusterTimeRange[0] = lastTime;
                
                while (it.hasNext())
                {
                    // PERST doesn't load object from disk until getValue() is called so we're good here
                    double recTime = (double)it.next().getKey();
                    
                    if (Double.isNaN(lastTime))
                    {
                        clusterTimeRange[0] = recTime;
                        lastTime = recTime;
                    }
                    else
                    {
                        double dt = recTime - lastTime;
                        lastTime = recTime;
                        if (dt > 60.0)
                            break;
                    }
                    
                    clusterTimeRange[1] = recTime;
                }
                
                return clusterTimeRange;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }    
        };
    }
}