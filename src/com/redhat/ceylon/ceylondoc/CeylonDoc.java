/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.redhat.ceylon.ceylondoc;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.IntersectionType;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.Scope;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.UnionType;
import com.redhat.ceylon.compiler.typechecker.model.Unit;

public abstract class CeylonDoc extends Markup {

    protected final CeylonDocTool tool;
    protected final Module module;

    public CeylonDoc(Module module, CeylonDocTool tool, Writer writer) {
        super(writer);
        this.module = module;
        this.tool = tool;
    }
    
    protected void link(ProducedType type) throws IOException {
        TypeDeclaration decl = type.getDeclaration();
        if(decl instanceof UnionType){
            UnionType ut = (UnionType) decl;
            // try to simplify if possible
            if (ut.getCaseTypes().size()==2) {
                Unit unit = decl.getUnit();
                if (com.redhat.ceylon.compiler.typechecker.model.Util.isElementOfUnion(ut, unit.getNothingDeclaration())) {
                    link(unit.getDefiniteType(type));
                    write("?");
                    return;
                }
                if (com.redhat.ceylon.compiler.typechecker.model.Util.isElementOfUnion(ut, unit.getEmptyDeclaration()) &&
                        com.redhat.ceylon.compiler.typechecker.model.Util.isElementOfUnion(ut, unit.getSequenceDeclaration())) {
                    link(unit.getElementType(type));
                    write("[]");
                    return;
                }
            }
            // simplification failed, do it the hard way
            boolean first = true;
            for(ProducedType ud : ut.getCaseTypes()){
                if(first){
                    first = false;
                }else{
                    write("|");
                }
                link(ud);
            }
        }else if(decl instanceof IntersectionType){
            IntersectionType it = (IntersectionType) decl;
            boolean first = true;
            for(ProducedType id : it.getSatisfiedTypes()){
                if(first){
                    first = false;
                }else{
                    write("&amp;");
                }
                link(id);
            }
        }else if(decl instanceof ClassOrInterface){
            link((ClassOrInterface) decl, type.getTypeArgumentList());
        } else if (decl instanceof TypeParameter) {
            around("span class='type-parameter'", decl.getName());
        } else {
            write(type.getProducedTypeName());
        }
    }

    protected void link(ClassOrInterface decl, List<ProducedType> typeParameters) throws IOException {
        String name = decl.getName();
        around("a href='" + getObjectUrl(decl) + "'", name);
        if (typeParameters != null && !typeParameters.isEmpty()) {
            write("&lt;");
            boolean once = false;
            for (ProducedType typeParam : typeParameters) {
                if (!once)
                    once = true;
                else
                    write(",");
                link(typeParam);
            }
            write("&gt;");
        }
    }

    protected void linkToMember(Declaration decl) throws IOException {
        ClassOrInterface container = (ClassOrInterface) decl.getContainer();
        String name = decl.getName();
        around("a href='" + getObjectUrl(container) + "#"+ name + "'", name);
    }

    protected String getFileName(Scope klass) {
        List<String> name = new LinkedList<String>();
        while (klass instanceof Declaration) {
            name.add(0, ((Declaration) klass).getName());
            klass = klass.getContainer();
        }
        return join(".", name) + ".html";
    }

    protected File getFolder(Package pkg) {
        File dir = new File(tool.getDestDir(), join("/", pkg.getName()));
        dir.mkdirs();
        return dir;
    }

    protected File getFolder(ClassOrInterface klass) {
        return getFolder(getPackage(klass));
    }

    protected static Package getPackage(Scope decl) {
        while (!(decl instanceof Package)) {
            decl = decl.getContainer();
        }
        return (Package) decl;
    }

    protected static String join(String str, List<String> parts) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iterator = parts.iterator();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
            if (iterator.hasNext())
                stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }

    protected boolean shouldInclude(Declaration decl){
        return tool.shouldInclude(decl);
    }
    
    protected abstract String getObjectUrl(Object to) throws IOException;
    
    protected abstract String getResourceUrl(String to) throws IOException;
    
    protected abstract String getSrcUrl(Object to) throws IOException;

    protected void writeNav(Module module, Object decl, DocType docType) throws IOException {
        open("div class='nav menu'");
        open("div");
        around("a href='"+getObjectUrl(module)+"'", getAccessKeyed("Overview", 'O', "Module documentation"));
        close("div");
        if(docType == DocType.PACKAGE)
            open("div class='selected'");
        else
            open("div");
        if(docType != DocType.MODULE
                && docType != DocType.SEARCH) {
            String url;
            if (decl instanceof Declaration) {
                url = getObjectUrl(getPackage(((Declaration)decl).getContainer()));
            } else if (decl instanceof Package) {
                url = getObjectUrl((Package)decl);
            } else {
                throw new RuntimeException("" + decl);
            }
            around("a href='" + url + "'", getAccessKeyed("Package", 'P', "Package documentation"));
        } else
            write("Package");
        close("div");
        if(docType == DocType.TYPE){
            open("div class='selected'");
            around("a href='"+getObjectUrl(decl)+"'", getAccessKeyed("Type", 'T', "Type documentation"));
        }else{
            open("div");
            write("Type");
        }
        close("div");

        open("div");
        around("a href='"+getResourceUrl("../search.html")+"'", getAccessKeyed("Search", 'S', "Search this module"));
        close("div");
        
        open("div");
        write(module.getNameAsString() + "/" + module.getVersion());
        close("div");

        close("div");
    }
    
    protected String getAccessKeyed(String string, char key, String tooltip) {
        int index = string.indexOf(key);
        if(index == -1)
            return string;
        String before = string.substring(0, index);
        String after = string.substring(index+1);
        return "<span title='" + tooltip + " [Shortcut: " + key + "]'>" +
                before + "<span class='accesskey'>" + key + "</span>" + after +
                "</span>";
    }

    protected void writeKeyboardShortcuts() throws IOException{
        // shortcuts
        open("script type='text/javascript'");
        write("jQuery('html').keypress(function(evt){\n");
        write(" evt = evt || window.event;\n");
        write(" var keyCode = evt.keyCode || evt.which;\n");
        writeKeyboardShortcut('s', getResourceUrl("../search.html"));
        writeKeyboardShortcut('o', getResourceUrl("../index.html"));
        writeAdditionalKeyboardShortcuts();
        write("});\n");
        close("script");
    }

    protected void writeAdditionalKeyboardShortcuts() throws IOException {
        // for subclasses
    }

    protected void writeKeyboardShortcut(char c, String url) throws IOException {
        write(" if(keyCode == "+(int)c+"){\n");
        write("  document.location = '"+url+"';\n"); 
        write(" }\n");
    }

    protected void htmlHead(String title, String... additional) throws IOException {
        write("<?xml charset='UTF-8'?>");
        write("<!DOCTYPE html>");
        open("html");
        open("head");
        around("title", title);
        tag("link href='" + getResourceUrl("style.css") + "' rel='stylesheet' type='text/css'");
        for (String add : additional) {
            if (add.endsWith(".css")) {
                tag("link href='" + getResourceUrl(add) + "' rel='stylesheet' type='text/css'");
            } else if (!add.endsWith(".js")) {
                throw new RuntimeException("Unexpected additional resource " + add);
            }
        }
        around("script type='text/javascript' src='" + getResourceUrl("jquery-1.7.min.js") + "'");
        around("script type='text/javascript' src='" + getResourceUrl("ceylond.js") + "'");
        for (String add : additional) {
            if (add.endsWith(".js")) {
                around("script type='text/javascript' src='" + getResourceUrl(add) + "'");
            } else if (!add.endsWith(".css")) {
                throw new RuntimeException("Unexpected additional resource " + add);
            }
        }
        close("head");
        open("body");
        writeKeyboardShortcuts();
    }
    
    protected void writeSourceLink(Object modPkgOrDecl) throws IOException {
        String srcUrl = getSrcUrl(modPkgOrDecl);
        if (!tool.isOmitSource()
                && srcUrl != null) {
            open("div class='source-code "+DocType.typeOf(modPkgOrDecl).name().toLowerCase()+"'");
            around("a href='" + srcUrl + "'", "Source Code");
            close("div");
        }
    }
}


