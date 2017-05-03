/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import org.sensorhub.api.common.Event;
import org.sensorhub.api.processing.ProcessingEvent.Type;


/**
 * <p>
 * Simple base data structure for all events linked to processing modules
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 20, 2015
 */
public class ProcessingEvent extends Event<Type>
{
	/**
	 * Possible event types for a ProcessingEvent
	 */
    public enum Type
	{
		PROCESSING_STARTED,
		PROCESSING_PROGRESS,
		PROCESSING_ENDED,
		PARAMS_CHANGED
	};
	
	
	/**
	 * ID of module that generated the event
	 */
	protected String processId;
	
	
	/**
	 * Type of sensor event
	 */
	protected Type type;
	
	
	/**
	 * Sole constructor
	 * @param timeStamp unix time of event generation
     * @param processId ID of originating process
     * @param type type of event
	 */
	public ProcessingEvent(long timeStamp, String processId, Type type)
	{
	    this.processId = processId;
	    this.timeStamp = timeStamp;
	    this.type = type;
	}


    public String getProcessId()
    {
        return processId;
    }


    @Override
    public Type getType()
    {
        return type;
    }
}
