/*******************************************************************************
 * Copyright (c) 2006-2012
 * Software Technology Group, Dresden University of Technology
 * DevBoost GmbH, Berlin, Amtsgericht Charlottenburg, HRB 140026
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Software Technology Group - TU Dresden, Germany;
 *   DevBoost GmbH - Berlin, Germany
 *      - initial API and implementation
 ******************************************************************************/
package org.emftext.runtime.owltext.transformation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.emftext.language.owl.Ontology;
import org.emftext.language.owl.OntologyDocument;
import org.emftext.language.owl.resource.owl.analysis.custom.CrossResourceIRIResolver;
import org.emftext.language.owl.resource.owl.mopp.OwlResource;

public class OWLTransformationHelper {

	private static BidiHashMap<EObject, Integer> uniqueIdMap = new BidiHashMap<EObject, Integer>();
	private static HashMap<Resource, Integer> counters = new HashMap<Resource, Integer>();
	private static Map<EPackage, Ontology> packageOntologyMap = new HashMap<EPackage, Ontology>();
	private static Map<String, String> datatypeMap;

	private static class BidiHashMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;
		private HashMap<V, K> reverseMap = new HashMap<V, K>();

		public K getKey(V value) {
			return reverseMap.get(value);
		}

		public V put(K key, V value) {
			reverseMap.put(value, key);
			return super.put(key, value);
		}
	}

	static {
		datatypeMap = new HashMap<String, String>();
		// # owl:real
		// # owl:rational
		// # xsd:decimal
		datatypeMap.put("java.math.BigDecimal", "xsd:decimal");
		datatypeMap.put("EBigDecimal", "xsd:decimal");
		// # xsd:integer
		datatypeMap.put("java.math.BigInteger", "xsd:integer");
		datatypeMap.put("java.lang.Integer", "xsd:integer");
		datatypeMap.put("EInt", "xsd:integer");
		datatypeMap.put("EInteger", "xsd:integer");
		datatypeMap.put("EIntegerObject", "xsd:integer");
		datatypeMap.put("EBigInteger", "xsd:integer");
		datatypeMap.put("integer", "xsd:integer");
		datatypeMap.put("int", "xsd:integer");
		// # xsd:nonNegativeInteger
		// # xsd:nonPositiveInteger
		// # xsd:positiveInteger
		// # xsd:negativeInteger
		// # xsd:long
		datatypeMap.put("long", "xsd:long");
		datatypeMap.put("java.lang.Long", "xsd:long");
		datatypeMap.put("ELong", "xsd:long");
		datatypeMap.put("ELongObject", "xsd:long");

		// # xsd:short
		datatypeMap.put("short", "xsd:short");
		datatypeMap.put("java.lang.Short", "xsd:short");
		datatypeMap.put("EShort", "xsd:short");
		datatypeMap.put("EShortObject", "xsd:short");
		// # xsd:byte
		datatypeMap.put("byte", "xsd:byte");
		datatypeMap.put("java.lang.Byte", "xsd:byte");
		datatypeMap.put("EByte", "xsd:byte");
		datatypeMap.put("EByteObject", "xsd:byte");

		// # xsd:unsignedLong
		// # xsd:unsignedInt
		// # xsd:unsignedShort
		// # xsd:unsignedByte
		//
		// # xsd:double
		datatypeMap.put("double", "xsd:double");
		datatypeMap.put("java.lang.Double", "xsd:double");
		datatypeMap.put("EDouble", "xsd:double");
		datatypeMap.put("EDoubleObject", "xsd:double");

		// # xsd:float
		datatypeMap.put("float", "xsd:float");
		datatypeMap.put("java.lang.Float", "xsd:float");
		datatypeMap.put("EFloat", "xsd:float");
		datatypeMap.put("EFloatObject", "xsd:float");
		//
		// # xsd:boolean
		datatypeMap.put("java.lang.Boolean", "xsd:boolean");
		datatypeMap.put("EBoolean", "xsd:boolean");
		datatypeMap.put("boolean", "xsd:boolean");
		datatypeMap.put("EBooleanObject", "xsd:boolean");
		//
		// # xsd:string
		datatypeMap.put("java.lang.String", "xsd:string");
		datatypeMap.put("EString", "xsd:string");
		datatypeMap.put("string", "xsd:string");
		// # xsd:normalizedString
		// # xsd:token
		// # xsd:language
		// # xsd:Name
		// # xsd:NCName
		// # xsd:NMTOKEN
		//
		// # xsd:hexBinary
		// # xsd:base64Binary
		//
		// # xsd:dateTime
		datatypeMap.put("java.lang.Date", "xsd:dateTime");
		datatypeMap.put("EDate", "xsd:dateTime");

		datatypeMap.put("java.lang.Char", "xsd:string");
		datatypeMap.put("char", "xsd:string");
		datatypeMap.put("EChar", "xsd:string");
		datatypeMap.put("ECharacterObject", "xsd:string");
	}

	public OWLTransformationHelper() {

	}

	public static String getNamespacePrefix(EPackage ePackage) {
		return ePackage.getName();
	}

	public static String getClassIdentificationIRI(EClassifier eClass) {
		EPackage ePackage = eClass.getEPackage();
		String iri = ePackage.getName();
		while (ePackage.getESuperPackage() != null) {
			ePackage = ePackage.getESuperPackage();
			iri = ePackage.getName() + "_" + iri;
		}
		iri += ":";
		iri += getSimpleClassIdentificationIRI(eClass);
		return iri;
	}

	public static String getSimpleClassIdentificationIRI(EClassifier eClass) {
		return eClass.getName();
	}
	
	public static String getSimpleClassIdentificationIRI(EEnumLiteral literal) {
		return literal.getName();
	}

	public static String getObjectIdentificationIRI(EObject eObject) {
		return "individual_" + getUniqueId(eObject);
	}

	public static String getUniqueId(EObject eObject) {
		Integer id = uniqueIdMap.get(eObject);
		if (id == null) {
			int counter = getCounter(eObject.eResource());
			id = counter++;
			updateCounter(eObject.eResource(), counter);
			uniqueIdMap.put(eObject, id);
		}
		return id.toString();
	}

	private static void updateCounter(Resource eResource, int counter) {
		counters.put(eResource, counter);
	}

	private static int getCounter(Resource eResource) {
		Integer counter = counters.get(eResource);
		if (counter == null) {
			counter = new Integer(0);
			counters.put(eResource, counter);
		}
		return counter;
	}

	public static EObject getEObjectFromIRI(String iri) {
		Integer id = Integer.parseInt(iri.replace("individual_", ""));
		return uniqueIdMap.getKey(id);
	}

	public static String getFeatureIdentificationIRI(EStructuralFeature feature) {
		String iri = getClassIdentificationIRI(feature.getEContainingClass());
		iri += "_" + feature.getName();
		return iri;
	}

	public static String getSimpleFeatureIdentificationIRI(
			EStructuralFeature feature, boolean usePrefix) {
		EClass containingClass = feature.getEContainingClass();
		String prefix = "";
		if (usePrefix) {
			prefix = getSimpleClassIdentificationIRI(containingClass) + "_";
		}
		String iri = prefix + feature.getName();
		return iri;
	}

	public static Ontology getOntology(EPackage ePackage, EObject root) {
		Ontology ontology = packageOntologyMap.get(ePackage);
		if (ontology == null) {
			Ecore2Owl transformation = new Ecore2Owl();
			URI metamodelPath = root.eResource().getURI();
			metamodelPath = metamodelPath.trimSegments(1);
			metamodelPath = metamodelPath.appendSegment(ePackage.getName()
					+ ".mm.owl");

			OntologyDocument transformedMetamodel = transformation.transformMetamodel(ePackage, metamodelPath);

			OwlResource outResource = (OwlResource) root.eResource()
			 .getResourceSet().createResource(metamodelPath);
			
			outResource.getContents().add(transformedMetamodel);
			try {
				outResource.save(Collections.EMPTY_MAP);
				String identifier = metamodelPath.lastSegment();
				ontology = CrossResourceIRIResolver.theInstance()
						.getRemoteLoader().loadOntology(identifier, root);
				packageOntologyMap.put(ePackage, ontology);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ontology;
	}

	public static Map<String, String> getDatatypeMap() {
		return datatypeMap;
	}

	public static String createValidIri(String error) {
		String iriFragment = error.replaceAll("\\s", "_");
		iriFragment = iriFragment.replace('"', '_');
		iriFragment = iriFragment.replace("'", "_");
		iriFragment = iriFragment.replace(".", "_");
		iriFragment = iriFragment.replace(",", "_");
		iriFragment = iriFragment.replace(";", "_");
		iriFragment = iriFragment.replace("!", "_");
		iriFragment = iriFragment.replace("?", "_");
		return iriFragment;
	}

}
