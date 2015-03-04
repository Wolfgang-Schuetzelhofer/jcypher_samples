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
import iot.jcypher.domain.DomainInformation;
import iot.jcypher.domain.DomainInformation.DomainObjectType;
import iot.jcypher.domain.IDomainAccess;
import iot.jcypher.domain.SyncInfo;
import iot.jcypher.domainquery.CountQueryResult;
import iot.jcypher.domainquery.DomainQuery;
import iot.jcypher.domainquery.DomainQueryResult;
import iot.jcypher.domainquery.api.DomainObjectMatch;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.JcResultException;
import iot.jcypher.samples.domain.people.graph_access.Config;
import iot.jcypher.samples.domain.people.model.Address;
import iot.jcypher.samples.domain.people.model.Area;
import iot.jcypher.samples.domain.people.model.AreaType;
import iot.jcypher.samples.domain.people.model.Person;
import iot.jcypher.samples.domain.people.model.Subject;
import iot.jcypher.samples.domain.people.util.CompareUtil;
import iot.jcypher.samples.domain.people.util.Util;
import iot.jcypher.util.QueriesPrintObserver;
import iot.jcypher.util.QueriesPrintObserver.ContentToObserve;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class PeopleDomain {

	public static void main(String[] args) {

		// demonstrates how to store and retrieve domain objects.
		storeAndRetrieveDomainObjects();
		
		// demonstrates how to retrieve domain information.
		retrieveDomainInformation();
		
		// demonstrates how to formulate and execute domain queries.
		// Part 1: Predicate Expressions
		performDomainQueries_PredicateExpressions();
		
		// demonstrates how to formulate and execute domain queries.
		// Part 1: Traversal Expressions
		performDomainQueries_TraversalExpressions();
		return;
	}
	
	/**
	 * demonstrates how to store and retrieve domain objects.
	 */
	public static void storeAndRetrieveDomainObjects() {
		List<JcError> errors;
		
		// A utility class which creates a sample population.
		Population domainPopulator = new Population();
		
		// Create the population,
		// return a list of root objects of the created object graph.
		List<Object> createdDomainObjects = domainPopulator.createPopulation();
		
		// Initially clear the database.
		// Note: On how to create an IDBAccess (access to a graph database)
		// and an IDomainAccess (access to a domain within a graph database)
		// hava a look at the 'Config class' in the 'graph_access' subpackage.
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
		// Or see: src/main/resources/people_domain.gif.
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
		// to a node property; strings, numbers, booleans, dates).
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

	/**
	 * demonstrates how to retrieve domain information.
	 */
	public static void retrieveDomainInformation() {
		List<JcError> errors;
		
		// Answer the names of available domains.
		List<String> available = DomainInformation.availableDomains(Config.getDBAccess());
		
		// Create a DomainInformation object for a certain domain.
		DomainInformation di = DomainInformation.forDomain(Config.getDBAccess(), Config.domainName);
		
		// Answer a list of DomainObjectTypes stored in the domain graph
		List<DomainObjectType> types = di.getDomainObjectTypes();
		
		DomainObjectType type = types.get(0);

		// You can ask a DomainObjectType for its type name
		// i.e. the fully qualified name of the java type
		String typeName = type.getTypeName();

		// You can ask a DomainObjectType for the
		// label of nodes to which domain objects
		// of that type are mapped
		String label = type.getNodeLabel();

		// You can retrieve the java type (Class)
		// from a DomainObjectType.
		// Note: this may raise a ClassNotFoundException
		Class<?> jType = type.getType();
		
		return;
	}
	
	/**
	 * demonstrates how to formulate and perform domain queries.
	 * Part 1: Predicate Expressions.
	 */
	public static void performDomainQueries_PredicateExpressions() {
		
		IDomainAccess domainAccess = Config.createDomainAccess();
		
		/****** Query 01 *************************************/
		// create a DomainQuery object
		DomainQuery q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Person
		DomainObjectMatch<Person> smithsMatch = q.createMatch(Person.class);
		// define a predicate expression in form of a WHERE clause
		// which constrains the set of Persons
		q.WHERE(smithsMatch.atttribute("lastName")).EQUALS("Smith");
		
		// you need to execute the query
		DomainQueryResult result = q.execute();
		// retrieve the list of matching domain objects
		List<Person> smiths = result.resultOf(smithsMatch);
		/*****************************************************/
		
		/****** Query 02 *************************************/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Person
		DomainObjectMatch<Person> j_smithMatch = q.createMatch(Person.class);

		// Constrain the set of Persons to contain
		// John Smith only
		q.WHERE(j_smithMatch.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smithMatch.atttribute("firstName")).EQUALS("John");
		
		// Note: Consecutive WHERE clauses are 'AND-ed' by default.
		// If you want to 'OR' them you need to explicitly separate them
		// by OR(); --> see next query
		
		// execute the query
		result = q.execute();
		// retrieve the list of matching domain objects
		List<Person> j_smith = result.resultOf(j_smithMatch);
		/*****************************************************/
		
		/****** Query 03 *************************************/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Person
		DomainObjectMatch<Person> a_j_smithMatch = q.createMatch(Person.class);

		// Constrain the set of Persons to contain
		// Angelina and Jeremy Smith only
		q.WHERE(a_j_smithMatch.atttribute("lastName")).EQUALS("Smith");
		q.BR_OPEN();
			q.WHERE(a_j_smithMatch.atttribute("firstName")).EQUALS("Angelina");
			q.OR();
			q.WHERE(a_j_smithMatch.atttribute("firstName")).EQUALS("Jeremy");
		q.BR_CLOSE();
		
		// Note: You can define blocks by using BR_OPEN (for bracket open)
		// and BR_CLOSE (for bracket close). You can nest blocks arbitrarily deep.
		
		// execute the query
		result = q.execute();
		// retrieve the list of matching domain objects
		List<Person> a_j_smith = result.resultOf(a_j_smithMatch);
		/*****************************************************/
		
		/****** Query 04 *************************************/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create DomainObjectMatches
		DomainObjectMatch<Person> a_smithMatch = q.createMatch(Person.class);
		DomainObjectMatch<Person> eyeColorMatch = q.createMatch(Person.class);

		// Constrain the set of Persons to contain
		// Angelina Smith only
		q.WHERE(a_smithMatch.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(a_smithMatch.atttribute("firstName")).EQUALS("Angelina");
		
		// Now constrain the set to be matched by 'eyeColorMatch'
		// to contain persons with the same eye color as Angelina Smith
		q.WHERE(eyeColorMatch.atttribute("eyeColor")).EQUALS(a_smithMatch.atttribute("eyeColor"));
		
		// execute the query
		result = q.execute();
		// retrieve the list of matching domain objects
		List<Person> a_smith = result.resultOf(a_smithMatch);
		List<Person> all_with_a_smith_eyeColor = result.resultOf(eyeColorMatch);
		/*****************************************************/
		
		/****** Query 05 **** calculate intersection *******/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create DomainObjectMatches
		DomainObjectMatch<Person> set_1Match = q.createMatch(Person.class);
		DomainObjectMatch<Person> set_2Match = q.createMatch(Person.class);
		DomainObjectMatch<Person> intersectionMatch = q.createMatch(Person.class);
		// Constrain set_1 to
		// a set with all smiths and christa berghammer
		q.WHERE(set_1Match.atttribute("lastName")).EQUALS("Smith");
		q.OR();
		q.BR_OPEN();
			q.WHERE(set_1Match.atttribute("lastName")).EQUALS("Berghammer");
			q.WHERE(set_1Match.atttribute("firstName")).EQUALS("Christa");
		q.BR_CLOSE();
		
		// Constrain set_2 to
		// a set with all berghammers
		q.WHERE(set_2Match.atttribute("lastName")).EQUALS("Berghammer");
		
		// the intersction of both set_1 and set_2
		// it will contain christa berghammer only
		q.WHERE(intersectionMatch).IN(set_1Match);
		q.WHERE(intersectionMatch).IN(set_2Match);
		
		result = q.execute();
		List<Person> set_1 = result.resultOf(set_1Match);
		List<Person> set_2 = result.resultOf(set_2Match);
		List<Person> intersection = result.resultOf(intersectionMatch);
		/*****************************************************/
		
		/****** Sorting Result Sets + Pagination **********/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create DomainObjectMatches
		DomainObjectMatch<Person> personsMatch = q.createMatch(Person.class);
		// Specify Pagination (offset + count)
		personsMatch.setPage(1, 5);
		// Specify sorting for the result set.
		// First: All persons are sorted by their last name (ascending)
		// Second: Having the same last name, persons are sorted
		// by their first name (descending)
		q.ORDER(personsMatch).BY("lastName");
		q.ORDER(personsMatch).BY("firstName").DESCENDING();
		
		result = q.execute();
		List<Person> sortedPersons = result.resultOf(personsMatch);
		/*****************************************************/
		
		/****** Change Pagination *************************/
		personsMatch.setPage(7, 3);
		// Retrieve the result set again.
		sortedPersons = result.resultOf(personsMatch);
		/*****************************************************/
		
		/****** Retrieve number of matching objects ********/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Person
		DomainObjectMatch<Person> smithMatch = q.createMatch(Person.class);
		// define a predicate expression in form of a WHERE clause
		// which constrains the set of Persons
		q.WHERE(smithMatch.atttribute("lastName")).EQUALS("Smith");
		
		// Retrieve the number of matching objects
		CountQueryResult countResult = q.executeCount();
		long numberOfSmiths = countResult.countOf(smithMatch);
		/*****************************************************/
		
		return;
	}
	
	/**
	 * demonstrates how to formulate and perform domain queries.
	 * Part 2: Traversal Expressions.
	 */
	public static void performDomainQueries_TraversalExpressions() {
		
		IDomainAccess domainAccess = Config.createDomainAccess();
		
		/****** Traversal 01 ********************************/
		// create a DomainQuery object
		DomainQuery q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Person
		DomainObjectMatch<Person> j_smithMatch = q.createMatch(Person.class);

		// Constrain the set of Persons to contain
		// 'John Smith' only
		q.WHERE(j_smithMatch.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smithMatch.atttribute("firstName")).EQUALS("John");
		
		// Traverse forward, start with 'John Smith'
		// (j_smithMatch is constraint to match 'John Smith' only),
		// navigate attribute 'pointsOfContact',
		// end matching objects of type Address.
		DomainObjectMatch<Address> j_smith_AddressesMatch =
				q.TRAVERSE_FROM(j_smithMatch).FORTH("pointsOfContact").TO(Address.class);
		
		// execute the query
		DomainQueryResult result = q.execute();
		// retrieve the list of matching domain objects
		// (i.e. all addresses of 'John Smith')
		List<Address> j_smith_Addresses = result.resultOf(j_smith_AddressesMatch);
		/*****************************************************/
		
		/****** Traversal 02 ********************************/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Subject
		DomainObjectMatch<Subject> smith_globcomMatch = q.createMatch(Subject.class);
		
		// Constrain the set of Subjects to contain
		// 'John Smith' and 'Global Company' only
		q.WHERE(smith_globcomMatch.atttribute("name")).EQUALS("Global Company");
		q.OR();
		q.BR_OPEN();
			q.WHERE(smith_globcomMatch.atttribute("lastName")).EQUALS("Smith");
			q.WHERE(smith_globcomMatch.atttribute("firstName")).EQUALS("John");
		q.BR_CLOSE();
		
		// Start with the set containing 'John Smith' and 'Global Company',
		// navigate forward via attribute 'pointsOfContact'
		//          (defined in abstract super class 'Subject'),
		// navigate forward via attribute 'area',
		// end matching objects of type Area
		// (these are the immediate areas referenced by addresses
		// possibly: Cities, Urban Districts, Villages, ...).
		DomainObjectMatch<Area> immediateAreasMatch =
			q.TRAVERSE_FROM(smith_globcomMatch).FORTH("pointsOfContact")
				.FORTH("area").TO(Area.class);
		
		// Start with the set of immediate areas (retrieved before),
		// navigate forward via attribute 'partOf'.
		// ( DISTANCE(1, -1) means that
		// all areas reachable from one hop up to an arbitrary number of hops
		// via attribute 'partOf' will be collected),
		// end matching objects of type Area.
		DomainObjectMatch<Area> areasMatch =
			q.TRAVERSE_FROM(immediateAreasMatch).FORTH("partOf")
				.DISTANCE(1, -1).TO(Area.class);

		// create a DomainObjectMatch for objects of type Area
		DomainObjectMatch<Area> citiesMatch = q.createMatch(Area.class);
		
		// build 'citiesMatch' as the union of 'immediateAreasMatch'
		// and 'areasMatch' (then you have a set containing all areas
		// reachable from the relevant addresses),
		// further constrain the set to only contain areas of type 'CITY'.
		q.BR_OPEN();
			q.WHERE(citiesMatch).IN(immediateAreasMatch);
			q.OR();
			q.WHERE(citiesMatch).IN(areasMatch);
		q.BR_CLOSE();
		q.WHERE(citiesMatch.atttribute("areaType")).EQUALS(AreaType.CITY);
		
		// execute the query
		result = q.execute();
		
		// retrieve the list of matching domain objects.
		// It will contain the cities in which either 'John Smith' or 'Global Company'
		// have an address.
		List<Area> cities = result.resultOf(citiesMatch);
		/*****************************************************/
		
		/****** Backward Traversal *************************/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Address
		DomainObjectMatch<Address> addressMatch = q.createMatch(Address.class);
		
		// Constrain the set of Addresses to contain
		// 'Market Street 20' only
		q.WHERE(addressMatch.atttribute("street")).EQUALS("Market Street");
		q.WHERE(addressMatch.atttribute("number")).EQUALS(20);
		
		// Traverse backward, start with 'Market Street 20'
		// (addressMatch is constraint to match 'Market Street 20' only),
		// navigate attribute 'pointsOfContact',
		// end matching objects of type Person.
		DomainObjectMatch<Person> residentsMatch =
			q.TRAVERSE_FROM(addressMatch).BACK("pointsOfContact")
				.TO(Person.class);
		// order the result by attribute 'firstName' ascending
		q.ORDER(residentsMatch).BY("firstName");
		
		// execute the query
		result = q.execute();
		
		// retrieve the list of matching domain objects.
		// It will contain the all residents of 'Market Street 20'
		// (ordered ascending).
		List<Person> residents = result.resultOf(residentsMatch);
		/*****************************************************/
		
		/****** Forward-Backward Traversal ***************/
		// create a DomainQuery object
		q = domainAccess.createQuery();
		// create a DomainObjectMatch for objects of type Person
		j_smithMatch = q.createMatch(Person.class);

		// Constrain the set of Persons to contain
		// John Smith only
		q.WHERE(j_smithMatch.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smithMatch.atttribute("firstName")).EQUALS("John");
		
		// Start with 'John Smith'
		// (j_smithMatch is constraint to match 'John Smith' only),
		// navigate forward via attribute 'pointsOfContact',
		// this will lead to 'John Smith's' Address(es),
		// then navigate backward via attribute 'pointsOfContact',
		// end matching objects of type Person.
		// This will lead to all other persons living at 'John Smith's' Address(es).
		DomainObjectMatch<Person> j_smith_residentsMatch =
			q.TRAVERSE_FROM(j_smithMatch).FORTH("pointsOfContact")
				.BACK("pointsOfContact").TO(Person.class);
		
		// execute the query
		result = q.execute();
		
		// retrieve the list of matching domain objects.
		// It will contain all other persons living at 'John Smith's' Address(es).
		List<Person> j_smith_residents = result.resultOf(j_smith_residentsMatch);
		/*****************************************************/
		
		return;
	}
	
	/**
	 * demonstrates how to formulate and perform domain queries.
	 * Part 3: Collection Expressions.
	 */
	public static void performDomainQueries_CollectionExpressions() {
		
		IDomainAccess domainAccess = Config.createDomainAccess();
		
		/****** Select 01 ********************************/
		// create a DomainQuery object
		DomainQuery q = domainAccess.createQuery();
		// Create a DomainObjectMatch for objects of type Person.
		// By default it will match all persons
		DomainObjectMatch<Person> personMatch = q.createMatch(Person.class);

		// Select from all persons
		// 'John Smith' only
		DomainObjectMatch<Person> j_smithMatch =
				q.SELECT_FROM(personMatch).ELEMENTS(
						q.WHERE(personMatch.atttribute("lastName")).EQUALS("Smith"),
						q.WHERE(personMatch.atttribute("firstName")).EQUALS("John")
				);
		
		// execute the query
		DomainQueryResult result = q.execute();
		// retrieve the list of matching domain objects
		List<Person> j_smith = result.resultOf(j_smithMatch);
		/*****************************************************/
		
		return;
	}
		
}
