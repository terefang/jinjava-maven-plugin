# jinjava-maven-plugin

use the jitpack repository

```
<pluginRepositories>
    <pluginRepository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </pluginRepository>
</pluginRepositories>
```

### standard jinja mode 

eg. render many templates with one data context

```
<plugin>
    <artifactId>jinjava-maven-plugin</artifactId>
    <groupId>com.github.terefang</groupId>
    <version>${jinjava.maven.plugin.version}</version>
    <executions>
        <execution>
            <id>jinjava-standard</id>
            <phase>generate-resources</phase>
            <goals>
                <goal>jinjava</goal>
            </goals>
            <configuration>
                <additionalContext>test.hson</additionalContext>
                <resourcesDirectory>${project.basedir}/src/main/templates</resourcesDirectory>
                <resourcesOutput>${project.build.outputDirectory}/jinjava-resources</resourcesOutput>
                <processLocalContext>true</processLocalContext>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### template mode 

eg. render one template against many data contexts

```
<plugin>
    <artifactId>jinjava-maven-plugin</artifactId>
    <groupId>com.github.terefang</groupId>
    <version>${jinjava.maven.plugin.version}</version>
    <executions>
        <execution>
            <id>jinjava-template</id>
            <phase>generate-resources</phase>
            <goals>
                <goal>template</goal>
            </goals>
            <configuration>
                <jinjaTemplate>${project.basedir}/src/main/resources/base.j2</jinjaTemplate>
                <resourcesDirectory>${project.basedir}/src/main/resources</resourcesDirectory>
                <includes>**/*.hson</includes>
                <resourcesOutput>${project.build.outputDirectory}/jinjava-resources</resourcesOutput>
                <destinationExtension>.md</destinationExtension>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### context data files

the following file formats are supported:

* yaml/yml -- ie. YAML 1.1 -- https://yaml.org/spec/1.1/current.html
* json -- ie. JSON -- https://www.json.org/json-en.html
* hson/hjson -- ie. human JSON -- https://hjson.github.io/

