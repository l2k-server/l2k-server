package org.l2kserver.example.java.plugin;

import org.l2kserver.plugin.api.L2kGameServerPlugin;
import org.l2kserver.plugin.api.annotation.GameServerPlugin;

@GameServerPlugin(pluginId = "Example plugin, written in Java")
public class ExampleJavaPlugin implements L2kGameServerPlugin {

    @Override
    public void invoke() {
        System.out.println("At the moment i am very little and simple plugin, so i can only say 'Hello' =( ");
        System.out.println("Hello!");
    }

}
