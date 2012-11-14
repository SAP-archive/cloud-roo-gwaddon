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

public class JavaSourceFieldBuilder {
	
	private String fieldPrefix = "";
	private String fieldType = "";
	private String fieldName = "";
	private String fieldValue = "";
	private String fieldAnnotations = "";
	
	public JavaSourceFieldBuilder() { }
	
	public JavaSourceField build() {
		return new JavaSourceField(fieldPrefix, fieldType, fieldName, fieldValue, fieldAnnotations);
	}
	
	public JavaSourceFieldBuilder fieldPrefix(String val) {
		fieldPrefix = val;
		return this;
	}
	
	public JavaSourceFieldBuilder fieldType(String val) {
		fieldType = val;
		return this;
	}
	
	public JavaSourceFieldBuilder fieldName(String val) {
		fieldName = val;
		return this;
	}
	
	public JavaSourceFieldBuilder fieldValue(String val) {
		fieldValue = val;
		return this;
	}
	
	public JavaSourceFieldBuilder fieldAnnotations(String val) {
		fieldAnnotations = val;
		return this;
	}	
	
}
