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

import iot.jcypher.domain.IDomainAccess;

import java.util.List;

public class DomainReader {

	/**
	 * given a list of ids load the domain object graph rooted by this ids.
	 * @param objectIds
	 * @return a list of domain objects
	 */
	public List<Object> loadDomainObjects(List<Long> objectIds) {
		IDomainAccess domainAccess = Config.createDomainAccess();
		
		long[] ids = new long[objectIds.size()];
		for (int i = 0; i < objectIds.size(); i++) {
			ids[i] = objectIds.get(i);
		}
		List<Object> domainObjects = domainAccess.loadByIds(Object.class, -1, ids);
		return domainObjects;
	}
}
