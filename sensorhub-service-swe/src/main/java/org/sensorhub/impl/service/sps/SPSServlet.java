/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.swe.ITaskingCallback;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.sensorhub.impl.service.sps.Task;
import org.sensorhub.impl.service.swe.Template;
import org.sensorhub.impl.service.swe.TransactionUtils;
import org.slf4j.Logger;
import org.vast.cdm.common.DataStreamParser;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.DataBlockList;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSException;
import org.vast.ows.OWSExceptionReport;
import org.vast.ows.OWSRequest;
import org.vast.ows.server.OWSServlet;
import org.vast.ows.sps.*;
import org.vast.ows.sps.StatusReport.RequestStatus;
import org.vast.ows.sps.StatusReport.TaskStatus;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEHelper;
import org.vast.util.DateTime;
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;


/**
 * <p>
 * Extension of OWSSServlet implementing Sensor Planning Service operations
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 15, 2015
 */
@SuppressWarnings("serial")
public class SPSServlet extends OWSServlet
{
    private static final String INVALID_WS_REQ_MSG = "Invalid Websocket request: ";        
    private static final String DEFAULT_VERSION = "2.0.0";
    private static final QName EXT_WS = new QName("websocket");
    private static final String TASK_ID_PREFIX = "urn:sensorhub:sps:task:";
    private static final String FEASIBILITY_ID_PREFIX = "urn:sensorhub:sps:feas:";
    
    final transient SPSService service;
    final transient SPSServiceConfig config;
    final transient SPSSecurity securityHandler;
    final transient Logger log;
    final transient WebSocketServletFactory factory = new WebSocketServerFactory();
    final transient ReentrantReadWriteLock capabilitiesLock = new ReentrantReadWriteLock();
    final transient SPSServiceCapabilities capabilities = new SPSServiceCapabilities();
    final transient Map<String, ISPSConnector> connectors = new HashMap<String, ISPSConnector>(); // key is procedure ID
    final transient Map<String, SPSOfferingCapabilities> offeringCaps = new HashMap<String, SPSOfferingCapabilities>(); // key is procedure ID
    final transient Map<String, TaskingSession> transmitSessionsMap = new HashMap<String, TaskingSession>();
    final transient Map<String, TaskingSession> receiveSessionsMap = new HashMap<String, TaskingSession>();
        
    final transient SMLUtils smlUtils = new SMLUtils(SMLUtils.V2_0);
    final transient ITaskDB taskDB = new InMemoryTaskDB();
    //SPSNotificationSystem notifSystem;
    
    
    static class TaskingSession
    {
        String procID;
        TimeExtent timeSlot;
        DataComponent taskingParams;
        DataEncoding encoding;
    }
    
    
    public SPSServlet(SPSService service, SPSSecurity securityHandler)
    {
        super(new SPSUtils());
        this.service = service;
        this.config = service.getConfiguration();
        this.log = service.getLogger();
        this.securityHandler = securityHandler;
        generateCapabilities();       
    }
    
    
    protected void stop()
    {
        // clean all connectors
        for (ISPSConnector connector: connectors.values())
            connector.cleanup();
    }
    
    
    @Override
    public void destroy()
    {
        stop();
    }
    
    
    /**
     * Generates the SPSServiceCapabilities object with info obtained from connector
     */
    protected void generateCapabilities()
    {
        connectors.clear();
        offeringCaps.clear();
                
        // get main capabilities info from config
        CapabilitiesInfo serviceInfo = config.ogcCapabilitiesInfo;
        capabilities.getIdentification().setTitle(serviceInfo.title);
        capabilities.getIdentification().setDescription(serviceInfo.description);
        capabilities.setFees(serviceInfo.fees);
        capabilities.setAccessConstraints(serviceInfo.accessConstraints);
        capabilities.setServiceProvider(serviceInfo.serviceProvider);
        
        // supported operations
        capabilities.getGetServers().put("GetCapabilities", config.endPoint);
        capabilities.getGetServers().put("DescribeSensor", config.endPoint);
        capabilities.getPostServers().putAll(capabilities.getGetServers());
        capabilities.getPostServers().put("Submit", config.endPoint);
        capabilities.getPostServers().put("DirectTasking", config.endPoint);
        capabilities.getGetServers().put("ConnectTasking", config.endPoint);
        
        if (config.enableTransactional)
        {
            //capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_SENSOR_INSERTION);
            //capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_SENSOR_DELETION);/
            capabilities.getPostServers().put("InsertSensor", config.endPoint);
            capabilities.getPostServers().put("DeleteSensor", config.endPoint);
            capabilities.getPostServers().put("InsertTaskingTemplate", config.endPoint);
        }
        
        // generate profile list
        /*capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_RETRIEVAL);
        if (config.enableTransactional)
        {
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_INSERTION);
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_OBS_INSERTION);
        }*/
        
        // process each provider config
        if (config.connectors != null)
        {
            for (SPSConnectorConfig connectorConf: config.connectors)
            {
                try
                {
                    // instantiate provider factories and map them to offering URIs
                    ISPSConnector connector = connectorConf.getConnector(this);
                    connectors.put(connector.getProcedureID(), connector);
                    if (connector.isEnabled())
                        showConnectorCaps(connector);
                }
                catch (Exception e)
                {
                    log.error("Error while initializing connector " + connectorConf.offeringID, e);
                }
            }
        }
    }
    
    
    protected SPSOfferingCapabilities generateCapabilities(ISPSConnector connector) throws IOException
    {
        try
        {
            SPSOfferingCapabilities caps = connector.generateCapabilities();            
            return caps;
        }
        catch (SensorHubException e)
        {
            throw new IOException("Cannot generate capabilities", e);
        }
    }
    
    
    protected synchronized void showConnectorCaps(ISPSConnector connector)
    {
        SPSConnectorConfig config = connector.getConfig();
        
        try
        {
            capabilitiesLock.writeLock().lock();
            
            // generate offering metadata
            SPSOfferingCapabilities offCaps = generateCapabilities(connector);
            String procedureID = offCaps.getMainProcedure();
            
            // update offering if it was already advertised
            if (offeringCaps.containsKey(procedureID))
            {
                // replace old offering
                SPSOfferingCapabilities oldCaps = offeringCaps.put(procedureID, offCaps);
                capabilities.getLayers().set(capabilities.getLayers().indexOf(oldCaps), offCaps);
                
                if (log.isDebugEnabled())
                    log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" updated for procedure " + procedureID);
            }
            
            // otherwise add new offering
            else
            {
                // add to maps and layer list
                offeringCaps.put(procedureID, offCaps);
                capabilities.getLayers().add(offCaps);
                
                if (log.isDebugEnabled())
                    log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" added for procedure " + procedureID);
            }            
        }
        catch (Exception e)
        {
            log.error("Cannot generate offering " + config.offeringID, e);
        }
        finally
        {
            capabilitiesLock.writeLock().unlock();
        }
    }
    
    
    protected synchronized void hideConnectorCaps(ISPSConnector connector)
    {
        try
        {
            capabilitiesLock.writeLock().lock();
            
            // get procedure ID
            String procID = connector.getProcedureID();
            if (procID == null)
                return;
            
            // remove offering from capabilities
            SPSOfferingCapabilities offCaps = offeringCaps.remove(procID);
            capabilities.getLayers().remove(offCaps);
            
            if (log.isDebugEnabled())
                log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" removed for procedure " + procID);
        }
        finally
        {
            capabilitiesLock.writeLock().unlock();
        }
    }
    
    
    /*
     * Completely removes a connector and corresponding offering
     * This is called when the tasking target is deleted
     */
    protected synchronized void removeConnector(String procedureID)
    {
        String offeringID = getOfferingID(procedureID);
        
        // delete connector
        ISPSConnector connector = connectors.remove(procedureID);
        if (connector != null)
        {
            hideConnectorCaps(connector);
            connector.cleanup();
        }
        
        // delete connector config
        Iterator<SPSConnectorConfig> it = config.connectors.iterator();
        while (it.hasNext())
        {
            if (offeringID.equals(it.next().offeringID))
                it.remove();
        }
    }
    
    
    @Override
    protected OWSRequest parseRequest(HttpServletRequest req, HttpServletResponse resp, boolean isXmlRequest) throws OWSException
    {
        // set current authentified user
        if (req.getRemoteUser() != null)
            securityHandler.setCurrentUser(req.getRemoteUser());
        else
            securityHandler.setCurrentUser(ISecurityManager.ANONYMOUS_USER);
        
        // reject request early if SPS not authorized at all
        securityHandler.checkPermission(securityHandler.rootPerm);
        
        try
        {
            OWSRequest owsRequest = super.parseRequest(req, resp, isXmlRequest);
            
            // detect websocket request
            if (factory.isUpgradeRequest(req, resp))
            {
                if (owsRequest instanceof ConnectTaskingRequest)
                    owsRequest.getExtensions().put(EXT_WS, true);
                else
                    throw new SPSException(INVALID_WS_REQ_MSG + owsRequest.getOperation() + " is not supported via this protocol");
            }
            
            return owsRequest;
        }
        finally
        {
            securityHandler.clearCurrentUser();
        }
    }
    
    
    /*
     * Overriden because we need additional info to parse tasking request
     */
    @Override
    protected OWSRequest parseRequest(DOMHelper dom, Element requestElt) throws OWSException
    {
        // case of tasking request, need to get tasking params for the selected procedure
        if (isTaskingRequest(requestElt))
        {
            String procID = dom.getElementValue(requestElt, "procedure");
            SPSOfferingCapabilities offering = offeringCaps.get(procID);
            if (offering == null)
                throw new SPSException(SPSException.invalid_param_code, "procedure", procID);
            DescribeTaskingResponse paramDesc = offering.getParametersDescription();
            
            // use full tasking params or updatable subset
            DataComponent taskingParams;
            if (requestElt.getLocalName().equals("Update"))
                taskingParams = paramDesc.getUpdatableParameters();
            else
                taskingParams = paramDesc.getTaskingParameters();
            
            return new SPSUtils().readSpsRequest(dom, requestElt, taskingParams);
        }
        else
            return super.parseRequest(dom, requestElt);
    }
    
    
    protected void acceptWebSocket(final OWSRequest owsReq, final WebSocketListener socket) throws IOException
    {
        factory.acceptWebSocket(new WebSocketCreator() {
            @Override
            public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
            {
                return socket;
            }            
        }, owsReq.getHttpRequest(), owsReq.getHttpResponse());
    }
    
    
    @Override
    protected void handleRequest(OWSRequest request) throws IOException
    {
        if (request instanceof GetCapabilitiesRequest)
            handleRequest((GetCapabilitiesRequest)request);
        else if (request instanceof DescribeSensorRequest)
            handleRequest((DescribeSensorRequest)request);
        else if (request instanceof DescribeTaskingRequest)
            handleRequest((DescribeTaskingRequest)request);
        else if (request instanceof GetStatusRequest)
            handleRequest((GetStatusRequest)request);
        else if (request instanceof GetFeasibilityRequest)
            handleRequest((GetFeasibilityRequest)request);
        else if (request instanceof SubmitRequest)
            handleRequest((SubmitRequest)request);
        else if (request instanceof UpdateRequest)
            handleRequest((UpdateRequest)request);
        else if (request instanceof CancelRequest)
            handleRequest((CancelRequest)request);
        else if (request instanceof ReserveRequest)
            handleRequest((ReserveRequest)request);
        else if (request instanceof ConfirmRequest)
            handleRequest((ConfirmRequest)request);
        else if (request instanceof DescribeResultAccessRequest)
            handleRequest((DescribeResultAccessRequest)request);
        else if (request instanceof DirectTaskingRequest)
            handleRequest((DirectTaskingRequest)request);
        else if (request instanceof ConnectTaskingRequest)
            handleRequest((ConnectTaskingRequest)request);
        
        // transactional operations
        else if (request instanceof InsertSensorRequest)
            handleRequest((InsertSensorRequest)request);
        else if (request instanceof InsertTaskingTemplateRequest)
            handleRequest((InsertTaskingTemplateRequest)request);
        
        else
            throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected boolean isTaskingRequest(Element requestElt)
    {
        String localName = requestElt.getLocalName();
        
        if (localName.equals("GetFeasibility"))
            return true;
        else if (localName.equals("Submit"))
            return true;
        else if (localName.equals("Update"))
            return true;
        else if (localName.equals("Reserve"))
            return true;
        
        return false;
    }
    
    
    protected void handleRequest(GetCapabilitiesRequest request) throws IOException
    {
        /*// check that version 2.0.0 is supported by client
        if (!request.getAcceptedVersions().isEmpty())
        {
            if (!request.getAcceptedVersions().contains(DEFAULT_VERSION))
                throw new SOSException(SOSException.version_nego_failed_code, "AcceptVersions", null,
                        "Only version " + DEFAULT_VERSION + " is supported by this server");
        }
        
        // set selected version
        request.setVersion(DEFAULT_VERSION);*/
        
        // security check
        securityHandler.checkPermission(securityHandler.sps_read_caps);
        
        // update operation URLs
        try
        {
            capabilitiesLock.writeLock().lock();
        
            String endpointUrl = request.getHttpRequest().getRequestURL().toString();
            for (Entry<String, String> op: capabilities.getGetServers().entrySet())
                capabilities.getGetServers().put(op.getKey(), endpointUrl);
            for (Entry<String, String> op: capabilities.getPostServers().entrySet())
                capabilities.getPostServers().put(op.getKey(), endpointUrl);
        }
        finally
        {            
            capabilitiesLock.writeLock().unlock();
        }
            
        try
        {
            capabilitiesLock.readLock().lock();
            sendResponse(request, capabilities);
        }
        finally
        {            
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected void handleRequest(DescribeSensorRequest request) throws IOException
    {
        String procID = request.getProcedureID();
        
        OWSExceptionReport report = new OWSExceptionReport();
        ISPSConnector connector = getConnector(procID);
        checkQueryProcedureFormat(procID, request.getFormat(), report);
        report.process();
        
        // security check
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_read_sensor);
        
        // serialize and send SensorML description
        try
        {
            OutputStream os = new BufferedOutputStream(request.getResponseStream());
            smlUtils.writeProcess(os, connector.generateSensorMLDescription(Double.NaN), true);
        }
        catch (SensorHubException e)
        {
            throw new IOException("Cannot generate SensorML document", e);
        }
        catch (IOException e)
        {
            throw new IOException(SEND_RESPONSE_ERROR_MSG, e);
        }
    }
    
    
    protected void handleRequest(DescribeTaskingRequest request) throws IOException
    {
        String procID = request.getProcedureID();
        SPSOfferingCapabilities offering = offeringCaps.get(procID);
        
        if (offering == null)
            throw new SPSException(SPSException.invalid_param_code, "procedure", procID);
        
        // security check
        securityHandler.checkPermission(offering.getIdentifier(), securityHandler.sps_read_params);
        
        sendResponse(request, offering.getParametersDescription());
    }
    
    
    protected void handleRequest(GetStatusRequest request) throws IOException
    {
        ITask task = findTask(request.getTaskID());
        StatusReport status = task.getStatusReport();
        
        // security check
        String procID = task.getRequest().getProcedureID();
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_read_task);
        
        GetStatusResponse gsResponse = new GetStatusResponse();
        gsResponse.setVersion("2.0.0");
        gsResponse.getReportList().add(status);
        
        sendResponse(request, gsResponse);
    }
    
    
    protected GetFeasibilityResponse handleRequest(GetFeasibilityRequest request) throws IOException
    {               
        /*GetFeasibilityResponse gfResponse = new GetFeasibilityResponse();
        
        // create task in DB
        ITask newTask = taskDB.createNewTask(request);
        String studyId = newTask.getID();
        
        // launch feasibility study
        //FeasibilityResult result = doFeasibilityStudy(request);
        String sensorId = request.getSensorID();
        
        // create response
        GetFeasibilityResponse gfResponse = new GetFeasibilityResponse();
        gfResponse.setVersion("2.0.0");
        FeasibilityReport report = gfResponse.getReport();
        report.setTitle("Automatic Feasibility Results");
        report.setTaskID(studyId);
        report.setSensorID(sensorId);
                
        if (!isFeasible(result))
        {
            report.setRequestStatus(RequestStatus.Rejected);
        }
        else
        {
            report.setRequestStatus(RequestStatus.Accepted);
            report.setPercentCompletion(1.0f);            
        }
        
        report.touch();
        taskDB.updateTaskStatus(report);
        
        return gfResponse;*/  
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(SubmitRequest request) throws IOException
    {
        // retrieve connector instance
        String procID = request.getProcedureID();
        ISPSConnector conn = getConnector(procID);
        
        // security check
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_task_submit);
        
        // validate task parameters
        request.validate();
        
        // create task in DB
        ITask newTask = createNewTask(request);
        
        // send command through connector
        // synchronize here so no concurrent commands are sent to same sensor
        synchronized (conn)
        {
            try
            {
                DataBlockList dataBlockList = (DataBlockList)request.getParameters().getData();
                Iterator<DataBlock> it = dataBlockList.blockIterator();
                while (it.hasNext())
                    conn.sendSubmitData(newTask, it.next());
            }
            catch (SensorHubException e)
            {
                throw new IOException("Cannot send command to sensor " + procID, e);
            }
        }
        
        // add report and send response
        SubmitResponse sResponse = new SubmitResponse();
        sResponse.setVersion("2.0");
        StatusReport report = newTask.getStatusReport();
        report.setRequestStatus(RequestStatus.Accepted);
        report.setTaskStatus(TaskStatus.Completed);
        report.touch();
        taskDB.updateTaskStatus(report);
        
        // send response
        sResponse.setReport(report);
        sendResponse(request, sResponse);
    }
    
    
    protected synchronized void handleRequest(DirectTaskingRequest request) throws IOException
    {
        // retrieve connector instance
        String procID = request.getProcedureID();
        
        // security check
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_task_direct);
        
        // retrieve tasking parameters
        SPSOfferingCapabilities offering = offeringCaps.get(procID);
        if (offering == null)
            throw new SPSException(SPSException.invalid_param_code, "procedure", procID);
        DataComponent taskingParams = offering.getParametersDescription().getTaskingParameters();
        
        // for now we don't support reserving a session for a specific time range
        if (!request.getTimeSlot().isBaseAtNow())
            throw new SPSException(SPSException.invalid_param_code, "timeSlot", null, "Scheduling direct tasking session is not supported yet");
        
        // fail if a session already exists for this procedure
        for (TaskingSession session: receiveSessionsMap.values())
        {
            if (procID.equals(session.procID))
                throw new SPSException("A direct tasking session is already started");
        }
        
        // create task in DB
        ITask newTask = createNewTask(request);
        final String taskID = newTask.getID();
        
        // create session
        TaskingSession newSession = new TaskingSession();
        newSession.procID = procID;
        newSession.timeSlot = request.getTimeSlot();
        newSession.taskingParams = taskingParams;
        newSession.encoding = request.getEncoding();
        receiveSessionsMap.put(taskID, newSession);
        
        // add report and send response
        DirectTaskingResponse sResponse = new DirectTaskingResponse();
        sResponse.setVersion("2.0");
        ITask task = findTask(taskID);
        task.getStatusReport().setRequestStatus(RequestStatus.Accepted);
        task.getStatusReport().setTaskStatus(TaskStatus.Reserved);
        task.getStatusReport().touch();
        sResponse.setReport(task.getStatusReport());
        
        sendResponse(request, sResponse);
    }
    
    
    // we dispatch request depending if it's for receiving or transmitting commands
    protected synchronized void handleRequest(ConnectTaskingRequest request) throws IOException
    {
        String sessionID = request.getSessionID();
        TaskingSession session;
        
        // check if receive sessions
        session = receiveSessionsMap.get(sessionID);
        if (session != null)
        {
            startReceiveTaskingStream(session, request);
            return;
        }
        
        // check if transmit sessions
        session = transmitSessionsMap.get(sessionID);
        if (session != null)
        {
            startTransmitTaskingStream(session, request);
            return;
        }
        
        throw new SPSException(SPSException.invalid_param_code, "session", sessionID, "Invalid session ID");
    }
    
    
    protected void startReceiveTaskingStream(TaskingSession session, ConnectTaskingRequest request) throws IOException
    {
        try
        {
            // retrieve connector
            String procID = session.procID;
            ISPSConnector conn = getConnector(procID);
            
            // security check
            securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_task_direct);
                        
            // prepare parser for tasking stream
            String taskID = request.getSessionID();
            ITask task = taskDB.getTask(taskID);
            DataStreamParser parser = SWEHelper.createDataParser(session.encoding);
            parser.setDataComponents(session.taskingParams);
            
            // if it's a websocket request
            if (isWebSocketRequest(request))
            {
                SPSWebSocketIn ws = new SPSWebSocketIn(this, task, parser, conn);
                acceptWebSocket(request, ws);
            }
            
            // else it's persistent HTTP
            else
            {
                //final AsyncContext aCtx = request.getHttpRequest().startAsync(request.getHttpRequest(), request.getHttpResponse());
                // TODO add support for HTTP persistent stream
                throw new IOException("Only WebSocket ConnectTasking requests are supported");
            }
        }
        finally
        {
            
        }
    }
    

    protected void handleRequest(UpdateRequest request) throws IOException
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(CancelRequest request) throws IOException
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    

    protected void handleRequest(ReserveRequest request) throws IOException
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(ConfirmRequest request) throws IOException
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(DescribeResultAccessRequest request) throws IOException
    {
        /*ITask task = findTask(request.getTaskID());
        
        DescribeResultAccessResponse resp = new DescribeResultAccessResponse();     
        StatusReport status = task.getStatusReport();
        
        // TODO DescribeResultAccess
        
        return resp;*/
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected ITask findTask(String taskID) throws SPSException
    {
        ITask task = taskDB.getTask(taskID);
        
        if (task == null)
            throw new SPSException(SPSException.invalid_param_code, "task", taskID);
        
        return task;
    }
    
    
    protected Task createNewTask(GetFeasibilityRequest request)
    {
        Task newTask = new Task();
        newTask.setStatusReport(new FeasibilityReport());
        newTask.setRequest(request);
        String taskID = FEASIBILITY_ID_PREFIX + UUID.randomUUID().toString();
        
        // initial status
        newTask.getStatusReport().setTaskID(taskID);
        newTask.getStatusReport().setTitle("Feasibility Study Report");
        newTask.getStatusReport().setSensorID(request.getProcedureID());
        newTask.getStatusReport().setRequestStatus(RequestStatus.Pending);
        
        // creation time
        newTask.setCreationTime(new DateTime());
        
        taskDB.addTask(newTask);        
        return newTask;
    }
    
    
    protected Task createNewTask(SubmitRequest request)
    {
        Task newTask = new Task();
        newTask.setRequest(request);
        String taskID = TASK_ID_PREFIX + UUID.randomUUID().toString();
        
        // initial status
        newTask.getStatusReport().setTaskID(taskID);
        newTask.getStatusReport().setTitle("Tasking Request Report");
        newTask.getStatusReport().setSensorID(request.getProcedureID());
        newTask.getStatusReport().setRequestStatus(RequestStatus.Pending);
        
        // creation time
        newTask.setCreationTime(new DateTime());
        
        taskDB.addTask(newTask);        
        return newTask;
    }
    
    
    protected Task createNewTask(DirectTaskingRequest request)
    {
        Task newTask = new Task();
        String taskID = TASK_ID_PREFIX + UUID.randomUUID().toString();
        
        // initial status
        newTask.getStatusReport().setTaskID(taskID);
        newTask.getStatusReport().setTitle("Direct Tasking Report");
        newTask.getStatusReport().setSensorID(request.getProcedureID());
        newTask.getStatusReport().setRequestStatus(RequestStatus.Pending);
        
        // creation time
        newTask.setCreationTime(new DateTime());
        
        taskDB.addTask(newTask);        
        return newTask;
    }
    
    
    protected void cleanupSession(String sessionID)
    {
        
    }
    
    
    //////////////////////////////
    // Transactional Operations //
    //////////////////////////////    
    
    protected void handleRequest(InsertSensorRequest request) throws IOException
    {
        checkTransactionalSupport(request);
        
        // security check
        securityHandler.checkPermission(securityHandler.sps_insert_sensor);
        
        // check query parameters
        OWSExceptionReport report = new OWSExceptionReport();
        TransactionUtils.checkSensorML(request.getProcedureDescription(), report);
        report.process();
        
        try
        {
            // get sensor UID
            String sensorUID = request.getProcedureDescription().getUniqueIdentifier();
            log.info("Registering new sensor " + sensorUID);
            
            // offering name is derived from sensor UID
            String offeringID = sensorUID + "-sps";
            
            ///////////////////////////////////////////////////////////////////////////////////////
            // we configure things step by step so we can fix config if it was partially altered //
            ///////////////////////////////////////////////////////////////////////////////////////
            HashSet<ModuleConfig> configSaveList = new HashSet<>();
            ModuleRegistry moduleReg = service.getParentHub().getModuleRegistry();
            
            // create new virtual sensor module if needed            
            IModule<?> sensorModule = moduleReg.getLoadedModuleById(sensorUID);
            if (sensorModule == null)
            {
                sensorModule = TransactionUtils.createSensorModule(getParentHub(), sensorUID, request.getProcedureDescription());
                configSaveList.add(sensorModule.getConfiguration());
            }            
            // else simply update description
            else
                TransactionUtils.updateSensorDescription(sensorModule, request.getProcedureDescription());
            
            // add new connector if needed
            ISPSConnector connector = connectors.get(sensorUID);
            if (connector == null)
            {
                // generate new provider config
                SensorConnectorConfig connectorConfig = new SensorConnectorConfig();
                connectorConfig.enabled = true;
                connectorConfig.sensorID = sensorUID;
                connectorConfig.offeringID = offeringID;
                config.connectors.replaceOrAdd(connectorConfig);
                
                // instantiate and register connector
                connector = connectorConfig.getConnector(this);
                connectors.put(sensorUID, connector);
                
                // add new permissions for this offering
                securityHandler.addOfferingPermissions(offeringID);
                
                configSaveList.add(config);
            }
            
            // save module configs so we don't loose anything on restart
            moduleReg.saveConfiguration(configSaveList.toArray(new ModuleConfig[0]));
            
            // update capabilities
            showConnectorCaps(connector);
            
            // build and send response
            InsertSensorResponse resp = new InsertSensorResponse();
            resp.setAssignedOffering(offeringID);
            resp.setAssignedProcedureId(sensorUID);
            sendResponse(request, resp);
        }
        catch (SensorHubException e)
        {
            throw new IOException("", e);
        }
    }
    
    
    protected void handleRequest(InsertTaskingTemplateRequest request) throws IOException
    {
        try
        {
            checkTransactionalSupport(request);
            
            // retrieve connector instance
            String procID = request.getProcedureID();
            ISPSTransactionalConnector connector = getTransactionalConnector(procID);
                        
            // security check
            securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_connect_tasking);
                        
            // generate template ID
            String templateID = connector.newTaskingTemplate(request.getTaskingParameters(), request.getEncoding());
            
            // only continue if session was not already created
            if (!transmitSessionsMap.containsKey(templateID))
            {                
                // create session
                TaskingSession newSession = new TaskingSession();
                newSession.procID = procID;
                newSession.taskingParams = request.getTaskingParameters();
                newSession.encoding = request.getEncoding();
                transmitSessionsMap.put(templateID, newSession);
                                
                // update offering capabilities
                showConnectorCaps(connector);
            }
            
            // build and send response
            InsertTaskingTemplateResponse resp = new InsertTaskingTemplateResponse();
            resp.setSessionID(templateID);
            sendResponse(request, resp);
        }
        catch (SensorHubException e)
        {
            throw new SPSException(SPSException.invalid_param_code, "TaskingTemplate", e);
        }
    }
    
    
    protected void startTransmitTaskingStream(TaskingSession session, ConnectTaskingRequest request) throws IOException
    {
        checkTransactionalSupport(request);
        
        // retrieve connector
        String procID = session.procID;
        ISPSTransactionalConnector conn = getTransactionalConnector(procID);
        
        // security check
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_connect_tasking);
                    
        // retrieve selected template
        String sessionID;
        Template template;
        try
        {
            sessionID = request.getSessionID();
            template = conn.getTemplate(sessionID);
        }
        catch (SensorHubException e)
        {
            throw new IllegalStateException("Invalid session", e);
        }
        
        // prepare writer
        final DataStreamWriter writer = SWEHelper.createDataWriter(template.encoding);
        writer.setDataComponents(template.component);
        
        // handle cases of websocket and persistent HTTP
        if (isWebSocketRequest(request))
        {
            SPSWebSocketOut ws = new SPSWebSocketOut(writer, log);
            acceptWebSocket(request, ws);
            conn.registerCallback(sessionID, ws);
        }
        else
        {
            final AsyncContext aCtx = request.getHttpRequest().startAsync(request.getHttpRequest(), request.getHttpResponse());
            writer.setOutput(new BufferedOutputStream(request.getResponseStream()));
            
            conn.registerCallback(sessionID, new ITaskingCallback() {
                @Override
                public void onCommandReceived(DataBlock cmdData)
                {
                    try
                    {
                        writer.write(cmdData);
                        writer.flush();
                    }
                    catch (IOException e)
                    {
                        log.error("Cannot send command message to connected device. Closing stream.", e);
                        onClose();
                    }
                }

                @Override
                public void onClose()
                {
                    try
                    {
                        writer.close();
                        aCtx.complete();
                    }
                    catch (IOException e)
                    {
                        log.trace("Cannot close tasking stream", e);
                    }
                }
            });
        }
    }
    
    
    protected ISPSConnector getConnector(String procedureID) throws OWSException
    {
        try
        {
            capabilitiesLock.readLock().lock();
            ISPSConnector connector = connectors.get(procedureID);
            
            if (connector == null)
                throw new SPSException(SPSException.invalid_param_code, "procedure", procedureID);
            
            return connector;
        }
        finally
        {            
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected ISPSTransactionalConnector getTransactionalConnector(String procedureID) throws OWSException
    {
        ISPSConnector connector = getConnector(procedureID);
        
        if (!(connector instanceof ISPSTransactionalConnector))
           throw new SPSException(SPSException.invalid_param_code, "procedure", procedureID, "Transactional operations are not supported for procedure " + procedureID);
                        
        return (ISPSTransactionalConnector)connector;
    }
    
    
    protected final String getOfferingID(String procedureID)
    {
        return offeringCaps.get(procedureID).getIdentifier();
    }
    
    
    protected void checkQueryProcedureFormat(String procedureID, String format, OWSExceptionReport report) throws OWSException
    {
        // ok if default format can be used
        if (format == null)
            return;
        
        SPSOfferingCapabilities offering = this.offeringCaps.get(procedureID);
        if (!offering.getProcedureFormats().contains(format))
            report.add(new SPSException(SPSException.invalid_param_code, "procedureDescriptionFormat", format, "Procedure description format " + format + " is not available for procedure " + procedureID));
    }
    
    
    protected void checkTransactionalSupport(OWSRequest request) throws OWSException
    {
        if (!config.enableTransactional)
            throw new SPSException(SPSException.invalid_param_code, "request", request.getOperation(), request.getOperation() + " operation is not supported on this endpoint"); 
    }
    
    
    /*
     * Check if request is through websocket protocol
     */
    protected boolean isWebSocketRequest(OWSRequest request)
    {
        return request.getExtensions().containsKey(EXT_WS);
    }


    @Override
    protected String getServiceType()
    {
        return SPSUtils.SPS;
    }


    @Override
    protected String getDefaultVersion()
    {
        return DEFAULT_VERSION;
    }
    
    
    protected SensorHub getParentHub()
    {
        return service.getParentHub();
    }
    
    
    protected Logger getLogger()
    {
        return log;
    }
}
