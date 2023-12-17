package cn.airframework.core.annotation;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author huangchengxing
 */
public class AnnotationFilterTest {

    @Test
    public void testLangFilter() {
        AnnotationFilter langFilter = AnnotationFilter.LANG;

        Assert.assertFalse(langFilter.test(java.lang.Override.class));
        Assert.assertFalse(langFilter.test(org.checkerframework.checker.nullness.qual.NonNull.class));
        Assert.assertTrue(langFilter.test(cn.airframework.core.annotation.Alias.class));
    }

    @Test
    public void testJavaFilter() {
        AnnotationFilter javaFilter = AnnotationFilter.JAVA;

        Assert.assertFalse(javaFilter.test(java.lang.Override.class));
        Assert.assertFalse(javaFilter.test(javax.annotation.processing.SupportedAnnotationTypes.class));
        Assert.assertTrue(javaFilter.test(org.checkerframework.checker.nullness.qual.NonNull.class));
        Assert.assertTrue(javaFilter.test(cn.airframework.core.annotation.Alias.class));
    }

    @Test
    public void testAirFrameworkFilter() {
        AnnotationFilter airFrameworkFilter = AnnotationFilter.AIR_FRAMEWORK;

        Assert.assertFalse(airFrameworkFilter.test(cn.airframework.core.annotation.Alias.class));
        Assert.assertTrue(airFrameworkFilter.test(java.lang.Override.class));
        Assert.assertTrue(airFrameworkFilter.test(javax.annotation.processing.SupportedAnnotationTypes.class));
        Assert.assertTrue(airFrameworkFilter.test(org.checkerframework.checker.nullness.qual.NonNull.class));
    }

    @Test
    public void testNoneFilter() {
        AnnotationFilter noneFilter = AnnotationFilter.NONE;

        Assert.assertTrue(noneFilter.test(java.lang.Override.class));
        Assert.assertTrue(noneFilter.test(cn.airframework.core.annotation.Alias.class));
        Assert.assertTrue(noneFilter.test(org.checkerframework.checker.nullness.qual.NonNull.class));
        Assert.assertTrue(noneFilter.test(javax.annotation.processing.SupportedAnnotationTypes.class));
    }

    @Test
    public void testAllFilter() {
        AnnotationFilter allFilter = AnnotationFilter.ALL;

        Assert.assertFalse(allFilter.test(java.lang.Override.class));
        Assert.assertFalse(allFilter.test(cn.airframework.core.annotation.Alias.class));
        Assert.assertFalse(allFilter.test(org.checkerframework.checker.nullness.qual.NonNull.class));
        Assert.assertFalse(allFilter.test(javax.annotation.processing.SupportedAnnotationTypes.class));
    }

    @Test
    public void testAllMatchFilter() {
        AnnotationFilter langFilter = AnnotationFilter.LANG;
        AnnotationFilter airFrameworkFilter = AnnotationFilter.AIR_FRAMEWORK;

        AnnotationFilter allMatchFilter = AnnotationFilter.allMatch(langFilter, airFrameworkFilter);

        Assert.assertFalse(allMatchFilter.test(java.lang.Override.class));
        Assert.assertFalse(allMatchFilter.test(cn.airframework.core.annotation.Alias.class));
        Assert.assertTrue(allMatchFilter.test(javax.annotation.processing.SupportedAnnotationTypes.class));
    }

    @Test
    public void testAnyMatchFilter() {
        AnnotationFilter langFilter = AnnotationFilter.LANG;
        AnnotationFilter airFrameworkFilter = AnnotationFilter.AIR_FRAMEWORK;

        AnnotationFilter anyMatchFilter = AnnotationFilter.anyMatch(langFilter, airFrameworkFilter);

        Assert.assertFalse(anyMatchFilter.test(java.lang.Override.class));
        Assert.assertFalse(anyMatchFilter.test(cn.airframework.core.annotation.Alias.class));
        Assert.assertFalse(anyMatchFilter.test(org.checkerframework.checker.nullness.qual.NonNull.class));
        Assert.assertTrue(anyMatchFilter.test(javax.annotation.processing.SupportedAnnotationTypes.class));
    }

    @Test
    public void testTypeNameFilter() {
        String[] prefixes = {"java", "javax"};

        AnnotationFilter typeNameFilter = AnnotationFilter.forTypeNamePrefix(prefixes);

        Assert.assertFalse(typeNameFilter.test(java.lang.Override.class));
        Assert.assertFalse(typeNameFilter.test(javax.annotation.processing.SupportedAnnotationTypes.class));
        Assert.assertTrue(typeNameFilter.test(org.checkerframework.checker.nullness.qual.NonNull.class));
        Assert.assertTrue(typeNameFilter.test(cn.airframework.core.annotation.Alias.class));
    }
}
