/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.login.test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.ProfileValueSource;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.util.StringUtils;

import java.util.Set;

public class ProfileActiveUtils {
    public static boolean isTestEnabledInThisEnvironment(Class<?> testClass) {
        IfProfileActive ifProfileActive = AnnotationUtils.findAnnotation(testClass, IfProfileActive.class);
        return isTestEnabledInThisEnvironment(ProfileValueUtils.retrieveProfileValueSource(testClass), ifProfileActive);
    }

    private static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource, IfProfileActive ifProfileActive) {
        if (ifProfileActive == null) {
            return true;
        }

        if (!StringUtils.hasText(ifProfileActive.value())) {
            throw new IllegalArgumentException("An empty 'value' attribute of @IfProfileActive is not allowed.");
        }

        Set<String> activeProfiles = StringUtils.commaDelimitedListToSet(profileValueSource.get("spring.profiles.active"));
        return activeProfiles.contains(ifProfileActive.value());
    }
}
