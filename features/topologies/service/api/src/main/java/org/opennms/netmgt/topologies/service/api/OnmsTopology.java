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

package org.opennms.netmgt.topologies.service.api;

import java.util.HashSet;
import java.util.Set;

public class OnmsTopology {

    private Set<OnmsTopologyVertex> m_vertices;
    private Set<OnmsTopologyShared> m_edges;
    
    public OnmsTopology() {
        m_vertices = new HashSet<OnmsTopologyVertex>();
        m_edges = new HashSet<OnmsTopologyShared>();
    }

    public OnmsTopologyVertex getVertex(String id) {
        return m_vertices.stream().filter(vertex -> id.equals(vertex.getId())).findAny().orElse(null);
    }

    public OnmsTopologyShared getEdge(String id) {
        return m_edges.stream().filter(edge -> id.equals(edge.getId())).findAny().orElse(null);
    }

    public Set<OnmsTopologyVertex> getVertices() {
        return m_vertices;
    }

    public void setVertices(Set<OnmsTopologyVertex> vertices) {
        m_vertices = vertices;
    }

    public Set<OnmsTopologyShared> getEdges() {
        return m_edges;
    }

    public void setConnections(Set<OnmsTopologyShared> edges) {
        m_edges = edges;
    }    

    public boolean hasVertex(String id) {
        return (getVertex(id) != null);
    }
    
    public boolean hasEdge(String id) {
        return (getEdge(id) != null);
    }
    
    public OnmsTopology clone() {
        OnmsTopology topo = new OnmsTopology();
        topo.setVertices(new HashSet<>(m_vertices));
        topo.setConnections(new HashSet<>(getEdges()));
        return topo;
    }
}
