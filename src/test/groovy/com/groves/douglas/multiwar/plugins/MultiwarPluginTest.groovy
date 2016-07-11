package com.groves.douglas.multiwar.plugins

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.*

/**
 * Functional tests for multiwar plugin
 * Created by dougg on 10/07/2016.
 */
class MultiwarPluginTest extends Specification {

    @Unroll
    def "Create #noOfTasks war tasks for #noOfTasks subprojects"(List subprojects, noOfTasks) {
        given: 'a project containing multiple submodules'
            File buildDir = File.createTempDir()
            File parentBuildFile = new File(buildDir, 'build.gradle'),
                 parentSettingsFile = new File(buildDir, 'settings.gradle')
            File configDir = new File(buildDir, 'config')
            configDir.mkdir()
            subprojects.each {
                File prjDir = new File(buildDir, it),
                    prjConfigDir = new File(configDir, it)
                prjDir.mkdir()
                prjConfigDir.mkdir()
                File buildFile = new File(prjDir, 'build.gradle'),
                    settingsFile = new File(prjDir, 'settings.gradle')
                settingsFile << """
                rootProject.name = '${it}'
                """
                buildFile << '''
                apply plugin: 'java'
                '''
            }
            parentSettingsFile << """
            rootProject.name = 'test-project'
            """
            subprojects.each {
                parentSettingsFile << """
                include '${it}'
                """
            }
            parentBuildFile << '''
            plugins { id 'multiwar'; id 'java' }
            multiwar { containers {
            '''
            subprojects.each {
                parentBuildFile << """
                ${it}{}
                """
            }
            parentBuildFile << '''
            }; verbose = true }
            '''
        when: 'the tasks task is executed'
            def result = GradleRunner.create()
                    .withProjectDir(buildDir)
                    .withArguments('tasks', '--all')
                    .withPluginClasspath()
                    .build()
        then: 'the output must define one task per subproject'
            subprojects.findAll {
                result.output.contains("assemble-${it}-war")
            }.size() == subprojects.size()
        where:
            subprojects << [['a','b'], ['a'], ['a', 'b', 'c']]
            noOfTasks << [2, 1, 3]
    }

    def 'Fail if project does not contain subprojects'(){
        given: 'a project containing no subprojects'
            File buildDir = File.createTempDir()
            File parentBuildFile = new File(buildDir, 'build.gradle'),
                 parentSettingsFile = new File(buildDir, 'settings.gradle')
            parentBuildFile << '''
            plugins { id 'multiwar'; id 'java' }
            multiwar { containers { a {}; b {} }; verbose = true; }
            '''
        when: 'the tasks task is executed'
            def result = GradleRunner.create()
                    .withProjectDir(buildDir)
                    .withArguments('tasks', '--all')
                    .withPluginClasspath()
                    .build()
        then: 'execution must fail with a valid message'
            UnexpectedBuildFailure ubf = thrown(UnexpectedBuildFailure)
            ubf.message.contains('Project must declare at least one submodule to be a valid multiwar project')
    }
}
