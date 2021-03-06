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

package ch.puzzle.itc.mobiliar.business.deploy.control;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.GenerationResult;
import ch.puzzle.itc.mobiliar.business.generator.control.RunSystemCallService;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.GenerationModus;
import ch.puzzle.itc.mobiliar.common.exception.ScriptExecutionException;
import ch.puzzle.itc.mobiliar.common.exception.ScriptExecutionException.REASON;

public class DeploymentAsynchronousExecuterTest {

	@InjectMocks
	DeploymentAsynchronousExecuter deploymentAsynchronousExecuter;
	
	@Mock
	DeploymentExecutionResultHandlerService deploymentExecutionResultHandlerService;
	
	@Mock
	Logger log;
	
	@Mock
	RunSystemCallService systemCallService;
	
	DeploymentEntity deployment;
	
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		deployment = new DeploymentEntity();
		deployment.setId(Integer.valueOf(100));
		
	}
	
	@Test
	public void executeDeployment_sucessfull_deploy_true() {
		// given
		
		final DeploymentEntity deploymentAfterCall = new DeploymentEntity();
		deploymentAfterCall.setId(Integer.valueOf(200));
		
		GenerationResult result = new GenerationResult();
		result.setDeployment(deploymentAfterCall);
		
		// when
		deploymentAsynchronousExecuter.executeDeployment(result, deployment, GenerationModus.DEPLOY);
		
		// then
		verify(deploymentExecutionResultHandlerService, times(1)).handleSuccessfulDeployment(GenerationModus.DEPLOY, result);
	}
	
	@Test
	public void executeDeployment_sucessfull_deploy_false() {
		// given
		
		final DeploymentEntity deploymentAfterCall = new DeploymentEntity();
		deploymentAfterCall.setId(Integer.valueOf(200));
		
		GenerationResult result = new GenerationResult();
		result.setDeployment(deploymentAfterCall);
		// when
		deploymentAsynchronousExecuter.executeDeployment(result, deployment, GenerationModus.SIMULATE);
		
		// then
		verify(deploymentExecutionResultHandlerService, times(1)).handleSuccessfulDeployment(GenerationModus.SIMULATE, result);
	}
	
	@Test
	public void executeDeployment_ScriptExecutionException() throws ScriptExecutionException {
		// given		
		
		final ScriptExecutionException se = new ScriptExecutionException("error", REASON.EXECUTIONEXCEPTION);
		
		String folder = "folder";
		Mockito.doThrow(se).when(systemCallService).getAndExecuteScriptFromGeneratedConfig(folder);

		GenerationResult result = Mockito.mock(GenerationResult.class);
		List<String> folders = new ArrayList<String>();
		folders.add(folder);
		Mockito.when(result.getAllFoldersToExecute()).thenReturn(folders);
		
		// when
		deploymentAsynchronousExecuter.executeDeployment(result, deployment, GenerationModus.DEPLOY);
		
		// then
		verify(deploymentExecutionResultHandlerService, times(0)).handleSuccessfulDeployment(any(GenerationModus.class), any(GenerationResult.class));
		verify(deploymentExecutionResultHandlerService, times(1)).handleUnSuccessfulDeployment(GenerationModus.DEPLOY,deployment, result, se);
	}

}
