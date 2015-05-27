package gov.nasa.jpl.mbee.ems.validation.actions;

import gov.nasa.jpl.mbee.ems.ImportUtility;
import gov.nasa.jpl.mgss.mbee.docgen.validation.IRuleViolationAction;
import gov.nasa.jpl.mgss.mbee.docgen.validation.RuleViolationAction;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Map;

import org.json.simple.JSONObject;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;

public class ImportConnector extends RuleViolationAction implements AnnotationAction, IRuleViolationAction {

    private static final long serialVersionUID = 1L;
    private Connector element;
    private JSONObject spec;
    private JSONObject result;
    public ImportConnector(Connector e, JSONObject spec, JSONObject result) {
        //JJS--MDEV-567 fix: changed 'Import' to 'Accept'
        //
        super("ImportConnector", "Accept connector", null, null);
        this.element = e;
        this.spec = spec;
        this.result = result;
    }
    
    @Override
    public boolean canExecute(Collection<Annotation> arg0) {
        return true;
    }

    @Override
    public void execute(Collection<Annotation> annos) {
        executeMany(annos, "Change Connectors");
    }
    
    @Override
    protected boolean doAction(Annotation anno) {
        if (anno != null) {
            Element e = (Element)anno.getTarget();
            if (!e.isEditable()) {
                Application.getInstance().getGUILog().log("[ERROR] " + e.get_representationText() + " isn't editable");
                return false;
            }
            JSONObject resultOb = (JSONObject)((Map<String, JSONObject>)result.get("elementsKeyed")).get(e.getID());
            ImportUtility.setConnectorEnds((Connector)e, (JSONObject)resultOb.get("specialization"));
        } else {
            ImportUtility.setConnectorEnds(element, spec);
        }
        return true;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!element.isEditable()) {
            Application.getInstance().getGUILog().log("[ERROR] Element is not editable!");
            return;
        }
        execute("Change Connector");
    }
}
