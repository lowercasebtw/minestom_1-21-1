package net.minestom.codegen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

public class CodeGenerator {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeGenerator.class);

    private final File outputFolder;

    public CodeGenerator(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void generate(InputStream resourceFile, String packageName, String typeName, String loaderName, String generatedName) {
        if (resourceFile == null) {
            LOGGER.error("Failed to find resource file for " + typeName);
            return;
        }
        ClassName typeClass = ClassName.get(packageName, typeName);
        ClassName loaderClass = ClassName.get(packageName, loaderName);

        JsonObject json;
        json = GSON.fromJson(new InputStreamReader(resourceFile), JsonObject.class);
        ClassName materialsCN = ClassName.get(packageName, generatedName);
        // BlockConstants class
        TypeSpec.Builder blockConstantsClass = TypeSpec.interfaceBuilder(materialsCN)
                // Add @SuppressWarnings("unused")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unused").build())
                .addJavadoc("Code autogenerated, do not edit!");

        // Use data
        json.keySet().forEach(namespace -> {
            final String constantName = namespace
                    .replace("minecraft:", "")
                    .replace(".", "_")
                    .toUpperCase(Locale.ROOT);
            blockConstantsClass.addField(
                    FieldSpec.builder(typeClass, constantName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer(
                                    // TypeClass.STONE = MaterialLoader.fromNamespaceId("minecraft:stone")
                                    "$T.get($S)",
                                    loaderClass,
                                    namespace
                            )
                            .build()
            );
        });
        writeFiles(
                List.of(JavaFile.builder(packageName, blockConstantsClass.build())
                        .indent("    ")
                        .skipJavaLangImports(true)
                        .build()),
                outputFolder);
    }

    public void generateKeys(InputStream resourceFile, String packageName, String typeName, String generatedName) {
        if (resourceFile == null) {
            LOGGER.error("Failed to find resource file for " + typeName);
            return;
        }

        ClassName typeClass = ClassName.bestGuess(packageName + "." + typeName); // Use bestGuess to handle nested class
        ClassName registryKeyClass = ClassName.get("net.minestom.server.registry", "DynamicRegistry", "Key");
        ParameterizedTypeName typedRegistryKeyClass = ParameterizedTypeName.get(registryKeyClass, typeClass);

        JsonObject json;
        json = GSON.fromJson(new InputStreamReader(resourceFile), JsonObject.class);
        ClassName materialsCN = ClassName.get(packageName, generatedName);
        // BlockConstants class
        TypeSpec.Builder blockConstantsClass = TypeSpec.interfaceBuilder(materialsCN)
                // Add @SuppressWarnings("unused")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unused").build())
                .addJavadoc("Code autogenerated, do not edit!");

        // Use data
        json.keySet().forEach(namespace -> {
            String constantName = namespace
                    .replace("minecraft:", "")
                    .replace(".", "_")
                    .toUpperCase(Locale.ROOT);
            if (!SourceVersion.isName(constantName)) {
                constantName = "_" + constantName;
            }
            blockConstantsClass.addField(
                    FieldSpec.builder(typedRegistryKeyClass, constantName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer(
                                    // TypeClass.STONE = NamespaceID.from("minecraft:stone")
                                    "$T.of($S)",
                                    registryKeyClass,
                                    namespace
                            )
                            .build()
            );
        });
        writeFiles(
                List.of(JavaFile.builder(packageName, blockConstantsClass.build())
                        .indent("    ")
                        .skipJavaLangImports(true)
                        .build()),
                outputFolder);
    }

    private void writeFiles(@NotNull List<JavaFile> fileList, File outputFolder) {
        for (JavaFile javaFile : fileList) {
            try {
                javaFile.writeTo(outputFolder);
            } catch (IOException e) {
                LOGGER.error("An error occured while writing source code to the file system.", e);
            }
        }
    }
}