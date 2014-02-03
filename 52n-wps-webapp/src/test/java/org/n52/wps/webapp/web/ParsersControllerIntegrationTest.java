package org.n52.wps.webapp.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.n52.wps.webapp.common.AbstractIntegrationTest;
import org.n52.wps.webapp.testmodules.TestConfigurationModule3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ParsersControllerIntegrationTest extends AbstractIntegrationTest {

	private MockMvc mock;

	@Autowired
	private TestConfigurationModule3 module;

	@Before
	public void setup() {
		mock = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void displayParsers() throws Exception {
		RequestBuilder builder = get("/parsers").accept(MediaType.TEXT_HTML);
		this.mock.perform(builder)
                .andExpect(status().isOk())
                .andExpect(view().name("parsers"))
				.andExpect(model().attributeExists("configurations"));
	}

	@Test
	public void processPost_success() throws Exception {
		RequestBuilder request = post("/parsers").param("key", "test.string.key")
				.param("value", "new posted value").param("module", module.getClass().getName());
		this.mock.perform(request).andExpect(status().isOk());
		assertEquals("new posted value", module.getStringMember());
		assertEquals("new posted value", module.getConfigurationEntries().get(0).getValue());
	}

	@Test
	public void processPost_failure() throws Exception {
		RequestBuilder request = post("/parsers").param("key", "test.integer.key")
				.param("value", "invalid integer").param("module", module.getClass().getName());
		this.mock.perform(request).andExpect(status().isBadRequest());
	}

	@Test
	public void toggleModuleStatus() throws Exception {
		assertTrue(module.isActive());
		RequestBuilder request = post("/parsers/activate/{moduleClassName}/false", module.getClass().getName());
		this.mock.perform(request).andExpect(status().isOk());
		assertFalse(module.isActive());
	}
}
