/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.cloudfoundry.identity.uaa.login;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.client.RestOperations;

/**
 * @author Dave Syer
 *
 */
public class ProfileControllerTests {

	private final RestOperations restTemplate = Mockito.mock(RestOperations.class);

	private final String approvalsUri = "http://example.com/approvals";

	private final ProfileController controller = new ProfileController(restTemplate);

	@Before
	public void create() {
		controller.setApprovalsUri(approvalsUri);
	}

	public void testGet() {
		controller.setLinks(Collections.singletonMap("foo", "http://example.com"));
		Mockito.when(restTemplate.getForObject(approvalsUri , Set.class)).thenReturn(
				Collections.singleton(Collections.singletonMap("clientId", "foo")));
		Model model = new ExtendedModelMap();
		controller.get(model);
		assertTrue(model.containsAttribute("links"));
		assertTrue(model.containsAttribute("approvals"));
	}

	public void testPostForUpdate() {
		controller.setLinks(Collections.singletonMap("foo", "http://example.com"));
		Mockito.when(restTemplate.getForObject(approvalsUri , Set.class)).thenReturn(
				Collections.singleton(Collections.singletonMap("clientId", "foo")));
		Model model = new ExtendedModelMap();
		controller.post(Collections.singleton("read"), "", null, "foo", model);
		assertTrue(model.containsAttribute("links"));
		assertTrue(model.containsAttribute("approvals"));
	}

	public void testPostForDelete() {
		controller.setLinks(Collections.singletonMap("foo", "http://example.com"));
		Mockito.when(restTemplate.getForObject(approvalsUri , Set.class)).thenReturn(
				Collections.singleton(Collections.singletonMap("clientId", "foo")));
		Model model = new ExtendedModelMap();
		controller.post(Collections.singleton("read"), null, "", "foo", model);
		assertTrue(model.containsAttribute("links"));
		assertTrue(model.containsAttribute("approvals"));
	}

}
