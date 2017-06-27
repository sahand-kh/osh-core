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

import java.io.File;
import java.io.PrintWriter;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.common.EventBus;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.FileAppender;


/**
 * <p>
 * Class providing default implementation of common module API methods 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Oct 30, 2014
 */
public abstract class AbstractModule<ConfigType extends ModuleConfig> implements IModule<ConfigType>
{
    protected ISensorHub hub;
    protected Logger logger;
    protected IEventHandler eventHandler;
    protected ConfigType config;
    protected ModuleState state = ModuleState.LOADED;
    protected ModuleSecurity securityHandler;
    protected final Object stateLock = new Object();
    protected boolean startRequested;
    protected Throwable lastError;
    protected String statusMsg;
    

    public AbstractModule() {}
    
    
    public void setParentHub(ISensorHub hub)
    {
        if (this.hub != null)
            throw new IllegalStateException("Parent hub has already been assigned");
        this.hub = hub;
    }
    
    
    public ISensorHub getParentHub()
    {
        return this.hub;
    }
    
    
    protected void checkParentHub()
    {
        Asserts.checkState(hub != null, "Parent sensor hub hasn't been set");
    }
    
    
    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    }
    
    
    @Override
    public ConfigType getConfiguration()
    {
        return config;
    }
    
    
    @Override
    public void setConfiguration(ConfigType config)
    {
        checkParentHub();
        
        if (this.config != config)
        {
            this.config = config;
            
            // get assigned event handler
            this.eventHandler = getParentHub().getEventBus().registerProducer(config.id, EventBus.MAIN_TOPIC);
            
            // set default security handler
            this.securityHandler = new ModuleSecurity(this, "all", false);
        }
    }
    
    
    /*
     * Default implementation is to re-init and restart the module when config was changed
     */
    @Override
    public synchronized void updateConfig(ConfigType config) throws SensorHubException
    {
        boolean wasStarted = isStarted();
        
        if (wasStarted)
            requestStop();
        
        try
        {
            setConfiguration(config);
            eventHandler.publishEvent(new ModuleEvent(this, ModuleEvent.Type.CONFIG_CHANGED));
        }
        catch (Exception e)
        {
            reportError(CANNOT_UPDATE_MSG, e);
        }
        
        // force re-init
        requestInit(true);
        
        // also restart if it was started
        if (wasStarted)
            requestStart();
    }
    
    
    @Override
    public boolean isInitialized()
    {
        return state.ordinal() >= ModuleState.INITIALIZED.ordinal();
    }

    
    @Override
    public boolean isStarted()
    {
        return state == ModuleState.STARTED;
    }
    
    
    @Override
    public ModuleState getCurrentState()
    {
        return state;
    }
    
    
    /**
     * Sets the module state and sends the appropriate event if it has changed
     * @param newState
     */
    protected void setState(ModuleState newState)
    {
        synchronized (stateLock)
        {
            if (newState != state)
            {
                this.state = newState;
                stateLock.notifyAll();
                getLogger().info("Module " + newState);
                
                if (eventHandler != null)
                {
                    ModuleEvent event = new ModuleEvent(this, newState);
                    eventHandler.publishEvent(event);
                }
                
                // process delayed start request
                try
                {
                    if (startRequested && (newState == ModuleState.INITIALIZED || newState == ModuleState.STOPPED))
                        requestStart();
                }
                catch (SensorHubException e)
                {
                    getLogger().error("Error during delayed start", e);
                }
            }
        }
    }
    
    
    @Override
    public boolean waitForState(ModuleState state, long timeout)
    {
        synchronized (stateLock)
        {
            try
            {
                long stopWait = System.currentTimeMillis() + timeout;
                
                // special case if we wait for restart
                boolean waitForRestart = false;
                if (this.state.ordinal() > ModuleState.STARTED.ordinal() &&
                    (state == ModuleState.STARTED || state == ModuleState.STARTING))
                    waitForRestart = true;
                    
                while (this.state.ordinal() < state.ordinal() || (waitForRestart && this.state != state) )
                {
                    //Throwable error = getCurrentError();
                    //if (error != null)
                    //    return false;
                    
                    if (timeout > 0)
                    {
                        long waitTime = stopWait - System.currentTimeMillis();
                        if (waitTime <= 0)
                            return false;
                        stateLock.wait(waitTime);
                    }
                    else
                        stateLock.wait();
                }
            }
            catch (InterruptedException e)
            {
                return false;
            }
            
            return true;
        }
    }
    
    
    @Override
    public Throwable getCurrentError()
    {
        return lastError;
    }
    
    
    /**
     * Sets the module error state and sends corresponding event
     * @param msg
     * @param error
     * false to log only at debug level
     */
    public void reportError(String msg, Throwable error)
    {
        reportError(msg, error, false);
    }
    
    
    /**
     * Sets the module error state and sends corresponding event
     * @param msg
     * @param error 
     * @param logAsDebug set to true to log the exception only at debug level,
     * false to log at error level
     */
    public void reportError(String msg, Throwable error, boolean logAsDebug)
    {
        synchronized (stateLock)
        {
            if (msg != null)
                this.lastError = new SensorHubException(msg, error);
            else
                this.lastError = error;
            
            if (!logAsDebug || getLogger().isDebugEnabled())
            {
                if (msg != null)
                    getLogger().error(msg, error);
                else
                    getLogger().error("Error", error);
            }
            else if (msg != null)
            {
                getLogger().error(msg);
            }
            
            if (eventHandler != null)
            {
                ModuleEvent event = new ModuleEvent(this, this.lastError);               
                eventHandler.publishEvent(event);
            }
        }
    }
    
    
    /**
     * Clears last error
     */
    public void clearError()
    {
        synchronized (stateLock)
        {
            this.lastError = null;
        }
    }
    
    
    @Override
    public String getStatusMessage()
    {
        return statusMsg;
    }
    
    
    /**
     * Sets the module status message and sends corresponding event
     * @param msg
     */
    public void reportStatus(String msg)
    {
        this.statusMsg = msg;
        getLogger().info(msg);
        
        if (eventHandler != null)
        {
            ModuleEvent event = new ModuleEvent(this, this.statusMsg);               
            eventHandler.publishEvent(event);
        }
    }
    
    
    /**
     * Clears the current status message
     */
    public void clearStatus()
    {
        this.statusMsg = null;
    }
    
    
    /**
     * Helper method to send and log connection/disconnection events
     * @param connected
     */
    protected void notifyConnectionStatus(boolean connected, String remoteServiceName)
    {
        if (connected)
        {
            reportStatus("Connected to " + remoteServiceName);
            eventHandler.publishEvent(new ModuleEvent(this, ModuleEvent.Type.CONNECTED));
        }
        else
        {
            reportStatus("Disconnected from " + remoteServiceName);
            eventHandler.publishEvent(new ModuleEvent(this, ModuleEvent.Type.DISCONNECTED));
        }
    }
    
    
    protected boolean canInit(boolean force) throws SensorHubException
    {
        synchronized (stateLock)
        {
            // error if config hasn't been set
            if (this.config == null)
                throw new SensorHubException("Module configuration must be set");
            
            // do nothing if we are already intializing or initialized
            if (!force && state.ordinal() >= ModuleState.INITIALIZING.ordinal())
                return false;
            
            // otherwise actually init the module
            setState(ModuleState.INITIALIZING);            
            return true;
        }
    }
    
    
    @Override
    public void requestInit(boolean force) throws SensorHubException
    {
        if (canInit(force))
        {
            try
            {
                // default implementation just calls init()
                // for backward compatibility, we must call the old init method
                init(config);
                setState(ModuleState.INITIALIZED);
            }
            catch (Exception e)
            {
                reportError(CANNOT_INIT_MSG, e);
                setState(ModuleState.LOADED);
            }
        }
    }


    @Override
    public void init() throws SensorHubException
    {        
    }
    
    
    @Override
    public void init(ConfigType config) throws SensorHubException
    {   
        setConfiguration(config);
        init();
    }


    protected boolean canStart() throws SensorHubException
    {
        synchronized (stateLock)
        {
            // error if we were never initialized
            if (state == ModuleState.LOADED)
                throw new SensorHubException("Module must first be initialized");
            
            // do nothing if we're already started or starting
            if (state == ModuleState.STARTED || state == ModuleState.STARTING)
            {
                getLogger().warn("Module was already started");
                return false;
            }
            
            // set to start later if we're still initializing or stopping
            if (state == ModuleState.INITIALIZING || state == ModuleState.STOPPING)
            {
                startRequested = true;
                return false;
            }
            
            // actually start if we're either initialized or stopped
            clearError();
            setState(ModuleState.STARTING);
            return true;
        }
    }
    
    
    @Override
    public void requestStart() throws SensorHubException
    {
        if (canStart())
        {
            try
            {
                // default implementation just calls start()
                start();
                setState(ModuleState.STARTED);
            }
            catch (Exception e)
            {
                reportError(CANNOT_START_MSG, e);
                requestStop();
            }
        }
    }
    
    
    protected boolean canStop() throws SensorHubException
    {
        synchronized (stateLock)
        {
            // do nothing if we're already stopping
            if (state == ModuleState.STOPPING)
                return false;
                        
            // otherwise we allow stop at any time
            // modules have to handle that properly
            setState(ModuleState.STOPPING);
            startRequested = false;
            return true;
        }
    }
    
    
    @Override
    public void requestStop() throws SensorHubException
    {
        ModuleState oldState = this.state;
        
        if (canStop())
        {
            try
            {
                // default implementation just calls stop()
                stop();
                clearStatus();
                
                // make sure we reset to LOADED if we didn't initialize correctly
                if (oldState == ModuleState.INITIALIZING)
                    setState(ModuleState.LOADED);
                else
                    setState(ModuleState.STOPPED);
            }
            catch (Exception e)
            {
                reportError(CANNOT_STOP_MSG, e);
            }
        }
    }


    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        // does nothing in the default implementation        
    }


    @Override
    public void loadState(IModuleStateManager loader) throws SensorHubException
    {
        // does nothing in the default implementation
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        if (eventHandler != null)
            eventHandler.publishEvent(new ModuleEvent(this, ModuleEvent.Type.DELETED));
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        checkParentHub();        
        synchronized (stateLock)
        {
            eventHandler.registerListener(listener);
            
            // notify current state synchronously while we're locked
            // so the listener can't miss it
            if (this.state != ModuleState.LOADED)
                listener.handleEvent(new ModuleEvent(this, this.state));
        }
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        checkParentHub();
        eventHandler.unregisterListener(listener);
    }
    
    
    public Logger getLogger()
    {
        checkParentHub();
        if (logger == null)
        {
            // first create logger object
            String localID = getLocalID();
            String loggerId = Integer.toHexString(localID.hashCode());
            loggerId.replaceAll("-", ""); // remove minus sign if any
            logger = LoggerFactory.getLogger(this.getClass().getCanonicalName() + ":" + loggerId);
        
            // also configure logger to append to log file in module folder
            File moduleDataFolder = getParentHub().getModuleRegistry().getModuleDataFolder(localID);
            if (moduleDataFolder != null && logger instanceof ch.qos.logback.classic.Logger)
            {
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                ch.qos.logback.classic.Logger logback = (ch.qos.logback.classic.Logger)logger;                
                
                PatternLayoutEncoder ple = new PatternLayoutEncoder();
                ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %marker - %msg%n");
                ple.setContext(lc);
                ple.start();
                
                File logFile = new File(moduleDataFolder, "log.txt");
                FileAppender<ILoggingEvent> fa = new FileAppender<>();
                fa.setFile(logFile.getAbsolutePath());
                fa.setEncoder(ple);
                fa.setAppend(true);
                fa.setContext(lc);
                fa.start(); 
                
                logback.addAppender(fa);
                logback.setLevel(Level.DEBUG);
                logback.setAdditive(true);
                
                // write initial messages on startup
                new PrintWriter(fa.getOutputStream()).println();
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "", null, null));
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "*************************************************", null, null));
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "SensorHub Restarted", null, null));
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "Starting log for " + MsgUtils.moduleString(this), null, null));
            }
        }
        
        return logger;
    }
    
    
    @Override
    public String toString()
    {
        return MsgUtils.moduleString(this);
    }
}
