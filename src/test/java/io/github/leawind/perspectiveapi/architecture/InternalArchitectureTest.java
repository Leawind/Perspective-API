package io.github.leawind.perspectiveapi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/// Enforces the layered package constraints documented in AGENTS.md on compiled bytecode, so
/// fully-qualified references that bypass import checks are also caught.
class InternalArchitectureTest {
  private static final String PROJECT = "io.github.leawind.perspectiveapi";
  private static final String API = PROJECT + ".api";
  private static final String INTERNAL = PROJECT + ".internal";
  private static final String BRIDGE = INTERNAL + ".bridge";
  private static final String MIXIN = BRIDGE + ".mixin";
  private static final String IMPL = INTERNAL + ".impl";
  private static final String LOGIC = INTERNAL + ".logic";
  private static final String UTILS = INTERNAL + ".utils";
  private static final String PLATFORM = PROJECT + ".platform";
  private static final String BRIDGE_CLASS = BRIDGE + ".Bridge";

  private static final Set<String> MIXIN_CATEGORIES =
      Set.of("cameraupdate", "fov", "gui", "input", "projection", "setupcamera");

  private static final JavaClasses PROJECT_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(PROJECT);

  @Test
  void apiDoesNotDependOnInternalExceptBridge() {
    assertNoDependencies(
        "public api",
        source -> isInPackageTree(source, API),
        "internal types other than the stateless Bridge helper",
        target -> isInPackageTree(target, INTERNAL) && !target.getName().equals(BRIDGE_CLASS));
  }

  @Test
  void bridgeDoesNotDependOnBusinessLayers() {
    assertNoDependencies(
        "bridge",
        source -> isInPackageTree(source, BRIDGE),
        "the public api, impl, or logic",
        target ->
            isInPackageTree(target, API)
                || isInPackageTree(target, IMPL)
                || isInPackageTree(target, LOGIC));
  }

  @Test
  void utilitiesStayBusinessNeutral() {
    assertNoDependencies(
        "utilities",
        source -> isInPackageTree(source, UTILS),
        "project code outside utils",
        target -> isInPackageTree(target, PROJECT) && !isInPackageTree(target, UTILS));
  }

  @Test
  void mixinsStayInApprovedCategories() {
    classes()
        .should(
            new ArchCondition<>("reside only in the mixin root or an approved category package") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                if (!isInPackageTree(item, MIXIN)
                    || isApprovedMixinPackage(item.getPackageName())) {
                  return;
                }
                events.add(
                    SimpleConditionEvent.violated(
                        item, item.getName() + " resides in " + item.getPackageName()));
              }
            })
        .check(PROJECT_CLASSES);
  }

  private static boolean isApprovedMixinPackage(String packageName) {
    if (packageName.equals(MIXIN)) return true;
    return MIXIN_CATEGORIES.stream().anyMatch(c -> packageName.equals(MIXIN + "." + c));
  }

  private static void assertNoDependencies(
      String sourceDescription,
      Predicate<JavaClass> sourcePredicate,
      String targetDescription,
      Predicate<JavaClass> targetPredicate) {
    classes()
        .should(
            new ArchCondition<>(sourceDescription + " not depend on " + targetDescription) {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                if (!sourcePredicate.test(item)) {
                  return;
                }
                item.getDirectDependenciesFromSelf().stream()
                    .filter(dependency -> targetPredicate.test(dependency.getTargetClass()))
                    .forEach(
                        dependency ->
                            events.add(
                                SimpleConditionEvent.violated(
                                    item, dependency.getDescription())));
              }
            })
        .check(PROJECT_CLASSES);
  }

  private static boolean isInPackageTree(JavaClass javaClass, String rootPackage) {
    return isInPackageTree(javaClass.getPackageName(), rootPackage);
  }

  private static boolean isInPackageTree(String packageName, String rootPackage) {
    return packageName.equals(rootPackage) || packageName.startsWith(rootPackage + ".");
  }
}
