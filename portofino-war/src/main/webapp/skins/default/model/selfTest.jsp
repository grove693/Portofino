<%@ page contentType="text/html;charset=ISO-8859-1" language="java"
         pageEncoding="ISO-8859-1"
%><%@ taglib prefix="s" uri="/struts-tags"
%><%@ taglib prefix="mdes" uri="/manydesigns-elements-struts2"
%><s:include value="/skins/default/header.jsp"/>
<s:form method="post">
    <s:include value="/skins/default/model/selfTestButtonsBar.jsp"/>
    <div id="inner-content">
        <h1>Self test</h1>
        <p>Source model: on database. Target model: in memory.</p>
        <p>
        Show:
        <br/>
        <s:checkbox name="showBothNull" value="showBothNull"/>Both null
        <br/>
        <s:checkbox name="showSourceNull" value="showSourceNull"/>Source null
        <br/>
        <s:checkbox name="showTargetNull" value="showTargetNull"/>Target null
        <br/>
        <s:checkbox name="showEqual" value="showEqual"/>Equal
        <br/>
        <s:checkbox name="showDifferent" value="showDifferent"/>Different
        </p><p>
        Expand tree? <s:checkbox name="expandTree" value="expandTree"/>
        </p><table id="tree">
            <thead>
                <tr><th>Datamodel object</th><th>Type</th><th>Status</th></tr>
            </thead>
            <tbody>
                <mdes:write value="treeTableDiffer"/>
            </tbody>
        </table>
        <script type="text/javascript">
            $("#tree").treeTable({
                initialState : "<s:property value="expandTree ? 'expanded' : 'collapsed'"/>"
            });
        </script>
    </div>
    <s:include value="/skins/default/model/selfTestButtonsBar.jsp"/>
</s:form>
<s:include value="/skins/default/footer.jsp"/>