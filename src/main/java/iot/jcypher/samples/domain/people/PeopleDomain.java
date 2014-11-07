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

import iot.jcypher.samples.domain.people.graph_access.DomainPopulator;
import iot.jcypher.samples.domain.people.graph_access.DomainReader;
import iot.jcypher.samples.domain.people.util.CompareUtil;

import java.util.List;

public class PeopleDomain {

	public static void main(String[] args) {
		DomainPopulator domainPopulator = new DomainPopulator();
		
		// create the population,
		// return a list of root objects of the created object graph.
		List<Object> createdDomainObjects = domainPopulator.createDomainPopulationPopulation();
		
		// store the object graph represented by a list of root objects to the database,
		// return a list of object ids (i.e. the ids of the nodes in the graph to which the root objects were mapped).
		List<Long> ids = domainPopulator.populateDomain(createdDomainObjects);
		
		DomainReader domainReader = new DomainReader();
		
		// load a graph of domain objects from the graph database.
		List<Object> loadedDomainObjects = domainReader.loadDomainObjects(ids);
		
		// check if the loaded graph of domain objects equals the initially created one.
		boolean equals = CompareUtil.equalsObjects(createdDomainObjects, loadedDomainObjects);

		return;
	}

}
