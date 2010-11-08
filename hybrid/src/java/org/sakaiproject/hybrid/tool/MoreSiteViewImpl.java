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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.Site;

/**
 * Helper class to sort sites based on terms and site types. Closely derived
 * from logic contained in
 * {@link #org.sakaiproject.portal.charon.site.MoreSiteViewImpl()} and related
 * classes.<br>
 * <a href=
 * "https://source.sakaiproject.org/svn/portal/tags/sakai-2.7.1/portal-impl/impl/src/java/org/sakaiproject/portal/charon/site/MoreSiteViewImpl.java"
 * >MoreSiteViewImpl.java</a>
 * <p>
 * Object is thread safe.
 */
public class MoreSiteViewImpl {
	private static final Log LOG = LogFactory.getLog(MoreSiteViewImpl.class);

	/**
	 * How much additional capacity will the Map&lt String, List&lt Site&gt&gt
	 * terms require? For example: All, Portfolio Project, Other.
	 */
	protected static int ADDITIONAL_TERMS_CAPACITY = 5;

	/**
	 * Injected via constructor
	 */
	ServerConfigurationService serverConfigurationService;

	/**
	 * Dependencies get injected at creation of object to help with unit
	 * testing.
	 * 
	 * @param serverConfigurationService
	 */
	public MoreSiteViewImpl(
			final ServerConfigurationService serverConfigurationService) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("new MoreSiteViewImpl(final ServerConfigurationService "
					+ serverConfigurationService + ")");
		}
		if (serverConfigurationService == null) {
			throw new IllegalArgumentException(
					"serverConfigurationService == null");
		}
		this.serverConfigurationService = serverConfigurationService;
	}

	/**
	 * Process a site list into categories based on term and sorted by title.
	 * 
	 * @param siteList
	 *            Note: the List will be mutated by sorting.
	 * @return
	 */
	public List<Map<String, List<Site>>> processMySites(
			final List<Site> siteList) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("processMySites(final List<Site> siteList)");
		}
		if (siteList == null) {
			throw new IllegalArgumentException("Illegal siteList parameter!");
		}
		// first sort by site titles (should only have to sort once)
		// FIXME sites may be coming back sorted already - may not be necessary.
		// Collections.sort(siteList, new TitleSorter());

		// Order term column headers according to order specified in
		// portal.term.order
		// Filter out terms for which user is not a member of any sites
		if (serverConfigurationService == null) {
			throw new IllegalStateException(
					"serverConfigurationService == null");
		}
		final String[] termOrder = serverConfigurationService
				.getStrings("portal.term.order");

		// try to determine a good initial capacity for Map terms
		int size = -1;
		if (termOrder != null) {
			size = termOrder.length + ADDITIONAL_TERMS_CAPACITY;
		} else {
			// Constructs an empty HashMap with the default initial capacity
			// (16) and the default load factor (0.75).
			size = 16;
		}

		final Map<String, List<Site>> terms = new HashMap<String, List<Site>>(
				size);
		// iterate through sites
		for (Site site : siteList) {
			final String type = site.getType();
			// determine term
			String term = null;
			if ("course".equals(type)) {
				term = site.getProperties().getProperty("term");
				if (term == null) {
					term = "i18n.moresite_unknown_term";
				}
			} else if ("project".equals(type)) {
				term = "i18n.moresite_projects";
			} else if ("portfolio".equals(type)) {
				term = "i18n.moresite_portfolios";
			} else if ("admin".equals(type)) {
				term = "i18n.moresite_administration";
			} else {
				term = "i18n.moresite_other";
			}
			// get list of sites for term
			List<Site> termSites = terms.get(term);
			if (termSites == null) {
				termSites = new ArrayList<Site>();
			}
			termSites.add(site);
			terms.put(term, termSites);
		}

		// object to contain final rendering of data
		final List<Map<String, List<Site>>> sortedTerms = new ArrayList<Map<String, List<Site>>>(
				terms.size() + 1); // best guess at initial capacity
		// first add all available sites to sortedTerms
		final Map<String, List<Site>> allSites = new HashMap<String, List<Site>>(
				1);
		allSites.put("i18n.moresite_all_sites", siteList);
		sortedTerms.add(allSites);
		if (termOrder != null && termOrder.length > 0) {
			// admin specified policy applies.
			// only specified terms and in specified order
			for (final String term : termOrder) {
				addSitesToTerm(term, terms, sortedTerms);
			}
		} else { // default sort ordering
			// display all found terms
			for (final String term : new TreeSet<String>(terms.keySet())) {
				addSitesToTerm(term, terms, sortedTerms);
			}
		}
		return sortedTerms;
	}

	/**
	 * Silly little helper to avoid code duplication.
	 * 
	 * @param term
	 * @param terms
	 * @param sortedTerms
	 */
	void addSitesToTerm(final String term, final Map<String, List<Site>> terms,
			final List<Map<String, List<Site>>> sortedTerms) {
		final List<Site> sortedSites = terms.get(term);
		if (sortedSites == null || sortedSites.isEmpty()) {
			; // do nothing
		} else {
			final Map<String, List<Site>> matches = new HashMap<String, List<Site>>(
					1);
			matches.put(term, sortedSites);
			sortedTerms.add(matches);
		}
	}

	/**
	 * Simple comparator for sorting sites based on site title.
	 */
	static class TitleSorter implements Comparator<Site>, Serializable {
		private static final long serialVersionUID = -3933901080921177017L;

		public int compare(Site first, Site second) {
			if (first == null || second == null) {
				return 0;
			}
			final String firstTitle = first.getTitle();
			final String secondTitle = second.getTitle();
			if (firstTitle != null) {
				return firstTitle.compareToIgnoreCase(secondTitle);
			}
			return 0;
		}
	}
}
