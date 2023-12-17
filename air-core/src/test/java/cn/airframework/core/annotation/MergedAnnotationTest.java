package cn.airframework.core.annotation;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * test for {@link MergedAnnotation}
 *
 * @author huangchengxing
 */
public class MergedAnnotationTest {

    @Test
    public void testOf() {
        AnnotationC annotationC = Foo.class.getDeclaredAnnotation(AnnotationC.class);
        AnnotationB annotationB = AnnotationC.class.getDeclaredAnnotation(AnnotationB.class);
        AnnotationA annotationA = AnnotationB.class.getDeclaredAnnotation(AnnotationA.class);

        MergedAnnotation ma = MergedAnnotation.of(annotationC, annotationB, annotationA);

        Assert.assertSame(annotationC, ma.getRoot());
        Assert.assertTrue(ma.isPresent(AnnotationC.class));
        Assert.assertFalse(ma.isMetaPresent(AnnotationC.class));

        Assert.assertSame(annotationB, ma.getMeta(AnnotationB.class));
        Assert.assertTrue(ma.isPresent(AnnotationB.class));
        Assert.assertTrue(ma.isMetaPresent(AnnotationB.class));

        Assert.assertSame(annotationA, ma.getMeta(AnnotationA.class));
        Assert.assertTrue(ma.isPresent(AnnotationA.class));
        Assert.assertTrue(ma.isMetaPresent(AnnotationA.class));
    }

    @Test
    public void testFrom() {
        AnnotationC annotationC = Foo.class.getDeclaredAnnotation(AnnotationC.class);
        AnnotationB annotationB = AnnotationC.class.getDeclaredAnnotation(AnnotationB.class);
        AnnotationA annotationA = AnnotationB.class.getDeclaredAnnotation(AnnotationA.class);

        // 针对同类型的注解缓存应生效
        MergedAnnotation ma = MergedAnnotation.from(annotationC);
        Assert.assertNotSame(ma, MergedAnnotation.from(annotationC));
        Assert.assertEquals(ma, MergedAnnotation.from(annotationC));

        Assert.assertSame(annotationC, ma.getRoot());
        Assert.assertSame(annotationC, ma.get(AnnotationC.class));
        Assert.assertTrue(ma.isPresent(AnnotationC.class));
        Assert.assertFalse(ma.isMetaPresent(AnnotationC.class));

        Assert.assertSame(annotationB, ma.getMeta(AnnotationB.class));
        Assert.assertSame(annotationB, ma.get(AnnotationB.class));
        Assert.assertTrue(ma.isPresent(AnnotationB.class));
        Assert.assertTrue(ma.isMetaPresent(AnnotationB.class));

        Assert.assertSame(annotationA, ma.getMeta(AnnotationA.class));
        Assert.assertSame(annotationA, ma.get(AnnotationA.class));
        Assert.assertTrue(ma.isPresent(AnnotationA.class));
        Assert.assertTrue(ma.isMetaPresent(AnnotationA.class));
    }

    @Test
    public void testGetAttributeValue() {
        AnnotationC annotationC = Foo.class.getDeclaredAnnotation(AnnotationC.class);
        AnnotationB annotationB = AnnotationC.class.getDeclaredAnnotation(AnnotationB.class);
        AnnotationA annotationA = AnnotationB.class.getDeclaredAnnotation(AnnotationA.class);
        MergedAnnotation ma = MergedAnnotation.from(annotationC);

        // 被根注解的属性映射
        Assert.assertEquals(annotationC.value1(), ma.getAttributeValue(AnnotationC.class, "value1"));
        Assert.assertEquals(annotationC.value1(), ma.getAttributeValue(AnnotationC.class, "value2"));
        Assert.assertEquals(annotationC.value1(), ma.getAttributeValue(AnnotationB.class, "value"));
        Assert.assertEquals(annotationC.value1(), ma.getAttributeValue(AnnotationA.class, "value"));

        // 被非根注解的属性映射
        Assert.assertEquals(annotationB.noAliasedForRoot(), ma.getAttributeValue(AnnotationA.class, "noAliasedForRoot"));
        Assert.assertEquals(annotationB.noAliasedForRoot(), ma.getAttributeValue(AnnotationB.class, "noAliasedForRoot"));

        Assert.assertNull(ma.getAttributeValue(AnnotationA.class, "none"));
    }

    @Test
    public void testSynthesis() {
        AnnotationC annotationC = Foo.class.getDeclaredAnnotation(AnnotationC.class);
        AnnotationB annotationB = AnnotationC.class.getDeclaredAnnotation(AnnotationB.class);
        AnnotationA annotationA = AnnotationB.class.getDeclaredAnnotation(AnnotationA.class);
        MergedAnnotation ma = MergedAnnotation.from(annotationC);

        AnnotationC syncC = ma.synthesis(AnnotationC.class).orElse(null);
        Assert.assertNotNull(syncC);
        Assert.assertSame(syncC, ma.synthesis(AnnotationC.class).orElse(null));
        Assert.assertEquals(annotationC.value1(), syncC.value1());
        Assert.assertEquals(annotationC.value1(), syncC.value2());
        Assert.assertNotEquals(annotationC, syncC);
        Assert.assertNotEquals(annotationC.hashCode(), syncC.hashCode());
        Assert.assertNotEquals(annotationC.toString(), syncC.toString());

        AnnotationB syncB = ma.synthesis(AnnotationB.class).orElse(null);
        Assert.assertNotNull(syncB);
        Assert.assertSame(syncB, ma.synthesis(AnnotationB.class).orElse(null));
        Assert.assertEquals(annotationC.value1(), syncB.value());
        Assert.assertNotEquals(annotationB, syncB);
        Assert.assertNotEquals(annotationB.hashCode(), syncB.hashCode());

        AnnotationA syncA = ma.synthesis(AnnotationA.class).orElse(null);
        Assert.assertNotNull(syncA);
        Assert.assertSame(syncA, ma.synthesis(AnnotationA.class).orElse(null));
        Assert.assertEquals(annotationC.value1(), syncA.value());
        Assert.assertNotEquals(annotationA, syncA);
        Assert.assertNotEquals(annotationA.hashCode(), syncA.hashCode());

        Map<Class<? extends Annotation>, Annotation> syncMap = ma.synthesisAll().stream()
            .collect(Collectors.toMap(Annotation::annotationType, Function.identity()));
        Assert.assertSame(syncA, syncMap.get(AnnotationA.class));
        Assert.assertSame(syncB, syncMap.get(AnnotationB.class));
        Assert.assertSame(syncC, syncMap.get(AnnotationC.class));
    }

    @Test
    public void testSynthesisWithNoMappedAttributes() {
        NoAttribute noAttribute = Foo.class.getDeclaredAnnotation(NoAttribute.class);
        MergedAnnotation ma = MergedAnnotation.from(noAttribute);
        NoAttribute sync = ma.synthesis(NoAttribute.class).orElse(null);
        Assert.assertNotNull(sync);
        Assert.assertSame(noAttribute, sync);
    }

    @Test
    public void testCircularReference() {
        CircularReference2 circularReference2 = Foo.class.getDeclaredAnnotation(CircularReference2.class);
        CircularReference1 circularReference1 = CircularReference2.class.getDeclaredAnnotation(CircularReference1.class);
        MergedAnnotation ma = MergedAnnotation.from(circularReference2);

        Assert.assertSame(circularReference1, ma.get(CircularReference1.class));
        Assert.assertSame(circularReference2, ma.get(CircularReference2.class));
    }



    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface AnnotationA {
        String value() default "";
        String noAliasedForRoot() default "";
    }

    @AnnotationA(value = "1", noAliasedForRoot = "1")
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface AnnotationB {
        @Alias(annotation = AnnotationA.class, attribute = "value")
        String value() default "";
        @Alias(annotation = AnnotationA.class)
        String noAliasedForRoot() default "";
    }

    @AnnotationB(value = "2", noAliasedForRoot = "2")
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface AnnotationC {
        @Alias(annotation = AnnotationA.class, attribute = "value")
        String value1() default "";
        @Alias(annotation = AnnotationB.class, attribute = "value")
        String value2() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface SingleAliasAnnotation {
        @Alias(attribute = "value2")
        String value1() default "";
        @Alias(attribute = "value1")
        String value2() default "";
        String value3() default "";
    }
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface NoAttribute {
    }


    @CircularReference2
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface CircularReference1 {}
    @CircularReference1
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface CircularReference2 {}

    @CircularReference2
    @NoAttribute
    @SingleAliasAnnotation(value1 = "single")
    @AnnotationC(value1 = "3")
    private static class Foo {}
}
