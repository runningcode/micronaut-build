/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.build.utils;

import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.provider.Provider;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VersionHandling {
    /**
     * Returns a version defined in the catalog. If not found,
     * looks for a property (typically declared in gradle.properties).
     */
    private static String versionOrDefault(
            Project project,
            String alias,
            String defaultVersion) {
        VersionCatalogsExtension catalogs = project.getExtensions().findByType(VersionCatalogsExtension.class);
        if (catalogs == null) {
            return projectProperty(project, alias, defaultVersion);
        }
        return catalogs.find("libs")
                .flatMap(catalog -> {
                    Optional<VersionConstraint> version = catalog.findVersion(alias);
                    if (version.isPresent()) {
                        return version;
                    }
                    return catalog.findVersion("managed." + alias);
                })
                .map(VersionConstraint::getRequiredVersion)
                .orElseGet(() -> projectProperty(project, alias, defaultVersion));
    }

    /**
     * Returns a version provider defined in the catalog. If not found,
     * looks for a property (typically declared in gradle.properties).
     */
    public static Provider<String> versionProviderOrDefault(
            Project project,
            String alias,
            String defaultVersion) {
        return project.provider(() -> versionOrDefault(project, alias, defaultVersion));
    }

    private static String propertyNameFor(String alias) {
        String[] components = alias.split("[.\\-_]");
        String propertyName = IntStream.range(0, components.length)
                .mapToObj(i -> {
                    if (i == 0) {
                        return components[i];
                    } else {
                        String c = components[i];
                        return Character.toUpperCase(c.charAt(0)) + c.substring(1).toLowerCase();
                    }
                })
                .collect(Collectors.joining(""));
        return propertyName + "Version";
    }

    private static String projectProperty(Project p, String alias, String defaultVersion) {
        Object projectProp = p.findProperty(propertyNameFor(alias));
        if (projectProp != null) {
            return String.valueOf(projectProp);
        }
        return defaultVersion;
    }
}
