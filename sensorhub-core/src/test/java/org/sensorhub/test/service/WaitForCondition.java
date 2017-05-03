/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service;


public abstract class WaitForCondition
{
    
    public WaitForCondition(long timeout)
    {
        loop(timeout);
    }
    
    
    protected synchronized void loop(long timeout)
    {
        try
        {
            long t0 = System.currentTimeMillis();
            while (!check() && System.currentTimeMillis() < t0+timeout)
                wait(100L);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }
    
    
    public abstract boolean check();
    
}
