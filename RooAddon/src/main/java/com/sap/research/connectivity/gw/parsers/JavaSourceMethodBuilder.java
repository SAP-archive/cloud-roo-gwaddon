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

public class JavaSourceMethodBuilder {

	private String methodName = "";
	private String methodPrefix = "";
	private String returnType = "";
	private String throwsDeclaration = "";
	private String annotations = "";
	private ArrayList<String> parameters = new ArrayList<String>();
	private String methodBody = "";
	
	public JavaSourceMethodBuilder() { }
	
	public JavaSourceMethod build() {
		return new JavaSourceMethod(methodName, methodPrefix, returnType, throwsDeclaration, annotations, parameters, methodBody);
	}
	
	public JavaSourceMethodBuilder methodName(String methodName) {
		this.methodName = methodName;
		return this;
	}
	
	public JavaSourceMethodBuilder methodPrefix(String methodPrefix) {
		this.methodPrefix = methodPrefix;
		return this;
	}
	
	public JavaSourceMethodBuilder returnType(String returnType) {
		this.returnType = returnType;
		return this;
	}
	
	public JavaSourceMethodBuilder throwsDeclaration(String throwsDeclaration) {
		this.throwsDeclaration = throwsDeclaration;
		return this;
	}
	
	public JavaSourceMethodBuilder annotations(String annotations) {
		this.annotations = annotations;
		return this;
	}
	
	public JavaSourceMethodBuilder parameters(ArrayList<String> parameters) {
		this.parameters = parameters;
		return this;
	}
	
	public JavaSourceMethodBuilder methodBody(String methodBody) {
		this.methodBody = methodBody;
		return this;
	}
	
}
