/*
* Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
* http://www.manydesigns.com/
*
* Unless you have purchased a commercial license agreement from ManyDesigns srl,
* the following license terms apply:
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License version 3 as published by
* the Free Software Foundation.
*
* There are special exceptions to the terms and conditions of the GPL
* as it is applied to this software. View the full text of the
* exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
* software distribution.
*
* This program is distributed WITHOUT ANY WARRANTY; and without the
* implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
* or write to:
* Free Software Foundation, Inc.,
* 59 Temple Place - Suite 330,
* Boston, MA  02111-1307  USA
*
*/

package com.manydesigns.portofino.breadcrumbs;

import com.manydesigns.elements.xml.XhtmlBuffer;
import com.manydesigns.elements.xml.XhtmlFragment;
import com.manydesigns.portofino.dispatcher.Dispatch;
import com.manydesigns.portofino.dispatcher.SiteNodeInstance;
import com.manydesigns.portofino.dispatcher.CrudNodeInstance;
import com.manydesigns.portofino.model.site.SiteNode;
import com.manydesigns.portofino.util.ShortNameUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class Breadcrumbs implements XhtmlFragment {
    public static final String copyright =
            "Copyright (c) 2005-2011, ManyDesigns srl";
    public static final String ITEM_CSS_CLASS = "breadcrumb-item";
    public static final String SEPARATOR_CSS_CLASS = "breadcrumb-separator";

    protected final Dispatch dispatch;
    protected final List<BreadcrumbItem> items;
    private String separator = " > ";

    public Breadcrumbs(Dispatch dispatch) {
        this.dispatch = dispatch;
        items = new ArrayList<BreadcrumbItem>();

        StringBuilder sb = new StringBuilder();
        sb.append(dispatch.getRequest().getContextPath());
        for (SiteNodeInstance current : dispatch.getSiteNodeInstancePath()) {
            sb.append("/");
            SiteNode siteNode = current.getSiteNode();
            sb.append(siteNode.getId());
            BreadcrumbItem item = new BreadcrumbItem(
                    sb.toString(), siteNode.getTitle(),
                    siteNode.getDescription());
            items.add(item);
            if (current instanceof CrudNodeInstance) {
                CrudNodeInstance instance =
                        (CrudNodeInstance) current;
                if (instance.getPk() != null) {
                    sb.append("/");
                    sb.append(instance.getPk());
                    String name = ShortNameUtils.getName(
                            instance.getClassAccessor(), instance.getObject());
                    String title = String.format("%s (%s)",
                            name, siteNode.getTitle());
                    BreadcrumbItem item2 = new BreadcrumbItem(
                            sb.toString(), name, title);
                    items.add(item2);
                }
            }
        }
    }

    public List<BreadcrumbItem> getItems() {
        return items;
    }

    public void toXhtml(@NotNull XhtmlBuffer xb) {
        for (int i = 0; i < items.size(); i++) {
            BreadcrumbItem current = items.get(i);
            if (i > 0) {
                xb.openElement("span");
                xb.addAttribute("class", SEPARATOR_CSS_CLASS);
                xb.write(separator);
                xb.closeElement("span");
            }
            if (i == items.size() - 1) {
                xb.openElement("span");
                xb.addAttribute("class", ITEM_CSS_CLASS);
                xb.addAttribute("title", current.getTitle());
                xb.write(current.getText());
                xb.closeElement("span");
            } else {
                xb.writeAnchor(current.getHref(),
                        current.getText(),
                        ITEM_CSS_CLASS,
                        current.getTitle());
            }
        }
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}