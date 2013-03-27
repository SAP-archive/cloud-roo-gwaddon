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

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.roo.process.manager.MutableFile;

public class JavaSourceFileEditor implements JavaSourceFileEditorInterface {

	private static final char SEPARATOR = File.separatorChar;
    
    /*
     * The name of the class (we suppose that we are editing a .java file containing a class having the same name as the file name)
     */
    public String CLASS_NAME; 
    /*
     * The header of the class (annotations and declaration, without {)
     */
    private String classDeclaration;
    
	private MutableFile sourceFile;
	
	private ArrayList<JavaSourceField> globalFieldList;
	private ArrayList<JavaSourceMethod> globalMethodList;
	private ArrayList<String> importList;
	
	/*
	 * Needed by the JAVA Parser class, so we can get already existing content in the file
	 */
	private CompilationUnit compilationUnit;

	
	public JavaSourceFileEditor(MutableFile sourceFile) {
		this.sourceFile = sourceFile;
		
		final String canonicalPath = sourceFile.getCanonicalPath();
		this.CLASS_NAME = canonicalPath.substring(canonicalPath.lastIndexOf(SEPARATOR) + 1, canonicalPath.lastIndexOf("."));
		
		importList = new ArrayList<String>();
		globalFieldList = new ArrayList<JavaSourceField>();
		globalMethodList = new ArrayList<JavaSourceMethod>();
		
		/*
		 * Here we parse the java file and get the already existing imports, methods and fields
		 */
		if (!sourceFile.toString().isEmpty())
			initJavaParser();
	}
	
	
	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#initJavaParser()
	 */
	public void initJavaParser() {
        // creates an input stream for the file to be parsed
        InputStream inputStream = sourceFile.getInputStream();

        try {
            // parse the file
        	compilationUnit = JavaParser.parse(inputStream);
        } catch (ParseException e) {
        	System.out.println("Java Parser could not be initialized!");
        	e.printStackTrace();
        	return;
        }	finally {
            IOUtils.closeQuietly(inputStream);
        }
        
        initImports();
        initVarsAndMethods();
    }


	private void initImports() {
		List<ImportDeclaration> imports = compilationUnit.getImports();
        for (ImportDeclaration imp : imports) {
        	importList.add(imp.getName().toString());
        }
	}

	/*
	 * We are translating the methods and fields from the java parser format to our format
	 */
	private void initVarsAndMethods() {
		List<TypeDeclaration> types = compilationUnit.getTypes();
        for (TypeDeclaration type : types) {
            List<BodyDeclaration> members = type.getMembers();
        	//We are dealing with class declaration
            StringBuffer s = new StringBuffer(type.toString());
            classDeclaration = type.toString().substring(0, s.indexOf("{", type.toString().indexOf(type.getName())));
            //log.severe(type.toString().substring(0, type.toString().indexOf(type.getName()) + type.getName().length()));
            for (BodyDeclaration member : members) {
            	//We are dealing with methods
                if (member instanceof MethodDeclaration) {
                	MethodDeclaration method = (MethodDeclaration) member;
                    JavaSourceMethod methodTranslated = new JavaSourceMethod(method);
                    globalMethodList.add(methodTranslated);
                }
                else
                //We are dealing with fields
                if (member instanceof FieldDeclaration) {
                	FieldDeclaration field = (FieldDeclaration) member;
                	JavaSourceField fieldTranslated = new JavaSourceField(field);
                	globalFieldList.add(fieldTranslated);
                }
            }
        }
	}

	
	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#addGlobalVariable(com.sap.research.connectivity.JavaSourceField)
	 */
	public boolean addGlobalField(JavaSourceField field) {
		
		if (fieldExists(field.getFieldName()))
			return false;
		
		globalFieldList.add(field);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#addGlobalVariable(com.sap.research.connectivity.JavaSourceField)
	 */
	public void addGlobalField(JavaSourceField field, boolean overwrite) {
		
		JavaSourceField currentVariable = fieldExists(field);
		if (currentVariable != null) {
			if (!overwrite)
				return;
			else {
					currentVariable = field;
					return;
			}
		}
		
		globalFieldList.add(field);
	}
	

	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#addImport(java.lang.String)
	 */
	public void addImport(String importClassName) {
		if (!importExists(importClassName))
			importList.add(importClassName);
	}
	
	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#addImports(java.util.ArrayList)
	 */
	public void addImports(ArrayList<String> importList) {
		for (String importClassName : importList) {
			addImport(importClassName);
		}
	}

	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#addMethod(com.sap.research.connectivity.JavaSourceMethod)
	 */
	public void addMethod(JavaSourceMethod method) {
		addMethod(method, true);
	}
	
	public void addMethod(JavaSourceMethod method, boolean overwrite) {
		JavaSourceMethod currentMethod = null;
		StringBuffer fileContent;
		if ((currentMethod = getMethod(method)) != null) {
			if (!overwrite)
				return;
			else {
				fileContent = new StringBuffer(getFileContent());
				if (fileContent.equals(""))
					return;
			
				replaceMethod(currentMethod, method, fileContent);
			}
		}
		else {
			fileContent = new StringBuffer(getFileContent());
			if (fileContent.equals(""))
				return;

			insertMethod(method, fileContent);
		}
	}


	private void insertMethod(JavaSourceMethod method, StringBuffer fileContent) {
		//We insert just before the end of the class
		try {
			fileContent.insert(fileContent.lastIndexOf("}"), method.METHOD_STRING);
			globalMethodList.add(method);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void replaceMethod(JavaSourceMethod currentMethod, JavaSourceMethod newMethod, StringBuffer fileContent) {
		try {
			currentMethod = newMethod;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#getSourceFile()
	 */
	public MutableFile getSourceFile() {
		return sourceFile;
	}
	
	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#getGlobalFieldList()
	 */
	public ArrayList<JavaSourceField> getGlobalFieldList() {
		return globalFieldList;
	}

	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#getGlobalMethodList()
	 */
	public ArrayList<JavaSourceMethod> getGlobalMethodList() {
		return globalMethodList;
	}
	
    /* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#getImportList()
	 */
    public ArrayList<String> getImportList() {
		return importList;
	}
    
	public JavaSourceMethod getMethod(JavaSourceMethod method) {
		
		for (JavaSourceMethod existingMethod : globalMethodList)
		{
			if (existingMethod.getReturnType().equals(method.getReturnType()) 
					&& existingMethod.getMethodName().equals(method.getMethodName())
					&& existingMethod.getParameters().containsAll(method.getParameters())
					&& method.getParameters().containsAll(existingMethod.getParameters()))
				return existingMethod;
		}
		
		return null;
	}

	private List<String> getFileLines() {
		InputStream inputStream = sourceFile.getInputStream();
		List<String> lines = null;
		try {
			lines = IOUtils.readLines(inputStream);
		} catch (IOException e) {
			System.out.println("Could not read the lines of the class file");
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
			
		return lines;
	}

	/* (non-Javadoc)
	 * @see com.sap.research.connectivity.JavaSourceFileEditorInterface#getFileContent()
	 */
	public String getFileContent() {
		InputStream inputStream = sourceFile.getInputStream();
		String fileContent = "";
		try {
			fileContent = IOUtils.toString(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		
		return fileContent;
	}
	

	public boolean fieldExists(String fieldName) {
		for (JavaSourceField existingField : globalFieldList) {
			if (existingField.getFieldName().equals(fieldName))
				return true;
		}
		return false;
	}
	
	public JavaSourceField fieldExists(JavaSourceField field) {
		for (JavaSourceField existingField : globalFieldList) {
			if (existingField.equals(field))
				return existingField;
		}
		return null;
	}
	
	
	public boolean importExists(String importClassName) {
		return importList.contains(importClassName);		
	}
	
	private void writeContentToFile(String tempContent) throws Exception {
		OutputStream outputStream = sourceFile.getOutputStream();
		try {
			IOUtils.write(tempContent, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception("Could not write into output class file");
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}
	
	public String makeFile() throws Exception{
		StringBuffer newFileContent = new StringBuffer();
		newFileContent.append(getPackageLine());
		newFileContent.append("\n\n");
		
		ArrayList<String> importClasses = getImportList();
		for (String importClass : importClasses) {
			newFileContent.append("import " + importClass + ";\n");
		}
		newFileContent.append("\n");
		
		newFileContent.append(classDeclaration);
		newFileContent.append("{\n\n");

		for (JavaSourceField globalField : globalFieldList) {
			newFileContent.append(globalField.FIELD_STRING + "\n");
		}
		newFileContent.append("\n");
		
		for (JavaSourceMethod globalMethod : globalMethodList) {
			newFileContent.append(globalMethod.METHOD_STRING);
		}
		
		newFileContent.append("\n}");
		
		String newFileContentString = newFileContent.toString();
		writeContentToFile(newFileContentString);
		
		return newFileContentString;
	}


	public String getPackageLine() throws Error{
		List<String> fileLines = getFileLines();
		//compilationUnit.
		if (fileLines == null) {
			throw new Error("Cannot get package name!");
		}
        
		for (String line : fileLines)
        {
			if (line.startsWith("package "))
				return line;
        }
		
		throw new Error ("Cannot get package name!");
	}
	
}