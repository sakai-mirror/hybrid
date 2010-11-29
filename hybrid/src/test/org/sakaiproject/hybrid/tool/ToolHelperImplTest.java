/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.hybrid.tool;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.api.Placement;

@RunWith(MockitoJUnitRunner.class)
public class ToolHelperImplTest {
	ToolHelperImpl toolHelperImpl = null;

	@Mock
	SecurityService securityService = null;
	@Mock
	Site site = null;
	@Mock
	Placement placement = null;
	@Mock
	Properties properties = null;

	@BeforeClass
	public static void beforeClass() {
		Properties log4jProperties = new Properties();
		log4jProperties.put("log4j.rootLogger", "ALL, A1");
		log4jProperties.put("log4j.appender.A1",
				"org.apache.log4j.ConsoleAppender");
		log4jProperties.put("log4j.appender.A1.layout",
				"org.apache.log4j.PatternLayout");
		log4jProperties.put("log4j.appender.A1.layout.ConversionPattern",
				PatternLayout.TTCC_CONVERSION_PATTERN);
		log4jProperties.put("log4j.threshold", "ALL");
		PropertyConfigurator.configure(log4jProperties);
	}

	@Before
	public void setUp() throws Exception {
		Properties log4jProperties = new Properties();
		log4jProperties.put("log4j.rootLogger", "ALL, A1");
		log4jProperties.put("log4j.appender.A1",
				"org.apache.log4j.ConsoleAppender");
		log4jProperties.put("log4j.appender.A1.layout",
				"org.apache.log4j.PatternLayout");
		log4jProperties.put("log4j.appender.A1.layout.ConversionPattern",
				PatternLayout.TTCC_CONVERSION_PATTERN);
		log4jProperties.put("log4j.threshold", "ALL");
		PropertyConfigurator.configure(log4jProperties);
		toolHelperImpl = new ToolHelperImpl(securityService);
		toolHelperImpl.securityService = securityService;
		when(site.getReference()).thenReturn("/foo/bar/baz");
		when(
				properties
						.getProperty(ToolHelperImpl.TOOLCONFIG_REQUIRED_PERMISSIONS))
				.thenReturn("annc.read,site.upd|site.visit");
		when(placement.getConfig()).thenReturn(properties);
		when(securityService.unlock("annc.read", "/foo/bar/baz")).thenReturn(
				true);
		when(securityService.unlock("site.upd", "/foo/bar/baz")).thenReturn(
				false);
		when(securityService.unlock("site.visit", "/foo/bar/baz")).thenReturn(
				true);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.tool.ToolHelperImpl#allowTool(org.sakaiproject.site.api.Site, org.sakaiproject.tool.api.Placement)}
	 * .
	 */
	@Test
	public void testAllowToolNullParameters() {
		boolean allowed = toolHelperImpl.allowTool(null, null);
		assertTrue("allowTool should return true", allowed == true);
		verify(placement, never()).getConfig();
		verify(properties, never()).getProperty(
				ToolHelperImpl.TOOLCONFIG_REQUIRED_PERMISSIONS);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.tool.ToolHelperImpl#allowTool(org.sakaiproject.site.api.Site, org.sakaiproject.tool.api.Placement)}
	 * .
	 */
	@Test
	public void testAllowToolNullToolConfigRequiredParameters() {
		when(
				properties
						.getProperty(ToolHelperImpl.TOOLCONFIG_REQUIRED_PERMISSIONS))
				.thenReturn(null);
		boolean allowed = toolHelperImpl.allowTool(site, placement);
		assertTrue("allowTool should return true", allowed == true);
		verify(securityService, never()).unlock(anyString(), anyString());
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.tool.ToolHelperImpl#allowTool(org.sakaiproject.site.api.Site, org.sakaiproject.tool.api.Placement)}
	 * .
	 */
	@Test
	public void testAllowToolEmptyToolConfigRequiredParameters() {
		when(
				properties
						.getProperty(ToolHelperImpl.TOOLCONFIG_REQUIRED_PERMISSIONS))
				.thenReturn(" ");
		boolean allowed = toolHelperImpl.allowTool(site, placement);
		assertTrue("allowTool should return true", allowed == true);
		verify(securityService, never()).unlock(anyString(), anyString());
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.tool.ToolHelperImpl#allowTool(org.sakaiproject.site.api.Site, org.sakaiproject.tool.api.Placement)}
	 * .
	 */
	@Test
	public void testAllowToolAffirmative() {
		boolean allowed = toolHelperImpl.allowTool(site, placement);
		assertTrue("allowTool should return true", allowed == true);
		verify(securityService).unlock("annc.read", "/foo/bar/baz");
		verify(securityService).unlock("site.upd", "/foo/bar/baz");
		verify(securityService).unlock("site.visit", "/foo/bar/baz");
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.tool.ToolHelperImpl#allowTool(org.sakaiproject.site.api.Site, org.sakaiproject.tool.api.Placement)}
	 * .
	 */
	@Test
	public void testAllowToolNegative() {
		when(securityService.unlock("site.visit", "/foo/bar/baz")).thenReturn(
				false);
		boolean allowed = toolHelperImpl.allowTool(site, placement);
		assertTrue("allowTool should return false", allowed == false);
		verify(securityService).unlock("annc.read", "/foo/bar/baz");
		verify(securityService).unlock("site.upd", "/foo/bar/baz");
		verify(securityService).unlock("site.visit", "/foo/bar/baz");
	}

	@Test
	public void testConstructor() {
		try {
			toolHelperImpl = new ToolHelperImpl(null);
			fail("Should not be reached");
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		}
	}

}
