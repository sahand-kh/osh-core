/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Default implementation of IModuleStateManager that saves info into files.</br>
 * A folder is created for each module the first time its state is saved.</br>
 * A main file is used for simple type values and a separate file is used for
 * each info stored as an OutputStream.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 2, 2015
 */
public class DefaultModuleStateManager implements IModuleStateManager
{
    private static final Logger log = LoggerFactory.getLogger(DefaultModuleStateManager.class);
    private static final String STATE_FILE_NAME = "state.txt";
    File folder;
    Properties stateProps;
    
    
    public DefaultModuleStateManager(String moduleDataPath, String localID)
    {
        folder = new File(moduleDataPath, FileUtils.safeFileName(localID));
        stateProps = new Properties();
        
        File stateFile = getStateFile();
        if (stateFile.exists()) 
        {
            try (InputStream is = new BufferedInputStream(new FileInputStream(stateFile)))
            {
                stateProps.load(is);
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Cannot read module state information", e);
            }
        }
    }


    @Override
    public String getAsString(String key)
    {
        return stateProps.getProperty(key);
    }


    @Override
    public Float getAsFloat(String key)
    {
        String val = stateProps.getProperty(key);
        if (val == null)
            return null;
        return Float.parseFloat(val);
    }


    @Override
    public Double getAsDouble(String key)
    {
        String val = stateProps.getProperty(key);
        if (val == null)
            return null;
        return Double.parseDouble(val);
    }


    @Override
    public Integer getAsInt(String key)
    {
        String val = stateProps.getProperty(key);
        if (val == null)
            return null;
        return Integer.parseInt(val);
    }


    @Override
    public Long getAsLong(String key)
    {
        String val = stateProps.getProperty(key);
        if (val == null)
            return null;
        return Long.parseLong(val);
    }


    @Override
    public InputStream getAsInputStream(String key)
    {
        File dataFile = getDataFile(key);
        FileInputStream is = null;
        
        try
        {
            if (!dataFile.exists())
                return null;
            
            is = new FileInputStream(dataFile);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot open data file for reading", e);
        }
        
        return new BufferedInputStream(is); 
    }


    @Override
    public void put(String key, float value)
    {
        stateProps.setProperty(key, Float.toString(value));
    }


    @Override
    public void put(String key, double value)
    {
        stateProps.setProperty(key, Double.toString(value));
    }


    @Override
    public void put(String key, int value)
    {
        stateProps.setProperty(key, Integer.toString(value));
    }


    @Override
    public void put(String key, long value)
    {
        stateProps.setProperty(key, Long.toString(value));
    }


    @Override
    public void put(String key, String value)
    {
        stateProps.setProperty(key, value);        
    }
    

    @Override
    public OutputStream getOutputStream(String key)
    {
        ensureFolder();
        FileOutputStream os = null;
        
        try
        {
            File dataFile = getDataFile(key);
            os = new FileOutputStream(dataFile);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot open data file for writing", e);
        }
        
        return new BufferedOutputStream(os);
    }
    
    
    @Override
    public void flush()
    {
        ensureFolder();
        
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(getStateFile())))
        {
            stateProps.store(os, null);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot save module state information", e);
        }        
    }
    
    
    @Override
    public String getFolder()
    {
        ensureFolder();
        return folder.getAbsolutePath();
    }


    @Override
    public void cleanup()
    {
        try
        {
            if (folder.exists())
                FileUtils.deleteRecursively(folder);
        }
        catch (IOException e)
        {
            log.error("Cannot delete module state information: " + folder, e);
        }
    }
    
    
    protected void ensureFolder()
    {
        if (!folder.exists())
            folder.mkdirs();
    }
    
    
    protected File getDataFile(String key)
    {
        String fileName = key.toLowerCase() + ".dat";
        return new File(folder, FileUtils.safeFileName(fileName));
    }
    
    
    protected File getStateFile()
    {
        return new File(folder, STATE_FILE_NAME);
    }
}
