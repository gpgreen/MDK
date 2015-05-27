/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.mbee.actions.vieweditor;

import gov.nasa.jpl.mbee.generator.DocumentGenerator;
import gov.nasa.jpl.mbee.generator.DocumentValidator;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mbee.model.Document;
import gov.nasa.jpl.mbee.viewedit.ViewCommentVisitor;
import gov.nasa.jpl.mbee.viewedit.ViewEditUtils;
import gov.nasa.jpl.mbee.web.JsonRequestEntity;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

/**
 * export view comments to view editor
 * 
 * @author dlam
 * 
 */
public class ExportViewCommentsAction extends MDAction {

    private static final long serialVersionUID = 1L;
    private Element            doc;
    public static final String actionid = "ExportViewComments";

    public ExportViewCommentsAction(Element e) {
    	//JJS--MDEV-567 fix: changed 'Export' to 'Commit'
    	//
        super(actionid, "Commit View Comments", null, null);
        doc = e;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GUILog gl = Application.getInstance().getGUILog();

        DocumentValidator dv = null;
        try {
            String response = null;
            Boolean recurse = Utils.getUserYesNoAnswer("Publish view comments recursively?");
            if (recurse == null)
                return;
            dv = new DocumentValidator(doc);
            dv.validateDocument();
            if (dv.isFatal()) {
                dv.printErrors();
                return;
            }
            DocumentGenerator dg = new DocumentGenerator(doc, dv, null);
            Document dge = dg.parseDocument(true, recurse, false);
            ViewCommentVisitor vcv = new ViewCommentVisitor();
            dge.accept(vcv);
            String json = vcv.getJSON();

            // gl.log(json);
            String url = ViewEditUtils.getUrl();
            if (url == null) {
                dv.printErrors();
                return;
            }
            url += "/rest/views/" + doc.getID() + "/comments";
            if (recurse) {
                url += "?recurse=true";
            }
            gl.log("*** Starting export view comments ***");
            PostMethod pm = new PostMethod(url);
            try {
                gl.log("[INFO] Sending...");
                pm.setRequestHeader("Content-Type", "application/json;charset=utf-8");
                pm.setRequestEntity(JsonRequestEntity.create(json));
                HttpClient client = new HttpClient();
                ViewEditUtils.setCredentials(client, url);
                int code = client.executeMethod(pm);
                if (ViewEditUtils.showErrorMessage(code))
                    return;
                response = pm.getResponseBodyAsString();
                // gl.log(response);
                if (response.equals("NotFound"))
                    gl.log("[ERROR] There are some views that are not exported yet, export the views first, then the comments");
                else if (response.equals("ok"))
                	//JJS--MDEV-567 fix: changed 'Export' to 'Commit'
                	//
                    gl.log("[INFO] Commit Successful.");
                else
                    gl.log(response);

            } finally {
                pm.releaseConnection();
            }

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            gl.log(sw.toString()); // stack trace as a string
            ex.printStackTrace();
        }
        if (dv != null)
            dv.printErrors();
    }

}
