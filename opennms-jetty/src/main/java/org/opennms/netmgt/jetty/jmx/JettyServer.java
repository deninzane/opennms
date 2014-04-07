/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.jetty.jmx;

import org.opennms.netmgt.daemon.AbstractSpringContextJmxServiceDaemon;

/**
 * <p>JettyServer class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class JettyServer extends AbstractSpringContextJmxServiceDaemon<org.opennms.netmgt.jetty.JettyServer> implements JettyServerMBean {

    /** {@inheritDoc} */
    @Override
    protected String getLoggingPrefix() {
        return "OpenNMS.JettyServer";
    }

    /** {@inheritDoc} */
    @Override
    protected String getSpringContext() {
        return "jettyServerContext";
    }

    /** {@inheritDoc} */
    @Override
    public long getHttpsConnectionsTotal() {
        return getDaemon().getHttpsConnectionsTotal();
    }

    /** {@inheritDoc} */
    @Override
    public long getHttpsConnectionsOpen() {
        return getDaemon().getHttpsConnectionsOpen();
    }

    /** {@inheritDoc} */
    @Override
    public long getHttpsConnectionsOpenMax() {
        return getDaemon().getHttpsConnectionsOpenMax();
    }

    /** {@inheritDoc} */
    @Override
    public long getHttpConnectionsTotal() {
        return getDaemon().getHttpConnectionsTotal();
    }

    /** {@inheritDoc} */
    @Override
    public long getHttpConnectionsOpen() {
        return getDaemon().getHttpConnectionsOpen();
    }

    /** {@inheritDoc} */
    @Override
    public long getHttpConnectionsOpenMax() {
        return getDaemon().getHttpConnectionsOpenMax();
    }
    
}
