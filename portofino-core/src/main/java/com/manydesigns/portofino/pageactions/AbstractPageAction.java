/*
 * Copyright (C) 2005-2017 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
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

package com.manydesigns.portofino.pageactions;

import com.manydesigns.elements.ElementsThreadLocals;
import com.manydesigns.elements.forms.Form;
import com.manydesigns.elements.forms.FormBuilder;
import com.manydesigns.elements.messages.SessionMessages;
import com.manydesigns.elements.options.DefaultSelectionProvider;
import com.manydesigns.elements.options.SelectionProvider;
import com.manydesigns.elements.util.MimeTypes;
import com.manydesigns.portofino.buttons.ButtonInfo;
import com.manydesigns.portofino.buttons.ButtonsLogic;
import com.manydesigns.portofino.buttons.GuardType;
import com.manydesigns.portofino.di.Inject;
import com.manydesigns.portofino.di.Injections;
import com.manydesigns.portofino.dispatcher.*;
import com.manydesigns.portofino.security.SecurityLogic;
import com.manydesigns.portofino.modules.BaseModule;
import com.manydesigns.portofino.modules.PageactionsModule;
import com.manydesigns.portofino.pageactions.registry.TemplateRegistry;
import com.manydesigns.portofino.pages.PageLogic;
import com.manydesigns.portofino.pages.Page;
import com.manydesigns.portofino.security.AccessLevel;
import com.manydesigns.portofino.security.RequiresPermissions;
import com.manydesigns.portofino.servlets.ServerInfo;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Convenient abstract base class for PageActions. It has fields to hold values of properties specified by the
 * PageAction interface as well as other useful objects injected by the framework. It provides standard
 * implementations of many of the PageAction methods, as well as important utility methods to handle hierarchical
 * relations among pages, such as embedding.
 *
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
@RequiresPermissions(level = AccessLevel.VIEW)
public abstract class AbstractPageAction extends NodeWithParameters implements PageAction {
    public static final String copyright =
        "Copyright (C) 2005-2017 ManyDesigns srl";

    public static final String DEFAULT_LAYOUT_CONTAINER = "default";
    public static final String[][] PAGE_CONFIGURATION_FIELDS =
            {{"id", "title", "description", "template", "detailTemplate", "applyTemplateRecursively"}};
    public static final String[][] PAGE_CONFIGURATION_FIELDS_NO_DETAIL =
            {{"id", "title", "description", "template", "applyTemplateRecursively"}};
    public static final String CONF_FORM_PREFIX = "config";

    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    /**
     * The PageInstance property. Injected.
     */
    public PageInstance pageInstance;

    /**
     * The global configuration object. Injected.
     */
    @Inject(BaseModule.PORTOFINO_CONFIGURATION)
    public Configuration portofinoConfiguration;

    /**
     * The templates registry. Injected.
     */
    @Inject(PageactionsModule.TEMPLATES_REGISTRY)
    public TemplateRegistry templates;

    /**
     * Information about the web server. Injected.
     */
    @Inject(BaseModule.SERVER_INFO)
    public ServerInfo serverInfo;

    /**
     * The page template to use. It can be set using a request parameter. If not set, the one from page.xml is used.
     */
    protected String pageTemplate;

    @Context
    protected UriInfo uriInfo;

    //--------------------------------------------------------------------------
    // UI
    //--------------------------------------------------------------------------

    private MultiMap embeddedPageActions;

    //--------------------------------------------------------------------------
    // Navigation
    //--------------------------------------------------------------------------

    //**************************************************************************
    // Scripting
    //**************************************************************************

    /**
     * The Groovy script for this page.
     */
    protected String script;

    //**************************************************************************
    // Page configuration
    //**************************************************************************

    /**
     * The Form to configure standard page settings.
     */
    public Form pageConfigurationForm;

    /**
     * The context object holds various elements of contextual information such
     * as the HTTP request and response objects.
     */
    protected PageActionContext context;

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger =
            LoggerFactory.getLogger(AbstractPageAction.class);

    protected AbstractPageAction() {
        maxParameters = PageActionLogic.supportsDetail(getClass()) ? Integer.MAX_VALUE : 0;
    }

    //--------------------------------------------------------------------------
    // Admin methods
    //--------------------------------------------------------------------------

    /**
     * Utility method to save the configuration object to a file in this page's directory.
     * @param configuration the object to save. It must be in a state that will produce a valid XML document.
     * @return true if the object was correctly saved, false otherwise.
     */
    protected boolean saveConfiguration(Object configuration) {
        try {
            FileObject confFile = PageLogic.saveConfiguration(pageInstance.getDirectory(), configuration);
            logger.info("Configuration saved to " + confFile.getName().getPath());
            return true;
        } catch (Exception e) {
            logger.error("Couldn't save configuration", e);
            SessionMessages.addErrorMessage("error saving conf");
            return false;
        }
    }

    //--------------------------------------------------------------------------
    // Dispatch
    //--------------------------------------------------------------------------

//    /**
//     * This is called to process a piece (fragment) of the requested URL. If the fragment matches a child page, that
//     * page is instantiated and initialized. Otherwise, the fragment is taken to be a parameter for the current page.
//     * Subclasses can override this method if they want to handle child pages differently (e.g. having them stored
//     * elsewhere).
//     * @param pathFragment the fragment to process. In path /foo/bar/baz, foo, bar and baz are three different fragments.
//     * @return the object that will potentially continue path dispatch if there are other fragments to consume. This is
//     * either a child page or <code>this</code>. A null return value means that the dispatch failed (no child page
//     * exists and this page does not accept parameters).
//     */
//    @Override
//    public DispatchElement consumePathFragment(String pathFragment) {
//        boolean acceptsPathParameter = acceptsPathParameter();
//        if(!acceptsPathParameter || pageInstance.getParameters().isEmpty()) {
//            PageAction subpage = PageLogic.getSubpage(portofinoConfiguration, pageInstance, pathFragment);
//            if (subpage != null) {
//                HttpServletRequest request = ElementsThreadLocals.getHttpServletRequest();
//                Injections.inject(subpage, request.getServletContext(), request);
//                return subpage;
//            }
//        }
//        if(acceptsPathParameter) {
//            pageInstance.getParameters().add(pathFragment);
//            return this;
//        } else {
//            return null;
//        }
//    }


    @Override
    protected void initSubResource(Object resource) {
        super.initSubResource(resource);
        //TODO use resourcecontext?
        HttpServletRequest request = ElementsThreadLocals.getHttpServletRequest();
        Injections.inject(resource, request.getServletContext(), request);
    }

    @Override
    protected void initSubResource(Resource resource) {
        super.initSubResource(resource);
        if(resource instanceof PageAction) {
            initPageAction(resource, getPageInstance(), uriInfo);
        }
    }

    public static void initPageAction(Resource resource, PageInstance parentPageInstance, UriInfo uriInfo) {
        PageAction pageAction = (PageAction) resource;
        HttpServletRequest request = ElementsThreadLocals.getHttpServletRequest();
        HttpServletResponse response = ElementsThreadLocals.getHttpServletResponse();
        Page page;
        try {
            page = PageLogic.getPage(resource.getLocation());
        } catch (PageNotActiveException e) {
            throw new WebApplicationException(e, 404);
        }
        PageInstance pageInstance = new PageInstance(
                parentPageInstance, resource.getLocation(), page, (Class<? extends PageAction>) resource.getClass());
        pageInstance.setActionBean(pageAction);
        PageLogic.configurePageAction(pageAction, pageInstance);
        PageActionContext context = new PageActionContext();
        context.setRequest(request);
        context.setResponse(response);
        context.setServletContext(request.getServletContext());
        if(uriInfo != null) { //TODO for Swagger
            String path = uriInfo.getPath();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            context.setActionPath(path); //TODO
        }
        pageAction.setContext(context);
    }

    @Override
    public void prepareForExecution() {

    }

    @Override
    protected void parametersAcquired() throws WebApplicationException {
        pageInstance.getParameters().addAll(parameters);
    }

    @Override
    public FileObject getChildLocation(String subResourceName) throws FileSystemException {
        if(parameters.isEmpty()) {
            return getLocation().getChild(subResourceName);
        } else {
            return getLocation().getChild(PageInstance.DETAIL).getChild(subResourceName);
        }
    }

    //    /**
//     * REST support. Called by the JAX-RS implementation to handle a path fragment.
//     * @param pathFragment the path fragment.
//     * @return @see #consumePathFragment(String)
//     */
//    @Path("{pathFragment}")
//    public Object getSubResource(@PathParam("pathFragment") String pathFragment) {
//        DispatchElement resource = consumePathFragment(pathFragment);
//        if(resource != this) {
//            if(context == null) {
//                setContext(getParent().getContext());
//            }
//            Response response = preparePage();
//            if(response != null) {
//                return response; //new TerminalResource(response);
//            }
//        }
//        return resource;
//    }

    @Override
    public PageAction getParent() {
        return (PageAction) super.getParent();
    }

    /*public static class TerminalResource {

        private final Object result;

        public TerminalResource(Object result) {
            this.result = result;
        }

        @GET
        public Object get() {
            return result;
        }

        @POST
        public Object post() {
            return result;
        }

        @PUT
        public Object put() {
            return result;
        }

        @DELETE
        public Object delete() {
            return result;
        }

    }*/

    /*@Override
    public MultiMap initEmbeddedPageActions() {
        if(embeddedPageActions == null) {
            MultiMap mm = new MultiValueMap();
            Layout layout = pageInstance.getLayout();
            for(ChildPage childPage : layout.getChildPages()) {
                String layoutContainerInParent = childPage.getContainer();
                if(layoutContainerInParent != null) {
                    String newPath = context.getActionPath() + "/" + childPage.getName();
                    newPath = ServletUtils.removePathParameters(newPath); //#PRT-1650 Path parameters mess with include
                    File pageDir = new File(pageInstance.getChildrenDirectory(), childPage.getName());
                    try {
                        Page page = PageLogic.getPage(pageDir);
                        EmbeddedPageAction embeddedPageAction =
                            new EmbeddedPageAction(
                                    childPage.getName(),
                                    childPage.getActualOrder(),
                                    newPath,
                                    page);

                        mm.put(layoutContainerInParent, embeddedPageAction);
                    } catch (PageNotActiveException e) {
                        logger.warn("Embedded page action is not active, skipping! " + pageDir, e);
                    }
                }
            }
            for(Object entryObj : mm.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObj;
                List pageActionContainer = (List) entry.getValue();
                Collections.sort(pageActionContainer);
            }
            embeddedPageActions = mm;
        }
        return embeddedPageActions;
    }*/

//    /**
//     * Returns the path inside the web application of a resource relative to this action's directory.
//     * E.g. getResourcePath("my.jsp") might return /WEB-INF/pages/this/that/my.jsp.
//     * @param resource the path of the resource, relative to this action's directory.
//     * @return the path of the resource, relative to the web application rootFactory.
//     */
//    public String getResourcePath(String resource) {
//        FileObject resourceFile = pageInstance.getDirectory().resolveFile(resource, NameScope.FILE_SYSTEM);
//        File appRoot = new File(serverInfo.getRealPath());
//        return ElementsFileUtils.getRelativePath(appRoot, resourceFile);
//    }

    public String getActionPath() {
        return context.getActionPath();
    }

    //--------------------------------------------------------------------------
    // Getters/Setters
    //--------------------------------------------------------------------------

    public boolean isMultipartRequest() {
        return false;
    }

    public Form getPageConfigurationForm() {
        return pageConfigurationForm;
    }

    public void setPageConfigurationForm(Form pageConfigurationForm) {
        this.pageConfigurationForm = pageConfigurationForm;
    }

    @Override
    public PageInstance getPageInstance() {
        return pageInstance;
    }

    @Override
    public void setPageInstance(PageInstance pageInstance) {
        this.pageInstance = pageInstance;
    }

    public Page getPage() {
        return getPageInstance().getPage();
    }

    //--------------------------------------------------------------------------
    // Page configuration
    //--------------------------------------------------------------------------

    /**
     * Sets up the Elements form(s) 
     */
    protected void prepareConfigurationForms() {
        Page page = pageInstance.getPage();

        PageInstance parent = pageInstance.getParent();
        assert parent != null;

        FormBuilder formBuilder = new FormBuilder(EditPage.class)
                .configPrefix(CONF_FORM_PREFIX)
                .configFields(PageActionLogic.supportsDetail(getClass()) ?
                              PAGE_CONFIGURATION_FIELDS :
                              PAGE_CONFIGURATION_FIELDS_NO_DETAIL)
                .configFieldSetNames("Page");

        SelectionProvider layoutSelectionProvider = createTemplateSelectionProvider();
        formBuilder.configSelectionProvider(layoutSelectionProvider, "template");
        SelectionProvider detailLayoutSelectionProvider = createTemplateSelectionProvider();
        formBuilder.configSelectionProvider(detailLayoutSelectionProvider, "detailTemplate");

        pageConfigurationForm = formBuilder.build();
        EditPage edit = new EditPage();
        edit.id = page.getId();
        edit.title = page.getTitle();
        edit.description = page.getDescription();
        edit.template = page.getLayout().getTemplate();
        edit.detailTemplate = page.getDetailLayout().getTemplate();
        pageConfigurationForm.readFromObject(edit);
//
//        if(script == null) {
//            prepareScript();
//        }
    }

    protected SelectionProvider createTemplateSelectionProvider() {
        DefaultSelectionProvider selectionProvider = new DefaultSelectionProvider("template");
        for(String template : templates) {
            selectionProvider.appendRow(template, template, true);
        }
        return selectionProvider;
    }

    /**
     * Reads the page configuration form values from the request. Can be called to re-use the standard page
     * configuration form.
     */
    protected void readPageConfigurationFromRequest() {
        pageConfigurationForm.readFromRequest(context.getRequest());
    }

    /**
     * Validates the page configuration form values. Can be called to re-use the standard page
     * configuration form.
     * @return true iff the form was valid.
     */
    protected boolean validatePageConfiguration() {
        return pageConfigurationForm.validate();
    }

//    /**
//     * Updates the page with values from the page configuration. Can be called to re-use the standard page
//     * configuration form. Should be called only after validatePageConfiguration() returned true.
//     * @return true iff the page was correctly saved.
//     */
//    protected boolean updatePageConfiguration() {
//        EditPage edit = new EditPage();
//        pageConfigurationForm.writeToObject(edit);
//        Page page = pageInstance.getPage();
//        page.setTitle(edit.title);
//        page.setDescription(edit.description);
//        page.getLayout().setTemplate(edit.template);
//        page.getDetailLayout().setTemplate(edit.detailTemplate);
//        try {
//            File pageFile = PageLogic.savePage(pageInstance.getDirectory(), page);
//            logger.info("Page saved to " + pageFile.getAbsolutePath());
//        } catch (Exception e) {
//            logger.error("Couldn't save page", e);
//            return false; //TODO handle return value + script + session msg
//        }
//        if(edit.applyTemplateRecursively) {
//            FileFilter filter = new FileFilter() {
//                public boolean accept(File pathname) {
//                    return pathname.isDirectory();
//                }
//            };
//            updateTemplate(pageInstance.getDirectory(), filter, edit);
//        }
//        Subject subject = SecurityUtils.getSubject();
//        if(SecurityLogic.hasPermissions(portofinoConfiguration, getPageInstance(), subject, AccessLevel.DEVELOP)) {
//            updateScript();
//        }
//        return true;
//    }

    //--------------------------------------------------------------------------
    // Scripting
    //--------------------------------------------------------------------------

//    protected void prepareScript() {
//        String pageId = pageInstance.getPage().getId();
//        File file = ScriptingUtil.getGroovyScriptFile(pageInstance.getDirectory(), "action");
//        FileReader fr = null;
//        try {
//            fr = new FileReader(file);
//            script = IOUtils.toString(fr);
//        } catch (Exception e) {
//            logger.warn("Couldn't load script for page " + pageId, e);
//        } finally {
//            IOUtils.closeQuietly(fr);
//        }
//    }
//
//    protected void updateScript() {
//        File directory = pageInstance.getDirectory();
//        File groovyScriptFile = ScriptingUtil.getGroovyScriptFile(directory, "action");
//        FileWriter fw = null;
//        try {
//            fw = new FileWriter(groovyScriptFile);
//            fw.write(script);
//            fw.flush();
//            fw.close();
//            Class<?> scriptClass = PageLogic.getActionClass(portofinoConfiguration, directory, false);
//            if(scriptClass == null) {
//                SessionMessages.addErrorMessage(ElementsThreadLocals.getText("script.class.is.not.valid"));
//            }
//        } catch (IOException e) {
//            logger.error("Error writing script to " + groovyScriptFile, e);
//            String msg = ElementsThreadLocals.getText("couldnt.write.script.to._", groovyScriptFile.getAbsolutePath());
//            SessionMessages.addErrorMessage(msg);
//        } catch (Exception e) {
//            String pageId = pageInstance.getPage().getId();
//            logger.warn("Couldn't compile script for page " + pageId, e);
//            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("couldnt.compile.script"));
//        } finally {
//            IOUtils.closeQuietly(fw);
//        }
//    }

    public Map getOgnlContext() {
        return ElementsThreadLocals.getOgnlContext();
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Configuration getPortofinoConfiguration() {
        return portofinoConfiguration;
    }

    @Override
    public void setContext(PageActionContext context) {
        this.context = context;
    }

    @Override
    public PageActionContext getContext() {
        return context;
    }

    //--------------------------------------------------------------------------
    // Utitilities
    //--------------------------------------------------------------------------

    /**
     * Returns a ForwardResolution to a standard page with an error message saying that the pageaction is not properly
     * configured.
     */
    public Response pageActionNotConfigured() {
        return Response.serverError().entity("page-action-not-configured").build();
    }

    public String getPageTemplate() {
        Pattern pattern = Pattern.compile("^(\\w|\\d|-)+");
        if(pageTemplate != null && pattern.matcher(pageTemplate).matches()) {
            return pageTemplate;
        } else {
            return getPageInstance().getLayout().getTemplate();
        }
    }

    public void setPageTemplate(String pageTemplate) {
        this.pageTemplate = pageTemplate;
    }

    @Path(":buttons")
    @GET
    @Produces(MimeTypes.APPLICATION_JSON_UTF8)
    public List getButtons() {
        HttpServletRequest request = context.getRequest();
        String list = request.getParameter("list");
        List<ButtonInfo> buttons = ButtonsLogic.getButtonsForClass(getClass(), list);
        List result = new ArrayList();
        Subject subject = SecurityUtils.getSubject();
        for(ButtonInfo button : buttons) {
            logger.trace("ButtonInfo: {}", button);
            Method handler = button.getMethod();
            boolean isAdmin = SecurityLogic.isAdministrator(request);
            if(!isAdmin &&
               ((pageInstance != null && !SecurityLogic.hasPermissions(
                       portofinoConfiguration, button.getMethod(), button.getFallbackClass(), pageInstance, subject)) ||
                !SecurityLogic.satisfiesRequiresAdministrator(request, this, handler))) {
                continue;
            }
            boolean visible = ButtonsLogic.doGuardsPass(this, handler, GuardType.VISIBLE);
            if(!visible) {
                continue;
            }
            boolean enabled = ButtonsLogic.doGuardsPass(this, handler, GuardType.ENABLED);
            Map<String, Object> buttonData = new HashMap<String, Object>();
            buttonData.put("list", button.getButton().list());
            buttonData.put("group", button.getButton().group());
            buttonData.put("icon", button.getButton().icon());
            buttonData.put("iconBefore", button.getButton().iconBefore());
            buttonData.put("text", ElementsThreadLocals.getText(button.getButton().key()));
            buttonData.put("order", button.getButton().order());
            buttonData.put("type", button.getButton().type());
            buttonData.put("method", button.getMethod().getName());
            buttonData.put("enabled", enabled);
            result.add(buttonData);
        }
        return result;
    }

    @Path(":configuration")
    @GET
    @Produces(MimeTypes.APPLICATION_JSON_UTF8)
    public Object getConfiguration() {
        return pageInstance.getConfiguration();
    }

    /**
     * Returns a description of this PageAction.
     * @since 4.2.1
     * @return the page's description as JSON.
     */
    @Path(":page")
    @GET
    @Produces(MimeTypes.APPLICATION_JSON_UTF8)
    public Map<String, Object> getPageDescription() {
        Map<String, Object> description = new HashMap<String, Object>();
        description.put("superclass", pageInstance.getActionClass().getSuperclass().getName());
        description.put("class", pageInstance.getActionClass().getName());
        description.put("page", pageInstance.getPage());
        return description;
    }
}