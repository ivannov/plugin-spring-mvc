/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.forge.scaffold.spring;

import static org.jvnet.inflector.Noun.pluralOf;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.scaffold.AccessStrategy;
import org.jboss.forge.scaffold.ScaffoldProvider;
import org.jboss.forge.scaffold.TemplateStrategy;
import org.jboss.forge.scaffold.util.ScaffoldUtil;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.spec.javaee.ServletFacet;
import org.jboss.forge.spring.metawidget.config.ForgeConfigReader;
import org.jboss.forge.spring.metawidget.widgetbuilder.HtmlAnchor;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.spi.TemplateResolver;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.jboss.seam.render.template.resolver.ClassLoaderTemplateResolver;
import org.metawidget.statically.StaticUtils.IndentedWriter;
import org.metawidget.statically.javacode.StaticJavaMetawidget;
import org.metawidget.statically.jsp.html.widgetbuilder.HtmlTag;
import org.metawidget.statically.spring.StaticSpringMetawidget;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.simple.StringUtils;

/**
 * Facet to generate a UI using the Spring JSP taglib.
 * <p>
 * This facet utilizes <a href="http://metawidget.org">Metawidget</a> internally. This enables the use of the Metawidget
 * SPI (pluggable WidgetBuilders, Layouts etc) for customizing the generated User Interface. For more information on
 * writing Metawidget plugins, see <a href="http://metawidget.org/documentation.php">the Metawidget documentation</a>.
 * <p>
 * This Facet does <em>not</em> require Metawidget to be in the final project.
 * 
 * @author <a href="mailto:ryan.k.bradley@gmail.com">Ryan Bradley</a>
 */

@RequiresFacet({ WebResourceFacet.class,
            PersistenceFacet.class})
public class SpringScaffold extends BaseFacet implements ScaffoldProvider {
    
    //
    // Private statics
    //

    private static String XMLNS_PREFIX = "xmlns:";

    private static final String SPRING_CONTROLLER_TEMPLATE = "scaffold/spring/SpringControllerTemplate.jv";
    private static final String DAO_INTERFACE_TEMPLATE = "scaffold/spring/DaoInterfaceTemplate.jv";
    private static final String DAO_IMPLEMENTATION_TEMPLATE = "scaffold/spring/DaoImplementationTemplate.jv";
    private static final String VIEW_TEMPLATE = "scaffold/spring/view.xhtml";
    private static final String CREATE_TEMPLATE = "scaffold/spring/create.xhtml";
    private static final String SEARCH_TEMPLATE = "scaffold/spring/search.xhtml";
    private static final String NAVIGATION_TEMPLATE = "scaffold/spring/page.xhtml";
    
    private static final String ERROR_TEMPLATE = "scaffold/spring/error.xhtml";
    private static final String INDEX_TEMPLATE = "scaffold/spring/index.xhtml";

    //
    // Protected members (nothing is private, to help sub-classing)
    //

    protected CompiledTemplateResource backingBeanTemplate;
    protected int backingBeanTemplateQbeMetawidgetIndent;

    protected CompiledTemplateResource springControllerTemplate;
    protected CompiledTemplateResource daoInterfaceTemplate;
    protected CompiledTemplateResource daoImplementationTemplate;
    protected CompiledTemplateResource viewTemplate;
    protected Map<String, String> viewTemplateNamespaces;
    protected int viewTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource createTemplate;
    protected Map<String, String> createTemplateNamespaces;
    protected int createTemplateEntityMetawidgetIndent;

    protected CompiledTemplateResource searchTemplate;
    protected Map<String, String> searchTemplateNamespaces;
    protected int searchTemplateSearchMetawidgetIndent;
    protected int searchTemplateBeanMetawidgetIndent;

    protected CompiledTemplateResource navigationTemplate;
    protected int navigationTemplateIndent;

    protected CompiledTemplateResource errorTemplate;
    protected CompiledTemplateResource indexTemplate;   
    private TemplateResolver<ClassLoader> resolver;
    
    private ShellPrompt prompt;
    private TemplateCompiler compiler;
    private Event<InstallFacets> install;
    private StaticSpringMetawidget entityMetawidget;
    private StaticSpringMetawidget searchMetawidget;
    private StaticSpringMetawidget beanMetawidget;
    private StaticJavaMetawidget qbeMetawidget;
    
    //
    // Constructor
    //
    
    @Inject
    public SpringScaffold(final ShellPrompt prompt,
                    final TemplateCompiler compiler,
                    final Event<InstallFacets> install)
    {
        this.prompt = prompt;
        this.compiler = compiler;
        this.install = install;
        
        this.resolver = new ClassLoaderTemplateResolver(SpringScaffold.class.getClassLoader());
        
        if(this.compiler != null)
        {
            this.compiler.getTemplateResolverFactory().addResolver(this. resolver);
        }
    }
    
    //
    // Public methods
    //

    @Override
    public List<Resource<?>> setup(Resource<?> template, boolean overwrite)
    {
        List<Resource<?>> resources = generateIndex(template, overwrite);

        return resources;
    }

    /**
     * Overridden to setup the Metawidgets.
     * <p>
     * Metawidgets must be configured per project <em>and per Forge invocation</em>. It is not sufficient to simply
     * configure them in <code>setup</code> because the user may restart Forge and not run <code>scaffold setup</code> a
     * second time.
     */    
    
    @Override
    public void setProject(Project project)
    {
        super.setProject(project);
        
        ForgeConfigReader configReader = new ForgeConfigReader(project);
        
        this.entityMetawidget = new StaticSpringMetawidget();
        this.entityMetawidget.setConfigReader(configReader);
        this.entityMetawidget.setConfig("scaffold/spring/metawidget-entity.xml");
        
        this.searchMetawidget = new StaticSpringMetawidget();
        this.searchMetawidget.setConfigReader(configReader);
        this.searchMetawidget.setConfig("scaffold/spring/metawidget-search.xml");
        
        this.beanMetawidget = new StaticSpringMetawidget();
        this.beanMetawidget.setConfigReader(configReader);
        this.beanMetawidget.setConfig("scaffold/spring/metawidget-bean.xml");
        
        this.qbeMetawidget = new StaticJavaMetawidget();
        this.qbeMetawidget.setConfigReader(configReader);
        this.qbeMetawidget.setConfig("scaffold/spring/metawidget-qbe.xml");
    }

    @Override
    public List<Resource<?>> generateFromEntity(Resource<?> template, JavaClass entity, boolean overwrite)
    {

        // Track the list of resources generated

        List<Resource<?>> result = new ArrayList<Resource<?>>();

        try
        {
            JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
            MetadataFacet meta = this.project.getFacet(MetadataFacet.class);
            
            loadTemplates();
            Map<Object, Object> context = CollectionUtils.newHashMap();
            context.put("entity", entity);
            String ccEntity = StringUtils.decapitalize(entity.getName());
            context.put("ccEntity", ccEntity);
            String daoPackage = meta.getTopLevelPackage() + ".repo";
            context.put("daoPackage", daoPackage);

            // Prepare qbeMetawidget

            this.qbeMetawidget.setPath(entity.getQualifiedName());
            StringWriter writer = new StringWriter();
            this.qbeMetawidget.write(writer, backingBeanTemplateQbeMetawidgetIndent);
            context.put("qbeMetawidget", writer.toString().trim());
            context.put("qbeMetawidgetImports",
                    CollectionUtils.toString(this.qbeMetawidget.getImports(), ";\r\n", true, false));

            // Set context for view generation

            context = getTemplateContext(template);

            JavaInterface daoInterface = JavaParser.parse(JavaInterface.class, this.daoInterfaceTemplate.render(context));
            JavaClass daoImplementation = JavaParser.parse(JavaClass.class, this.daoImplementationTemplate.render(context));

            // Save the created interface and class implementation, so they can be referenced by the controller.

            java.saveJavaSource(daoInterface);
            result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(daoInterface), daoInterface.toString(), overwrite));

            java.saveJavaSource(daoImplementation);
            result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(daoImplementation), daoImplementation.toString(), overwrite));
            
            String mvcPackage = meta.getTopLevelPackage() + ".mvc";
            context.put("mvcPackage",  mvcPackage);
            context.put("entityPlural", pluralOf(entity.getName().toLowerCase()));

            // Create a Spring MVC controller for the passed entity, using SpringControllerTemplate.jv

            JavaClass entityController = JavaParser.parse(JavaClass.class, this.springControllerTemplate.render(context));
            java.saveJavaSource(entityController);
            result.add(ScaffoldUtil.createOrOverwrite(this.prompt, java.getJavaResource(entityController), entityController.toString(), overwrite));
            
        } catch (Exception e)
        {
            throw new RuntimeException("Error generating Spring controller, or backing DAO, for " + entity.getName(), e);
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")    
    public boolean install()
    {

        if(!(this.project.hasFacet(WebResourceFacet.class) && this.project.hasFacet(PersistenceFacet.class)))
        {
            this.install.fire(new InstallFacets(WebResourceFacet.class, PersistenceFacet.class));
        }
        
        return true;
    }

    @Override
    public boolean isInstalled()
    {
        return true;
    }

    @Override
    public List<Resource<?>> generateIndex(Resource<?> template, boolean overwrite)
    {
        List<Resource<?>> result = new ArrayList<Resource<?>>();
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

        this.project.getFacet(ServletFacet.class).getConfig().welcomeFile("index.html");
        loadTemplates();

        generateTemplates(overwrite);
        HashMap<Object, Object> context = getTemplateContext(template);

        // Basic pages

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("index.html"),
                getClass().getResourceAsStream("/scaffold/spring/index.html"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("index.xhtml"), 
                getClass().getResourceAsStream("/scaffold/spring/index.xhtml"), overwrite));

        result.add(ScaffoldUtil.createOrOverwrite(this.prompt, web.getWebResource("error.xhmtl"),
                getClass().getResourceAsStream("/scaffold/spring/error.xhtml"), overwrite));

        // Static resources

        return result;
    }

    @Override
    public TemplateStrategy getTemplateStrategy()
    {
        return new SpringTemplateStrategy(this.project);
    }

    @Override
    public List<Resource<?>> generateTemplates(final boolean overwrite)
    {
        List<Resource<?>> result = new ArrayList<Resource<?>>();

        try
        {
            WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);

            result.add(ScaffoldUtil.createOrOverwrite(this.prompt,
                    web.getWebResource("/resources/scaffold/paginator.xhtml"),
                    getClass().getResourceAsStream("/resources/scaffold/paginator.xhtml"),
                    overwrite));

            result.add(generateNavigation(overwrite));
        } catch (Exception e)
        {
            throw new RuntimeException("Error generating default templates.", e);
        }

        return result;
    }

    @Override
    public List<Resource<?>> getGeneratedResources()
    {
        throw new RuntimeException("Not yet implemented.");
    }

    @Override
    public AccessStrategy getAccessStrategy()
    {
        // TODO Auto-generated method stub
        return null;
    }

    //
    // Protected methods (nothing is private, to help sub-classing)
    //

    protected void loadTemplates()
    {
        // Compile the DAO interface Java template.
        
        if(this.daoInterfaceTemplate == null) {
            daoInterfaceTemplate = compiler.compile(DAO_INTERFACE_TEMPLATE);
        }
        
        // Compile the DAO interface implementation Java template.
        
        if(this.daoImplementationTemplate == null) {
            daoImplementationTemplate = compiler.compile(DAO_IMPLEMENTATION_TEMPLATE);
        }
        
        // Compile the Spring MVC controller Java template.
        
        if(this.springControllerTemplate == null) {
            springControllerTemplate = compiler.compile(SPRING_CONTROLLER_TEMPLATE);
        }
              
        return;
    }

    protected HashMap<Object, Object> getTemplateContext(final Resource<?> template)
    {
        HashMap<Object, Object> context = new HashMap<Object, Object>();
        context.put("template", template);
        context.put("templateStrategy", getTemplateStrategy());
        return context;
    }

    /**
     * Generates the navigation menu based on scaffolded entities.
     */

    protected Resource<?> generateNavigation(final boolean overwrite)
            throws IOException
    {
        WebResourceFacet web = this.project.getFacet(WebResourceFacet.class);
        HtmlTag unorderedList = new HtmlTag("ul");

        for (Resource<?> resource : web.getWebResource("scaffold").listResources())
        {
            HtmlAnchor link = new HtmlAnchor();
            link.putAttribute("href", "/scaffold/" + resource.getName() + "/search");
            link.setTextContent(StringUtils.uncamelCase(resource.getName()));

            HtmlTag listItem = new HtmlTag("li");
            listItem.getChildren().add(link);
            unorderedList.getChildren().add(listItem);
        }

        Writer writer = new IndentedWriter(new StringWriter(), this.navigationTemplateIndent);
        unorderedList.write(writer);
        Map<Object, Object> context = CollectionUtils.newHashMap();
        context.put("navigation", writer.toString().trim());

        if (this.navigationTemplate == null)
        {
            loadTemplates();
        }

        return ScaffoldUtil.createOrOverwrite(this.prompt, (FileResource<?>) getTemplateStrategy().getDefaultTemplate(),
                this.navigationTemplate.render(context), overwrite);
    }
}
