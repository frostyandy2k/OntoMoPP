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
package org.emftext.language.owl.resource.owl.analysis;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.emftext.language.owl.resource.owl.IOwlTokenResolveResult;

public class OwlINVERSETokenResolver extends BooleanAttributeResolver {

	@Override
	public void resolve(String lexem, EStructuralFeature feature,
			IOwlTokenResolveResult result) {
		if (lexem.equalsIgnoreCase("inv")) {
			result.setResolvedToken(true);
			return;
		}
		super.resolve(lexem, feature, result);
	}

	@Override
	public String deResolve(Object value, EStructuralFeature feature,
			EObject container) {
		if ((Boolean) value) {
			return "inv";
		} else {
			return "";
		}

	}
}
