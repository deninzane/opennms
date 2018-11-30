/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.enlinkd;

import java.util.Map;
import java.util.stream.Collectors;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.enlinkd.service.api.MacPort;
import org.opennms.netmgt.enlinkd.service.api.Node;
import org.opennms.netmgt.enlinkd.service.api.NodeTopologyService;
import org.opennms.netmgt.enlinkd.service.api.ProtocolSupported;
import org.opennms.netmgt.enlinkd.service.api.Topology;
import org.opennms.netmgt.enlinkd.service.api.TopologyService;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.topologies.service.api.OnmsTopology;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyDao;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyException;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyMessage;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyPort;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyRef;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyShared;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyUpdater;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EnlinkdOnmsTopologyUpdater extends Discovery implements OnmsTopologyUpdater {

    public static OnmsTopologyVertex create(MacPort macPort) {
        return OnmsTopologyVertex.create(Topology.getId(macPort),
                                         ProtocolSupported.BRIDGE.name(),
                                         Topology.getId(macPort), 
                                         macPort.getIpMacInfo(), 
                                         null);
    }
    
    public static OnmsTopologyVertex create(Node node, ProtocolSupported protocol) {
        return OnmsTopologyVertex.create(node.getId(), 
                                         protocol.name(),
                                         node.getLabel(), 
                                         InetAddressUtils.str(node.getSnmpPrimaryIpAddr()), 
                                         node.getSysoid());
    }

    private static final Logger LOG = LoggerFactory.getLogger(EnlinkdOnmsTopologyUpdater.class);

    private final OnmsTopologyDao m_topologyDao;
    private final NodeTopologyService m_nodeTopologyService;
    private final TopologyService m_topologyService;

    private OnmsTopology m_topology;
    private boolean m_runned = false;
    
    public EnlinkdOnmsTopologyUpdater(EventForwarder eventforwarder,
            TopologyService topologyService,
            OnmsTopologyDao topologyDao, NodeTopologyService nodeTopologyService,
            long interval, long initialsleeptime) {
        super(eventforwarder, interval, initialsleeptime);
        m_topologyDao = topologyDao;
        m_topologyService = topologyService;
        m_nodeTopologyService = nodeTopologyService;
        m_topology = new OnmsTopology();
    }            
    
    private <T extends OnmsTopologyRef>void update(T topoObject) {
        try {
            m_topologyDao.update(this, OnmsTopologyMessage.update(topoObject));
        } catch (OnmsTopologyException e) {
            LOG.error("update: {}: {} {} {}", e.getMessageStatus(), e.getId(), e.getProtocol(),e.getMessage());
        }
    }

    private <T extends OnmsTopologyRef>void delete(T topoObject) {
        try {
            m_topologyDao.update(this, OnmsTopologyMessage.delete(topoObject));
        } catch (OnmsTopologyException e) {
            LOG.error("delete: {}: {} {} {}", e.getMessageStatus(), e.getId(), e.getProtocol(),e.getMessage());
        }
    }

    private <T extends OnmsTopologyRef>void create(T topoObject) {
        try {
            m_topologyDao.update(this, OnmsTopologyMessage.create(topoObject));
        } catch (OnmsTopologyException e) {
            LOG.error("delete: {}: {} {} {}", e.getMessageStatus(), e.getId(), e.getProtocol(),e.getMessage());
        }
    }

    @Override
    public void runDiscovery() {
        LOG.debug("run: start");
        if (m_topologyService.parseUpdates()) {
            LOG.debug("run: updates");
            OnmsTopology topo = buildTopology();
            synchronized (m_topology) {
                if (m_runned) {
                    m_topology.getVertices().stream().filter(v -> !topo.hasVertex(v.getId())).forEach(v -> delete(v));
                    m_topology.getEdges().stream().filter(g -> !topo.hasEdge(g.getId())).forEach(g -> delete(g));

                    topo.getVertices().stream().filter(v -> !m_topology.hasVertex(v.getId())).forEach(v -> create(v));
                    topo.getEdges().stream().filter(g -> !m_topology.hasEdge(g.getId())).forEach(g -> create(g));
                    
                    topo.getVertices().stream().filter(v -> m_topology.hasVertex(v.getId())).forEach(v -> {
                        
                    });
                    topo.getEdges().stream().filter(g -> m_topology.hasEdge(g.getId())).forEach(g -> {
                        OnmsTopologyShared og = m_topology.getEdge(g.getId()); 
                        boolean updated = false;
                        for (OnmsTopologyPort op: og.getSources()) {
                            if (!g.hasPort(op.getId())) {
                                update(g);
                                updated=true;
                                break;
                            }
                        }
                        if (!updated) {
                            for (OnmsTopologyPort p: g.getSources()) {
                                if (!og.hasPort(p.getId())) {
                                    update(g);
                                    break;
                                }
                            }
                        }
                    });
                } else {
                    topo.getVertices().stream().forEach(v -> create(v));
                    topo.getEdges().stream().forEach(g -> create(g));
                }
                m_topology = topo;
            }
            m_runned=true;
        }
        LOG.debug("run: end");
    }

    public OnmsTopologyDao getTopologyDao() {
        return m_topologyDao;
    }

    public NodeTopologyService getNodeTopologyService() {
        return m_nodeTopologyService;
    }

    public Map<Integer, Node> getNodeMap() {
        return m_nodeTopologyService.findAll().stream().collect(Collectors.toMap(node -> node.getNodeId(), node -> node, (n1,n2) ->n1));
    }
    
    public abstract OnmsTopology buildTopology();
    
    @Override
    public OnmsTopology getTopology() {
        synchronized (m_topology) {
            return m_topology.clone();
        }
    }
    
    
            
}

