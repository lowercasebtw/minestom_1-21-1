package net.minestom.codegen.fluid;

import com.google.gson.JsonObject;
import com.squareup.javapoet.*;
import net.minestom.codegen.MinestomCodeGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public final class FluidGenerator extends MinestomCodeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FluidGenerator.class);
    private final InputStream fluidsFile;
    private final File outputFolder;

    public FluidGenerator(@Nullable InputStream fluidsFile, @NotNull File outputFolder) {
        this.fluidsFile = fluidsFile;
        this.outputFolder = outputFolder;
    }

    @Override
    public void generate() {
        if (fluidsFile == null) {
            LOGGER.error("Failed to find fluids.json.");
            LOGGER.error("Stopped code generation for fluids.");
            return;
        }
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            LOGGER.error("Output folder for code generation does not exist and could not be created.");
            return;
        }
        // Important classes we use alot
        ClassName namespaceIDClassName = ClassName.get("net.minestom.server.utils", "NamespaceID");
        ClassName registriesClassName = ClassName.get("net.minestom.server.registry", "FluidRegistries");

        JsonObject fluids = GSON.fromJson(new InputStreamReader(fluidsFile), JsonObject.class);
        ClassName fluidClassName = ClassName.get("net.minestom.server.fluid", "Fluid");

        // Particle
        TypeSpec.Builder fluidClass = TypeSpec.enumBuilder(fluidClassName)
                .addSuperinterface(ClassName.get("net.kyori.adventure.key", "Keyed"))
                .addModifiers(Modifier.PUBLIC).addJavadoc("AUTOGENERATED by " + getClass().getSimpleName());

        fluidClass.addField(
                FieldSpec.builder(namespaceIDClassName, "id")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL).addAnnotation(NotNull.class).build()
        );
        // static field
        fluidClass.addField(
                FieldSpec.builder(ArrayTypeName.of(fluidClassName), "VALUES")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("values()")
                        .build()
        );

        fluidClass.addMethod(
                MethodSpec.constructorBuilder()
                        .addParameter(ParameterSpec.builder(namespaceIDClassName, "id").addAnnotation(NotNull.class).build())
                        .addStatement("this.id = id")
                        .addStatement("$T.fluids.put(id, this)", registriesClassName)
                        .build()
        );
        // Override key method (adventure)
        fluidClass.addMethod(
                MethodSpec.methodBuilder("key")
                        .returns(ClassName.get("net.kyori.adventure.key", "Key"))
                        .addAnnotation(Override.class)
                        .addAnnotation(NotNull.class)
                        .addStatement("return this.id")
                        .addModifiers(Modifier.PUBLIC)
                        .build()
        );
        // getId method
        fluidClass.addMethod(
                MethodSpec.methodBuilder("getId")
                        .returns(TypeName.SHORT)
                        .addStatement("return (short) ordinal()")
                        .addModifiers(Modifier.PUBLIC)
                        .build()
        );
        // getNamespaceID method
        fluidClass.addMethod(
                MethodSpec.methodBuilder("getNamespaceID")
                        .returns(namespaceIDClassName)
                        .addAnnotation(NotNull.class)
                        .addStatement("return this.id")
                        .addModifiers(Modifier.PUBLIC)
                        .build()
        );
        // toString method
        fluidClass.addMethod(
                MethodSpec.methodBuilder("toString")
                        .addAnnotation(NotNull.class)
                        .addAnnotation(Override.class)
                        .returns(String.class)
                        // this resolves to [Namespace]
                        .addStatement("return \"[\" + this.id + \"]\"")
                        .addModifiers(Modifier.PUBLIC)
                        .build()
        );

        // fromId Method
        fluidClass.addMethod(
                MethodSpec.methodBuilder("fromId")
                        .returns(fluidClassName)
                        .addAnnotation(Nullable.class)
                        .addParameter(TypeName.SHORT, "id")
                        .beginControlFlow("if(id >= 0 && id < VALUES.length)")
                        .addStatement("return VALUES[id]")
                        .endControlFlow()
                        .addStatement("return null")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .build()
        );

        // Use data
        fluids.entrySet().forEach(entry -> {
            final String fluidName = entry.getKey();
            fluidClass.addEnumConstant(toConstant(fluidName), TypeSpec.anonymousClassBuilder(
                            "$T.from($S)",
                            namespaceIDClassName,
                            fluidName
                    ).build()
            );
        });

        // Write files to outputFolder
        writeFiles(
                List.of(
                        JavaFile.builder("net.minestom.server.fluid", fluidClass.build())
                                .indent("    ")
                                .skipJavaLangImports(true)
                                .build()
                ),
                outputFolder
        );
    }
}
