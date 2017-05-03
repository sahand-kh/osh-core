/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.io.EOFException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.sensorhub.impl.service.swe.WebSocketOutputStream;
import org.sensorhub.impl.service.swe.WebSocketUtils;
import org.slf4j.Logger;
import org.vast.ows.OWSRequest;
import org.vast.ows.sos.GetResultRequest;


/**
 * <p>
 * Output only websocket for sending SOS live responses
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 19, 2015
 */
public class SOSWebSocketOut implements WebSocketListener, Runnable
{
    final static String WS_ERROR = "Error while sending websocket stream";
    final static String INPUT_NOT_SUPPORTED = "Incoming data not supported";
    
    Logger log;
    Session session;
    SOSServlet parentService;
    OWSRequest request;
    String userID;
    WebSocketOutputStream respOutputStream;
    Executor threadPool;
    
    
    public SOSWebSocketOut(SOSServlet parentService, OWSRequest request, String userID, Logger log)
    {
        this.parentService = parentService;
        this.request = request;
        this.userID = userID;
        this.threadPool = Executors.newSingleThreadExecutor();
        this.log = log;
        
        // enforce no XML wrapper to GetResult response
        if (request instanceof GetResultRequest)
            ((GetResultRequest) request).setXmlWrapper(false);
    }
    
    
    @Override
    public void onWebSocketConnect(Session session)
    {
        this.session = session;
        
        respOutputStream = new WebSocketOutputStream(session, 1024);
        request.setResponseStream(respOutputStream);
        
        // launch processing in separate thread
        threadPool.execute(this);
    }


    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        WebSocketUtils.logClose(statusCode, reason, log);    
        if (respOutputStream != null)
            respOutputStream.close();
        session = null;
    }


    @Override
    public void run()
    {
        try
        {
            // set user in this thread for proper auth
            parentService.securityHandler.setCurrentUser(userID);
            
            // handle request using same servlet logic as for HTTP
            parentService.handleRequest(request);
            
            log.debug("Data provider done");
            if (session != null)
                session.close(StatusCode.NORMAL, null);
        }
        catch (EOFException e)
        {
            // if connection was closed by client during processing, we end on
            // an EOFException when writing next record to respOutputStream.
            // this is a normal state so we have nothing special to do
            log.debug("Data provider exited on client abort", e);
        }
        catch (Exception e)
        {
            log.debug("Data provider exited on error", e);
            if (session != null)
                session.close(StatusCode.PROTOCOL, e.getMessage());
        }
    }
    
    
    @Override
    public void onWebSocketError(Throwable e)
    {
        log.error(WS_ERROR, e);
    }
    
    
    @Override
    public void onWebSocketBinary(byte payload[], int offset, int len)
    {
        session.close(StatusCode.BAD_DATA, INPUT_NOT_SUPPORTED);
    }


    @Override
    public void onWebSocketText(String msg)
    {
        session.close(StatusCode.BAD_DATA, INPUT_NOT_SUPPORTED);
    }
}
