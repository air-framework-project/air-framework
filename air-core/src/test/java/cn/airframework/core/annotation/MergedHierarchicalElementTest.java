package cn.airframework.core.annotation;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

/**
 * test for {@link MergedHierarchicalElement}
 *
 * @author huangchengxing
 */
public class MergedHierarchicalElementTest {

    @Test
    public void testCache() {
        var he = MergedHierarchicalElement.from(Foo.class);
        Assert.assertSame(he, MergedHierarchicalElement.from(Foo.class));
        MergedHierarchicalElement.clearCaches();
        Assert.assertNotSame(he, MergedHierarchicalElement.from(Foo.class));

        he = MergedHierarchicalElement.from(Foo.class);
        Assert.assertSame(he, MergedHierarchicalElement.from(Foo.class));
        MergedHierarchicalElement.clearCaches();
        Assert.assertNotSame(he, MergedHierarchicalElement.from(Foo.class));

        Assert.assertEquals(
            MergedHierarchicalElement.from(getClass()).toString(),
            MergedHierarchicalElement.from(getClass()).toString()
        );
    }

    @SneakyThrows
    @Test
    public void testResolveHierarchy() {
        // 解析获取类的层级结构
        var classElement = MergedHierarchicalElement.from(Foo.class);
        Assert.assertEquals(4, classElement.hierarchies().size());
        Assert.assertArrayEquals(
            new Object[]{ Foo.class, Super.class, Object.class, Interface.class },
            classElement.hierarchyStream().map(MergedHierarchicalElement::getRoot).toArray()
        );

        // 解析方法的层级结构
        Method interfaceMethod = Interface.class.getDeclaredMethod("getNum");
        Method SuperMethod = Super.class.getDeclaredMethod("getNum");
        Method FooMethod = Foo.class.getDeclaredMethod("getNum");
        var methodElement = MergedHierarchicalElement.from(FooMethod);
        Assert.assertEquals(3, methodElement.hierarchies().size());
        Assert.assertArrayEquals(
            new Object[]{ FooMethod, SuperMethod, interfaceMethod },
            methodElement.hierarchyStream().map(MergedHierarchicalElement::getRoot).toArray()
        );

        // 解析注解的层级结构
        var annotationTypeElement = MergedHierarchicalElement.from(ChildAnnotation.class);
        Assert.assertEquals(2, annotationTypeElement.hierarchies().size());
        Assert.assertArrayEquals(
            new Object[]{ ChildAnnotation.class, ParentAnnotation.class },
            annotationTypeElement.hierarchyStream().map(MergedHierarchicalElement::getRoot).toArray()
        );

        // 解析不具备层级结构的注解
        Parameter parameter = Interface.class.getDeclaredMethod("getNum", Object.class).getParameters()[0];
        var otherElement = MergedHierarchicalElement.from(parameter);
        Assert.assertEquals(1, otherElement.hierarchies().size());
        Assert.assertArrayEquals(
            new Object[]{ parameter },
            otherElement.hierarchyStream().map(MergedHierarchicalElement::getRoot).toArray()
        );
    }

    @Test
    public void testGetAnnotation() {
        var ele = MergedHierarchicalElement.from(Foo.class);

        // 检查是否互为别名
        var childAnnotation = ele.getAnnotation(ChildAnnotation.class);
        Assert.assertNotNull(childAnnotation);
        Assert.assertEquals(childAnnotation.value(), childAnnotation.name());

        // 检查元注解是否可以被获取，且被子注解进行属性覆写
        var parentAnnotation = ele.getAnnotation(ParentAnnotation.class);
        Assert.assertNotNull(parentAnnotation);
        Assert.assertEquals(parentAnnotation.value(), childAnnotation.value());
    }

    @Test
    public void testGetAnnotationByType() {
        var ele = MergedHierarchicalElement.from(Foo.class);

        // 检查是否成功获取到所有的注解，并且别名机制都生效
        var childAnnotations = ele.getAnnotationsByType(ChildAnnotation.class);
        Assert.assertEquals(4, childAnnotations.length);
        Arrays.asList(childAnnotations).forEach(a -> Assert.assertEquals(a.value(), a.name()));

        // 检查是否成功获取到一个直接注解与三个属性值被覆写过的元注解
        var parentAnnotations = ele.getAnnotationsByType(ParentAnnotation.class);
        Assert.assertEquals(4, parentAnnotations.length);
        Assert.assertEquals(
            Set.of("Interface Parent", "Interface", "Super", "Foo").stream().sorted().toList(),
            Stream.of(parentAnnotations).map(ParentAnnotation::value).sorted().toList()
        );
    }

    @Test
    public void testGetAnnotations() {
        var ele = MergedHierarchicalElement.from(Foo.class);
        var annotations = ele.getAnnotations();
        Assert.assertEquals(8, annotations.length);

        // 获取一个直接注解和三个属性值被覆写的元注解
        var parents = Stream.of(annotations)
            .filter(a -> a instanceof ParentAnnotation)
            .map(ParentAnnotation.class::cast)
            .toList();
        Assert.assertEquals(4, parents.size());
        Assert.assertEquals(
            Set.of("Interface Parent", "Interface", "Super", "Foo").stream().sorted().toList(),
            parents.stream().map(ParentAnnotation::value).sorted().toList()
        );

        // 获取三个直接注解和一个数组被覆写的元注解
        var cas = Stream.of(annotations)
            .filter(a -> a instanceof ChildAnnotation)
            .map(ChildAnnotation.class::cast)
            .toList();
        Assert.assertEquals(4, parents.size());
        cas.forEach(a -> Assert.assertEquals(a.value(), a.name()));
        Assert.assertEquals(
            Set.of("Interface Parent", "Interface", "Super", "Foo").stream().sorted().toList(),
            parents.stream().map(ParentAnnotation::value).sorted().toList()
        );
    }

    @Test
    public void testGetDeclaredAnnotations() {
        // 获取类上的直接注解和元注解
        var ele = MergedHierarchicalElement.from(Foo.class);
        var annotations = ele.getDeclaredAnnotations();
        Assert.assertEquals(2, annotations.length);

        // 获取属性值被覆写的元注解
        var pas = Stream.of(annotations)
            .filter(a -> a instanceof ParentAnnotation)
            .map(ParentAnnotation.class::cast)
            .toList();
        Assert.assertEquals(1, pas.size());
        Assert.assertEquals(
            Set.of("Foo").stream().sorted().toList(),
            pas.stream().map(ParentAnnotation::value).sorted().toList()
        );

        // 获取直接注解数
        var cas = Stream.of(annotations)
            .filter(a -> a instanceof ChildAnnotation)
            .map(ChildAnnotation.class::cast)
            .toList();
        Assert.assertEquals(1, pas.size());
        cas.forEach(a -> Assert.assertEquals(a.value(), a.name()));
        Assert.assertEquals(
            Set.of("Foo").stream().sorted().toList(),
            pas.stream().map(ParentAnnotation::value).sorted().toList()
        );
    }

    @SneakyThrows
    @Test
    public void testIsAnnotationPresent() {
        Method method = Foo.class.getDeclaredMethod("getStr");
        var ele = MergedHierarchicalElement.from(method);
//        Assert.assertTrue(ele.isAnnotationPresent(Annotation1.class));
        Assert.assertTrue(ele.isAnnotationPresent(Annotation2.class));
    }

    @SneakyThrows
    @Test
    public void testIsDeclaredAnnotationPresent() {
        Method method = Foo.class.getDeclaredMethod("getStr");

        var ele = MergedHierarchicalElement.from(method);
        Assert.assertTrue(ele.isDeclaredAnnotationPresent(Annotation1.class));
        Assert.assertFalse(ele.isDeclaredAnnotationPresent(Annotation2.class));
    }

    @SneakyThrows
    @Annotation1
    @Annotation2({
        @Annotation1, @Annotation1, @Annotation1
    })
    @Test
    public void getRepeatableAnnotations() {
        Method method = getClass().getDeclaredMethod("getRepeatableAnnotations");
        var ele = MergedHierarchicalElement.from(method);
        Annotation1[] annotations = ele.getDeclaredAnnotationsByType(Annotation1.class);
        Assert.assertEquals(4, annotations.length);
    }

    @Annotation1
    @Annotation2
    @SneakyThrows
    @Test
    public void testStreamable() {
        Method method = getClass().getDeclaredMethod("testStreamable");
        var ele = MergedHierarchicalElement.from(method);
        var mhe = ele.stream().toList();
        Assert.assertEquals(3, mhe.size());
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation2 {
        Annotation1[] value() default {};
    }

    @Repeatable(Annotation2.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation1 {}

    @ChildAnnotation
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ParentAnnotation {
        String value() default "";
    }

    @ParentAnnotation
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ChildAnnotation {
        @Alias(annotation = ParentAnnotation.class)
        String value() default "";
        @Alias("value")
        String name() default "";
    }

    @ParentAnnotation("Interface Parent")
    @ChildAnnotation("Interface")
    private interface Interface {
        Number getNum();
        default Number getNum(Object parameter) {
            return 1;
        }

        @Annotation2
        String getStr();
    }

    @ChildAnnotation("Super")
    private static abstract class Super implements Interface {
        @Override
        public abstract Integer getNum();
    }

    @ChildAnnotation("Foo")
    private static class Foo extends Super {
        @Override
        public Integer getNum() {
            return 1;
        }

        @Annotation1
        @Override
        public String getStr() {
            return null;
        }
    }
}
