package com.stanfy.enroscar.async.internal;

import com.squareup.javawriter.JavaWriter;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * Base class for generators.
 */
abstract class BaseGenerator {

  /** Environment. */
  private final ProcessingEnvironment env;

  /** Class name. */
  final String className;
  /** Package name. */
  final String packageName;

  /** Base class. */
  final TypeElement operationsClass;

  /** Methods. */
  final List<MethodData> methods;

  /** Imports to emit. */
  private final Set<String> imports = new HashSet<>();

  /** Loader IDs. */
  private static final HashMap<ExecutableElement, Integer> LOADER_IDS = new HashMap<>();

  /** What we extend. */
  private String extendsClass;

  public BaseGenerator(final ProcessingEnvironment env, final TypeElement type,
                       final List<MethodData> methods, final String suffix) {
    this.env = env;
    this.operationsClass = type;
    this.packageName = env.getElementUtils().getPackageOf(type).getQualifiedName().toString();
    this.className = GenUtils.getGeneratedClassName(packageName, type.getQualifiedName().toString(), suffix);
    this.methods = methods;
  }

  public String getFqcn() {
    if (packageName.length() == 0) {
      return className;
    }
    return packageName + "." + className;
  }

  protected final void addImports(final String... imports) {
    this.imports.addAll(Arrays.asList(imports));
  }

  protected final void addImports(final Collection<String> imports) {
    this.imports.addAll(imports);
  }

  protected final void setExtendsClass(final String name) {
    this.extendsClass = name;
  }

  protected final int getLoaderId(final ExecutableElement method) {
    Integer res = LOADER_IDS.get(method);
    if (res == null) {
      res = GenUtils.nextLoaderId();
      LOADER_IDS.put(method, res);
    }
    return res;
  }

  public final void generateCode() {
    Writer out = null;
    try {
      JavaFileObject jfo = env.getFiler().createSourceFile(getFqcn(), operationsClass);
      out = jfo.openWriter();
      GenUtils.generate(this, out);
      out.flush();
    } catch (IOException e) {
      env.getMessager().printMessage(
          ERROR,
          "Cannot generate loader for base class " + operationsClass + ": " + e.getMessage(),
          operationsClass
      );
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          // nothing
        }
      }
    }
  }

  final void generateTo(final Writer out) throws IOException {
    JavaWriter w = new JavaWriter(out);

    w.emitSingleLineComment("Code generated by Enroscar. Do not edit. %s",
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    w.emitPackage(packageName);

    w.emitImports(imports);
    w.emitEmptyLine();

    classJavaDoc(w);
    w.beginType(className, "class", modifiers(operationsClass), extendsClass);
    w.emitEmptyLine();

    writeClassBody(w);

    w.endType();
  }

  protected void classJavaDoc(final JavaWriter w) throws IOException {
    // nothing
  }

  protected abstract void writeClassBody(final JavaWriter w) throws IOException;

  private static Set<Modifier> modifiers(final Element e) {
    Set<Modifier> modifiers = e.getModifiers();
    if (modifiers instanceof EnumSet) {
      return modifiers;
    }
    if (modifiers.isEmpty()) {
      return EnumSet.noneOf(Modifier.class);
    }
    final EnumSet<Modifier> resultSet = EnumSet.copyOf(modifiers);
    resultSet.remove(STATIC);
    return resultSet;
  }

  protected Set<Modifier> constructorModifiers() {
    Set<Modifier> m = modifiers(operationsClass);
    return m.contains(PUBLIC) ? EnumSet.of(PUBLIC) : EnumSet.noneOf(Modifier.class);
  }

}
