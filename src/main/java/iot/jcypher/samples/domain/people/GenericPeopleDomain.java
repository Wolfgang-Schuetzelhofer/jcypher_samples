/************************************************************************
 * Copyright (c) 2015 IoT-Solutions e.U.
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
import iot.jcypher.domain.DomainAccessFactory;
import iot.jcypher.domain.DomainInformation;
import iot.jcypher.domain.IGenericDomainAccess;
import iot.jcypher.domain.DomainInformation.DomainObjectType;
import iot.jcypher.domain.genericmodel.DOField;
import iot.jcypher.domain.genericmodel.DOType;
import iot.jcypher.domain.genericmodel.DOType.DOClassBuilder;
import iot.jcypher.domain.genericmodel.DOType.DOEnumBuilder;
import iot.jcypher.domain.genericmodel.DOType.DOInterfaceBuilder;
import iot.jcypher.domain.genericmodel.DOType.Kind;
import iot.jcypher.domain.genericmodel.DOTypeBuilderFactory;
import iot.jcypher.domain.genericmodel.DomainObject;
import iot.jcypher.domain.genericmodel.internal.DOWalker;
import iot.jcypher.domainquery.DomainQueryResult;
import iot.jcypher.domainquery.GDomainQuery;
import iot.jcypher.domainquery.api.DomainObjectMatch;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.NATIVE;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.JcResultException;
import iot.jcypher.query.writer.Format;
import iot.jcypher.samples.domain.people.graph_access.Config;
import iot.jcypher.samples.domain.people.util.GenObjectToString;
import iot.jcypher.samples.domain.people.util.Util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class GenericPeopleDomain {
	
	public static String domainName = "GENERIC-PEOPLE-DOMAIN";

	public static void main(String[] args) {
		
		// load 'GENERIC-PEOPLE-DOMAIN'
		clearGraphAndloadDomain();
		
		// demonstrates how to retrieve generic domain objects.
		retrieveDomainObjects();
		
		// demonstrates how to formulate and perform domain queries.
		performDomainQueries();
		
		// demonstrates how to dynamically explore the structure of generic domain objects.
		exploreDomainObjects();
		
		// demonstrates how to dynamically create a domain model.
		createDomainModel();
		
		// demonstrates how to retrieve types from a loaded Generic Domain Model
		retrieveExistingDomainTypes();
		
		// demonstrates how to instantiate a generic domain object
		instantiateDomainObject();
		
		return;
	}
	
	/**
	 * load the people domain (you don't have the java classes at hand).
	 * <br/>The domain is a copy of the original people domain with all classes moved into
	 * a different package (genericdomain.people.model).
	 * <br/>In addition the domain is renamed to 'GENERIC-PEOPLE-DOMAIN'.
	 */
	public static void clearGraphAndloadDomain() {
		// Initially clear the database.
		// Note: On how to create an IDBAccess (access to a graph database)
		// and an IDomainAccess (access to a domain within a graph database)
		// hava a look at the 'Config class' in the 'graph_access' subpackage.
		IDBAccess dbAccess = Config.getDBAccess();
		List<JcError> errors = dbAccess.clearDatabase();
		if (errors.size() > 0) {
			Util.printErrors(errors);
			throw new JcResultException(errors);
		}
		
		// load domain model
		InputStreamReader ir = null;
		try {
			InputStream in = GenericPeopleDomain.class.getResourceAsStream("/load/generic_people_domain.txt");
			ir = new InputStreamReader(in);
			
			LineNumberReader lnr = new LineNumberReader(ir);
			List<String> lns = new ArrayList<String>();
			String line = lnr.readLine();
			while(line != null) {
				lns.add(line);
				line = lnr.readLine();
			}
			String[] lines = lns.toArray(new String[lns.size()]);
			IClause[] clauses = new IClause[] {
					NATIVE.cypher(lines)
			};
			JcQuery q = new JcQuery();
			q.setClauses(clauses);
			
			JcQueryResult result = dbAccess.execute(q);
			if (result.hasErrors()) {
				Util.printErrors(errors);
				throw new JcResultException(errors);
			}
			
		} catch(Throwable e) {
			throw new RuntimeException(e);
		} finally {
			try {
				ir.close();
			} catch (Throwable e) {}
		}
	}
	
	/**
	 * demonstrates how to retrieve generic domain objects.
	 */
	public static void retrieveDomainObjects() {
		IDBAccess dbAccess = Config.getDBAccess();
		
		// Instantiate an IGenericDomainAccess
		IGenericDomainAccess genricDomainAccess = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		
		// You need to specify the fully qualified type name
		// Note: You cannot specify the type directly.
		// (Normally you don't have the domain object classes on your classpath
		// when working with a generic domain model).
		// A list of 'DomainObject' instances is returned.
		// 'DomainObject' represents an object of the generic model.
		List<DomainObject> domainObjects =
				genricDomainAccess.loadByType("genericdomain.people.model.Person", -1, 0, -1);
		
		// You can visualize the result
		// You can play with the resolution depth of the string representation
		// -1 means until a leaf of the object tree is reached
		int depth = 1;
		GenObjectToString toString = new GenObjectToString(Format.PRETTY_1, depth);
		DOWalker walker = new DOWalker(domainObjects, toString);
		walker.walkDOGraph();
		String str = toString.getBuffer().toString();
		System.out.println("\nObjectGraph:" + str);
		
		return;
	}
	
	/**
	 * demonstrates how to formulate and perform domain queries.
	 */
	public static void performDomainQueries() {
		IDBAccess dbAccess = Config.getDBAccess();
		
		// Instantiate an IGenericDomainAccess
		IGenericDomainAccess genricDomainAccess = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		
		// create a DomainQuery object
		GDomainQuery q = genricDomainAccess.createQuery();
		
		// create a DomainObjectMatch for objects of type Person
		DomainObjectMatch<DomainObject> j_smithMatch = q.createMatch("genericdomain.people.model.Person");

		// Constrain the set of Persons to contain
		// 'John Smith' only
		q.WHERE(j_smithMatch.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smithMatch.atttribute("firstName")).EQUALS("John");
		
		// Traverse forward, start with 'John Smith'
		// (j_smithMatch is constraint to match 'John Smith' only),
		// navigate attribute 'pointsOfContact',
		// end matching objects of type Address.
		DomainObjectMatch<DomainObject> j_smith_AddressesMatch =
				q.TRAVERSE_FROM(j_smithMatch).FORTH("pointsOfContact").TO_GENERIC("genericdomain.people.model.Address");
		
		// execute the query
		DomainQueryResult result = q.execute();
		// retrieve the list of matching domain objects
		// (i.e. all addresses of 'John Smith')
		List<DomainObject> j_smith_Addresses = result.resultOf(j_smith_AddressesMatch);
		
		// You can visualize the result
		// You can play with the resolution depth of the string representation
		// -1 means until a leaf of the object tree is reached
		int depth = 1;
		GenObjectToString toString = new GenObjectToString(Format.PRETTY_1, depth);
		DOWalker walker = new DOWalker(j_smith_Addresses, toString);
		walker.walkDOGraph();
		String str = toString.getBuffer().toString();
		System.out.println("\nObjectGraph:" + str);
		
		return;
	}
	
	/**
	 * demonstrates how to dynamically explore the structure of generic domain objects.
	 */
	@SuppressWarnings("unused")
	public static void exploreDomainObjects() {
		IDBAccess dbAccess = Config.getDBAccess();
		
		// Instantiate an IGenericDomainAccess
		IGenericDomainAccess genricDomainAccess = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		
		// create a DomainQuery object
		GDomainQuery q = genricDomainAccess.createQuery();
		
		// create a DomainObjectMatch for objects of type Person
		DomainObjectMatch<DomainObject> j_smithMatch = q.createMatch("genericdomain.people.model.Person");

		// Constrain the set of Persons to contain
		// 'John Smith' only
		q.WHERE(j_smithMatch.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smithMatch.atttribute("firstName")).EQUALS("John");
		
		// execute the query
		DomainQueryResult result = q.execute();
		// retrieve the list of matching domain objects
		// (i.e. all addresses of 'John Smith')
		DomainObject j_smith = result.resultOf(j_smithMatch).get(0);
		
		// ask the type of a generic domain object
		DOType doType = j_smith.getDomainObjectType();
		
		// get a string representation of the type
		String str = doType.asString("   ");
		System.out.println(str);
		
		// ask which kind of type the given type is
		Kind kind = doType.getKind();
		
		// ask the type for it's super type
		DOType sType = doType.getSuperType();
		
		// get a string representation of the super type
		String sstr = sType.asString("   ");
		System.out.println(sstr);
		
		// ask the type for interfaces it implements
		List<DOType> interfaces = doType.getInterfaces();
		
		// ask the type for it's declared fields
		List<DOField> declaredFields = doType.getDeclaredFields();
		
		// ask the type for all it's fields (including those declared by super types)
		List<DOField> allFields = doType.getFields();
		
		// ask the type for the names it's declared fields
		List<String> declaredFieldNames = doType.getDeclaredFieldNames();
				
		// ask the type for all it's fields names (including those fields declared by super types)
		List<String> allFieldNames = doType.getFieldNames();
		
		return;
	}
	
	/**
	 * demonstrates how to dynamically create a domain model.
	 * @return an array where array[0] = personType and array[1] = addressType,
	 * array[2] = subjectTypes
	 */
	public static DOType[] createDomainModel() {
		IDBAccess dbAccess = Config.getDBAccess();
		
		// Instantiate an IGenericDomainAccess
		IGenericDomainAccess genricDomainAccess = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		
		// create a type builder factory
		DOTypeBuilderFactory tpf = genricDomainAccess.getTypeBuilderFactory();
		
		// now construct the domain model
		DOEnumBuilder subjectTypesBuilder = tpf.createEnumBuilder("mydomain.model.SubjectTypes");
		subjectTypesBuilder.addEnumValue("NAT_PERSON");
		subjectTypesBuilder.addEnumValue("JUR_PERSON");
		DOType subjectTypes = subjectTypesBuilder.build();
		
		DOInterfaceBuilder pointOfContactBuilder = tpf.createInterfaceBuilder("mydomain.model.PointOfContact");
		DOType pointOfContact = pointOfContactBuilder.build();
		
		DOClassBuilder addressBuilder = tpf.createClassBuilder("mydomain.model.Address");
		addressBuilder.addInterface(pointOfContact);
		addressBuilder.addField("street", String.class.getName());
		addressBuilder.addField("number", int.class.getName());
		DOType addressType = addressBuilder.build();
		
		DOClassBuilder subjectTypeBuilder = tpf.createClassBuilder("mydomain.model.Subject");
		subjectTypeBuilder.setAbstract();
		subjectTypeBuilder.addField("subjectType", subjectTypes.getName());
		subjectTypeBuilder.addListField("pointsOfContact", "mydomain.model.PointOfContact");
		DOType subject = subjectTypeBuilder.build();
		
		DOClassBuilder personTypeBuilder = tpf.createClassBuilder("mydomain.model.Person");
		personTypeBuilder.addField("firstName", String.class.getName());
		personTypeBuilder.addField("lastName", String.class.getName());
		personTypeBuilder.addField("birthDate", Date.class.getName());
		personTypeBuilder.setSuperType(subject);
		DOType personType = personTypeBuilder.build();
		
		// get a string representation of the type
		String str = personType.asString("   ");
		System.out.println(str);
		
		return new DOType[] {personType, addressType, subjectTypes};
	}
	
	/**
	 * demonstrates how to retrieve types from a loaded Generic Domain Model
	 */
	public static void retrieveExistingDomainTypes() {
		IDBAccess dbAccess = Config.getDBAccess();
		
		// Create a DomainInformation object for a certain domain.
		DomainInformation di = DomainInformation.forDomain(dbAccess, domainName);
				
		// Answer a list of names of DomainObjectTypes stored in the domain graph
		List<String> typeNames = di.getDomainObjectTypeNames();
		System.out.println(typeNames);
		
		// Instantiate an IGenericDomainAccess
		IGenericDomainAccess genricDomainAccess = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		
		// retrieve an existing type
		DOType doType = genricDomainAccess.getDomainObjectType(typeNames.get(0));
		// get a string representation of the type
		String str = doType.asString("   ");
		System.out.println(str);
		
		return;
	}
	
	/**
	 * demonstrates how to instantiate a generic domain object
	 */
	public static void instantiateDomainObject() {
		DOType[] types = createDomainModel();
		DOType personType = types[0];
		DOType addressType = types[1];
		
		// that's an enum type
		DOType subjectTypes = types[2];
		
		DomainObject anAddress = new DomainObject(addressType);
		anAddress.setFieldValue("street", "Market Street");
		anAddress.setFieldValue("number", 42);
		
		DomainObject aPerson = new DomainObject(personType);
		aPerson.setFieldValue("firstName", "Maxwell");
		aPerson.setFieldValue("lastName", "Smart");
		GregorianCalendar cal = new GregorianCalendar(1940, 0, 22);
		Date birthDate = cal.getTime();
		aPerson.setFieldValue("birthDate", birthDate);
		aPerson.setFieldValue("subjectType", subjectTypes.getEnumValue("NAT_PERSON"));
		aPerson.addListFieldValue("pointsOfContact", anAddress);
		
		IDBAccess dbAccess = Config.getDBAccess();
		// Instantiate an IGenericDomainAccess
		IGenericDomainAccess genricDomainAccess = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		// now store the newly created person
		List<JcError> errors = genricDomainAccess.store(aPerson);
		if (errors.size() > 0) {
			Util.printErrors(errors);
			throw new JcResultException(errors);
		}
		
		return;
	}
}
