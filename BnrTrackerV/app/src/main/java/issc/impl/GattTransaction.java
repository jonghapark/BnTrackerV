// vim: et sw=4 sts=4 tabstop=4
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package issc.impl;

import issc.gatt.GattCharacteristic;
import issc.gatt.GattDescriptor;
import issc.util.TransactionQueue.Transaction;

public class GattTransaction implements Transaction {

    public GattCharacteristic chr;
    public GattDescriptor     desc;
    public byte[] value;
    public boolean isWrite;
    public long timeout;

    public GattTransaction(GattCharacteristic c, byte[] v) {
        this(c, v, true);
    }

    public GattTransaction(GattCharacteristic c, byte[] v, long t) {
        this(c, v, true, t);
    }

    public GattTransaction(GattCharacteristic c, byte[] v, boolean w) {
        this(c, v, w, Transaction.TIMEOUT_NONE);
    }

    public GattTransaction(GattCharacteristic c,
            byte[] v,
            boolean w,
            long t) {
        chr = c;
        value = v;
        isWrite = w;
        timeout = t;
    }

    public boolean isForCharacteristic() {
        return (chr != null);
    }

    public GattTransaction(GattDescriptor d, byte[] v) {
        this(d, v, true);
    }

    public GattTransaction(GattDescriptor d, byte[] v, long t) {
        this(d, v, true, t);
    }

    public GattTransaction(GattDescriptor d, byte[] v, boolean w) {
        this(d, v, w, Transaction.TIMEOUT_NONE);
    }

    public GattTransaction(GattDescriptor d,
            byte[] v,
            boolean w,
            long t) {
        desc = d;
        value = v;
        isWrite = w;
        timeout = t;
    }

    public boolean isForDescriptor() {
        return (desc != null);
    }

    @Override
    public long getTimeout() {
        return timeout;
    }
}

