/************************************************************************
 * Copyright (c) 2014 IoT-Solutions e.U.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ************************************************************************/

package iot.jcypher.samples.domain.people.graph_access;

import iot.jcypher.database.DBAccessFactory;
import iot.jcypher.database.DBProperties;
import iot.jcypher.database.DBType;
import iot.jcypher.database.IDBAccess;
import iot.jcypher.domain.DomainAccessFactory;
import iot.jcypher.domain.IDomainAccess;

import java.util.Properties;

public class Config {

	public static String domainName;
	private static IDBAccess dbAccess;
	
	static {
		// a domain needs a unique name within a graph database
		domainName = "PEOPLE-DOMAIN";
		
		// properties for remote access and for embedded access
		// (not needed for in memory access)
		Properties props = new Properties();
		
		// properties for remote access
		props.setProperty(DBProperties.SERVER_ROOT_URI, "http://localhost:7474");
		// properties for embedded access
		props.setProperty(DBProperties.DATABASE_DIR, "C:/NEO4J_DBS/01");
		// no properties needed for in memory access
		
		dbAccess = DBAccessFactory.createDBAccess(DBType.REMOTE, props);
//		dbAccess = DBAccessFactory.createDBAccess(DBType.EMBEDDED, props);
//		dbAccess = DBAccessFactory.createDBAccess(DBType.IN_MEMORY, props);
	}
	
	/**
	 * answer an IDBAccess to access a graph database
	 * @return an IDBAccess
	 */
	public static IDBAccess getDBAccess() {
		return dbAccess;
	}
	
	/**
	 * answer a new IDomainAccess to work with a certain domain within a graph database
	 * @return a new IDomainAccess
	 */
	public static IDomainAccess createDomainAccess() {
		return DomainAccessFactory.createDomainAccess(dbAccess, domainName);
	}
}
