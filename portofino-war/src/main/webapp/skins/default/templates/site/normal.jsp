<%
    // Avoid caching of dynamic pages
    response.setHeader("Pragma", "no-cache");
    response.addHeader("Cache-Control", "must-revalidate");
    response.addHeader("Cache-Control", "no-cache");
    response.addHeader("Cache-Control", "no-store");
    response.setDateHeader("Expires", 0);
%><%@ page import="com.manydesigns.portofino.logic.SecurityLogic"
%><%@ page import="java.util.Map"
%><%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"
%><%@ taglib tagdir="/WEB-INF/tags" prefix="portofino"
%><%@ taglib prefix="mde" uri="/manydesigns-elements"
%><stripes:layout-definition><%--
--%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml" lang="en">
    <head>
        <jsp:include page="../../head.jsp"/>
        <link rel="stylesheet" type="text/css"
              href="<stripes:url value="/skins/default/templates/site/site.css"/>"/>
        <stripes:layout-component name="customScripts"/>
        <jsp:useBean id="actionBean" scope="request"
             type="com.manydesigns.portofino.pageactions.AbstractPageAction"/>
        <title><c:out value="${actionBean.dispatch.lastPageInstance.page.description}"/></title>
    </head>
    <body class="yui-skin-sam">
    <div id="doc4" class="yui-t4">
        <div id="hd">
            <jsp:include page="../../header.jsp"/>
            <div class="tabs">
                <jsp:include page="tabs.jsp">
                    <jsp:param name="" value="1" />
                </jsp:include>
            </div>
        </div>
        <div id="bd">
            <div id="yui-main">
                <div id="content" class="yui-b">
                    <div class="contentBody">
                        <stripes:layout-component name="contentBody">
                            <div class="portlets">
                                <mde:sessionMessages/>
                                <portofino:portlets list="default" />
                                <div class="yui-g first">
                                    <portofino:portlets list="contentLayoutLeft" cssClass="yui-u first" />
                                    <portofino:portlets list="contentLayoutRight" cssClass="yui-u" />
                                </div>
                                <portofino:portlets list="contentLayoutBottom" />
                            </div>
                        </stripes:layout-component>
                    </div>
                </div>
            </div>
            <div id="sidebar" class="yui-b">
                <div style="border: dashed 1px black; padding: 5px;">
                    <stripes:form action="/actions/admin/page" method="post" id="pageAdminForm">
                        <input type="hidden" name="originalPath" value="${actionBean.dispatch.originalPath}" />
                        <!-- Admin buttons -->
                        <% if(SecurityLogic.isAdministrator(request)) { %>
                            <div class="contentBarButtons">
                                <portofino:page-layout-button />
                                <portofino:reload-model-button />
                                <portofino:page-children-button />
                                <portofino:page-permissions-button />
                                <portofino:page-copy-button />
                                <portofino:page-new-button />
                                <portofino:page-delete-button />
                                <portofino:page-move-button />
                            </div>
                        <% } %>
                    </stripes:form>
                </div>
                <jsp:include page="../../navigation.jsp">
                    <jsp:param name="navigation.startingLevel" value="1" />
                </jsp:include>
            </div>
            <script type="text/javascript">
                fixSideBar();
            </script>
        </div>
        <div id="ft">
            <jsp:include page="../../footer.jsp"/>
        </div>
    </div>
    </body>
    </html>
</stripes:layout-definition>