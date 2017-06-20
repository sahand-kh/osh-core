/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sps;

import static org.junit.Assert.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.service.sps.SPSConnectorConfig;
import org.sensorhub.impl.service.sps.SPSService;
import org.sensorhub.impl.service.sps.SensorConnectorConfig;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorControl1;
import org.sensorhub.test.sensor.FakeSensorControl2;
import org.sensorhub.test.sensor.FakeSensorData;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockString;
import org.vast.data.TextEncodingImpl;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSException;
import org.vast.ows.OWSResponse;
import org.vast.ows.OWSUtils;
import org.vast.ows.sps.ConnectTaskingRequest;
import org.vast.ows.sps.DescribeTaskingRequest;
import org.vast.ows.sps.DescribeTaskingResponse;
import org.vast.ows.sps.InsertSensorRequest;
import org.vast.ows.sps.InsertTaskingTemplateRequest;
import org.vast.ows.sps.InsertTaskingTemplateResponse;
import org.vast.ows.sps.SPSOfferingCapabilities;
import org.vast.ows.sps.SPSServiceCapabilities;
import org.vast.ows.sps.SPSUtils;
import org.vast.ows.sps.SubmitRequest;
import org.vast.ows.swe.InsertSensorResponse;
import org.vast.ows.swe.SWESUtils;
import org.vast.util.DateTimeFormat;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;


public class TestSPSTService
{
    private static String SENSOR_UID = "urn:test:newtaskablesensor:0001";  
    TestSPSService spsTest;
    Throwable error = null;
    
    
    @Before
    public void setup() throws Exception
    {
        spsTest = new TestSPSService();
        spsTest.setup();
    }
    
    
    protected SPSService deployService(SPSConnectorConfig... connectorConfigs) throws Exception
    {   
        return spsTest.deployService(true, connectorConfigs);
    }
    
    
    protected SensorConnectorConfig buildSensorConnector1() throws Exception
    {
        return spsTest.buildSensorConnector1();
    }
    
    
    protected InsertSensorRequest buildInsertSensor() throws Exception
    {
        FakeSensor sensor = new FakeSensor();
        sensor.setParentHub(new SensorHub());
        SensorConfig config = new SensorConfig();
        config.id = "REG_SENSOR";
        config.name = "Auto-Registered Sensor";
        sensor.init(config);
        sensor.setSensorUID(SENSOR_UID);
        sensor.setDataInterfaces(new FakeSensorData(sensor, "output1", 10, 1.0, 0));
        sensor.setControlInterfaces(new FakeSensorControl1(sensor), new FakeSensorControl2(sensor));
        
        // build insert sensor request
        InsertSensorRequest req = new InsertSensorRequest();
        req.setPostServer(TestSPSService.HTTP_ENDPOINT);
        req.setVersion("2.0");        
        req.setProcedureDescription(sensor.getCurrentDescription());
        req.setProcedureDescriptionFormat(SWESUtils.DEFAULT_PROCEDURE_FORMAT);
        
        return req;
    }
    
    
    protected InsertTaskingTemplateRequest buildInsertTaskingTemplate(DataComponent param, DataEncoding encoding, InsertSensorResponse resp) throws Exception
    {
        InsertTaskingTemplateRequest req = new InsertTaskingTemplateRequest();
        req.setPostServer(TestSPSService.HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.setProcedureID(resp.getAssignedProcedureId());
        req.setTaskingParameters(param);
        req.setEncoding(encoding);
        return req;
    }
    
    
    protected ConnectTaskingRequest buildConnect(String sessionID) throws Exception
    {
        ConnectTaskingRequest req = new ConnectTaskingRequest();
        req.setGetServer(TestSPSService.WS_ENDPOINT);
        req.setVersion("2.0");
        req.setSessionID(sessionID);
        return req;
    }    
    
    
    protected SPSOfferingCapabilities getCapabilities(int offeringIndex) throws Exception
    {
        OWSUtils utils = new OWSUtils();
        
        // check capabilities has one more offering
        GetCapabilitiesRequest getCap = new GetCapabilitiesRequest();
        getCap.setService(SPSUtils.SPS);
        getCap.setVersion("2.0");
        getCap.setGetServer(TestSPSService.HTTP_ENDPOINT);
        SPSServiceCapabilities caps = (SPSServiceCapabilities)utils.sendRequest(getCap, false);
        //utils.writeXMLResponse(System.out, caps);
        assertEquals("No offering added", offeringIndex+1, caps.getLayers().size());
        
        return (SPSOfferingCapabilities)caps.getLayers().get(offeringIndex);
    }    
    
    
    @Test
    public void testSetupService() throws Exception
    {
        deployService(buildSensorConnector1());
    }
    
    
    @Test
    public void testInsertSensor() throws Exception
    {
        deployService(new SPSConnectorConfig[0]);
        OWSUtils utils = new OWSUtils();
        InsertSensorRequest req = buildInsertSensor();
        
        try
        {
            utils.writeXMLQuery(System.out, req);
            OWSResponse resp = utils.sendRequest(req, false);
            utils.writeXMLResponse(System.out, resp);
        }
        catch (OWSException e)
        {
            utils.writeXMLException(System.out, "SOS", "2.0", e);
            throw e;
        }
        
        // check new offering has correct properties
        SPSOfferingCapabilities newOffering = getCapabilities(0);
        String procUID = req.getProcedureDescription().getUniqueIdentifier();
        assertEquals(procUID, newOffering.getProcedures().iterator().next());
    }
    
    
    @Test
    public void testInsertTaskingTemplate() throws Exception
    {
        deployService(buildSensorConnector1());
        OWSUtils utils = new OWSUtils();
        
        // first register sensor
        InsertSensorRequest req = buildInsertSensor();
        InsertSensorResponse resp = (InsertSensorResponse)utils.sendRequest(req, false);
        
        // send template for 1st control input
        DataEncoding encoding = new TextEncodingImpl();
        DataComponent controlInput = (DataComponent)req.getProcedureDescription().getParameterList().get(0);
        utils.sendRequest(buildInsertTaskingTemplate(controlInput, encoding, resp), false);
        
        // check describe tasking response
        DescribeTaskingRequest dtReq = spsTest.buildDescribeTasking(SENSOR_UID);
        DescribeTaskingResponse dtResp = (DescribeTaskingResponse)utils.sendRequest(dtReq, false);
        assertTrue("Top level element should be a DataRecord", dtResp.getTaskingParameters() instanceof DataRecord);
        assertEquals("Wrong number of tasking parameters", 2, dtResp.getTaskingParameters().getComponentCount());
        
        // send template for 2nd control i,put
        controlInput = (DataComponent)req.getProcedureDescription().getParameterList().get(1);
        utils.sendRequest(buildInsertTaskingTemplate(controlInput, encoding, resp), false);
        
        // check describe tasking response again
        dtResp = (DescribeTaskingResponse)utils.sendRequest(dtReq, false);
        assertTrue("Top level element should be a DataChoice", dtResp.getTaskingParameters() instanceof DataChoice);
        assertEquals("Wrong number of tasking parameters", 2, dtResp.getTaskingParameters().getComponentCount());
    }
    
    
    protected Future<String[]> sendConnectTasking(String url) throws Exception
    {
        class MyWsHandler extends WebSocketAdapter implements Future<String[]>
        {
            ArrayList<String> records = new ArrayList<String>();
            
            public synchronized void onWebSocketBinary(byte payload[], int offset, int len)
            {
                String rec = new String(payload, offset, len);
                System.out.print(rec);
                records.add(rec);
                this.notifyAll();
            }

            public boolean cancel(boolean mayInterruptIfRunning)
            {
                return false;
            }

            public String[] get() throws InterruptedException, ExecutionException
            {
                return null;
            }

            public synchronized String[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
                if (records.isEmpty())
                    this.wait(timeout);
                return records.toArray(new String[0]);
            }

            public boolean isCancelled()
            {
                return false;
            }

            public boolean isDone()
            {
                return false;
            }            
        };
        
        String currentTime = new DateTimeFormat().formatIso(System.currentTimeMillis()/1000., 0);
        System.out.println("Sending WebSocket request @ " + currentTime);
        WebSocketClient client = new WebSocketClient();
        MyWsHandler wsHandler = new MyWsHandler();
        client.start();
        client.connect(wsHandler, new URI(url));
        
        return wsHandler;
    }
    
    
    @Test
    public void testConnectTasking() throws Exception
    {
        deployService(buildSensorConnector1());
        OWSUtils utils = new OWSUtils();
        
        // first register sensor
        InsertSensorRequest isReq = buildInsertSensor();
        InsertSensorResponse isResp = (InsertSensorResponse)utils.sendRequest(isReq, false);
        
        // send template for 1st control input
        DataEncoding encoding = new TextEncodingImpl();
        DataComponent controlInput = (DataComponent)isReq.getProcedureDescription().getParameterList().get(0);
        InsertTaskingTemplateRequest itReq = buildInsertTaskingTemplate(controlInput, encoding, isResp);
        InsertTaskingTemplateResponse itResp = utils.sendRequest(itReq, false);
        
        // connect tasking stream
        ConnectTaskingRequest connReq = buildConnect(itResp.getSessionID());
        Future<String[]> result = sendConnectTasking(utils.buildURLQuery(connReq));
        Thread.sleep(500); // wait for ws to be connected
        
        // get tasking msg structure
        DescribeTaskingRequest dtReq = spsTest.buildDescribeTasking(SENSOR_UID);
        DescribeTaskingResponse dtResp = (DescribeTaskingResponse)utils.sendRequest(dtReq, false);
                
        // submit command
        DataBlock dataBlock = new DataBlockMixed(new DataBlockDouble(1), new DataBlockString(1));
        dataBlock.setDoubleValue(0, 10.0);
        dataBlock.setStringValue(1, "HIGH");
        SubmitRequest subReq = spsTest.buildSubmit(SENSOR_UID, dtResp.getTaskingParameters(), dataBlock);
        spsTest.sendRequest(subReq, true);
        
        // check command was received
        try
        {
            String[] commands = result.get(1000, TimeUnit.MILLISECONDS);
            assertEquals("Wrong number of commands received", 1, commands.length);
            String[] tokens = commands[0].split(",");
            assertEquals("Wrong command value received", 10.0, Double.parseDouble(tokens[0]), 1e-15);
            assertEquals("Wrong command value received", "HIGH", tokens[1].trim());
        }
        catch (TimeoutException e)
        {
            fail("No data received before timeout");
        }
    }
    
    
    @After
    public void cleanup()
    {
        spsTest.cleanup();
    }
}
