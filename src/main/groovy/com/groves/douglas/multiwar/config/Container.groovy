package com.groves.douglas.multiwar.config

/**
 * Created by Douglas Groves on 08/07/2016.
 */
class Container {
    String name
    def configuration
    def deploymentDescriptor

    Container(String name) {
        this.name = name
    }
}
