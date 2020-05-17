package com.github.terefang.jinjava_maven_plugin;

import com.github.terefang.jinjava_maven_plugin.util.ContextUtil;
import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import lombok.SneakyThrows;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "template", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JinjavaTemplateMojo extends AbstractMojo {

    /**
     * The directory which contains data files
     */
    @Parameter(defaultValue = "${project.build.scriptSourceDirectory}")
    private File resourcesDirectory;

    /**
     * where to place the processed files
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/jinja")
    private File resourcesOutput;


    /**
     * the extension used by the destination files
     */
    @Parameter(defaultValue = ".txt")
    private String destinationExtension;

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
     * the jinja template to be applied
     */
    @Parameter(defaultValue = "${project.build.directory}/template.j2")
    private File jinjaTemplate;

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

    @SneakyThrows
    public void execute() throws MojoExecutionException {

        Jinjava jinjava = new Jinjava();

        if(!jinjaTemplate.exists())
        {
            throw new MojoExecutionException(MessageFormat.format("template {0} not found", jinjaTemplate.getAbsolutePath()));
        }

        String sourceContent = FileUtils.fileRead(jinjaTemplate);

        Map<String, Object> context = Maps.newHashMap();

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
                scanner.setIncludes(new String[]{ "**/*.yaml" });
            }

            if (excludes != null && excludes.length != 0) {
                scanner.setExcludes(excludes);
            }

            scanner.addDefaultExcludes();
            scanner.scan();

            for (String key : scanner.getIncludedFiles())
            {
                File file = new File(resourcesOutput, key.substring(0, key.lastIndexOf(".")).concat(destinationExtension));
                try
                {
                    Map<String, Object> _tcontext = Maps.newHashMap();
                    _tcontext.putAll(context);
                    File localContext = new File(resourcesDirectory, key);
                    getLog().info(MessageFormat.format("loading data from {0}", localContext.getName()));
                    _tcontext.putAll(ContextUtil.loadContextFrom(localContext));

                    getLog().info(MessageFormat.format("start processing jinja template {0}", jinjaTemplate));
                    String targetContent = jinjava.render(sourceContent, _tcontext);

                    file.getParentFile().mkdirs();

                    PrintWriter out = new PrintWriter(file);
                    out.print(targetContent);
                    out.close();
                    getLog().info(MessageFormat.format("finished processed to {0}", file.getAbsolutePath()));

                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(MessageFormat.format("Unable to process data file {0}", key), e);
                }
            }
        }
    }
}
