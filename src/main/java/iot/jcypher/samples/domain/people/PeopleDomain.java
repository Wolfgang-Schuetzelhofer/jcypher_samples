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

package iot.jcypher.samples.domain.people;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.domain.IDomainAccess;
import iot.jcypher.domain.SyncInfo;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.JcResultException;
import iot.jcypher.samples.domain.people.graph_access.Config;
import iot.jcypher.samples.domain.people.model.Subject;
import iot.jcypher.samples.domain.people.util.CompareUtil;
import iot.jcypher.samples.domain.people.util.Population;
import iot.jcypher.samples.domain.people.util.Util;

import java.util.List;

public class PeopleDomain {

	public static void main(String[] args) {
		List<JcError> errors;
		
		// A utility class which simply creates a sample population.
		Population domainPopulator = new Population();
		
		// Create the population,
		// return a list of root objects of the created object graph.
		List<Object> createdDomainObjects = domainPopulator.createPopulation();
		
		// Initially clear the database.
		// Note: On how to create an IDBAccess (access to a graph database)
		// and an IDomainAccess (access to a domain within a graph database)
		// hava a look at the 'Config class'.
		IDBAccess dbAccess = Config.getDBAccess();
		errors = dbAccess.clearDatabase();
		if (errors.size() > 0) {
			Util.printErrors(errors);
			throw new JcResultException(errors);
		}
		
		// Store the graph of domain objects in the graph database.
		// Starting with the root objects, the entire object graph is stored in the graph database.
		IDomainAccess domainAccess = Config.createDomainAccess();
		errors = domainAccess.store(createdDomainObjects);
		if (errors.size() > 0) {
			Util.printErrors(errors);
			throw new JcResultException(errors);
		}
		// you can have a look to the graph database with Neo4Js graph database browser,
		// in order to see the created domain graph.
		// Note: By default the maximum number of nodes displayed in the browser is limited to 25.
		// You have to change the browser query to see all created nodes.
		// E.g. to 'MATCH n RETURN n LIMIT 100'
		
		// Retrieve the 'SyncInfo' for each domain object in the list in order to get each
		// objects id (i.e. the id of the node in the graph to which the object was mapped).
		// We need the ids to later on load the objects from the graph database.
		List<SyncInfo> syncInfos = domainAccess.getSyncInfos(createdDomainObjects);
		long[] ids = new long[syncInfos.size()];
		for (int i = 0; i < syncInfos.size(); i++) {
			SyncInfo syncInfo = syncInfos.get(i);
			ids[i] = syncInfo.getId();
		}
		
		// Create a new IDomainAccess to simulate a new starting point.
		// We do this to see how domain objects are freshly created and loaded from the database.
		// If we would use the same IDomainAccess through which we have stored the domain objects
		// we would simply get back the identical objects
		IDomainAccess domainAccess_2 = Config.createDomainAccess();
		
		// Load a graph of domain objects from the graph database.
		// By means of the second parameter you can specify the resolution depth.
		// 0 would mean you only load simple attributes into the objects (i.e. attributes which have been stored
		// to a node property; e.g. strings or numbers).
		// Complex attributes are stored in separate nodes accessed via relations.
		// So with the second parameter you can specify how deep exactly the graph should be navigated
		// when loading objects.
		// -1 means resolution as deep as possible (until leafs of the graph,
		// i.e. objects which only have simple attributes, are reached or until a cycle (loop) is detected).
		List<Object> loadedDomainObjects = domainAccess_2.loadByIds(Object.class, -1, ids);
		
		// Check if the loaded graph of domain objects equals the initially created one.
		// The utility class will do this check for you
		boolean equals = CompareUtil.equalsObjects(createdDomainObjects, loadedDomainObjects);
		
		// If you don't know the ids of the stored objects, which will most commonly be the case,
		// you can load domain objects by type.
		// Again create a new IDomainAccess to simulate a new starting point.
		IDomainAccess domainAccess_3 = Config.createDomainAccess();
		
		// All objects of the specified type or any subtype of it will be loaded from the domain graph
		// and returned in a list.
		// You can even use 'Object.class' in which case all domain objects in the graph will be returned.
		// Again you can specify the resolution depth.
		// Additionally, you can specify an offset (parameter 3) and a count (parameter 4) of objects
		// to be returned with respect to a list containing the total number of objects of the specified type.
		// This feature provides the possibility to do pagination.
		// Offset 0 and count -1 will return a list of all objeccts of the specified type.
		// To really make use of the pagination feature you need to know the total number
		// of objects of a certain type.
		// There is an interface method which provides this information. We will see that soon.
		List<Subject> loadedDomainObjects_2 = domainAccess_3.loadByType(Subject.class, -1, 0, -1);
		
		// Once again check if the loaded graph of domain objects equals the initially created one.
		// Use the method 'equalsUnorderedList(...)', as the content of the two lists will be the same
		// but will probably not be in the same order
		equals = CompareUtil.equalsUnorderedList(createdDomainObjects, loadedDomainObjects_2);
		
		// You can query the total number of instances of a certain type stored in the domain graph.
		// The number of stored instances of the specified type and of all its subtypes is returned.
		// Note: Specifying 'Object.class' will therefore return the total number of domain objects stored in the domain graph
		long num = domainAccess_3.numberOfInstancesOf(Subject.class);

		return;
	}

}
