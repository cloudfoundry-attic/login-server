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

import org.springframework.security.core.Authentication;

/**
 * @author Dave Syer
 *
 */
public interface AutologinCodeStore {

	/**
	 * @param code the code to redeem
	 * @return a user associated with the code or null
	 */
	Authentication getUser(String code);

	/**
	 * @param a user to store
	 * @return code the code to be redeemed later
	 */
	String storeUser(Authentication user);

}
