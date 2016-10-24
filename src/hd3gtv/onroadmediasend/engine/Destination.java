/*
 * This file is part of On Road Media Send.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.onroadmediasend.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

public class Destination {
	
	public enum Protocol {
		FTP;
	}
	
	private Protocol protocol;
	private String host;
	private String username;
	private String password;
	private int port;
	private boolean passive;
	private String label;
	
	public Destination(String host, String username, String password, int port, boolean passive) {
		protocol = Protocol.FTP;
		this.host = host;
		if (host == null) {
			throw new NullPointerException("\"host\" can't to be null"); //$NON-NLS-1$
		}
		this.username = username;
		if (username == null) {
			throw new NullPointerException("\"username\" can't to be null"); //$NON-NLS-1$
		}
		this.password = password;
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null"); //$NON-NLS-1$
		}
		this.port = port;
		if (port < 1 | port > 65535) {
			throw new IndexOutOfBoundsException("port: " + port); //$NON-NLS-1$
		}
		this.passive = passive;
	}
	
	public static ArrayList<Destination> getConfiguredDestinations(List<HierarchicalConfiguration<ImmutableNode>> conf) {
		ArrayList<Destination> result = new ArrayList<>();
		
		conf.forEach(node -> {
			if (node.getString("protocol").equalsIgnoreCase("ftp")) { //$NON-NLS-1$ //$NON-NLS-2$
				Destination d = new Destination(node.getString("host"), node.getString("username"), node.getString("password"), node.getInt("port"), node.getBoolean("passive")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				d.label = node.getString("label"); //$NON-NLS-1$
				result.add(d);
			} else {
				throw new NullPointerException("Protocol is invalid: " + node.getString("protocol")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		return result;
	}
	
	public String toString() {
		return label;
	}
	
	public Protocol getProtocol() {
		return protocol;
	}
	
	public String getHost() {
		return host;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public int getPort() {
		return port;
	}
	
	public boolean isPassive() {
		return passive;
	}
	
	public String getLabel() {
		return label;
	}
	
}
