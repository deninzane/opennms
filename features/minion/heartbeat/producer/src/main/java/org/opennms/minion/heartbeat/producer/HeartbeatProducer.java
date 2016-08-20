/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.minion.heartbeat.producer;

import java.util.Timer;
import java.util.TimerTask;

import org.opennms.core.ipc.sink.api.MessageProducer;
import org.opennms.core.ipc.sink.api.MessageProducerFactory;
import org.opennms.minion.core.api.MinionIdentity;
import org.opennms.minion.heartbeat.common.HeartbeatModule;
import org.opennms.minion.heartbeat.common.MinionIdentityDTO;

public class HeartbeatProducer {

    private static final int PERIOD_MS = 30 * 1000;

    final Timer timer;

    public HeartbeatProducer(MinionIdentity identity, MessageProducerFactory messageProducerFactory) {
        final MinionIdentityDTO identityDTO = new MinionIdentityDTO(identity);
        final MessageProducer<MinionIdentityDTO> delegate = messageProducerFactory.getProducer(new HeartbeatModule());
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                delegate.send(identityDTO);
            }
        }, 0, PERIOD_MS);
    }

    /**
     * Used to cancel the timer when the Blueprint is destroyed.
     */
    public void cancel() {
        timer.cancel();
    }
}
