package org.emftext.language.petrinets.resource.petrinets.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emftext.language.petrinets.Arc;
import org.emftext.language.petrinets.BasicFunction;
import org.emftext.language.petrinets.BooleanLiteral;
import org.emftext.language.petrinets.DoubleLiteral;
import org.emftext.language.petrinets.Expression;
import org.emftext.language.petrinets.FloatLiteral;
import org.emftext.language.petrinets.Function;
import org.emftext.language.petrinets.FunctionCall;
import org.emftext.language.petrinets.IntegerLiteral;
import org.emftext.language.petrinets.ListFunction;
import org.emftext.language.petrinets.LongLiteral;
import org.emftext.language.petrinets.PGenericType;
import org.emftext.language.petrinets.PList;
import org.emftext.language.petrinets.Parameter;
import org.emftext.language.petrinets.PetriNet;
import org.emftext.language.petrinets.PetrinetsFactory;
import org.emftext.language.petrinets.Place;
import org.emftext.language.petrinets.StringLiteral;
import org.emftext.language.petrinets.Variable;
import org.emftext.language.petrinets.VariableCall;
import org.emftext.language.petrinets.impl.PetrinetsFactoryImpl;

public class FunctionCache {

	private static FunctionCache theInstance;
	private Map<EClassifier, List<Function>> functionCache;
	private Map<String, List<Function>> basicFunctions;

	private FunctionCache() {
		functionCache = new HashMap<EClassifier, List<Function>>();
		basicFunctions = new HashMap<String, List<Function>>();
		initBasicFunctionCache();
	}

	private void initBasicFunctionCache() {
		ResourceSetImpl rs = new ResourceSetImpl();
		URI uri = URI
				.createPlatformPluginURI(
						"/org.emftext.language.petrinets.resource.petrinets/library/standardlib.petrinets",
						true);
		Resource resource = rs.getResource(uri, true);
		if (resource.getContents().size() == 1) {
			PetriNet p = (PetriNet) resource.getContents().get(0);
			EList<Function> functions = p.getFunctions();
			for (Function function : functions) {
				if (function instanceof BasicFunction) {
					EClassifier context = ((BasicFunction) function)
							.getContext();
					addBasicFunction(context.getInstanceClassName(), function);
				}
				if (function instanceof ListFunction) {
					PList eList = PetrinetsFactory.eINSTANCE.createPList();
					addBasicFunction(eList.getName(), function);
				}

			}
		}
	}
	
	private void addBasicFunction(String context, Function function) {
		List<Function> list = basicFunctions.get(context);
		if (list == null) {
			list = new ArrayList<Function>();
			basicFunctions.put(context, list);
		}
		list.add(function);
	}
	
	public static FunctionCache getInstance() {
		if (theInstance == null) {
			theInstance = new FunctionCache();
		}
		return theInstance;
	}

	
	public List<Function> getDeclaredFunctions(Expression expression) {
		List<Function> functions = new ArrayList<Function>();
		EClassifier contextType = getContextType(expression);
	
		
		if (contextType != null) {
			addFunctionsToList(functions, contextType);
		}
		return functions;
	}
	

	public EClassifier getContextType(Expression expression) {
		Expression contextExpression = expression.getPreviousExpression();
		EClassifier contextType = null;
		
		if (contextExpression != null){
			Expression e = (Expression) contextExpression;
			contextType = getType(e);
			return contextType;
		}
		EObject container = expression.eContainer();
		
			while (!(container instanceof Arc) && container != null) {
				container = container.eContainer();
			}
			if (container instanceof Arc) {
				contextType = calculateArcContextType((Arc) container);
			}
		return contextType;
	}

	public void addFunctions(List<Function> functions, EClassifier type) {
		List<Function> basics = basicFunctions.get(type.getInstanceClassName());
		if (basics != null) {
			functions.addAll(basics);
		}
		List<Function> cachedFunctions = functionCache.get(type);
		if (cachedFunctions == null) {
			cachedFunctions = calculateFunctions(type);
			functionCache.put(type, cachedFunctions);
		}
		functions.addAll(cachedFunctions);

	}

	private List<Function> calculateFunctions(EClassifier type) {
		List<Function> functions = new ArrayList<Function>();
		if (type instanceof EClass) {
			EClass cls = (EClass) type;
			EList<EStructuralFeature> eStructuralFeatures = cls
					.getEAllStructuralFeatures();
			for (EStructuralFeature eStructuralFeature : eStructuralFeatures) {
				PetrinetsFactory factory = PetrinetsFactoryImpl.eINSTANCE;
				Function getter = factory.createBasicFunction();
				String name = eStructuralFeature.getName();
				name = name.substring(0, 1).toUpperCase() + name.substring(1);
				getter.setName("get" + name);
				EClassifier eType = eStructuralFeature.getEType();
				if (eStructuralFeature.getUpperBound() == -1) {
					PList elist = PetrinetsFactoryImpl.eINSTANCE.createPList();
					elist.setType(eType);
					eType = elist;
				}
				getter.setType(eType);
				functions.add(getter);

				if (eStructuralFeature.getUpperBound() == 1) {
					Function setter = factory.createBasicFunction();
					setter.setName("set" + name);
					Parameter parameter = factory.createParameter();
					parameter.setType(eType);
					parameter.setName(eStructuralFeature.getName());
					setter.getParameters().add(parameter);
					functions.add(setter);
				}
			}
		}
		return functions;
	}

	

	private void addFunctionsToList(List<Function> functions, EClassifier type) {
		addFunctions(functions, type);
	}

	private EClassifier calculateArcContextType(Arc arc) {
		if (arc.getOut() instanceof Place) {
			Place p = (Place) arc.getOut();
			return p.getType();
		} else if (arc.getIn() instanceof Place) {
			Place p = (Place) arc.getIn();
			return p.getType();
		}
		return null;
	}

	public EClassifier getType(Expression e) {
		if (e == null)
			return null;
		EClassifier type = e.getType();
		if (type == null) {
			type = calculateType(e);
			e.setType(type);
		}
		return type;
	}

	private EClassifier calculateType(Expression e) {
		if (e instanceof VariableCall) {
			VariableCall vc = (VariableCall) e;
			Variable variable = vc.getVariable();
			if (variable.eIsProxy()) {
				resolveAllRequired(variable, e);

			}
			Expression expression = variable.getInitialisation();
			while (expression != null && expression.getNextExpression() != null) {
				expression = expression.getNextExpression();
			}
			EClassifier type = getType(expression);
			expression.setType(type);
			vc.setType(type);
			return type;
		}
		if (e instanceof FunctionCall) {
			FunctionCall fc = (FunctionCall) e;
			Function function = fc.getFunction();
			EClassifier type = null;
			if (function instanceof BasicFunction) {
				type = function.getType();
			}
			if (function instanceof ListFunction) {
				type = function.getType();
				if (type instanceof PGenericType) {
					type = getGenericTypeBinding(e);
				} if (type instanceof PList) {
					PList listType = (PList) type;
					listType.setType(getGenericTypeBinding(e));
					type = listType;
				}
			}
			fc.setType(type);
			return type;
		}
		if (e instanceof StringLiteral) {
			return EcorePackage.eINSTANCE.getEString();
		}
		if (e instanceof IntegerLiteral) {
			return EcorePackage.eINSTANCE.getEInt();
		}
		if (e instanceof DoubleLiteral) {
			return EcorePackage.eINSTANCE.getEDouble();
		}
		if (e instanceof FloatLiteral) {
			return EcorePackage.eINSTANCE.getEFloat();
		}
		if (e instanceof LongLiteral) {
			return EcorePackage.eINSTANCE.getELong();
		}
		if (e instanceof BooleanLiteral) {
			return EcorePackage.eINSTANCE.getEBoolean();
		}
		return null;
	}

	private EClassifier getGenericTypeBinding(Expression e) {
		EClassifier contextType = getContextType(e);
		// PLIST.getContextBinding
		if (contextType instanceof PList) {
			PList listtype = (PList) contextType;
			if (listtype.getType() != null && !(listtype.getType() instanceof PGenericType)) {
				return listtype.getType();
			}
			
		}
		return null;
	}

	private void resolveAllRequired(Variable variable, Expression container) {
		EcoreUtil.resolve(variable, container);
	}
}