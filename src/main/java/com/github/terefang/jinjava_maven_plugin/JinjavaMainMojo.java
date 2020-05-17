package com.github.terefang.jinjava_maven_plugin;

import com.github.terefang.jinjava_maven_plugin.util.ContextUtil;
import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;

@Mojo(name = "jinjava", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JinjavaMainMojo extends AbstractMojo {

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*.j2"};

    /**
     * The directory which contains jinja template files
     */
    @Parameter(defaultValue = "${project.build.scriptSourceDirectory}")
    private File resourcesDirectory;

    /**
     * where to place the processed template files
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/jinja")
    private File resourcesOutput;


    /**
     * additional global context
     */
    @Parameter(defaultValue = "${project.build.directory}/jinja.yaml")
    private File additionalContext;

    /**
     * additional global context key
     */
    @Parameter(defaultValue = "context")
    private String additionalContextRoot;

    /**
     * process local contexts
     */
    @Parameter(defaultValue = "false")
    private boolean processLocalContext;

    /**
     * local context extensions
     */
    @Parameter(defaultValue = ".yaml .yml .json .hson .hjson .properties")
    private String localContextExtensions;

    /**
     * local context key
     */
    @Parameter(defaultValue = "local")
    private String localContextRoot;

    /**
     * list of includes. ant-style/double wildcards.
     */
    @Parameter
    private String[] includes;

    /**
     * list of excludes. ant-style/double wildcards.
     */
    @Parameter
    private String[] excludes;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {

        Jinjava jinjava = new Jinjava();

        Map<String, Object> context = Maps.newHashMap();

        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            reader = new FileReader(new File("pom.xml"));
            model = mavenreader.read(reader);
            model.setPomFile(new File("pom.xml"));
        } catch (Exception ex) {
        }

        MavenProject mavenProject = new MavenProject(model);

        Map<String, String> details = new HashMap<String, String>();
        details.put("groupId", mavenProject.getModel().getGroupId());
        details.put("artifactId", mavenProject.getModel().getArtifactId());
        details.put("description", mavenProject.getModel().getDescription());
        details.put("name", mavenProject.getModel().getName());
        context.put("project", details);

        List<Map<String, String>> dependencies = new ArrayList<Map<String, String>>();
        for (Dependency dependency : mavenProject.getDependencies()) {
            Map<String, String> map = new HashMap<>();
            map.put("groupId", dependency.getGroupId());
            map.put("artifactId", dependency.getArtifactId());
            map.put("type", dependency.getType());
            map.put("version", dependency.getVersion());
            dependencies.add(map);
        }
        context.put("dependencies", dependencies);

        Map<String, String> properties = new HashMap<String, String>();
        for (Map.Entry<Object, Object> property : mavenProject.getProperties().entrySet()) {
            properties.put(property.getKey().toString(), property.getValue().toString());
        }
        context.put("properties", properties);

        if(additionalContext.exists())
        {
            getLog().info(MessageFormat.format("loading context {0} from {1}", additionalContextRoot, additionalContext.getName()));
            context.put(additionalContextRoot, ContextUtil.loadContextFrom(additionalContext));
        }

        FileUtils.mkdir(resourcesOutput.getPath());

        DirectoryScanner scanner = new DirectoryScanner();

        if( resourcesDirectory.isDirectory()) {

            scanner.setBasedir(resourcesDirectory);
            if (includes != null && includes.length != 0) {
                scanner.setIncludes(includes);
            } else {
                scanner.setIncludes(DEFAULT_INCLUDES);
            }

            if (excludes != null && excludes.length != 0) {
                scanner.setExcludes(excludes);
            }

            scanner.addDefaultExcludes();
            scanner.scan();

            for (String key : scanner.getIncludedFiles())
            {
                File file = new File(resourcesOutput, key.substring(0, key.lastIndexOf(".")));
                try
                {
                    if(processLocalContext)
                    {
                        context.remove(localContextRoot);
                        File localContext = null;
                        for(String _ext : StringUtils.split(localContextExtensions, " "))
                        {
                            //getLog().info(MessageFormat.format("checking extension {0}", _ext));
                            File _localContext = new File(resourcesDirectory, key+_ext);
                            if(_localContext.exists())
                            {
                                localContext = _localContext;
                                break;
                            }
                            else
                            {
                                //getLog().info(MessageFormat.format("context not found: {0}", _localContext.getName()));
                            }
                        }

                        if(localContext!=null)
                        {
                            getLog().info(MessageFormat.format("loading context {0} from {1}", localContextRoot, localContext.getName()));
                            context.put(localContextRoot, ContextUtil.loadContextFrom(localContext));
                        }
                    }
                    getLog().info(MessageFormat.format("start processing jinja template {0}", key));
                    String sourceContent = FileUtils.fileRead(new File(resourcesDirectory, key));
                    String targetContent = jinjava.render(sourceContent, context);

                    file.getParentFile().mkdirs();

                    PrintWriter out = new PrintWriter(file);
                    out.print(targetContent);
                    out.close();
                    getLog().info(MessageFormat.format("finished processed to {0}", file.getAbsolutePath()));

                } catch (IOException e) {
                    throw new MojoExecutionException("Unable to process jinja template file", e);
                }
            }
        }
    }
}
