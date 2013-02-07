/*
 * Copyright 2012 SAP AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sap.research.connectivity.gw.parsers;

import java.util.ArrayList;
import java.util.List;

import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;

public final class JavaSourceParserUtils {

	public static String translateModifiers(int modifiers) {
		String returnString = "";
        if (ModifierSet.isPrivate(modifiers)) {
            returnString += "private ";
        }
        if (ModifierSet.isProtected(modifiers)) {
            returnString += "protected ";
        }
        if (ModifierSet.isPublic(modifiers)) {
            returnString += "public ";
        }
        if (ModifierSet.isAbstract(modifiers)) {
            returnString += "abstract ";
        }
        if (ModifierSet.isStatic(modifiers)) {
            returnString += "static ";
        }
        if (ModifierSet.isFinal(modifiers)) {
            returnString += "final ";
        }
        if (ModifierSet.isNative(modifiers)) {
            returnString += "native ";
        }
        if (ModifierSet.isStrictfp(modifiers)) {
            returnString += "strictfp ";
        }
        if (ModifierSet.isSynchronized(modifiers)) {
            returnString += "synchronized ";
        }
        if (ModifierSet.isTransient(modifiers)) {
            returnString += "transient ";
        }
        if (ModifierSet.isVolatile(modifiers)) {
            returnString += "volatile ";
        }
        
        if (!returnString.isEmpty())
        	returnString = returnString.substring(0, returnString.length()-1);
        
        return returnString;
    }
	
	public static String translateThrows(List<NameExpr> throwsExpr) {
		String throwsTranslated = "";
		if (throwsExpr != null)
		{
			throwsTranslated = "throws ";
		    for (NameExpr t : throwsExpr) {
		    	throwsTranslated += t.toString();
		    }
		}
		return throwsTranslated;
	}

	public static String translateAnnotations(List<AnnotationExpr> annotations) {
		String annotationTranslated = "";
		if (annotations != null){
			for (AnnotationExpr ann : annotations) {
				annotationTranslated += ann.toString() + "\n\t";
			}
		}
		if (annotationTranslated.endsWith("\n\t"))
			annotationTranslated = annotationTranslated.substring(0, annotationTranslated.length()-2);
		return annotationTranslated;
	}

	public static ArrayList<String> translateParameters(List<Parameter> parameters) {
		ArrayList<String> parametersTranslated = new ArrayList<String>();
		if (parameters != null){
			for (Parameter param : parameters) {
				parametersTranslated.add(param.toString());
			}
		}
		return parametersTranslated;
	}

	public static String translateFieldName(List<VariableDeclarator> variables) {
		
		for (VariableDeclarator var : variables) {
			return var.getId().toString(); 
		}
		
		return "";
	}
	
	public static String translateFieldValue(List<VariableDeclarator> variables) {
		
		for (VariableDeclarator var : variables) {
			if (var.getInit() != null)
				return var.getInit().toString();
		}
		
		return "";
	}
}
