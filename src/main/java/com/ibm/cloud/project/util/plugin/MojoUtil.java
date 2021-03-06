/* Copyright 2020 IBM Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */    
package com.ibm.cloud.project.util.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MojoUtil {
  
  static final boolean APPSODY_DEV_MODE = System.getenv("APPSODY_DEV_MODE") != null;

  
  protected static boolean verifyVersion(String version, String range) {
    return VersionRange.valueOf(range).includes(Version.valueOf(version));
  }
  
  protected static boolean verifyFileExists(String path) {
    
    File candidate = new File(path);
    if( candidate.exists() && candidate.isFile() )
      return true;
    
    return APPSODY_DEV_MODE;    
  }
  
  protected static boolean verifyFolderExists(String path) {

    File candidate = new File(path);
    if( candidate.exists() && candidate.isDirectory() )
      return true;
    
    return APPSODY_DEV_MODE;
  }
  
  protected static ArtifactCoordinate getPomCoordinate(String pomPath) throws MojoExecutionException {

    if( ! verifyFileExists(pomPath) )
      throw new MojoExecutionException(MessageFormat.format(Messages.get(Messages.MISSING_POM), pomPath));

    String g_path = "/project/groupId";
    String a_path = "/project/artifactId";
    String v_path = "/project/version";

    DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
    coordinate.setExtension("pom");

    String[] expectedElements = {g_path, a_path, v_path};
    Map<String,String> map = getPomElementText(pomPath, expectedElements);
    
    coordinate.setGroupId(map.get(g_path));
    coordinate.setArtifactId(map.get(a_path));
    coordinate.setVersion(map.get(v_path));

    return coordinate;
  }
  
  protected static ArtifactCoordinate getParentPomCoordinate(String pomPath) throws MojoExecutionException {
  
    if( ! verifyFileExists(pomPath) )
      throw new MojoExecutionException(MessageFormat.format(Messages.get(Messages.MISSING_POM), pomPath));
  
    String g_path = "/project/parent/groupId";
    String a_path = "/project/parent/artifactId";
    String v_path = "/project/parent/version";
  
    DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
    coordinate.setExtension("pom");
  
    String[] expectedElements = {g_path, a_path, v_path};
    Map<String,String> map = getPomElementText(pomPath, expectedElements);
    
    coordinate.setGroupId(map.get(g_path));
    coordinate.setArtifactId(map.get(a_path));
    coordinate.setVersion(map.get(v_path));
  
    return coordinate;
  }
  
  protected static String getMD5Digest(File file) throws IOException, NoSuchAlgorithmException {

    int bufflen = 8192;
    
    MessageDigest digest = MessageDigest.getInstance("MD5");     

    try( FileInputStream inputStream = new FileInputStream(file) ) {
      byte[] buff = new byte[bufflen];
      int bytesRead = 0; 
      while ((bytesRead = inputStream.read(buff)) != -1)
        digest.update(buff, 0, bytesRead);      
    }
          
    return DatatypeConverter.printHexBinary(digest.digest());
  }
  
  protected static boolean verifyFileIntegrity(File file, URL cksumURL) throws NoSuchAlgorithmException, IOException {

      String localDigest = getMD5Digest(file);
      String sourceDigest = downloadText(cksumURL);

      return localDigest.equals(sourceDigest);
  }
  
  protected static void replaceFile(URL sourceURL, File file ) throws IOException {
    
    String replacementText = downloadText(sourceURL);
    
    try( OutputStream outputStream = new FileOutputStream(file) ) {
      outputStream.write(replacementText.getBytes());
    }
  }

  protected static String downloadText(URL sourceURL) throws IOException {
    
    int buflen = 4096;
    
    StringBuilder stringBuilder = new StringBuilder(buflen);
    
    byte[] buff = new byte[buflen];
    try( InputStream inputStream = sourceURL.openStream() ) {
      int n = inputStream.read(buff); 
      do {
        String s = new String(buff, 0, n);
        stringBuilder.append(s);
      }
      while ( (n = inputStream.read(buff)) > 0 );
    }
    
    return stringBuilder.toString();
  }
  
  static private Map<String,String> getPomElementText(String pomPath, String[] elements) throws MojoExecutionException {
  
    if( ! verifyFileExists(pomPath) )
      throw new MojoExecutionException(MessageFormat.format(Messages.get(Messages.MISSING_POM), pomPath));
  
    XPath xPath = XPathFactory.newInstance().newXPath();

    Map<String,String> map = new HashMap<String,String>(); 
    try {
      Document pomObject = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomPath);      
      for( int k=0; k<elements.length; ++k )
        map.put(elements[k], getPomElementText(pomPath, pomObject, elements[k], xPath));      
    }
    catch (SAXException | IOException | ParserConfigurationException | XPathException e) {
      throw new MojoExecutionException("POM parsing error: "+pomPath, e);
    }
  
    return map;
  }

  static private String getPomElementText(String pomPath, Document pomObject, String expectedElement, XPath xPath) throws XPathException, MojoExecutionException {
    NodeList nodeList = (NodeList) xPath.compile(expectedElement).evaluate(pomObject, XPathConstants.NODESET);
    if( nodeList.getLength() == 0 )
      throw new MojoExecutionException(MessageFormat.format(Messages.get(Messages.MISSING_POM_ELEMENT), expectedElement, pomPath));     
    return nodeList.item(0).getTextContent();
  }

}
