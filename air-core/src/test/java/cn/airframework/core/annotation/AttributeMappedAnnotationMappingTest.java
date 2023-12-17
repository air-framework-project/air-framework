package cn.airframework.core.annotation;

import cn.airframework.core.exception.AirFrameworkException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * test for {@link AttributeMappedAnnotationMapping}
 *
 * @author huangchengxing
 */
public class AttributeMappedAnnotationMappingTest {

    @Test
    public void testObjectMethod() {
        SingleAliasAnnotation annotation = Foo.class.getDeclaredAnnotation(SingleAliasAnnotation.class);
        AttributeMappedAnnotationMapping mapping1 = new AttributeMappedAnnotationMapping(null, annotation.annotationType(), annotation);
        AttributeMappedAnnotationMapping mapping2 = new AttributeMappedAnnotationMapping(null, annotation.annotationType(), annotation);
        Assert.assertEquals(mapping1, mapping2);
        Assert.assertEquals(mapping1.toString(), mapping2.toString());
        Assert.assertEquals(mapping1.hashCode(), mapping2.hashCode());
    }

    @Test
    public void testCopyConstructorWithNoAttributes() {
        NoAttributes noAttributes = Foo.class.getDeclaredAnnotation(NoAttributes.class);
        AttributeMappedAnnotationMapping mapping1 = new AttributeMappedAnnotationMapping(null, noAttributes.annotationType(), noAttributes);
        Assert.assertEquals(0, mapping1.getAttributes().length);
        AttributeMappedAnnotationMapping mapping2 = mapping1.bound(noAttributes);
        Assert.assertEquals(mapping1, mapping2);
        Assert.assertSame(mapping1.getAttributes(), mapping2.getAttributes());
    }

    @Test
    public void testCopyConstructorWithAttributes() {
        SingleAliasAnnotation annotation = Foo.class.getDeclaredAnnotation(SingleAliasAnnotation.class);
        AttributeMappedAnnotationMapping mapping1 = new AttributeMappedAnnotationMapping(null, annotation.annotationType(), annotation);
        AttributeMappedAnnotationMapping mapping2 = mapping1.bound(annotation);
        Assert.assertEquals(mapping1, mapping2);
        Assert.assertSame(mapping2.getAttributes(), mapping1.getAttributes());
    }

    @Test
    public void testCommonMethods() {
        SingleAliasAnnotation annotation = Foo.class.getDeclaredAnnotation(SingleAliasAnnotation.class);
        AttributeMappedAnnotationMapping mapping = new AttributeMappedAnnotationMapping(null, annotation.annotationType(), annotation);

        Assert.assertSame(annotation, mapping.getAnnotation());
        Assert.assertEquals(annotation.annotationType(), mapping.getAnnotationType());
        Assert.assertTrue(mapping.hasMappedAttributes());
        Assert.assertTrue(mapping.isRoot());
        Assert.assertNull(mapping.getSource());
        Assert.assertEquals(0, mapping.getLevel());
        Assert.assertArrayEquals(AnnotationUtils.getAnnotationAttributes(SingleAliasAnnotation.class), mapping.getAttributes());
    }

    @Test
    public void testAccessAttribute() throws NoSuchMethodException {
        SingleAliasAnnotation annotation = Foo.class.getDeclaredAnnotation(SingleAliasAnnotation.class);
        AttributeMappedAnnotationMapping mapping = new AttributeMappedAnnotationMapping(null, annotation.annotationType(), annotation);

        Assert.assertNotNull(mapping.getAttributeMapping("value1"));
        Assert.assertNotNull(mapping.getAttributeMapping("value2"));
        Assert.assertNotNull(mapping.getAttributeMapping("value3"));
        Assert.assertNull(mapping.getAttributeMapping("none"));

        AttributeMappedAnnotationMapping.AttributeMapping attributeMappingOfValue1 = mapping.getAttributeMapping("value1");
        Assert.assertNotNull(attributeMappingOfValue1);
        Assert.assertEquals(0, attributeMappingOfValue1.idx());
        Assert.assertSame(mapping, attributeMappingOfValue1.mapping());
        Assert.assertEquals(SingleAliasAnnotation.class.getMethod("value1"), attributeMappingOfValue1.method());
        Assert.assertEquals(annotation.value1(), attributeMappingOfValue1.getValue());
        Assert.assertNotEquals("", attributeMappingOfValue1.toString());

        Assert.assertThrows(AirFrameworkException.class, () -> mapping.getAttributeMapping(-1));
        Assert.assertThrows(AirFrameworkException.class, () -> mapping.getAttributeMapping(Integer.MAX_VALUE));


        Assert.assertTrue(mapping.isMappedAttribute("value1"));
        Assert.assertTrue(mapping.isMappedAttribute("value2"));
        Assert.assertFalse(mapping.isMappedAttribute("value3"));
        Assert.assertFalse(mapping.isMappedAttribute("none"));

        Assert.assertFalse(mapping.hasAttribute("none"));
        Assert.assertNull(mapping.getAttributeMapping("none"));

        int idxOfValue1 = mapping.getAttributeIndex("value1");
        Assert.assertEquals(0, idxOfValue1);
        Assert.assertEquals(AttributeMappedAnnotationMapping.NOT_FOUND_INDEX, mapping.getAttributeIndex("none"));
        Assert.assertNull(mapping.getAttributeMapping("none"));
    }

    @Test
    public void testDiffValueAlias() {
        DiffValueAlias diffValueAlias = Foo.class.getDeclaredAnnotation(DiffValueAlias.class);
        Assert.assertThrows(AirFrameworkException.class, () -> new AttributeMappedAnnotationMapping(null, DiffValueAlias.class, diffValueAlias));
    }

    @Test
    public void testDiffDefaultValueAlias() {
        DiffDefaultValueAlias diffDefaultValueAlias = Foo.class.getDeclaredAnnotation(DiffDefaultValueAlias.class);
        Assert.assertThrows(AirFrameworkException.class, () -> new AttributeMappedAnnotationMapping(null, DiffDefaultValueAlias.class, diffDefaultValueAlias));
    }

    @Test
    public void testAliasForSingleAnnotation() {
        SingleAliasAnnotation annotation = Foo.class.getDeclaredAnnotation(SingleAliasAnnotation.class);
        AttributeMappedAnnotationMapping mapping = new AttributeMappedAnnotationMapping(null, annotation.annotationType(), annotation);

        AttributeMappedAnnotationMapping.AttributeMapping value1 = mapping.getAttributeMapping("value1");
        Assert.assertNotNull(value1);
        Assert.assertEquals(annotation.value1(), value1.getValue());

        AttributeMappedAnnotationMapping.AttributeMapping value2 = mapping.getAttributeMapping("value2");
        Assert.assertNotNull(value2);
        Assert.assertEquals(annotation.value1(), value2.getValue());
    }

    @Test
    public void testAliasForMultiAnnotationWhenNotBoundRootAnnotation() {
        AnnotationC annotationC = Foo.class.getDeclaredAnnotation(AnnotationC.class);
        AttributeMappedAnnotationMapping mappingOfC = new AttributeMappedAnnotationMapping(null, annotationC.annotationType(), null);
        AnnotationB annotationB = AnnotationC.class.getDeclaredAnnotation(AnnotationB.class);
        AttributeMappedAnnotationMapping mappingOfB = new AttributeMappedAnnotationMapping(mappingOfC, annotationB.annotationType(), annotationB);
        AnnotationA annotationA = AnnotationB.class.getDeclaredAnnotation(AnnotationA.class);
        AttributeMappedAnnotationMapping mappingOfA = new AttributeMappedAnnotationMapping(mappingOfB, annotationA.annotationType(), annotationA);

        AttributeMappedAnnotationMapping.AttributeMapping value1OfC = mappingOfC.getAttributeMapping("value1");
        Assert.assertNotNull(value1OfC);
        Assert.assertNull(value1OfC.mapping().getAnnotation());
        AttributeMappedAnnotationMapping.AttributeMapping value2OfC =mappingOfC.getAttributeMapping("value2");
        Assert.assertEquals(value1OfC, value2OfC);
    }

    @Test
    public void testAliasForMultiAnnotation() {
        AnnotationC annotationC = Foo.class.getDeclaredAnnotation(AnnotationC.class);
        AttributeMappedAnnotationMapping mappingOfC = new AttributeMappedAnnotationMapping(null, annotationC.annotationType(), annotationC);
        AnnotationB annotationB = AnnotationC.class.getDeclaredAnnotation(AnnotationB.class);
        AttributeMappedAnnotationMapping mappingOfB = new AttributeMappedAnnotationMapping(mappingOfC, annotationB.annotationType(), annotationB);
        AnnotationA annotationA = AnnotationB.class.getDeclaredAnnotation(AnnotationA.class);
        AttributeMappedAnnotationMapping mappingOfA = new AttributeMappedAnnotationMapping(mappingOfB, annotationA.annotationType(), annotationA);

        AttributeMappedAnnotationMapping.AttributeMapping value1OfC = mappingOfC.getAttributeMapping("value1");
        Assert.assertNotNull(value1OfC);
        Assert.assertEquals(annotationC.value1(), value1OfC.getValue());

        AttributeMappedAnnotationMapping.AttributeMapping value2OfC = mappingOfC.getAttributeMapping("value2");
        Assert.assertNotNull(value2OfC);
        Assert.assertEquals(annotationC.value1(), value2OfC.getValue());

        AttributeMappedAnnotationMapping.AttributeMapping value1OfB = mappingOfB.getAttributeMapping("value");
        Assert.assertNotNull(value1OfB);
        Assert.assertEquals(annotationC.value1(), value1OfB.getValue());

        AttributeMappedAnnotationMapping.AttributeMapping valueOfA = mappingOfA.getAttributeMapping("value");
        Assert.assertNotNull(valueOfA);
        Assert.assertEquals(annotationC.value1(), valueOfA.getValue());
    }

    @Test
    public void testNoAttributes() {
        NoAttributes noAttributes = Foo.class.getDeclaredAnnotation(NoAttributes.class);
        AttributeMappedAnnotationMapping mappingOfNoAttributes1 = new AttributeMappedAnnotationMapping(null, noAttributes.annotationType(), noAttributes);
        Assert.assertEquals(0, mappingOfNoAttributes1.getAttributes().length);
        AttributeMappedAnnotationMapping mappingOfNoAttributes2 = new AttributeMappedAnnotationMapping(null, noAttributes.annotationType(), noAttributes);
        Assert.assertSame(mappingOfNoAttributes1.getAttributes(), mappingOfNoAttributes2.getAttributes());
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface UnidirectionalAlias {
        @Alias(attribute = "value2")
        String value() default "";
        String value2() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface AnnotationA {
        String value() default "";
    }

    @AnnotationA("1")
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface AnnotationB {
        @Alias(annotation = AnnotationA.class, attribute = "value")
        String value() default "";
    }

    @AnnotationB(value = "2")
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
        // 可以不用加 @Alias(attribute = "value1")
        String value2() default "";
        String value3() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface DiffValueAlias {
        @Alias("value2")
        String value1();
        @Alias("value1")
        String value2();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface DiffDefaultValueAlias {
        @Alias("value2")
        String value1() default "a";
        @Alias("value1")
        String value2() default "b";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface NoAttributes {
    }
    @NoAttributes
    @DiffDefaultValueAlias
    @DiffValueAlias(value1 = "a", value2 = "b")
    @UnidirectionalAlias
    @SingleAliasAnnotation(value1 = "single")
    @AnnotationC(value1 = "3")
    private static class Foo {}
}
