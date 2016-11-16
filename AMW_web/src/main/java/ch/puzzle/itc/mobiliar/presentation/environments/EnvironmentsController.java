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

package ch.puzzle.itc.mobiliar.presentation.environments;

import java.io.Serializable;

import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ch.puzzle.itc.mobiliar.business.environment.control.EnvironmentsScreenDomainService;
import ch.puzzle.itc.mobiliar.business.security.control.SecurityScreenDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.PermissionEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.RoleNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.SavePropertyException;
import ch.puzzle.itc.mobiliar.common.util.NameChecker;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;
import ch.puzzle.itc.mobiliar.presentation.util.UserSettings;

@Named
@RequestScoped
public class EnvironmentsController implements Serializable {

	private static final long serialVersionUID = 1L;


	@Inject
	EnvironmentsScreenDomainService envScreenService;
	
	@Inject
	SecurityScreenDomainService securityScreenDomainService;

	@Inject
	UserSettings userSettings;

	public ContextEntity loadContextWithType(Integer contextId) {
		ContextEntity result = null;
		try {
			if (contextId == null) {
				String message = "No context selected.";
				GlobalMessageAppender.addErrorMessage(message);
			} else {
				result = envScreenService.getContextWithType(contextId);
			}
		} catch (ResourceNotFoundException e) {
			String message = "The selected context can not be found.";
			GlobalMessageAppender.addErrorMessage(message);
		}
		return result;
	}

	public boolean doSave(Integer contextId, String contextName) {
		boolean isSuccessful = false;
		try {
			if (contextId == null) {
				String message = "No context selected.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (contextName == null) {
				String message = "No context name selected.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (!NameChecker.isNameValid(contextName)){
				GlobalMessageAppender.addErrorMessage(NameChecker.getErrorText("environment", contextName));
			} else {
				try{
					envScreenService.saveEnvironment(contextId, contextName);
					String message = "Changes successfully saved.";
					GlobalMessageAppender.addSuccessMessage(message);
					isSuccessful = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (ResourceNotFoundException e) {
			String message = "The selected context can not be found.";
			GlobalMessageAppender.addErrorMessage(message);
		} catch (SavePropertyException e) {
			String message = "Error while saving the changes.";
			GlobalMessageAppender.addErrorMessage(message);
		} 
		return isSuccessful;
	}
	

	public boolean doCreateContext(String newContextName, Integer superContextId, Integer roleId) {
		boolean isSuccessful = false;
		try {
			if (newContextName == null) {
				String message = "Could not read name for new context.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (newContextName.isEmpty()) {
				String message = "The name for the context must not be empty.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (!NameChecker.isNameValid(newContextName)){
				GlobalMessageAppender.addErrorMessage(
						NameChecker.getErrorText("environment", newContextName));
			} else if (superContextId == null) {
				String message = "No parent context selected.";
				GlobalMessageAppender.addErrorMessage(message);
			}else {
				try{
				     ContextEntity newContext = envScreenService.createContextByName(newContextName, superContextId);
				     if(newContext.isEnvironment()) {
					    doCreateNewPermissionToDeployEnviroments(newContextName, roleId, newContext);
					}
					isSuccessful = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (ResourceNotFoundException e) {
			String message = "The selected context can not be found.";
			GlobalMessageAppender.addErrorMessage(message);
		} catch (ElementAlreadyExistsException e) {
			ElementAlreadyExistsException ex = (ElementAlreadyExistsException) e;
			String errorMessage = null;
			if(ex.getExistingObjectClass() == ContextEntity.class){
				errorMessage = " Context with the name " + ex.getExistingObjectName() + " already exists"; 
			}
			GlobalMessageAppender.addErrorMessage(errorMessage);
		}
		
		return isSuccessful;
	}

	private boolean doCreateNewPermissionToDeployEnviroments(String newContextName, Integer roleId, ContextEntity context) {
		boolean isSuccessful = false;
		try {
			if(context == null){
				String message = "Could not context find";
				GlobalMessageAppender.addErrorMessage(message);
			}else if(roleId == null){
				String message = "No role selected";
				GlobalMessageAppender.addErrorMessage(message);
			}else if(roleId==0){
				securityScreenDomainService.createPermissionForRole(newContextName, securityScreenDomainService.createOrGetPermissionWithoutAssignedRole().getId(), context);
				String message = "Context " + newContextName + " successfully created (" + securityScreenDomainService.getRoleById(securityScreenDomainService.createOrGetPermissionWithoutAssignedRole().getId()).getName() +" )";
				GlobalMessageAppender.addSuccessMessage(message);
			}else{
				securityScreenDomainService.createPermissionForRole(newContextName, roleId, context);
				String message = "Context " + newContextName + " successfully created ( Assigned to role:" + securityScreenDomainService.getRoleById(roleId).getName() +" )";
				GlobalMessageAppender.addSuccessMessage(message);
			}
		} catch (RoleNotFoundException e) {
			String message = "Could not role find";
			GlobalMessageAppender.addErrorMessage(message);
		} catch (ElementAlreadyExistsException e) {
			ElementAlreadyExistsException ex = (ElementAlreadyExistsException) e;
				String errorMessage = null;
			if (ex.getExistingObjectClass() == PermissionEntity.class) {
				errorMessage = "An permission with the name " + ex.getExistingObjectName() + " already exists.";
			}else if(ex.getExistingObjectClass() == ContextEntity.class){
				errorMessage = "A Context with the name " + ex.getExistingObjectName() + " already exists"; 
			} else{
				errorMessage = "An object ("+ex.getExistingObjectClass().getSimpleName()+") already exists";
			}
			GlobalMessageAppender.addErrorMessage(errorMessage);
		}
		return isSuccessful;
		
	}

	public boolean doRemoveContext(Integer contextId) {
		boolean isSuccessful = false;
		String deletedContextName=null;
		try {
			if (contextId == null){
				String message = "No context selected.";
				GlobalMessageAppender.addErrorMessage(message);
			} else {
				try{

					deletedContextName =  envScreenService.deleteContext(contextId);
					String message = "Context and Permission: " + deletedContextName + " successfully removed";
					GlobalMessageAppender.addSuccessMessage(message);
					isSuccessful = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (AMWException e) {
			String message = "Could not remove context.";
			GlobalMessageAppender.addErrorMessage(message);
		} 
		return isSuccessful;
	}

}