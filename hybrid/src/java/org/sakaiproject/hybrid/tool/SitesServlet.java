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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Based on
 * https://source.caret.cam.ac.uk/camtools/trunk/camtools/sdata/tool/sakai
 * -sdata-impl/src/main/java/org/sakaiproject/sdata/services/mcp/
 * MyCoursesAndProjectsBean.java
 * <p>
 * No required get parameters. Runs in the context of the current user. Returns
 * all sites that the user has access to visit.
 */
@SuppressWarnings(value = "MTIA_SUSPECT_SERVLET_INSTANCE_FIELD", justification = "dependencies only mutated only during init()")
public class SitesServlet extends HttpServlet {
	private static final long serialVersionUID = 7907409301065984518L;
	private static final Log LOG = LogFactory.getLog(SitesServlet.class);
	private static final String CATEGORIZED = "categorized";
	@SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient SessionManager sessionManager;
	@SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient SiteService siteService;
	@SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient ServerConfigurationService serverConfigurationService;
	@SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient ComponentManager componentManager;
	protected transient MoreSiteViewImpl moreSiteViewImpl;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("doGet(HttpServletRequest " + req
					+ ", HttpServletResponse " + resp + ")");
		}
		final String categorizedParam = req.getParameter(CATEGORIZED);
		boolean categorized = false;
		if (categorizedParam != null) {
			categorized = Boolean.parseBoolean(categorizedParam);
		}
		// sites for current user
		final JSONObject json = new JSONObject();
		final String principal = sessionManager.getCurrentSession()
				.getUserEid();
		if (principal == null || "".equals(principal)) {
			json.element("principal", "anonymous");
		} else {
			json.element("principal", principal);
		}
		final List<Site> siteList = siteService.getSites(
				org.sakaiproject.site.api.SiteService.SelectionType.ACCESS,
				null, null, null,
				org.sakaiproject.site.api.SiteService.SortType.TITLE_ASC, null);
		if (siteList != null) {
			if (categorized) {
				List<Map<String, List<Site>>> categorizedSitesList = moreSiteViewImpl
						.categorizeSites(siteList);
				final JSONArray categoriesArrayJson = new JSONArray();
				for (final Map<String, List<Site>> map : categorizedSitesList) {
					if (map.size() != 1) {
						throw new IllegalStateException(
								"The categorized maps must contain only one key per map!");
					}
					for (final Entry<String, List<Site>> entry : map.entrySet()) {
						final String category = entry.getKey();
						final List<Site> sortedSites = entry.getValue();
						final JSONObject categoryJson = new JSONObject();
						categoryJson.element("category", category);
						categoryJson.element("size", sortedSites.size());
						final JSONArray sitesArrayJson = new JSONArray();
						for (final Site site : sortedSites) {
							sitesArrayJson.add(renderSiteJson(site));
						}
						categoryJson.element("sites", sitesArrayJson);
						categoriesArrayJson.add(categoryJson);
					}
				}
				json.element("size", siteList.size());
				json.element("categories", categoriesArrayJson);
			} else { // not categorized
				final JSONArray sitesArrayJson = new JSONArray();
				for (Site site : siteList) {
					sitesArrayJson.add(renderSiteJson(site));
				}
				json.element("size", siteList.size());
				json.element("sites", sitesArrayJson);
			}
		}
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setStatus(HttpServletResponse.SC_OK);
		json.write(resp.getWriter());
	}

	private JSONObject renderSiteJson(Site site) {
		final JSONObject siteJson = new JSONObject();
		siteJson.element("title", site.getTitle());
		siteJson.element("id", site.getId());
		siteJson.element("url", site.getUrl());
		siteJson.element("description", site.getDescription());
		// siteJson.element("iconUrl", site.getIconUrl());
		// siteJson.element("owner",
		// site.getCreatedBy().getDisplayName());
		// siteJson.element("members", site.getMembers().size());
		// siteJson.element("siteType", site.getType());
		// TO DO ISO8601 date format or other?
		// siteJson.element("creationDate", new
		// SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz")
		// .format(site.getCreatedDate()));
		return siteJson;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (componentManager == null) {
			componentManager = (ComponentManager) org.sakaiproject.component.cover.ComponentManager
					.get(ComponentManager.class);
		}
		if (componentManager == null) {
			throw new IllegalStateException("componentManager == null");
		}
		sessionManager = (SessionManager) componentManager
				.get(SessionManager.class);
		if (sessionManager == null) {
			throw new IllegalStateException("SessionManager == null");
		}
		siteService = (SiteService) componentManager.get(SiteService.class);
		if (siteService == null) {
			throw new IllegalStateException("SiteService == null");
		}
		serverConfigurationService = (ServerConfigurationService) componentManager
				.get(ServerConfigurationService.class);
		if (serverConfigurationService == null) {
			throw new IllegalStateException(
					"ServerConfigurationService == null");
		}
		moreSiteViewImpl = new MoreSiteViewImpl(serverConfigurationService);
	}

	/**
	 * Only used for unit testing setup.
	 * 
	 * @param componentManager
	 */
	protected void setupTestCase(ComponentManager componentManager) {
		if (componentManager == null) {
			throw new IllegalArgumentException("componentManager == null");
		}
		this.componentManager = componentManager;
	}
}
