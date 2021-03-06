# IBM Project Plugin

This is a maven plugin containing some general purpose utility goals. 

### GOAL: CHECK_VERSION

This is a goal that uses `org.osgi.framework.Version` to verify that a found version ID matches requirements of a known [semantic version](https://semver.org) range.

Example:  

``` bash
 % mvn -q com.ibm.cloud:project-util-plugin:check-version -Drange_spec="[0.3,0.4)" -Dversion="0.3.7"
```

This goal can be used as one of a list of Maven goals to be executed for a "build", and will cause the build to fail if the specified `version` does not match the `range_spec`. 

### GOAL: CHECK_PARENT_POM

This is a goal that can be used to verify consistency between a POM and its required parent POM.  Specificlally:

1. Ensure the parent POM file is installed in the local Maven repository
2. Verify a parent POM reference exists and that its identifiers match the required parent POM

Example:

``` bash
% mvn com.ibm.cloud:project-util-plugin:check-parent-pom \
    -Dparent_path=testcase-parent-pom.xml \
    -Dchild_path=testcase-pom.xml \
    -Drepo_path=/mvn/repository
```

### GOAL: VERIFY_FILE

This is a goal that supports enforcement of read-only policies on local files, using MD5 digests as cksums to detect changes relative to expectation. 

Parameters:  
* `file_path` A path on the local filesystem for the file to verify  
* `cksum_url` A URL for a file containing the MD5 digest for the file to verify  
* `ref_url` A URL for the reference copy of the file to verify (optional)  
* `replace_on_fail` Replace the local file with the reference copy if the cksum match fails. (optional, default=false)  

URL parameters accept either 'file' or 'http(s)' scheme. If <code>ref_url</code> is specified and <code>replace_on_fail</code> is false, then save a copy of the reference file and fail.

Example:  

``` bash
 % mvn com.ibm.cloud:project-util-plugin:verify-file \
     -Dfile_path=parent-pom.xml \
     -Dcksum_url=file:///tmp/parent-pom-cksum.txt
```

### GOAL: GENERATE_CKSUM

Generate an MD5 checksum for a specified file.

Example:

``` bash
% s=`mvn -q com.ibm.cloud:project-util-plugin:gen-cksum -Dfile_path=a.xml`
```

### HELP

The plugin uses the [Maven Plugin Plugin](http://maven.apache.org/plugin-tools/maven-plugin-plugin/index.html) builtin [HelpMojo](http://maven.apache.org/plugin-tools/maven-plugin-plugin/examples/generate-help.html) for generated plugin help.

``` bash
 % mvn -q com.ibm.cloud:project-util-plugin:help

[INFO] project-util Maven Plugin 0.1.0-RELEASE
  This is a maven plugin containing some general purpose utility functions.

project-util:check-version
  This is a Maven Mojo that can be used to verify that a version ID satisfies a
  semantic version range requirement. Both of those values are required as
  plugin parameters, as shown in the example:
  % mvn -q com.ibm.cloud:project-util-plugin:check-version
  -Drange_spec='[0.3,0.4)' -Dversion='0.3.7'

```

### INSTALLATION

For local use, with this source project, the plugin can be installed to the user's local Maven repository. From the project root directory, run:

``` bash
% mvn clean install
```

### CONFIGURING PLUGIN SEARCH

A briefer invocation style is possible through configuration, as documented in [Configuring Maven to Search for Plugins](https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html#configuring-maven-to-search-for-plugins).

As an example, suppose the user's `~/.m2/settings.xml` file contains:

``` xml                                                                         
<settings>
  <pluginGroups>
    <pluginGroup>com.ibm.cloud</pluginGroup>
  </pluginGroups>
</settings>
```

Then `check-version` can be invoked as:

``` bash
% mvn -q project-util:check-version -Drange_spec="[0.3,0.4)" -Dversion="0.3.7"
```

