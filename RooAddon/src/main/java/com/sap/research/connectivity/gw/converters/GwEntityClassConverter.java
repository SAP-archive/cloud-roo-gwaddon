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

package com.sap.research.connectivity.gw.converters;

import java.io.File;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.file.monitor.event.FileDetails;

import com.sap.research.connectivity.gw.GWOperationsUtils;
import com.sap.research.connectivity.gw.GwEntityClass;
import com.sap.research.connectivity.gw.GwUtils;
import com.sap.research.connectivity.gw.parsers.JavaSourceFileEditor;

@Component
@Service

public class GwEntityClassConverter extends GWOperationsUtils implements Converter<GwEntityClass> {
	
	private static final char SEPARATOR = File.separatorChar;

	public GwEntityClass convertFromText(String value, Class<?> targetType,
			String optionContext) {
		return new GwEntityClass(value);
	}

	public boolean getAllPossibleValues(List<Completion> completions,
			Class<?> targetType, String existingData, String optionContext,
			MethodTarget target) {

        //Retrieve all the .java files using provided option context (sub package folder)
		SortedSet<FileDetails> files = new TreeSet<FileDetails>();
		if (!optionContext.isEmpty()) {
			files = fileManager.findMatchingAntPath(getSubPackagePath(optionContext) + SEPARATOR + "*.java");
		}

        //Iterate through each class and include only those classes which have gateway connectivity for auto completion
		for (FileDetails f : files) {
			
			String filePath = f.getCanonicalPath();
			String entityClass = filePath.substring(filePath.lastIndexOf(SEPARATOR) + 1, filePath.length());
			
			String entityName = entityClass.substring(0, entityClass.indexOf(".java"));

			JavaSourceFileEditor entityClassFile = getJavaFileEditor("domain", entityName); 
			
			String nameSpace = GwUtils.getNamespaceFromClass(entityClassFile);
			
			if(!nameSpace.isEmpty())
				completions.add(new Completion(entityName));			
		}

        return false;
	}
	
	public boolean supports(Class<?> type, String optionContext) {
		return GwEntityClass.class.isAssignableFrom(type);
	}

}
