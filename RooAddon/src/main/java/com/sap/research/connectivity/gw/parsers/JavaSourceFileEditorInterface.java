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

import org.springframework.roo.process.manager.MutableFile;

public interface JavaSourceFileEditorInterface {

	abstract void initJavaParser();

	abstract MutableFile getSourceFile();

	abstract ArrayList<JavaSourceField> getGlobalFieldList();

	abstract ArrayList<JavaSourceMethod> getGlobalMethodList();

	abstract ArrayList<String> getImportList();

	abstract void addImports(ArrayList<String> importList);

	abstract void addImport(String importClassName);

	abstract boolean addGlobalField(JavaSourceField field);

	abstract void addMethod(JavaSourceMethod method, boolean overwrite);

	abstract String getFileContent();

}