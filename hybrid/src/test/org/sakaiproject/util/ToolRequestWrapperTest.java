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
package org.sakaiproject.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

public class ToolRequestWrapperTest extends TestCase {
	ToolRequestWrapper toolRequestWrapper;
	HttpServletRequest request;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		request = mock(HttpServletRequest.class);
		when(request.getRemoteUser()).thenReturn("username");
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.ToolRequestWrapper#ToolRequestWrapper(javax.servlet.http.HttpServletRequest)}
	 * .
	 */
	public void testToolRequestWrapperHttpServletRequest() {
		toolRequestWrapper = new ToolRequestWrapper(request);
		assertEquals("username", toolRequestWrapper.getRemoteUser());
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.ToolRequestWrapper#ToolRequestWrapper(javax.servlet.http.HttpServletRequest, java.lang.String)}
	 * .
	 */
	public void testToolRequestWrapperHttpServletRequestString() {
		toolRequestWrapper = new ToolRequestWrapper(request, "username2");
		assertEquals("username2", toolRequestWrapper.getRemoteUser());
	}

}
