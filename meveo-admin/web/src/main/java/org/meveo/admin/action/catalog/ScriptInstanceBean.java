/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.catalog;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.collections.CollectionUtils;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.admin.ViewBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.elresolver.ELException;
import org.meveo.model.scripts.*;
import org.meveo.model.security.Role;
import org.meveo.service.admin.impl.RoleService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.script.CustomScriptService;
import org.meveo.service.script.ScriptInstanceService;
import org.primefaces.model.DualListModel;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Standard backing bean for {@link org.meveo.model.scripts.ScriptInstance} (extends {@link org.meveo.admin.action.BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their
 * create, edit, view, delete operations). It works with Manaty custom JSF components.
 */
@Named
@ViewScoped
@ViewBean
public class ScriptInstanceBean extends BaseBean<ScriptInstance> {
    private static final long serialVersionUID = 1L;
    /**
     * Injected @{link ScriptInstance} service. Extends {@link org.meveo.service.base.PersistenceService}.
     */
    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private RoleService roleService;

    private DualListModel<Role> execRolesDM;
    private DualListModel<Role> sourcRolesDM;

    private List<ScriptIO> inputs = new ArrayList<>();
    private List<ScriptIO> outputs = new ArrayList<>();


    public void initCompilationErrors() {
        if (FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest()) {
            return;
        }
        if (getObjectId() == null) {
            return;
        }

        if (entity == null) {
            initEntity();
        }

        if (entity.isError()) {
            scriptInstanceService.compileScript(entity, true);
        }
    }

    public DualListModel<Role> getExecRolesDM() {

        if (execRolesDM == null) {
            List<Role> perksSource = roleService.getAllRoles();
            List<Role> perksTarget = new ArrayList<>();
            if (getEntity().getExecutionRoles() != null) {
                perksTarget.addAll(getEntity().getExecutionRoles());
            }
            perksSource.removeAll(perksTarget);
            execRolesDM = new DualListModel<>(perksSource, perksTarget);
        }
        return execRolesDM;
    }

    public DualListModel<Role> getSourcRolesDM() {

        if (sourcRolesDM == null) {
            List<Role> perksSource = roleService.getAllRoles();
            List<Role> perksTarget = new ArrayList<>();
            if (getEntity().getSourcingRoles() != null) {
                perksTarget.addAll(getEntity().getSourcingRoles());
            }
            perksSource.removeAll(perksTarget);
            sourcRolesDM = new DualListModel<>(perksSource, perksTarget);
        }
        return sourcRolesDM;
    }

    public void setExecRolesDM(DualListModel<Role> perks) {
        this.execRolesDM = perks;
    }

    public void setSourcRolesDM(DualListModel<Role> perks) {
        this.sourcRolesDM = perks;
    }

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link org.meveo.admin.action.BaseBean}.
     */
    public ScriptInstanceBean() {
        super(ScriptInstance.class);

    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<ScriptInstance> getPersistenceService() {
        return scriptInstanceService;
    }

    @Override
    protected String getListViewName() {
        return "scriptInstances";
    }

    /**
     * Fetch customer field so no LazyInitialize exception is thrown when we access it from account edit view.
     * 
     */
    @Override
    protected List<String> getFormFieldsToFetch() {
        return Arrays.asList("executionRoles", "sourcingRoles");
    }

    @Override
    protected String getDefaultSort() {
        return "code";
    }

    @Override
    @ActionMethod
    public String saveOrUpdate(boolean killConversation) throws BusinessException, ELException {
        String code = entity.getCode();
        if (entity.getSourceTypeEnum() == ScriptSourceTypeEnum.JAVA) {
            code = CustomScriptService.getFullClassname(entity.getScript());

            // check script existed full class name in class path
            if (CustomScriptService.isOverwritesJavaClass(code)) {
                messages.error(new BundleKey("messages", "message.scriptInstance.classInvalid"), code);
                return null;
            }
        }

        // check duplicate script
        CustomScript scriptDuplicate = scriptInstanceService.findByCode(code); // genericScriptService
        if (scriptDuplicate != null && !scriptDuplicate.getId().equals(entity.getId())) {
            messages.error(new BundleKey("messages", "scriptInstance.scriptAlreadyExists"), code);
            return null;
        }

        // Update roles
        getEntity().getExecutionRoles().clear();
        if (execRolesDM != null) {
            getEntity().getExecutionRoles().addAll(roleService.refreshOrRetrieve(execRolesDM.getTarget()));
        }

        // Update roles
        getEntity().getSourcingRoles().clear();
        if (sourcRolesDM != null) {
            getEntity().getSourcingRoles().addAll(roleService.refreshOrRetrieve(sourcRolesDM.getTarget()));
        }
        if (CollectionUtils.isNotEmpty(inputs)) {
            List<String> scriptInputs = new ArrayList<>();
            for (ScriptIO scriptIO : inputs) {
                if (scriptIO.isEditable()) {
                    scriptInputs.add(scriptIO.getName());
                }
                getEntity().getScriptInputs().clear();
                getEntity().getScriptInputs().addAll(scriptInputs);
            }
        }

        if (CollectionUtils.isNotEmpty(outputs)) {
            List<String> scriptOutputs = new ArrayList<>();
            for (ScriptIO scriptIO : outputs) {
                if (scriptIO.isEditable()) {
                    scriptOutputs.add(scriptIO.getName());
                }
                getEntity().getScriptOutputs().clear();
                getEntity().getScriptOutputs().addAll(scriptOutputs);
            }
        }

        String result = super.saveOrUpdate(false);

        if (entity.isError()) {
        	result = "scriptInstanceDetail.xhtml?faces-redirect=true&objectId=" + getObjectId() + "&edit=true&cid=" + conversation.getId();
        }else {
            if (killConversation) {
                endConversation();
            }
        }

        return result;
    }

    public String execute() {
        scriptInstanceService.test(entity.getCode(), null);
        return null;
    }

    public List<String> getLogs() {
        return scriptInstanceService.getLogs(entity.getCode());
    }

    public boolean isUserHasSourcingRole(ScriptInstance scriptInstance) {
        return scriptInstanceService.isUserHasSourcingRole(scriptInstance);
    }

    public void testCompilation() {
        String code = entity.getCode();
        // check script existed full class name in class path
        if (entity.getSourceTypeEnum() == ScriptSourceTypeEnum.JAVA) {
            code = CustomScriptService.getFullClassname(entity.getScript());
            if (CustomScriptService.isOverwritesJavaClass(code)) {
                messages.error(new BundleKey("messages", "message.scriptInstance.classInvalid"), code);
                return;
            }
        }

        // check duplicate script
        CustomScript scriptDuplicate = scriptInstanceService.findByCode(code); // genericScriptService
        if (scriptDuplicate != null && !scriptDuplicate.getId().equals(entity.getId())) {
            messages.error(new BundleKey("messages", "scriptInstance.scriptAlreadyExists"), code);
            return;
        }

        scriptInstanceService.compileScript(entity, true);
        if (!entity.isError()) {
            messages.info(new BundleKey("messages", "scriptInstance.compilationSuccessfull"));
        }
    }
    
    /**
     * Autocomplete method for selecting a class that implement ICustomFieldEntity. Return a human readable class name. Used in conjunction with CustomFieldAppliesToConverter
     * 
     * @param query Partial class name to match
     * @return
     */
    public List<ScriptInstance> autocompleteScriptsNames(String query) {
        return scriptInstanceService.findByCodeLike(query);
    }

    public List<ScriptIO> getInputs() {
        if (CollectionUtils.isEmpty(inputs)) {
            if (entity.getId() != null && entity.getSourceTypeEnum() == ScriptSourceTypeEnum.JAVA) {
                final List<Accessor> setters = entity.getSetters();
                if (CollectionUtils.isNotEmpty(setters)) {
                    for (Accessor accessor : setters) {
                        inputs.add(createIO(accessor.getName()));
                    }
                }
            }
            if (entity.getId() != null && CollectionUtils.isNotEmpty(entity.getScriptInputs())) {
                ScriptIO input;
                for (String item : entity.getScriptInputs()) {
                    input = new ScriptIO();
                    input.setName(item);
                    inputs.add(input);
                }
            }
        }
        return inputs;
    }

    public List<ScriptIO> getOutputs() {
        if (CollectionUtils.isEmpty(outputs)) {
            if (entity.getId() != null && entity.getSourceTypeEnum() == ScriptSourceTypeEnum.JAVA) {
                final List<Accessor> getters = entity.getGetters();
                if (CollectionUtils.isNotEmpty(getters)) {
                    for (Accessor accessor : getters) {
                        outputs.add(createIO(accessor.getName()));
                    }
                }
            }

            if (entity.getId() != null && CollectionUtils.isNotEmpty(entity.getScriptOutputs())) {
                ScriptIO output;
                for (String item : entity.getScriptOutputs()) {
                    output = new ScriptIO();
                    output.setName(item);
                    outputs.add(output);
                }
            }
        }
        return outputs;
    }

    public void addNewInput() {
        ScriptIO scriptIO = new ScriptIO();
        scriptIO.setEditable(true);
        inputs.add(scriptIO);
    }

    public void addNewOutput() {
        ScriptIO scriptIO = new ScriptIO();
        scriptIO.setEditable(true);
        outputs.add(scriptIO);
    }

    public void removeScriptInput(ScriptIO scriptIO) {
        inputs.remove(scriptIO);
    }

    public void removeScriptOutput(ScriptIO scriptIO) {
        outputs.remove(scriptIO);
    }

    private ScriptIO createIO(String name) {
        ScriptIO scriptIO = new ScriptIO();
        scriptIO.setName(name);
        scriptIO.setEditable(false);
        return scriptIO;
    }
}