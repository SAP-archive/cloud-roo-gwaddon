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

import japa.parser.ast.body.MethodDeclaration;

import java.util.ArrayList;

public class JavaSourceMethod {
	
	public String METHOD_STRING;
	
	private String methodName;
	private String methodPrefix;
	private String returnType;
	private String throwsDeclaration;
	private String annotations;
	private ArrayList<String> parameters;
	private String methodBody;

	JavaSourceMethod(String methodName, String methodPrefix, String returnType, String throwsDeclaration, String annotations,
			ArrayList<String> parameters, String methodBody) {
		
		this.methodName = methodName;
		this.methodPrefix = methodPrefix;
		this.returnType = returnType;
		this.throwsDeclaration = throwsDeclaration;
		this.annotations = annotations;
		this.parameters = parameters;
		this.methodBody = methodBody;
		
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public JavaSourceMethod(MethodDeclaration method) {

		this(method.getName(), 
			JavaSourceParserUtils.translateModifiers(method.getModifiers()), 
			method.getType().toString(), 
			JavaSourceParserUtils.translateThrows(method.getThrows()), 
			JavaSourceParserUtils.translateAnnotations(method.getAnnotations()), 
			JavaSourceParserUtils.translateParameters(method.getParameters()), 
			method.getBody().toString().substring(2, method.getBody().toString().length()-1));
	}
	
	
	public String getMethodName() {
		return methodName;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getMethodPrefix() {
		return methodPrefix;
	}
	
	public void setMethodPrefix(String methodPrefix) {
		this.methodPrefix = methodPrefix;
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getReturnType() {
		return returnType;
	}
	
	public void setReturnType(String returnType) {
		this.returnType = returnType;
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getThrowsDeclaration() {
		return throwsDeclaration;
	}
	
	public void setThrowsDeclaration(String throwsDeclaration) {
		this.throwsDeclaration = throwsDeclaration;
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getAnnotations() {
		return annotations;
	}
	
	public void setAnnotations(String annotations) {
		this.annotations = annotations;
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> getParameters() {
		return parameters;
	}
	
	public void setParameters(ArrayList<String> parameters) {
		this.parameters = parameters;
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getMethodBody() {
		return methodBody;
	}
	
	public void setMethodBody(String methodBody) {
		this.methodBody = methodBody;
		try {
			METHOD_STRING = makeMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String makeMethod(String methodName, String methodPrefix, String returnType, String throwsDeclaration, String annotations, 
			ArrayList<String> parameters, String methodBody) throws Exception { 
		
		if (methodName.isEmpty())
			throw new Exception("Method name cannot be empty!");
		
		if (!annotations.isEmpty())
			annotations += "\n";
		
		if (!methodPrefix.isEmpty())
			methodPrefix += " ";
		
		if (!returnType.isEmpty())
			returnType += " ";
		
		String parameterString = "";
		for (String parameter : parameters) {
			parameterString += parameter + ", ";
		}
		if (!parameterString.isEmpty())
			parameterString = parameterString.substring(0, parameterString.lastIndexOf(','));
		
		if (!throwsDeclaration.isEmpty())
			throwsDeclaration += " ";
		
		String returnString = null;

		if (!annotations.isEmpty()){
			returnString = "\n\t" + annotations + "\t" + methodPrefix + returnType + methodName + "(" + parameterString + ")" + 
								  throwsDeclaration + "{\n" + methodBody + "}\n";
		} else {
			returnString = "\n\t" + methodPrefix + returnType + methodName + "(" + parameterString + ")" + 
					  throwsDeclaration + "{\n" + methodBody + "}\n";			
		}
		
		return returnString;
	}

}
