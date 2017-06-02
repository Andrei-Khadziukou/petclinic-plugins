package com.epam.petclinic.plugin.util

import org.gradle.api.Project

/**
 * Utility class to configure specific modules of the project tree.
 *
 * Date: 5/31/2017
 *
 * @author Stanislau Halauniou
 */
class ModuleTree {
    private ModuleTree() {
        throw new AssertionError("${ModuleTree.class.name} is utility class. Creating an instance is prohibited")
    }

    /**
     * Configure each leaf module in the project structure, or root project if no sub-projects exist.
     * Configuration closure will not affect top-level modules which have children itself.
     *
     * @param project root of the project hierarchy or single project
     * @param moduleConfigurationClosure closure to configure child modules
     */
    public static void eachChildModule(Project project, Closure<Void> moduleConfigurationClosure) {
        if (project.subprojects.empty) {
            moduleConfigurationClosure(project)
        } else {
            project.childProjects.values().each { subProject ->
                eachChildModule(subProject, moduleConfigurationClosure)
            }
        }
    }
}
