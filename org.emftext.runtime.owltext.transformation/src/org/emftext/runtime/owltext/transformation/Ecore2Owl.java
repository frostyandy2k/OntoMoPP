package org.emftext.runtime.owltext.transformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.emftext.language.owl.AbbreviatedXSDStringLiteral;
import org.emftext.language.owl.Annotation;
import org.emftext.language.owl.AnnotationProperty;
import org.emftext.language.owl.Class;
import org.emftext.language.owl.ClassAtomic;
import org.emftext.language.owl.Conjunction;
import org.emftext.language.owl.DataProperty;
import org.emftext.language.owl.DataPropertyFact;
import org.emftext.language.owl.Datatype;
import org.emftext.language.owl.DatatypeReference;
import org.emftext.language.owl.Description;
import org.emftext.language.owl.DisjointClasses;
import org.emftext.language.owl.Feature;
import org.emftext.language.owl.FeatureReference;
import org.emftext.language.owl.FeatureRestriction;
import org.emftext.language.owl.Frame;
import org.emftext.language.owl.Individual;
import org.emftext.language.owl.IndividualsAtomic;
import org.emftext.language.owl.LiteralTarget;
import org.emftext.language.owl.Namespace;
import org.emftext.language.owl.NestedDescription;
import org.emftext.language.owl.ObjectProperty;
import org.emftext.language.owl.ObjectPropertyFact;
import org.emftext.language.owl.ObjectPropertyMax;
import org.emftext.language.owl.ObjectPropertyMin;
import org.emftext.language.owl.ObjectPropertyReference;
import org.emftext.language.owl.Ontology;
import org.emftext.language.owl.OntologyDocument;
import org.emftext.language.owl.OwlFactory;
import org.emftext.language.owl.reasoning.EMFTextPelletReasoner;
import org.emftext.language.owl.resource.owl.analysis.custom.CrossResourceIRIResolver;

public class Ecore2Owl {

	public Ecore2Owl() {
		super();
	}

	private OwlFactory owlFactory = OwlFactory.eINSTANCE;
	private Ontology ontology;
	private HashMap<ENamedElement, Frame> eType2owlClass = new HashMap<ENamedElement, Frame>();
	private HashMap<EStructuralFeature, Feature> references2objectProperties = new HashMap<EStructuralFeature, Feature>();
	private int constraintCounter = 0;
	private HashMap<EClass, List<EClass>> allSupertypes = new HashMap<EClass, List<EClass>>();
	private HashMap<EClass, List<EClass>> allSubtypes = new HashMap<EClass, List<EClass>>();
	private HashMap<EClass, List<EClass>> directSubtypes = new HashMap<EClass, List<EClass>>();
	private EPackage currentMetamodel;
	private HashMap<EPackage, HashMap<ENamedElement, Frame>> importedTypeMaps = new HashMap<EPackage, HashMap<ENamedElement, Frame>>();
	private URI targetURI;

	private void addSubtype(EClass key, EClass subtype) {
		List<EClass> subtypes = this.allSubtypes.get(key);
		if (subtypes == null) {
			subtypes = new ArrayList<EClass>();
			this.allSubtypes.put(key, subtypes);
		}
		subtypes.add(subtype);
	}

	private void addDirectSubtype(EClass directSupertype, EClass subtype) {
		List<EClass> subtypes = this.directSubtypes.get(directSupertype);
		if (subtypes == null) {
			subtypes = new ArrayList<EClass>();
			this.directSubtypes.put(directSupertype, subtypes);
		}
		subtypes.add(subtype);
	}

	private void addSupertypes(EClass key, List<EClass> supertypes) {
		List<EClass> currentSupertypes = this.allSupertypes.get(key);
		if (currentSupertypes == null) {
			currentSupertypes = new ArrayList<EClass>();
			this.allSupertypes.put(key, currentSupertypes);
		}
		currentSupertypes.addAll(supertypes);
	}

	private void addTypeMapping(ENamedElement type, Frame owlClass) {
		eType2owlClass.put(type, owlClass);
	}

	private Frame getTypeMapping(ENamedElement type) {
		Frame frame = eType2owlClass.get(type);
		if (frame == null) {
			EPackage eContainer = (EPackage) type.eResource().getContents()
					.get(0);
			if (eContainer != currentMetamodel) {
				HashMap<ENamedElement, Frame> importedMap = this.importedTypeMaps
						.get(eContainer);
				if (importedMap == null) {
					importedMap = addMetamodelImport(eContainer);
					this.importedTypeMaps.put(eContainer, importedMap);
				}
				frame = importedMap.get(type);
			}
		}

		return frame;
	}

	private void initDatatypes() {

		EList<EClassifier> eClassifiers = EcorePackage.eINSTANCE
				.getEClassifiers();
		for (EClassifier eclassifier : eClassifiers) {
			if (eclassifier instanceof EDataType) {
				EDataType primitive = (EDataType) eclassifier;
				Datatype property = owlFactory.createDatatype();
				String typeName = OWLTransformationHelper.getDatatypeMap().get(
						primitive.getInstanceClassName());
				if (typeName == null)
					typeName = primitive.getName();
				property.setIri(typeName);
				addTypeMapping(primitive, property);

				// ontology.getFrames().add(property);
			}

		}
	}

	public OntologyDocument transformMetamodel(EPackage metamodel, URI targetURI) {
		this.targetURI = targetURI;
		currentMetamodel = metamodel;
		OntologyDocument d = owlFactory.createOntologyDocument();
		ontology = owlFactory.createOntology();
		d.setOntology(ontology);
		initDatatypes();
		initStandardImports(d, metamodel);
		ontology.setUri(metamodel.getNsURI());
		propagateMetamodel(metamodel);
		if (targetURI != null)
			saveOntology(targetURI, d);
		return d;
	}

	private void saveOntology(URI targetURI, OntologyDocument d) {
		Resource documentResource = new ResourceSetImpl()
				.createResource(targetURI);
		documentResource.getContents().add(d);
		try {
			documentResource.save(null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private HashMap<ENamedElement, Frame> addMetamodelImport(
			EPackage importedMetamodel) {
		EPackage rootPackageOfImport = (EPackage) importedMetamodel.eResource()
				.getContents().get(0);

		Ecore2Owl transformation = new Ecore2Owl();
		String importedMetamodelPrefix = importedMetamodel.getNsPrefix();
		URI importedTargetURI = targetURI.trimFileExtension()
				.appendFileExtension(importedMetamodelPrefix)
				.appendFileExtension("owl");
		OntologyDocument importedDocument = transformation.transformMetamodel(
				rootPackageOfImport, importedTargetURI);
		OntologyDocument importingDocument = (OntologyDocument) this.ontology
				.eContainer();

		OwlFactory factory = OwlFactory.eINSTANCE;
		Namespace importNamespace = factory.createNamespace();
		importNamespace.setPrefix(importedMetamodelPrefix + ":");
		importNamespace.setImportedOntology(importedDocument.getOntology());

		importingDocument.getNamespace().add(importNamespace);
		this.ontology.getImports().add(importedDocument.getOntology());

		EList<Frame> frames = importedDocument.getOntology().getFrames();
		for (Frame frame : frames) {

			if (frame.getIri() != null && frame.getIri().length() > 0) {
				Frame declarationFrame = null;
				if (frame instanceof Class) {
					declarationFrame = factory.createClass();
				} else if (frame instanceof ObjectProperty) {
					declarationFrame = factory.createObjectProperty();
				} else if (frame instanceof DataProperty) {
					declarationFrame = factory.createDataProperty();
				} else if (frame instanceof Datatype) {
					declarationFrame = factory.createDatatype();
				} else if (frame instanceof AnnotationProperty) {
					declarationFrame = factory.createAnnotationProperty();
				} else if (frame instanceof Individual) {
					declarationFrame = factory.createIndividual();
				}
				declarationFrame.setIri(importedMetamodelPrefix + ":"
						+ frame.getIri());
				ontology.getFrames().add(declarationFrame);

			}
		}
		return transformation.getTypeMappings();

	}

	private HashMap<ENamedElement, Frame> getTypeMappings() {
		return this.eType2owlClass;

	}

	private void initStandardImports(OntologyDocument d, EPackage metamodel) {

		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.putAll(CrossResourceIRIResolver.standardNamespaces);
		// namespaces.put("owl2xml", "http://www.w3.org/2006/12/owl2-xml#");

		namespaces.put(metamodel.getNsPrefix() + ":", metamodel.getNsURI());
		// addSubpackages(metamodel.getNsPrefix() + ":", metamodel.getNsURI(),
		// namespaces, metamodel.getESubpackages());

		namespaces.put(":", metamodel.getNsURI());
		for (String prefix : namespaces.keySet()) {
			Namespace namespace = owlFactory.createNamespace();
			d.getNamespace().add(namespace);
			namespace.setPrefix(prefix);
			Ontology o = owlFactory.createOntology();
			o.setUri(namespaces.get(prefix));
			namespace.setImportedOntology(o);
		}

	}

	// private void addSubpackages(String nsPrefix, String nsURI,
	// Map<String, String> namespaces, EList<EPackage> eSubpackages) {
	// for (EPackage ePackage : eSubpackages) {
	// namespaces.put(nsPrefix + "_" + ePackage.getNsPrefix(), nsURI);
	// addSubpackages(nsPrefix + "_" + ePackage.getNsPrefix(), nsURI,
	// namespaces, ePackage.getESubpackages());
	// }
	// }

	public OntologyDocument transform(Collection<EObject> eObjects) {
		OntologyDocument d = owlFactory.createOntologyDocument();
		ontology = owlFactory.createOntology();
		initDatatypes();
		d.setOntology(ontology);

		for (EObject eObject : eObjects) {
			// TODO propagate each metamodel only once
			EPackage metamodel = eObject.eClass().getEPackage();
			while (metamodel.getESuperPackage() != null) {
				metamodel = metamodel.getESuperPackage();
			}
			propagateMetamodel(metamodel);
			propagateInstances(eObject);
		}
		return d;
	}

	private void propagateInstances(EObject eo) {
		HashMap<EObject, Individual> eobject2individual = new HashMap<EObject, Individual>();
		TreeIterator<EObject> allContents = eo.eAllContents();
		while (allContents.hasNext()) {
			EObject instance = allContents.next();
			Individual individual = owlFactory.createIndividual();
			individual.setIri("eid_" + instance.hashCode());
			eobject2individual.put(instance, individual);

			Class metaclass = (Class) getTypeMapping(instance.eClass());
			ClassAtomic metaclassAtomic = owlFactory.createClassAtomic();
			metaclassAtomic.setClazz(metaclass);
			individual.getTypes().add(metaclassAtomic);
			ontology.getFrames().add(individual);

		}
		allContents = eo.eAllContents();
		while (allContents.hasNext()) {
			EObject instance = allContents.next();
			Individual contextIndividual = eobject2individual.get(instance);
			EList<EStructuralFeature> allStructuralFeatures = instance.eClass()
					.getEAllStructuralFeatures();
			for (EStructuralFeature structuralFeature : allStructuralFeatures) {
				Object get = instance.eGet(structuralFeature);
				if (get instanceof EObject) {
					Individual i = eobject2individual.get(get);
					ObjectPropertyFact fact = owlFactory
							.createObjectPropertyFact();
					fact.setObjectProperty((ObjectProperty) references2objectProperties
							.get(structuralFeature));
					fact.setIndividual(i);
					contextIndividual.getFacts().add(fact);
				} else if (get instanceof String) {
					DataPropertyFact fact = owlFactory.createDataPropertyFact();
					fact.setDataProperty((DataProperty) references2objectProperties
							.get(structuralFeature));
					AbbreviatedXSDStringLiteral l = owlFactory
							.createAbbreviatedXSDStringLiteral();
					l.setValue((String) get);
					fact.setLiteral(l);
					contextIndividual.getFacts().add(fact);
				}
			}
		}
	}

	private void propagateMetamodel(EPackage metamodel) {
		TreeIterator<EObject> allContents = metamodel.eAllContents();
		while (allContents.hasNext()) {
			EObject elem = allContents.next();
			if (elem instanceof EClass) {
				EClass eClass = (EClass) elem;
				transformEClass(eClass);
				EList<EClass> superTypes = eClass.getEAllSuperTypes();
				addSupertypes(eClass, superTypes);
				for (EClass supertype : superTypes) {
					addSubtype(supertype, eClass);
				}
				EList<EClass> directSuperTypes = eClass.getESuperTypes();
				for (EClass directSupertype : directSuperTypes) {
					addDirectSubtype(directSupertype, eClass);
				}
			} else if (elem instanceof EEnum)
				transformEEnum((EEnum) elem);
			else if (elem instanceof EDataType)
				transformEDatatype((EDataType) elem);

		}
		allContents = metamodel.eAllContents();
		while (allContents.hasNext()) {
			EObject elem = allContents.next();
			if (elem instanceof EReference)
				transformEReference((EReference) elem);
			if (elem instanceof EAttribute)
				transformEAttribute((EAttribute) elem);
		}

		Set<ENamedElement> keySet = eType2owlClass.keySet();
		for (ENamedElement classifier : keySet) {
			if (classifier instanceof EClass) {
				EClass eclass = (EClass) classifier;
				Class owlClass = (Class) getTypeMapping(eclass);
				EList<EClass> superTypes = eclass.getESuperTypes();
				Conjunction supertypes = owlFactory.createConjunction();

				for (EClass superclass : superTypes) {
					Class superframe = (Class) getTypeMapping(superclass);
					ClassAtomic superClassAtomic = owlFactory
							.createClassAtomic();
					superClassAtomic.setClazz(superframe);
					supertypes.getPrimaries().add(superClassAtomic);
				}

				addCardinalityConstraintsClasses(eclass, owlClass);
				if (supertypes.getPrimaries().size() == 0) {
					Class owlThing = (Class) owlFactory.createClass();
					owlThing.setIri("owl:Thing");
					ClassAtomic superClassAtomic = owlFactory
							.createClassAtomic();
					superClassAtomic.setClazz(owlThing);
					supertypes.getPrimaries().add(superClassAtomic);
				}

				addOwlDefinitionSupertypes(owlClass, eclass, ontology);
				owlClass.getSuperClassesDescriptions().add(supertypes);
				addUserDefinedConstraints(eclass, owlClass);

			}

		}

		List<ENamedElement> allNamedElements = new ArrayList<ENamedElement>();
		allNamedElements.addAll(eType2owlClass.keySet());

		Set<EClass> supertypes = this.directSubtypes.keySet();
		for (EClass supertype : supertypes) {
			List<EClass> subtypes = this.directSubtypes.get(supertype);
			allNamedElements.removeAll(subtypes);
			DisjointClasses disjointClasses = owlFactory
					.createDisjointClasses();
			ontology.getFrames().add(disjointClasses);
			for (EClass subtype : subtypes) {
				Class subframe = (Class) getTypeMapping(subtype);
				ClassAtomic subclassAtomic = owlFactory.createClassAtomic();
				subclassAtomic.setClazz(subframe);
				disjointClasses.getDescriptions().add(subclassAtomic);
			}
		}

		// make remaining types disjoint
		DisjointClasses disjointClasses = owlFactory.createDisjointClasses();
		ontology.getFrames().add(disjointClasses);
		for(ENamedElement eNamedElement : allNamedElements) {
			if (eNamedElement instanceof EClass) {
				EClass cls = (EClass) eNamedElement;
				Class subframe = (Class) getTypeMapping(cls);
				ClassAtomic subclassAtomic = owlFactory.createClassAtomic();
				subclassAtomic.setClazz(subframe);
				disjointClasses.getDescriptions().add(subclassAtomic);
			}

		}

	}

	private void addOwlDefinitionSupertypes(Class owlClass, EClass eclass,
			Ontology ontology) {
		EList<EAnnotation> eAnnotations = eclass.getEAnnotations();
		for (EAnnotation eAnnotation : eAnnotations) {
			if (eAnnotation.getSource().equals(
					OWLTransformationConstants.OWL_DEFINITION)
					&& eAnnotation.getDetails().size() > 0) {
				EMap<String, String> details = eAnnotation.getDetails();

				Entry<String, String> definitionEntry = eAnnotation
						.getDetails().get(0);
				String definition = definitionEntry.getValue();
				String className = definitionEntry.getKey();

				Class typeClass = owlFactory.createClass();
				typeClass.setIri(className);
				ontology.getFrames().add(typeClass);

				Conjunction conjunction = owlFactory.createConjunction();
				typeClass.getEquivalentClassesDescriptions().add(conjunction);

				ClassAtomic ca = owlFactory.createClassAtomic();
				ca.setClazz(owlClass);

				conjunction.getPrimaries().add(ca);
				OWLParsingHelper oph = new OWLParsingHelper();
				Description definitionDescription = oph.parseSubClassOf(
						definition, eclass.eResource());
				conjunction.getPrimaries().add(definitionDescription);

				for (Entry<String, String> entry : details.subList(1,
						details.size())) {
					String error = entry.getKey();
					String constraint = entry.getValue();

					Description constraintDescription = oph.parseSubClassOf(
							constraint, eclass.eResource());
					if (constraintDescription != null) {
						NestedDescription nestedDescription = owlFactory
								.createNestedDescription();
						nestedDescription.setDescription(constraintDescription);
						nestedDescription.setNot(true);
						String iriFragment = OWLTransformationHelper
								.createValidIri(error);
						String iri = "_constraint_" + iriFragment;
						createConstraintClass(typeClass, iri, error,
								nestedDescription);
					}
				}
			}
		}

	}

	private void addUserDefinedConstraints(EClass eclass, Class owlClass) {
		EList<EAnnotation> eAnnotations = eclass.getEAnnotations();
		for (EAnnotation eAnnotation : eAnnotations) {
			if (eAnnotation.getSource().equals(
					OWLTransformationConstants.OWL_CONSTRAINT)) {
				EMap<String, String> details = eAnnotation.getDetails();
				for (Entry<String, String> entry : details) {
					String error = entry.getKey();
					String constraint = entry.getValue();
					OWLParsingHelper oph = new OWLParsingHelper();
					Description constraintDescription = oph.parseSubClassOf(
							constraint, eclass.eResource());
					if (constraintDescription != null) {
						NestedDescription nestedDescription = owlFactory
								.createNestedDescription();
						nestedDescription.setDescription(constraintDescription);
						nestedDescription.setNot(true);
						String iriFragment = OWLTransformationHelper
								.createValidIri(error);
						String iri = "_constraint_" + iriFragment;
						createConstraintClass(owlClass, iri, error,
								nestedDescription);
					}
				}
			}
		}
	}

	private void addCardinalityConstraintsClasses(EClass eclass,
			Class constrainedClass) {
		EList<EAttribute> attributes = eclass.getEAttributes();
		for (EAttribute attribute : attributes) {
			// DataProperty dataProperty = (DataProperty)
			Feature f = references2objectProperties.get(attribute);
			if (f instanceof DataProperty)// EAttributes
				addCardinalityConstraintsClassesForEAttributes(attribute,
						(DataProperty) f, constrainedClass);
			if (f instanceof ObjectProperty)// EEnum
				addCardinalityConstraintsClassesForEReferenceAndEEnum(
						attribute, (ObjectProperty) f, constrainedClass);
		}

		EList<EReference> references = eclass.getEReferences();
		for (EReference reference : references) {
			ObjectProperty objectProperty = (ObjectProperty) references2objectProperties
					.get(reference);

			addCardinalityConstraintsClassesForEReferenceAndEEnum(reference,
					objectProperty, constrainedClass);

		}
	}

	private void addCardinalityConstraintsClassesForEAttributes(
			EAttribute attribute, DataProperty dataProperty,
			Class constrainedClass) {
		if (attribute.getLowerBound() != 0) {
			ObjectPropertyMin minRestriction = owlFactory
					.createObjectPropertyMin();
			setFeature(minRestriction, dataProperty);
			minRestriction.setValue(attribute.getLowerBound());
			DatatypeReference primary = owlFactory.createDatatypeReference();

			Datatype dataType = (Datatype) getTypeMapping(attribute.getEType());
			primary.setTheDatatype(dataType);
			minRestriction.setDataPrimary(primary);

			NestedDescription nestedDescription = owlFactory
					.createNestedDescription();
			nestedDescription.setDescription(minRestriction);
			nestedDescription.setNot(true);

			String constraintID = "_min_" + attribute.getLowerBound() + "_"
					+ attribute.getName();
			String errorMsg = "The minimal cardinality of '"
					+ attribute.getLowerBound() + "' for attribute '"
					+ attribute.getName() + "' is not satisfied.";

			createConstraintClass(constrainedClass, constraintID, errorMsg,
					nestedDescription);
		}

		if (attribute.getUpperBound() != -1) {
			ObjectPropertyMax maxRestriction = owlFactory
					.createObjectPropertyMax();
			setFeature(maxRestriction, dataProperty);
			maxRestriction.setValue(attribute.getUpperBound());
			DatatypeReference primary = owlFactory.createDatatypeReference();

			Datatype dataType = (Datatype) getTypeMapping(attribute.getEType());
			primary.setTheDatatype(dataType);
			maxRestriction.setDataPrimary(primary);

			NestedDescription nestedDescription = owlFactory
					.createNestedDescription();
			nestedDescription.setDescription(maxRestriction);
			nestedDescription.setNot(true);

			String iri = "_max_" + attribute.getUpperBound() + "_"
					+ attribute.getName();
			String errorMsg = "The maximal cardinality of '"
					+ attribute.getUpperBound() + "' for attribute '"
					+ attribute.getName() + "' is not satisfied.";

			createConstraintClass(constrainedClass, iri, errorMsg,
					nestedDescription);
		}
	}

	private void addCardinalityConstraintsClassesForEReferenceAndEEnum(
			EStructuralFeature structuralFeature,
			ObjectProperty objectProperty, Class constrainedClass) {
		if (structuralFeature.getLowerBound() != 0) {
			ObjectPropertyMin minRestriction = owlFactory
					.createObjectPropertyMin();
			setFeature(minRestriction, objectProperty);
			ClassAtomic classAtomic = owlFactory.createClassAtomic();
			classAtomic.setClazz((Class) getTypeMapping(structuralFeature
					.getEType()));
			minRestriction.setPrimary(classAtomic);
			minRestriction.setValue(structuralFeature.getLowerBound());

			NestedDescription nestedDescription = owlFactory
					.createNestedDescription();
			nestedDescription.setDescription(minRestriction);
			nestedDescription.setNot(true);
			String iri = "_min_" + structuralFeature.getLowerBound() + "_"
					+ structuralFeature.getName();

			String errorMsg = "The minimal cardinality of '"
					+ structuralFeature.getLowerBound() + "' for reference '"
					+ structuralFeature.getName() + "' is not satisfied.";

			createConstraintClass(constrainedClass, iri, errorMsg,
					nestedDescription);
		}

		if (structuralFeature.getUpperBound() != -1) {
			ObjectPropertyMax maxRestriction = owlFactory
					.createObjectPropertyMax();
			setFeature(maxRestriction, objectProperty);
			ClassAtomic classAtomic = owlFactory.createClassAtomic();
			classAtomic.setClazz((Class) getTypeMapping(structuralFeature
					.getEType()));
			maxRestriction.setPrimary(classAtomic);

			maxRestriction.setValue(structuralFeature.getUpperBound());

			NestedDescription nestedDescription = owlFactory
					.createNestedDescription();
			nestedDescription.setDescription(maxRestriction);
			nestedDescription.setNot(true);

			String iri = "_max_" + structuralFeature.getUpperBound() + "_"
					+ structuralFeature.getName();
			String errorMsg = "The maximal cardinality of '"
					+ structuralFeature.getUpperBound() + "' for reference '"
					+ structuralFeature.getName() + "' is not satisfied.";

			createConstraintClass(constrainedClass, iri, errorMsg,
					nestedDescription);
		}

		if ((structuralFeature instanceof EReference)
				&& (((EReference) structuralFeature).getEOpposite() != null)) {
			EReference eOpposite = ((EReference) structuralFeature)
					.getEOpposite();
			ObjectProperty oppositeProperty = (ObjectProperty) references2objectProperties
					.get(eOpposite);
			ObjectPropertyReference ref = owlFactory
					.createObjectPropertyReference();
			ref.setObjectProperty(oppositeProperty);
			objectProperty.getInverseProperties().add(ref);

			ObjectPropertyReference oppRef = owlFactory
					.createObjectPropertyReference();
			oppRef.setObjectProperty(objectProperty);
			oppositeProperty.getInverseProperties().add(oppRef);

		}
	}

	private void createConstraintClass(Class constrainedClass,
			String iriSuffix, String errorMsg, Description constraintDescription) {

		Class constraintClass = owlFactory.createClass();
		ontology.getFrames().add(constraintClass);
		constraintClass.setIri(EMFTextPelletReasoner.CONSTRAINT_CLASS_PREFIX
				+ constrainedClass.getIri() + +constraintCounter++ + iriSuffix);

		Annotation annotation = owlFactory.createAnnotation();
		AbbreviatedXSDStringLiteral stringLiteral = owlFactory
				.createAbbreviatedXSDStringLiteral();
		stringLiteral.setValue(errorMsg);
		LiteralTarget lt = owlFactory.createLiteralTarget();
		lt.setLiteral(stringLiteral);
		annotation.getTarget().add(lt);
		AnnotationProperty annotationProperty = owlFactory
				.createAnnotationProperty();
		annotationProperty
				.setIri(EMFTextPelletReasoner.CONSTRAINT_PROPERTY_NAME);
		annotation.getAnnotationProperty().add(annotationProperty);
		constraintClass.getAnnotations().add(annotation);

		ClassAtomic constrainedClassAtomic = owlFactory.createClassAtomic();
		constrainedClassAtomic.setClazz(constrainedClass);
		Conjunction and = owlFactory.createConjunction();
		and.getPrimaries().add(constrainedClassAtomic);
		constraintClass.getEquivalentClassesDescriptions().add(and);

		and.getPrimaries().add(constraintDescription);
	}

	private void setFeature(FeatureRestriction restriction, Feature feature) {
		FeatureReference reference = OwlFactory.eINSTANCE
				.createFeatureReference();
		reference.setFeature(feature);
		restriction.setFeatureReference(reference);
	}

	private void transformEAttribute(EAttribute elem) {
		if (elem.getEAttributeType() instanceof EEnum) { // EEnum
			ObjectProperty o = owlFactory.createObjectProperty();
			o.setIri(OWLTransformationHelper
					.getSimpleFeatureIdentificationIRI(elem));
			ontology.getFrames().add(o);

			Class rangeClass = (Class) getTypeMapping(elem.getEType());
			ClassAtomic rangeClassAtomic = owlFactory.createClassAtomic();
			rangeClassAtomic.setClazz(rangeClass);
			o.getPropertyRange().add(rangeClassAtomic);

			Class domainClass = (Class) getTypeMapping(elem
					.getEContainingClass());
			ClassAtomic domainClassAtomic = owlFactory.createClassAtomic();
			domainClassAtomic.setClazz(domainClass);
			o.getPropertyDomain().add(domainClassAtomic);

			// is checked using cardinality constraints
			// if (elem.getUpperBound() == 1)
			// o.getCharacteristics().add(Characteristic.FUNCTIONAL);

			references2objectProperties.put(elem, o);

		} else {// EAttribute
			DataProperty d = owlFactory.createDataProperty();
			ontology.getFrames().add(d);
			d.setIri(OWLTransformationHelper
					.getSimpleFeatureIdentificationIRI(elem));
			Class domainClass = (Class) getTypeMapping(elem
					.getEContainingClass());
			ClassAtomic domainClassAtomic = owlFactory.createClassAtomic();
			domainClassAtomic.setClazz(domainClass);
			d.getDomain().add(domainClassAtomic);

			elem.getEAttributeType();
			DatatypeReference dtr = owlFactory.createDatatypeReference();
			Datatype dataType = (Datatype) getTypeMapping(elem
					.getEAttributeType());
			dtr.setTheDatatype(dataType);
			d.getRange().add(dtr);

			// is checked using cardinality constraints
			// if (elem.getUpperBound() == 1)
			// d.setCharacteristic(Characteristic.FUNCTIONAL);
			references2objectProperties.put(elem, d);
		}
	}

	private void transformEReference(EReference elem) {
		ObjectProperty o = owlFactory.createObjectProperty();
		o.setIri(OWLTransformationHelper
				.getSimpleFeatureIdentificationIRI(elem));
		ontology.getFrames().add(o);

		Class rangeClass = (Class) getTypeMapping(elem.getEType());
		ClassAtomic rangeClassAtomic = owlFactory.createClassAtomic();
		rangeClassAtomic.setClazz(rangeClass);
		o.getPropertyRange().add(rangeClassAtomic);

		Class domainClass = (Class) getTypeMapping(elem.getEContainingClass());
		ClassAtomic domainClassAtomic = owlFactory.createClassAtomic();
		domainClassAtomic.setClazz(domainClass);
		o.getPropertyDomain().add(domainClassAtomic);

		// is checked using cardinality constraints
		// if (elem.getUpperBound() == 1)
		// o.getCharacteristics().add(Characteristic.FUNCTIONAL);

		references2objectProperties.put(elem, o);
	}

	private void transformEEnum(EEnum elem) {

		Class d = owlFactory.createClass();
		ontology.getFrames().add(d);
		d.setIri(OWLTransformationHelper.getSimpleClassIdentificationIRI(elem));
		addTypeMapping(elem, d);

		IndividualsAtomic description = owlFactory.createIndividualsAtomic();
		d.getEquivalentClassesDescriptions().add(description);
		EList<EEnumLiteral> literals = elem.getELiterals();
		for (EEnumLiteral eEnumLiteral : literals) {
			transformEEnumLiteral(eEnumLiteral);
			Individual individual = (Individual) getTypeMapping(eEnumLiteral);
			description.getIndividuals().add(individual);
		}

	}

	private void transformEEnumLiteral(EEnumLiteral eEnumLiteral) {
		Individual individual = owlFactory.createIndividual();
		individual.setIri(eEnumLiteral.toString());
		addTypeMapping(eEnumLiteral, individual);
	}

	private void transformEDatatype(EDataType elem) {
		Datatype d = owlFactory.createDatatype();
		ontology.getFrames().add(d);
		d.setIri(OWLTransformationHelper.getSimpleClassIdentificationIRI(elem));
		addTypeMapping(elem, d);

	}

	private void transformEClass(EClass elem) {
		Class c = owlFactory.createClass();
		ontology.getFrames().add(c);
		c.setIri(OWLTransformationHelper.getSimpleClassIdentificationIRI(elem));
		addTypeMapping(elem, c);
	}

}
