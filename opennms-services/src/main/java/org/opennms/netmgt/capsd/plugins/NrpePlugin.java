//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc. All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2007 Jan 20: Add SSL support to NrpePlugin. jeffg@opennms.org
// 2005 Oct 25: Create NrpePlugin based on TcpPlugin. dj@gregor.com
//
// Original code base Copyright (C) 1999-2001 Oculan Corp. All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact:
//      OpenNMS Licensing <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.capsd.plugins;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Category;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.opennms.core.utils.ParameterMap;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.capsd.AbstractPlugin;
import org.opennms.netmgt.poller.nrpe.CheckNrpe;
import org.opennms.netmgt.poller.nrpe.NrpePacket;
import org.opennms.netmgt.utils.RelaxedX509TrustManager;

/**
 * <P>
 * This class is designed to be used by the capabilities daemon to test for the
 * existance of an TCP server on remote interfaces. The class implements the
 * Plugin interface that allows it to be used along with other plugins by the
 * daemon.
 * </P>
 * 
 * @author <A HREF="mailto:mike@opennms.org">Mike </A>
 * @author <A HREF="mailto:weave@oculan.com">Weaver </A>
 * @author <A HREF="http://www.opennsm.org">OpenNMS </A>
 * 
 * 
 */
public final class NrpePlugin extends AbstractPlugin {

    /**
     * The protocol supported by the plugin
     */
    private final static String PROTOCOL_NAME = "NRPE";

    /**
     * Default number of retries for TCP requests
     */
    private final static int DEFAULT_RETRY = 0;

    /**
     * Default timeout (in milliseconds) for TCP requests
     */
    private final static int DEFAULT_TIMEOUT = 5000; // in milliseconds
    
    /**
     * Default whether to use SSL
     */
    private final static boolean DEFAULT_USE_SSL = true;
    
    /**
     * List of cipher suites to use when talking SSL to NRPE, which uses anonymous DH
     */
    private static final String[] ADH_CIPHER_SUITES = new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"};
    
    /**
     * Whether to use SSL for this instantiation
     */
    private boolean m_useSsl = DEFAULT_USE_SSL;

    /**
     * <P>
     * Test to see if the passed host-port pair is the endpoint for a TCP
     * server. If there is a TCP server at that destination then a value of true
     * is returned from the method. Otherwise a false value is returned to the
     * caller. In order to return true the remote host must generate a banner
     * line which contains the text from the bannerMatch argument.
     * </P>
     * 
     * @param host
     *            The remote host to connect to.
     * @param port
     *            The remote port on the host.
     * @param bannerResult
     *            Banner line generated by the remote host must contain this
     *            text.
     * 
     * @return True if a connection is established with the host and the banner
     *         line contains the bannerMatch text.
     */
    private boolean isServer(InetAddress host, int port, String command, int padding, int retries, int timeout, RE regex, StringBuffer bannerResult) {
        Category log = ThreadCategory.getInstance(getClass());

        boolean isAServer = false;
        for (int attempts = 0; attempts <= retries && !isAServer; attempts++) {
            Socket socket = null;
            try {
                // create a connected socket
                //
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), timeout);
                socket = wrapSocket(socket, host.toString(), port);
                socket.setSoTimeout(timeout);
                log.debug("NrpePlugin: connected to host: " + host + " on port: " + port);
				
				NrpePacket p = new NrpePacket(NrpePacket.QUERY_PACKET, (short) 0,
						command);
				byte[] b = p.buildPacket(padding);
				OutputStream o = socket.getOutputStream();
				o.write(b);

				NrpePacket response = NrpePacket.receivePacket(socket.getInputStream(), padding);
				if (response.getResultCode() == 0) {
                    isAServer = true;
				} else if (response.getResultCode() <= 2) {
						String response_msg = response.getBuffer();
						RE r = new RE("OK|WARNING|CRITICAL");
						if (r.match(response_msg)) {
							isAServer = true;
						} else {
							log.info("received 1-2 return code, " +
									response.getResultCode() + ", with message: " + 
									response.getBuffer());
							isAServer = false;
							break;
						}
				} else {
						log.info("received 3+ return code, " +
								response.getResultCode() + ", with message: " +
								response.getBuffer());
                        isAServer = false;
						break;
                }

				/*
                // If banner matching string is null or wildcard ("*") then we
                // only need to test connectivity and we've got that!
                //
                if (regex == null) {
                    isAServer = true;
                } else {
                    // get a line reader
                    //
                    BufferedReader lineRdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Read the server's banner line ouptput and validate it
                    // against
                    // the bannerMatch parameter to determine if this interface
                    // supports the
                    // service.
                    //
                    String response = lineRdr.readLine();
                    if (regex.match(response)) {
                        if (log.isDebugEnabled())
                            log.debug("isServer: matching response=" + response);
                        isAServer = true;
                        if (bannerResult != null)
                            bannerResult.append(response);
                    } else {
                        // Got a response but it didn't match...no need to
                        // attempt retries
                        isAServer = false;
                        if (log.isDebugEnabled())
                            log.debug("isServer: NON-matching response=" + response);
                        break;
                    }
                }
                */
            } catch (ConnectException e) {
                // Connection refused!! Continue to retry.
                //
                log.debug("NrpePlugin: Connection refused to " + host.getHostAddress() + ":" + port);
                isAServer = false;
            } catch (NoRouteToHostException e) {
                // No Route to host!!!
                //
                e.fillInStackTrace();
                log.info("NrpePlugin: Could not connect to host " + host.getHostAddress() + ", no route to host", e);
                isAServer = false;
                throw new UndeclaredThrowableException(e);
            } catch (InterruptedIOException e) {
                // This is an expected exception
                //
                log.debug("NrpePlugin: did not connect to host within timeout: " + timeout + " attempt: " + attempts);
                isAServer = false;
            } catch (IOException e) {
                log.info("NrpePlugin: An expected I/O exception occured connecting to host " + host.getHostAddress() + " on port " + port, e);
                isAServer = false;
            } catch (Throwable t) {
                isAServer = false;
                log.warn("NrpePlugin: An undeclared throwable exception was caught connecting to host " + host.getHostAddress() + " on port " + port, t);
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                }
            }
        }

        //
        // return the success/failure of this
        // attempt to contact an ftp server.
        //
        return isAServer;
    }

    /**
     * Returns the name of the protocol that this plugin checks on the target
     * system for support.
     * 
     * @return The protocol name for this plugin.
     */
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    /**
     * Returns true if the protocol defined by this plugin is supported. If the
     * protocol is not supported then a false value is returned to the caller.
     * 
     * @param address
     *            The address to check for support.
     * 
     * @return True if the protocol is supported by the address.
     * 
     * @throws java.lang.UnsupportedOperationException
     *             This is always thrown by this plugin.
     */
    public boolean isProtocolSupported(InetAddress address) {
        throw new UnsupportedOperationException("Undirected TCP checking not supported");
    }

    /**
     * Returns true if the protocol defined by this plugin is supported. If the
     * protocol is not supported then a false value is returned to the caller.
     * The qualifier map passed to the method is used by the plugin to return
     * additional information by key-name. These key-value pairs can be added to
     * service events if needed.
     * 
     * @param address
     *            The address to check for support.
     * @param qualifiers
     *            The map where qualification are set by the plugin.
     * 
     * @return True if the protocol is supported by the address.
     */
    public boolean isProtocolSupported(InetAddress address, Map<String, Object> qualifiers) {
        int retries = DEFAULT_RETRY;
        int timeout = DEFAULT_TIMEOUT;
        int port = -1;
		int padding = -1;

        String banner = null;
        String match = null;
		String command = null;

        if (qualifiers != null) {
	        command = ParameterMap.getKeyedString(qualifiers, "command", NrpePacket.HELLO_COMMAND);
	        port = ParameterMap.getKeyedInteger(qualifiers, "port", CheckNrpe.DEFAULT_PORT);
	        padding = ParameterMap.getKeyedInteger(qualifiers, "padding", NrpePacket.DEFAULT_PADDING);
            retries = ParameterMap.getKeyedInteger(qualifiers, "retry", DEFAULT_RETRY);
            timeout = ParameterMap.getKeyedInteger(qualifiers, "timeout", DEFAULT_TIMEOUT);
            banner = ParameterMap.getKeyedString(qualifiers, "banner", null);
            match = ParameterMap.getKeyedString(qualifiers, "match", null);
            m_useSsl = ParameterMap.getKeyedBoolean(qualifiers, "usessl", DEFAULT_USE_SSL);
        }

        try {
            StringBuffer bannerResult = null;
            RE regex = null;
            if (match == null && (banner == null || banner.equals("*"))) {
                regex = null;
            } else if (match != null) {
                regex = new RE(match);
                bannerResult = new StringBuffer();
            } else if (banner != null) {
                regex = new RE(banner);
                bannerResult = new StringBuffer();
            }

            boolean result = isServer(address, port, command, padding, retries, timeout, regex, bannerResult);
            if (result && qualifiers != null) {
                if (bannerResult != null && bannerResult.length() > 0)
                    qualifiers.put("banner", bannerResult.toString());
            }

            return result;
        } catch (RESyntaxException e) {
            throw new java.lang.reflect.UndeclaredThrowableException(e);
        }
    }
    
    protected Socket wrapSocket(Socket socket, String hostAddress, int hostPort) throws Exception {
    	if (! m_useSsl) {
    		if (log().isDebugEnabled()) {
    			log().debug("Parameter 'usessl' is unset or false, not using SSL");
    		}
    		return socket;
    	} else {
    		if (log().isDebugEnabled()) {
    			log().debug("Parameter 'usessl' is true, using SSL");
    		}
    	}

    	Socket wrappedSocket;

        // set up the certificate validation. USING THIS SCHEME WILL ACCEPT ALL
        // CERTIFICATES
        SSLSocketFactory sslSF = null;

        TrustManager[] tm = { new RelaxedX509TrustManager() };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, tm, new java.security.SecureRandom());
        sslSF = sslContext.getSocketFactory();
        wrappedSocket = sslSF.createSocket(socket, hostAddress, hostPort, true);
        SSLSocket sslSocket = (SSLSocket) wrappedSocket;
        // Set this socket to use anonymous Diffie-Hellman ciphers. This removes the authentication
        // benefits of SSL, but it's how NRPE rolls so we have to play along.
        sslSocket.setEnabledCipherSuites(ADH_CIPHER_SUITES);
        return wrappedSocket;
    }
    
    protected Category log() {
    	return ThreadCategory.getInstance(getClass());
    }
}
