package org.l2kserver.plugin.api.annotation.processor;

import org.l2kserver.plugin.api.L2kGameServerPlugin;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes("org.l2kserver.plugin.api.annotation.GameServerPlugin")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class GameServerPluginAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach((annotation) ->
            roundEnv.getElementsAnnotatedWith(annotation).forEach((element) -> {
                if (element instanceof TypeElement typeElement) {
                    try {
                        var writer = processingEnv.getFiler().createResource(
                            StandardLocation.CLASS_OUTPUT, "",
                            "META-INF/services/" + L2kGameServerPlugin.class.getCanonicalName()
                        ).openWriter();

                        writer.write(typeElement.getQualifiedName().toString());
                        writer.close();
                    } catch (IOException e) {
                        processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR, "Failed to generate META-INF: " + e.getMessage()
                        );
                    }
                }
            })
        );

        return false;
    }
}
