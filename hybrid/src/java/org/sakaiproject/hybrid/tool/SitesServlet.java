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
import java.util.Collections;
import java.util.HashMap;
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
import org.sakaiproject.api.app.messageforums.SynopticMsgcntrItem;
import org.sakaiproject.api.app.messageforums.SynopticMsgcntrManager;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;

/**
 * Based on
 * https://source.caret.cam.ac.uk/camtools/trunk/camtools/sdata/tool/sakai
 * -sdata-impl/src/main/java/org/sakaiproject/sdata/services/mcp/
 * MyCoursesAndProjectsBean.java
 * <p>
 * No required get parameters. Runs in the context of the current user. Returns
 * all sites that the user has access to visit.
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MTIA_SUSPECT_SERVLET_INSTANCE_FIELD", justification = "dependencies only mutated only during init()")
@SuppressWarnings({ "PMD.LongVariable", "PMD.CyclomaticComplexity" })
public class SitesServlet extends HttpServlet {
	// TODO i18n category names
	private static final long serialVersionUID = 7907409301065984518L;
	private static final Log LOG = LogFactory.getLog(SitesServlet.class);
	/**
	 * Optional GET parameter which categorizes the JSON by site term and type.
	 */
	private static final String CATEGORIZED = "categorized";
	/**
	 * Optional GET parameter which will include Messages and Forums unread
	 * counts.
	 */
	private static final String UNREAD = "unread";
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient SessionManager sessionManager;
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient SiteService siteService;
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient ServerConfigurationService serverConfigurationService;
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient ComponentManager componentManager;
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MSF_MUTABLE_SERVLET_FIELD", justification = "dependency mutated only during init()")
	protected transient SynopticMsgcntrManager synopticMsgcntrManager;
	protected transient MoreSiteViewImpl moreSiteViewImpl;

	@Override
	@SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.NPathComplexity" })
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("doGet(HttpServletRequest " + req
					+ ", HttpServletResponse " + resp + ")");
		}
		final String categorizedParam = req.getParameter(CATEGORIZED);
		boolean categorized = false;
		if (categorizedParam != null) {
			categorized = Boolean.parseBoolean(categorizedParam);
		}
		final String unreadParam = req.getParameter(UNREAD);
		boolean unread = false;
		if (unreadParam != null) {
			unread = Boolean.parseBoolean(unreadParam);
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
			// initialize values to an empty map to avoid null check later
			Map<String, Integer> unreadForums = Collections.emptyMap();
			Map<String, Integer> unreadMessages = unreadForums;
			if (unread) {
				final String uuid = sessionManager.getCurrentSession()
						.getUserId();
				final List<SynopticMsgcntrItem> synopticMsgcntrItems = synopticMsgcntrManager
						.getWorkspaceSynopticMsgcntrItems(uuid);
				if (synopticMsgcntrItems != null) {
					final int initialCapacity = synopticMsgcntrItems.size();
					unreadForums = new HashMap<String, Integer>(initialCapacity);
					unreadMessages = new HashMap<String, Integer>(
							initialCapacity);
					for (SynopticMsgcntrItem synopticMsgcntrItem : synopticMsgcntrItems) {
						final String siteId = synopticMsgcntrItem.getSiteId();
						final int forumCount = synopticMsgcntrItem
								.getNewForumCount();
						// omit counts < 1
						if (forumCount > 0) {
							unreadForums.put(siteId, forumCount);
						}
						final int messageCount = synopticMsgcntrItem
								.getNewMessagesCount();
						// omit counts < 1
						if (messageCount > 0) {
							unreadMessages.put(siteId, messageCount);
						}
					}
				}
			}
			if (categorized) {
				final List<Map<String, List<Site>>> categorizedSitesList = moreSiteViewImpl
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
						final JSONArray sitesArrayJson = new JSONArray();
						for (final Site site : sortedSites) {
							sitesArrayJson.add(renderSiteJson(site,
									unreadForums, unreadMessages));
						}
						categoryJson.element("sites", sitesArrayJson);
						categoriesArrayJson.add(categoryJson);
					}
				}
				json.element("categories", categoriesArrayJson);
			} else { // not categorized
				final JSONArray sitesArrayJson = new JSONArray();
				for (Site site : siteList) {
					sitesArrayJson.add(renderSiteJson(site, unreadForums,
							unreadMessages));
				}
				json.element("sites", sitesArrayJson);
			}
		}
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setStatus(HttpServletResponse.SC_OK);
		json.write(resp.getWriter());
	}

	private JSONObject renderSiteJson(final Site site,
			final Map<String, Integer> unreadForums,
			final Map<String, Integer> unreadMessages) {
		final JSONObject siteJson = new JSONObject();
		final String siteId = site.getId();
		siteJson.element("title", site.getTitle());
		siteJson.element("id", siteId);
		siteJson.element("url", site.getUrl());
		siteJson.element("description", site.getDescription());
		siteJson.element("forums", unreadForums.get(siteId));
		siteJson.element("messages", unreadMessages.get(siteId));
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
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		if (componentManager == null) {
			componentManager = org.sakaiproject.component.cover.ComponentManager
					.getInstance();
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
		synopticMsgcntrManager = (SynopticMsgcntrManager) componentManager
				.get(SynopticMsgcntrManager.class);
		if (synopticMsgcntrManager == null) {
			throw new IllegalStateException("SynopticMsgcntrManager == null");
		}
		moreSiteViewImpl = new MoreSiteViewImpl(serverConfigurationService);
	}

	/**
	 * Only used for unit testing setup.
	 * 
	 * @param componentManager
	 */
	protected void setupTestCase(final ComponentManager componentManager) {
		if (componentManager == null) {
			throw new IllegalArgumentException("componentManager == null");
		}
		this.componentManager = componentManager;
	}
}
