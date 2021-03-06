/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.presentation.templateEdit;

import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditRelation;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.Permissions;
import ch.puzzle.itc.mobiliar.business.template.boundary.TemplateEditor;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.template.control.TemplatesScreenDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import ch.puzzle.itc.mobiliar.common.exception.GeneralDBException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceTypeNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.TemplateNotDeletableException;
import ch.puzzle.itc.mobiliar.presentation.CompositeBackingBean;
import ch.puzzle.itc.mobiliar.presentation.common.context.SessionContext;
import ch.puzzle.itc.mobiliar.presentation.resourceRelation.events.ChangeSelectedRelationEvent;
import ch.puzzle.itc.mobiliar.presentation.resourcesedit.EditResourceView;
import ch.puzzle.itc.mobiliar.presentation.util.TestingMode;
import ch.puzzle.itc.mobiliar.presentation.util.UserSettings;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CompositeBackingBean
public class TemplateEditDataProvider implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	TemplatesScreenDomainService templatesService;

	@Inject
	Permissions permission;

	@Inject
	SessionContext context;

	@Inject
	TemplateEditor editor;

	@Inject
	UserSettings userSettings;

	@Getter
	private List<TemplateDescriptorEntity> instanceTemplates = new ArrayList<TemplateDescriptorEntity>();

	@Getter
	private List<TemplateDescriptorEntity> resourceTypeTemplates = new ArrayList<TemplateDescriptorEntity>();

	public List<TemplateDescriptorEntity> getInstanceTemplates(boolean relation) {
		return relation ? relationInstanceTemplates : instanceTemplates;
	}

	public List<TemplateDescriptorEntity> getTypeTemplates(boolean relation) {
		return relation ? relationTypeTemplates : resourceTypeTemplates;
	}

	@Getter
	private List<TemplateDescriptorEntity> relationInstanceTemplates = new ArrayList<TemplateDescriptorEntity>();

	@Getter
	private List<TemplateDescriptorEntity> relationTypeTemplates = new ArrayList<TemplateDescriptorEntity>();

	@Getter
	@Setter
	boolean editOrCreateRelationTemplate;

	@Getter
	boolean canDelete;

	@Getter
	boolean canAdd;

	@Getter
	boolean canEdit;

	@Getter
	boolean canListInstanceTemplates;

	@Getter
	boolean canListResTypeTemplates;

	@Inject
	@TestingMode
	private Boolean testing;

	@TestingMode
	public void onChangedTestingMode(@Observes Boolean isTesting) {
		this.testing = isTesting;
	}

	ResourceEditRelation relation;

	private Identifiable resourceOrType;

    /**
     * @return true if testing is tue and initialized, otherwise false
     */
    public boolean isTesting() {
        return testing != null && testing;
    }

	@PostConstruct
	public void init() {
		canListInstanceTemplates = permission.hasPermission(Permission.INSTANCE_TEMP_LIST);
		canListResTypeTemplates = permission.hasPermission(Permission.RES_RESTYPE_TEMPLATE_LIST);
		canEdit = false;
		canAdd = false;
		canDelete = false;
	}

	private void refreshPermissions() {
		canEdit = canEdit();
		canAdd = canAdd();
		canDelete = canDelete(getResourceType());
	}

	public void onChangedContext(@Observes ContextEntity contextEntity) {
		if (resourceOrType != null) {
			refreshPermissions();
		}
	}

	public void onChangedResource(@Observes ResourceEntity resourceEntity) throws ResourceNotFoundException,
			GeneralDBException, ResourceTypeNotFoundException {
		resourceOrType = resourceEntity;
		refreshPermissions();

		loadTemplates();
	}



	public void onChangedResourceType(@Observes ResourceTypeEntity resourceTypeEntity)
			throws GeneralDBException, ResourceTypeNotFoundException, ResourceNotFoundException {
		resourceOrType = resourceTypeEntity;
		refreshPermissions();
		canListInstanceTemplates = false;

		loadTemplates();
	}

	public void onLoadResourceRelation(@Observes ChangeSelectedRelationEvent relation)
			throws GeneralDBException, ResourceTypeNotFoundException, ResourceNotFoundException {
		loadResourceRelation(relation.getRelation());
	}

	public void loadResourceRelation(ResourceEditRelation relation) throws ResourceNotFoundException,
			GeneralDBException, ResourceTypeNotFoundException {
		this.relation = relation;
		if (this.relation != null) {
			if (canListInstanceTemplates && !this.relation.isResourceTypeRelation()) {
				relationInstanceTemplates = templatesService.getGlobalTemplatesForResourceRelation(
						this.relation, userSettings.isTestingMode());
			}
			relationTypeTemplates = templatesService.getGlobalTemplatesForResourceRelationType(
					this.relation, userSettings.isTestingMode());
		}
		Collections.sort(relationInstanceTemplates, new TemplateComparator());
		Collections.sort(relationTypeTemplates, new TemplateComparator());
	}

	@Getter
	@Setter
	private Integer removeTemplateId;

	public void remove() throws ResourceTypeNotFoundException, TemplateNotDeletableException,
			GeneralDBException, ResourceNotFoundException {
		editor.removeTemplate(removeTemplateId);
		if (editOrCreateRelationTemplate) {
			loadResourceRelation(this.relation);
		}
		else {
			loadTemplates();
		}
	}

	private void loadTemplates() throws ResourceNotFoundException, GeneralDBException,
			ResourceTypeNotFoundException {
		if (resourceOrType != null) {
			if (isEditResource() && canListInstanceTemplates) {
				instanceTemplates = templatesService.getGlobalTemplateDescriptorsForResource(
						getResource(), userSettings.isTestingMode());
				Collections.sort(instanceTemplates, new TemplateComparator());
			}

			resourceTypeTemplates = canListResTypeTemplates ? templatesService
					.getGlobalTemplateDescriptorsForResourceType(getResourceType(),
							userSettings.isTestingMode()) : new ArrayList<TemplateDescriptorEntity>();
			Collections.sort(resourceTypeTemplates, new TemplateComparator());
		}
	}

	private boolean canAdd() {
		return context.getIsGlobal()
				&& permission.hasPermissionToTemplateModify(resourceOrType,
				userSettings.isTestingMode());
	}

	private boolean canDelete(ResourceTypeEntity resourceType) {
		return context.getIsGlobal()
				&& (canAddEditOrDeleteShakedownTest() || (isEditResource() ? permission
						.hasPermission(Permission.DELETE_RES_TEMPLATE) : permission
						.hasPermission(Permission.DELETE_RESTYPE_TEMPLATE)));
	}

	private boolean canEdit() {
		return context.getIsGlobal()
				&& (canAddEditOrDeleteShakedownTest() || (isEditResource() ? permission
						.hasPermission(Permission.EDIT_RES_TEMP) : permission
						.hasPermission(Permission.EDIT_RESTYPE_TEMPLATE)));
	}

	private boolean canAddEditOrDeleteShakedownTest() {
		return (userSettings.isTestingMode() && permission.hasPermission(Permission.SHAKEDOWN_TEST_MODE));
	}


	private ResourceTypeEntity getResourceType() {
		if (isEditResource()) {
			return ((ResourceEntity)resourceOrType).getResourceType();
		} else if (isEditResourceType()) {
			return ((ResourceTypeEntity)resourceOrType);
		}
		return null;
	}

	public boolean isEditResource() {
		return resourceOrType != null && resourceOrType instanceof ResourceEntity;
	}

	private boolean isEditResourceType() {
		return resourceOrType != null && resourceOrType instanceof ResourceTypeEntity;
	}

	private ResourceEntity getResource() {
		if (isEditResource()){
			return (ResourceEntity)resourceOrType;
		}
		return null;
	}

}
