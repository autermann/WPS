/**
 * Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 * 
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 * 
 * This program is free software; you can redistribute and/or modify it under 
 * the terms of the GNU General Public License version 2 as published by the 
 * Free Software Foundation.
 * 
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.wps.webapp.web;

import java.util.Map;

import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("repositories")
public class RepositoriesController extends BaseConfigurationsController {

	/**
	 * Display repository configuration modules
	 * 
	 * @return The repositories view
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String displayRepositories(Model model) {
		ConfigurationCategory category = ConfigurationCategory.REPOSITORY;
		Map<String, ConfigurationModule> configurations = configurationManager.getConfigurationServices()
				.getConfigurationModulesByCategory(category);
		model.addAttribute("configurations", configurations);
		LOGGER.info("Reterived '{}' configurations.", category);
		return "repositories";
	}

	/**
	 * Toggle the status of an algorithm
	 */
	// {algorithm:.+} is used in case the name has dots, otherwise, it will be truncated
	@RequestMapping(value = "algorithms/activate/{moduleClassName}/{algorithm:.+}", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	public void toggleAlgorithmStatus(@PathVariable String moduleClassName, @PathVariable String algorithm) {
		ConfigurationModule module = configurationManager.getConfigurationServices().getConfigurationModule(
				moduleClassName);
		AlgorithmEntry algorithmEntry = configurationManager.getConfigurationServices().getAlgorithmEntry(module,
				algorithm);
		boolean currentStatus = algorithmEntry.isActive();
		configurationManager.getConfigurationServices().setAlgorithmEntry(moduleClassName, algorithm, !currentStatus);
		LOGGER.info("Algorithm '{}' status in module '{}' has been updated to '{}'", algorithm, moduleClassName,
				!currentStatus);
	}
}
