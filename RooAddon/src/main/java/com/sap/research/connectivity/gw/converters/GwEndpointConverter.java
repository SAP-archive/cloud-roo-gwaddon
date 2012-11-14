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

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

import com.sap.research.connectivity.gw.GWOperationsUtils;
import com.sap.research.connectivity.gw.GwEndpoint;

@Component
@Service

public class GwEndpointConverter extends GWOperationsUtils implements Converter<GwEndpoint> {

	public GwEndpoint convertFromText(String value, final Class<?> requiredType,
            final String optionContext) {
		
		return new GwEndpoint(value);
	}

	public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, String existingData,
            final String optionContext, final MethodTarget target) {

		existingData = StringUtils.stripToEmpty(existingData);

		// Search for all files matching the pattern *_metadata.xml (we assume that the connectivity class corresponding to the endpoint xml 
		// contains the same name (e.g. gw1.java and gw1_metadata.xml)
		SortedSet<FileDetails> files = new TreeSet<FileDetails>();
		if (!optionContext.isEmpty()) {
			files = fileManager.findMatchingAntPath(getSubPackagePath(optionContext) + SEPARATOR + "*_metadata.xml");
		}
		
		String filePath = "", nameSpace = "";
		for (FileDetails f : files) {
			filePath = f.getCanonicalPath();
			nameSpace = filePath.substring(filePath.lastIndexOf(SEPARATOR) + 1, filePath.lastIndexOf("_metadata.xml"));
			completions.add(new Completion(nameSpace));			
		}
		
        return false;
    }

	public boolean supports(Class<?> requiredType, String optionContext) {
		return GwEndpoint.class.isAssignableFrom(requiredType);
	}
}
