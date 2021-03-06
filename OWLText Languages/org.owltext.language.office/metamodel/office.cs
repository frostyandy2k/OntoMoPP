//*******************************************************************************
// Copyright (c) 2006-2010 
// Software Technology Group, Dresden University of Technology
// 
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0 
// which accompanies this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html
// 
// Contributors:
//   Software Technology Group - TU Dresden, Germany 
//      - initial API and implementation
// ******************************************************************************/

SYNTAXDEF office
FOR <http://emftext.org/office>
START OfficeModel

OPTIONS {	
	licenceHeader ="../../org.dropsbox/licence.txt";
	generateCodeFromGeneratorModel = "true";
	disableLaunchSupport = "true";
	disableDebugSupport = "true";
	overrideBuilder = "false";
}

TOKENS {
	DEFINE TITLE $'Prof.'|'Ph.D.'$;
	
	DEFINE SL_COMMENT $'//'(~('\n'|'\r'|'\uffff'))* $ ;
	DEFINE ML_COMMENT $'/*'.*'*/'$ ;
}

TOKENSTYLES {
	"TITLE" COLOR #7F0055, BOLD;
	
	"SL_COMMENT" COLOR #008000;
	"ML_COMMENT" COLOR #008000;
}

RULES { 
	OfficeModel ::= "officemodel" name[]
					"{" elements* "}" ;
	
	Office ::= "office" name[];
	
	Employee ::= "employee" (title[TITLE])? name[] 
				 "works" "in" worksIn[] 
				( "works" "with" 
				 worksWith[] ("," worksWith[])* )?;
}