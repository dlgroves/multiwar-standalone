package com.groves.douglas.multiwar.plugins

import com.groves.douglas.multiwar.config.Container
import com.groves.douglas.multiwar.extensions.MultiwarExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.War

/**
 * Created by Douglas Groves on 07/07/2016.
 */
class MultiwarPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        assert !project.childProjects.isEmpty(): 'Project must declare at least one submodule to be a valid multiwar project'
        project.extensions.create('multiwar', MultiwarExtension)
        def containers = project.multiwar.extensions.containers =
                project.container(Container)
        initialiseTasks(project, containers)
        disableWarTask(project)
    }

    def initialiseTasks(Project project, containers) {
        project.afterEvaluate {
            addConfigurations(project, containers)
            containers.all { Container container ->
                !project.multiwar.verbose ?: project.getLogger().quiet("Creating task assemble-{}-war...", container.name)
                def task = project.task([type: War], "assemble-${container.name}-war") { myTask ->
                    from("${project.rootDir}/config/${container.name}") {
                        into 'WEB-INF'
                    }
                    classpath project.configurations[container.name]
                    classifier = container.name
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    doLast {
                        !project.multiwar.verbose ?: !project.getLogger().quiet("Assembling {} war file...", container.name)
                    }
                }
                project.tasks.assemble.dependsOn(task)
            }
        }
    }

    def disableWarTask(Project project) {
        project.gradle.taskGraph.whenReady { taskGraph ->
            def tasks = taskGraph.getAllTasks()
            if (tasks.find { it.name == 'assemble' }) {
                tasks.findAll { it.name == 'war' }.each { task ->
                    !project.multiwar.verbose ?: project.getLogger().quiet("Disabling default war plugin task...")
                    task.enabled = false
                }
            }
        }
    }

    def addConfigurations(Project project, containers) {
        containers.all { Container container ->
            assert project.childProjects.get(container.name) : "Module ${container.name} doesn't exist"
            assert project.file("${project.rootDir}/config/${container.name}").exists(): "Configuration directory ${project.rootDir}/config/${container.name} doesn't exist"
            !project.multiwar.verbose ?: project.getLogger().quiet("Configuring ${container.name}...")
            project.configurations.create(container.name).setTransitive(true).setVisible(true)
            project.dependencies.add(container.name, project.childProjects.get(container.name))
        }
    }
}
