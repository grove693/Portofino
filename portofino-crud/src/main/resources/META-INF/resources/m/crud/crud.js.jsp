<%@page contentType="text/javascript" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
var portofino = portofino || {};

portofino.dataTable = function(elem) {
    function removeQueryStringArgument(href, arg) {
        href = href.replace(new RegExp("[?]" + arg + "=[^&]*&", "g"), "?");
        href = href.replace(new RegExp("[?]" + arg + "=[^&]*", "g"), "");
        href = href.replace(new RegExp("[&]" + arg + "=[^&]*&", "g"), "&");
        href = href.replace(new RegExp("[&]" + arg + "=[^&]*", "g"), "");
        return href;
    }

    function makeLoaderFunction(elem) {
        return function loadLinkHref() {
            var href = $(this).attr("href");
            var eventName = elem.find("input[name=eventName]").val();
            if(eventName && !(eventName.length == 0)) {
                href = removeQueryStringArgument(href, eventName);
            }
            href = removeQueryStringArgument(href, "ajax");
            var additionalParameters = (href.indexOf("?") > -1 ? "&" : "?") + "getSearchResultsPage=&ajax=true";
            $.ajax(href + additionalParameters, {
                dataType: "text",
                success: function(data, status, xhr) {
                    var newElem = $(data);
                    elem.replaceWith(newElem);
                    setupDataTable(newElem);
                },
                error: function(xhr, status, errorThrown) {
                    if(xhr.status == 403) {
                        //Redirect to login page (link included in the response)
                        var loginUrl = xhr.responseText;
                        loginUrl = removeQueryStringArgument(loginUrl, "returnUrl");
                        loginUrl = removeQueryStringArgument(loginUrl, "returnUrl");
                        window.location.href =
                                loginUrl + (loginUrl.indexOf("?") > -1 ? "&" : "?") + "returnUrl=" +
                                encodeURIComponent(window.location.href);
                    } else {
                        //TODO
                        alert("There was an error fetching the requested data");
                    }
                }
            });
            return false;
        }
    }
    function setupDataTable(elem) {
        elem.find("a.paginator-link").click(makeLoaderFunction(elem));
        elem.find("a.sort-link").click(makeLoaderFunction(elem));
        elem.find("button[name=bulkDelete]").click(function() {
            return confirm ('<fmt:message key="commons.confirm" />');
        });
    }

    setupDataTable($(elem));
};

$(function() {
    var form = $(".crud-search-form");

    //Enable AJAX paginators
    $(".portofino-datatable").each(function(index, elem) {
        portofino.dataTable(elem);
    });

    form.each(function(i, form) {
        form = $(form);
        form.find(".search_form_toggle_link").click(makeToggleFunction(form));
    });

    function makeToggleFunction(form) {
        var visible = form.data("search-visible");
        return function(event) {
            $(this).next().slideToggle(300);
            visible = !visible;
            if(visible) {
                $(event.target).html('<fmt:message key="layouts.crud.search.hideSearch" />');
            } else {
                $(event.target).html('<fmt:message key="layouts.crud.search.showSearch" />');
            }
            return false;
        };
    }
});