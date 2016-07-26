/*
* Copyright (C) 2005-2016 ManyDesigns srl.  All rights reserved.
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

package com.manydesigns.portofino.servlets;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class DemoTTListener extends PortofinoListener {
    public static final String copyright =
            "Copyright (C) 2005-2016, ManyDesigns srl";

//    @Override
//    protected void discoverModules(ModuleRegistry moduleRegistry, ClassLoader classLoader) {
//        logger.info("Registering modules");
//        try {
//            Module[] modules = new Module[] {
//                    new BaseModule(), new PageactionsModule(), new PageactionsadminModule(),
//                    new DatabaseModule(), new MysqlModule(), new GooglecloudsqlModule(),
//                    new CrudModule(), new TextModule(), new ThemeModule(),
//                    (Module) classLoader.loadClass("com.manydesigns.portofino.modules.DemoTTModule").newInstance()
//            };
//            for(Module module : modules) {
//                logger.debug("Adding module " + module);
//                moduleRegistry.getModules().add(module);
//            }
//        } catch (Exception e) {
//            logger.error("Could not register modules", e);
//            super.discoverModules(moduleRegistry, classLoader);
//        }
//    }
}
