package cn.airframework.core.annotation;

import cn.airframework.core.exception.AirFrameworkException;
import cn.airframework.core.util.Asserts;
import cn.airframework.core.util.CollectionUtils;
import cn.airframework.core.util.MultiMap;
import cn.airframework.core.util.ReflectUtils;
import cn.airframework.core.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * <p>表示一个注解类型的映射，多个注解类型之间可以互相关联，
 * 从而形成一个注解类型的层级结构。<br/>
 * 在层级结构中的注解将根据属性上的{@link Alias}配置进行属性映射，
 * 从而实现属性间的别名和覆写效果。
 * 
 * <p>每当对象创建时，将会扫描整个依赖链上的所有注解映射，
 * 并解析它们的属性配置从而使属性别名和覆写机制生效。<br/>
 * 这是一个比较消耗性能的操作，因此推荐当构建完依赖树后，
 * 直接缓存所有的节点，避免重复解析带来的额外开销。
 *
 * @author huangchengxing
 * @see Alias
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class AttributeMappedAnnotationMapping {

    private static final Method[] EMPTY_ATTRIBUTES = new Method[0];
    private static final int[] EMPTY_MAPPED_ATTRIBUTES = new int[0];
    private static final AttributeMappedAnnotationMapping[] EMPTY_MAPPED_ATTRIBUTE_VALUE_SOURCES = new AttributeMappedAnnotationMapping[0];
    private static final AliasSet[] EMPTY_ALIAS_SETS = new AliasSet[0];


    /**
     * 不存在的属性对应的默认下标
     */
    static final int NOT_FOUND_INDEX = -1;

    /**
     * 注解所处在的层级，根注解的层级为0，依次递增
     *
     * @see #source
     */
    @Getter
    @ToString.Include
    private final int level;

    /**
     * 源注解，若源注解为空，则认为当前映射即对应一个根注解
     */
    @Getter
    @EqualsAndHashCode.Include
    @Nullable
    private final AttributeMappedAnnotationMapping source;

    /**
     * 该类型绑定的注解对象，在绑定对象为根注解时，可能为{@code null}
     */
    @Getter
    @EqualsAndHashCode.Include
    @Nullable
    private final Annotation annotation;

    /**
     * 注解类型
     */
    @ToString.Include
    @Getter
    @EqualsAndHashCode.Include
    private final Class<? extends Annotation> annotationType;

    /**
     * 注解属性，属性在该数组中的下标等同于属性本身
     */
    @Getter
    private final Method[] attributes;

    /**
     * 被其他属性覆写的属性下标
     */
    private final int[] mappedAttributes;

    /**
     * 被其他属性覆写的属性下标来源
     */
    private final AttributeMappedAnnotationMapping[] mappedAttributeValueSources;

    /**
     * 当前注解中互为别名的属性列表
     */
    private final AliasSet[] aliasSets;

    /**
     * 被人重写引为别名的属性
     */
    private final MultiMap<Method, Method> aliasedBy;

    /**
     * 构建一个注解映射
     *
     * @param source         源注解
     * @param annotationType 注解类型
     * @param annotation     元注解
     */
    AttributeMappedAnnotationMapping(
        @Nullable AttributeMappedAnnotationMapping source, Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {
        // 基本参数
        Asserts.isTrue(
            Objects.isNull(annotation) || Objects.equals(annotation.annotationType(), annotationType),
            "The annotation to be bound must match the annotation type to be mapped: [{}]", annotationType
        );
        this.annotation = annotation;
        this.annotationType = annotationType;

        // 层级结构参数，如果不是根注解，则映射必须绑定一个注解实例
        this.source = source;
        this.level = Objects.isNull(source) ? 0 : source.level + 1;
        // 若指定了源注解，则需要检查是否出现类型的循环依赖
        checkCircularReference(source, annotationType);
        Asserts.isFalse(
            !isRoot() && Objects.isNull(annotation),
            "Mapping must bound to annotation instance when it's not a root annotation type!"
        );

        // 如果注解有属性
        Method[] annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotationType);
        if (annotationAttributes.length > 0) {
            this.attributes = annotationAttributes;
            this.aliasedBy = resolveAliasedBy(annotationType);
            this.aliasSets = new AliasSet[annotationAttributes.length];
            this.mappedAttributes = new int[annotationAttributes.length];
            Arrays.fill(mappedAttributes, NOT_FOUND_INDEX);
            this.mappedAttributeValueSources = new AttributeMappedAnnotationMapping[annotationAttributes.length];
            Arrays.fill(mappedAttributeValueSources, this);
            resolveAliasedAttributes();
        }
        // 如果注解没有任何属性
        else {
            this.attributes = EMPTY_ATTRIBUTES;
            this.aliasedBy = MultiMap.empty();
            this.aliasSets = EMPTY_ALIAS_SETS;
            this.mappedAttributes = EMPTY_MAPPED_ATTRIBUTES;
            this.mappedAttributeValueSources = EMPTY_MAPPED_ATTRIBUTE_VALUE_SOURCES;
        }
    }

    /**
     * <p>拷贝构造器，一般用于拷贝一个未绑定注解实例的根注解类型，
     * 并绑定到特定的注解对象，从而使属性解析生效。
     *
     * @param annotation 注解
     * @param mapping 映射
     */
    private AttributeMappedAnnotationMapping(@Nullable Annotation annotation, AttributeMappedAnnotationMapping mapping) {
        Asserts.isTrue(
            Objects.isNull(annotation) || Objects.equals(annotation.annotationType(), mapping.getAnnotationType()),
            "The annotation to be bound must match the annotation type to be mapped: [{}]", mapping.getAnnotationType()
        );
        this.annotation = annotation;
        this.annotationType = mapping.getAnnotationType();
        this.source = mapping.source;
        this.level = mapping.level;

        // 如果注解有属性
        if (mapping.mappedAttributes.length > 0) {
            this.attributes = mapping.attributes;
            this.aliasedBy = mapping.aliasedBy;
            this.aliasSets = mapping.aliasSets;
            this.mappedAttributes = Arrays.copyOf(mapping.mappedAttributes, attributes.length);
            this.mappedAttributeValueSources = Arrays.copyOf(mapping.mappedAttributeValueSources, attributes.length);

            // 确认别名最终值
            if (Objects.isNull(mapping.annotation) && Objects.nonNull(annotation) && attributes.length > 0) {
                Stream.of(aliasSets)
                    .distinct()
                    .filter(Objects::nonNull)
                    .forEach(as -> as.resolveAndUpdate(this));
            }
        }
        // 注解没有任何属性
        else {
            this.attributes = EMPTY_ATTRIBUTES;
            this.aliasedBy = MultiMap.empty();
            this.aliasSets = EMPTY_ALIAS_SETS;
            this.mappedAttributes = EMPTY_MAPPED_ATTRIBUTES;
            this.mappedAttributeValueSources = EMPTY_MAPPED_ATTRIBUTE_VALUE_SOURCES;
        }
    }

    private static void checkCircularReference(@Nullable AttributeMappedAnnotationMapping source, Class<? extends Annotation> annotationType) {
        if (Objects.nonNull(source)) {
            AttributeMappedAnnotationMapping check = source;
            do {
                Asserts.isFalse(
                    Objects.equals(check.annotationType, annotationType),
                    "There is a circular reference between the source annotation type [{}] and the current annotation type [{}]",
                    source.annotationType, annotationType
                );
                check = check.source;
            } while (Objects.nonNull(check));
        }
    }

    // ========== 属性访问 ==========

    /**
     * 返回一个当前实例的拷贝，并绑定注解实例
     *
     * @param annotation 注解实例
     * @return 注解实例
     */
    AttributeMappedAnnotationMapping bound(@NonNull Annotation annotation) {
        return new AttributeMappedAnnotationMapping(annotation, this);
    }

    /**
     * 是否有解析后的属性
     *
     * @return 是否
     */
    public boolean hasMappedAttributes() {
        return Arrays.stream(mappedAttributes)
            .anyMatch(idx -> idx != NOT_FOUND_INDEX);
    }

    /**
     * 该属性是否是一个映射属性
     *
     * @param attributeName 属性名称
     * @return 是否
     */
    public boolean isMappedAttribute(String attributeName) {
        int index = getAttributeIndex(attributeName);
        if (index == NOT_FOUND_INDEX) {
            return false;
        }
        return mappedAttributes[index] != NOT_FOUND_INDEX;
    }

    /**
     * 该注解是否有指定属性
     *
     * @param attributeName 属性名称
     * @return 是否
     */
    public boolean hasAttribute(String attributeName) {
        return getAttributeIndex(attributeName) != NOT_FOUND_INDEX;
    }

    /**
     * 获取要访问的属性
     *
     * @param attributeName 属性名称
     * @return 属性映射
     */
    @Nullable
    public AttributeMapping getAttributeMapping(String attributeName) {
        int index = getAttributeIndex(attributeName);
        if (index == NOT_FOUND_INDEX) {
            return null;
        }
        return getAttributeMapping(index);
    }

    /**
     * 获取属性下标
     *
     * @param attribute 属性
     * @return 属性下标
     */
    private int getAttributeIndex(Method attribute) {
        return Arrays.asList(attributes).indexOf(attribute);
    }

    /**
     * 获取属性下标
     *
     * @param attributeName 属性名称
     * @return 属性下标
     */
    public int getAttributeIndex(String attributeName) {
        for (int i = 0; i < attributes.length; i++) {
            Method attribute = attributes[i];
            if (Objects.equals(attributeName, attribute.getName())) {
                return i;
            }
        }
        return NOT_FOUND_INDEX;
    }

    /**
     * 获取要访问的属性
     *
     * @param index 属性下标
     * @return 属性映射
     */
    public AttributeMapping getAttributeMapping(int index) {
        Asserts.isTrue(
            index > -1 && index < attributes.length,
            "Attribute index out of bounds, accessing [{}] but the actual scope is [0 ,{}]",
            index, attributes.length
        );
        // 若属性没有被覆写，则直接返回
        int resolveIndex = mappedAttributes[index];
        if (resolveIndex == NOT_FOUND_INDEX) {
            return new AttributeMapping(attributes[index], index, this);
        }
        // 若属性被覆写，则检查是否被同一注解内的另一个属性覆写
        AttributeMappedAnnotationMapping attributeSource = mappedAttributeValueSources[index];
        if (Objects.isNull(attributeSource) || attributeSource == this) {
            return new AttributeMapping(attributes[resolveIndex], resolveIndex, this);
        }
        // 若属性被其他注解中的属性覆写，则递归查找
        return attributeSource.getAttributeMapping(resolveIndex);
    }

    /**
     * 当前注解是否为根注解
     *
     * @return 是否
     */
    public boolean isRoot() {
        return level == 0;
    }

    // ========== 别名解析 ==========

    private MultiMap<Method, Method> resolveAliasedBy(Class<? extends Annotation> annotationType) {
        MultiMap<Method, Method> methods = MultiMap.arrayListMultimap();
        forEachAttribute((idx, attribute) -> {
            Alias alias = attribute.getAnnotation(Alias.class);
            if (Objects.isNull(alias)) {
                return;
            }
            // 若不指定注解类型，则默认注解类型即为当前类型
            Class<?> targetAnnoType = Objects.equals(alias.annotation(), Annotation.class) ? annotationType : alias.annotation();
            // 若不指定属性名称，则默认为当前属性名称
            String targetAttrName = determineAttributeName(attribute, alias);
            Method targetAttr = ReflectUtils.getDeclaredMethod(targetAnnoType, targetAttrName);
            // 别名属性必须存在
            Asserts.isNotNull(
                targetAttr, "Cannot find method [{}] from annotation type [{}]",
                targetAttrName, targetAnnoType
            );
            // 别名属性不能是自己本身
            Asserts.isFalse(
                Objects.equals(attribute, targetAttr), "Attribute [{}] from annotation type [{}] cannot be an alias for itself",
                attribute.getName(), annotationType
            );
            // 互为别名的属性的类型必须一致
            Asserts.isTrue(
                attribute.getReturnType().isAssignableFrom(targetAttr.getReturnType()), "Attribute [{}] from annotation type [{}] is not assignable to alias attribute [{}] from annotation type [{}]",
                attribute.getName(), annotationType, targetAttrName, targetAnnoType
            );
            methods.put(targetAttr, attribute);
        });
        return MultiMap.copyOf(methods);
    }

    @NonNull
    private static String determineAttributeName(Method attribute, Alias alias) {
        return Stream.of(alias.attribute(), alias.value())
            .filter(StringUtils::isNotEmpty)
            .findFirst()
            .orElse(attribute.getName());
    }

    private void resolveAliasedAttributes() {
        forEachAttribute((idx, attribute) -> {
            // 从高层向低层递归，依次搜集每一个注解中直接或间接关联的属性
            // 比如，a -> b -> c 三层结构中，每级别注解属性与指向关系如下:
            // c: c1
            // b: b1(c1)
            // a: a1(c1), a2(b2)
            // 则从 c 向 a 递归遍历后，aliases 集合的变化情况如下：
            // c: [c1]
            // b: [c1, b1(通过 c1 找到 b1)]
            // a: [c1, b1, a1(通过 c1 找到 a1), a2(通过 b1 找到 a2)]
            // 如此，当完成递归后，aliases 将有包含 c1 在内，所有直接(b1, a1)或间接(a2)与 c1 关联的属性
            List<AttributeMapping> aliases = CollectionUtils.newCollection(ArrayList::new, new AttributeMapping(attribute, idx, this));
            doResolveAliasedAttributes(aliases);
            if (!aliases.isEmpty()) {
                determineFinallyActiveAttribute(aliases);
            }
        });
    }

    private void doResolveAliasedAttributes(List<AttributeMapping> aliases) {
        forEachMapping(mapping -> {
            // 获取同一注解内互为别名的属性
            List<AttributeMapping> aliasesFromSameMapping = aliases.stream()
                .map(attr -> mapping.aliasedBy.get(attr.method))
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .map(attr -> new AttributeMapping(attr, mapping.getAttributeIndex(attr), mapping))
                .toList();
            aliases.addAll(aliasesFromSameMapping);
            updateAliasSetIfNecessary(mapping, aliases);
        });
    }

    private void updateAliasSetIfNecessary(AttributeMappedAnnotationMapping mapping, List<AttributeMapping> aliases) {
        // 筛选出在同一注解中直接或间接互为别名的属性
        int[] indexes = aliases.stream()
            .filter(attr -> attr.mapping == mapping)
            .mapToInt(AttributeMapping::idx)
            .sorted()
            .toArray();
        // 需要至少有一对别名，并且至少有一个属性还未与其他属性关联
        if (indexes.length < 2 || isAllAliased(indexes)) {
            return;
        }
        // 重新设置别名的关联关系
        AliasSet aliasSet = new AliasSet(indexes);
        IntStream.of(indexes).forEach(idx -> mapping.aliasSets[idx] = aliasSet);
    }

    private void determineFinallyActiveAttribute(List<AttributeMapping> aliases) {
        // 需要至少有一对别名
        if (aliases.size() < 2) {
            return;
        }
        // 按属性所在注解的层级由低到高排序，
        // 由于层级越低（离根注解越近）则覆写优先级越高，因此首个属性的覆写优先级必定最高
        AttributeMapping highestPriorityAttribute = aliases.stream()
            .min(Comparator.comparingInt(am -> am.mapping.level))
            .orElseThrow(() -> new AirFrameworkException("Highest priority attribute must not be null"));

        AttributeMappedAnnotationMapping activeSource = highestPriorityAttribute.mapping;
        int active;
        // 如果属性没有被一个代表根注解的、尚未绑定注解实例的注解中的属性覆写，那么此时即可确认覆写关系:
        // 1.如果属性有别名，那么此时即可计算最终值，然后使用该值覆盖；
        // 2.如果属性没有别名，那么此时可以直接使用该属性覆盖当前属性
        if (Objects.nonNull(activeSource.annotation)) {
            active = Optional.ofNullable(activeSource.aliasSets[highestPriorityAttribute.idx])
                .map(as -> as.resolve(activeSource))
                .orElse(highestPriorityAttribute.idx());
        }
        // 如果属性被来自根注解的属性覆写，且当前映射尚未绑定注解实例，那么有两种情况：
        // 1.如果这个属性有别名，那么此时由于没有绑定实例，因此无法计算得到最终值，只能将属性下标记录下来，等到绑定实例后再计算
        // 2.如果这个属性没有别名，那么此时可以直接使用该属性覆盖当前属性
        // 不管哪一种情况，最终我们都直接使用该属性覆盖当前属性
        else {
            Asserts.isTrue(
                activeSource.isRoot(),
                "Cannot resolve attribute [{}] from annotation type [{}] because it is not bound to any annotation instance and it is not root annotation type",
                highestPriorityAttribute.method.getName(), annotationType
            );
            active = highestPriorityAttribute.idx;
        }

        // 后续的属性都使用首个属性的值进行覆写
        for (AttributeMapping attr : aliases) {
            attr.mapping.mappedAttributes[attr.idx] = active;
            attr.mapping.mappedAttributeValueSources[attr.idx] = activeSource;
        }
    }

    // ========== 辅助方法 ==========

    private boolean isAllAliased(int... indexes) {
        AliasSet shared = null;
        for (int index : indexes) {
            AliasSet curr = aliasSets[index];
            // 该属性没有关联到任何别名属性
            if (Objects.isNull(curr)) {
                return false;
            }
            if (Objects.isNull(shared)) {
                shared = curr;
                continue;
            }
            // 该属性只关联到了其他别名属性
            if (shared != curr) {
                return false;
            }
        }
        return true;
    }

    private void forEachMapping(Consumer<AttributeMappedAnnotationMapping> consumer) {
        AttributeMappedAnnotationMapping mapping = this;
        while (Objects.nonNull(mapping)) {
            consumer.accept(mapping);
            mapping = mapping.source;
        }
    }

    private void forEachAttribute(BiConsumer<Integer, Method> consumer) {
        for (int i = 0; i < attributes.length; i++) {
            consumer.accept(i, attributes[i]);
        }
    }

    /**
     * 别名设置，一组具有别名关系的属性会共用同一实例
     */
    @RequiredArgsConstructor
    static class AliasSet {

        /**
         * 关联的别名字段对应的属性在{@link #attributes}中的下标
         */
        private final int[] indexes;

        /**
         * 从所有关联的别名属性中，选择出唯一个最终有效的属性：
         * <ul>
         *     <li>若所有属性都只有默认值，则要求所有的默认值都必须相等，若符合则返回首个属性，否则报错；</li>
         *     <li>若有且仅有一个属性具有非默认值，则返回该属性；</li>
         *     <li>若有多个属性具有非默认值，则要求所有的非默认值都必须相等，若符合并返回该首个具有非默认值的属性，否则报错；</li>
         * </ul>
         *
         * @return 用于获取最终值的属性下标
         */
        int resolve(AttributeMappedAnnotationMapping mapping) {
            int resolvedIndex = NOT_FOUND_INDEX;
            boolean hasNotDef = false;
            Object lastValue = null;
            for (int index : indexes) {
                Method attribute = mapping.attributes[index];

                // 获取属性的值，并确认是否为默认值
                Object def = attribute.getDefaultValue();
                Object undef = ReflectUtils.invokeRaw(mapping.annotation, attribute);
                boolean isDefault = Objects.equals(def, undef);

                // 若是首个属性
                if (resolvedIndex == NOT_FOUND_INDEX) {
                    resolvedIndex = index;
                    lastValue = isDefault ? def : undef;
                    hasNotDef = !isDefault;
                }

                // 不是首个属性，且已存在非默认值
                else if (hasNotDef) {
                    // 如果当前也是非默认值，则要求两值必须相等
                    if (!isDefault) {
                        Asserts.isTrue(
                            Objects.equals(lastValue, undef),
                            "Aliased attribute [{}] and [{}] must have same not default value, but is different: [{}] <==> [{}]",
                            mapping.attributes[resolvedIndex], attribute, lastValue, undef
                        );
                    }
                }

                // 不是首个属性，但是还没有非默认值，而当前值恰好是非默认值，直接更新当前有效值与对应索引
                else if (!isDefault) {
                    hasNotDef = true;
                    lastValue = undef;
                    resolvedIndex = index;
                }
                // 不是首个属性，还没有非默认值，如果当前也是默认值，则要求两值必须相等
                else {
                    Asserts.isTrue(
                        Objects.equals(lastValue, def),
                        "Aliased attribute [{}] and [{}] must have same default value, but is different: [{}] <==> [{}]",
                        mapping.attributes[resolvedIndex], attribute, lastValue, def
                    );
                }
            }
            Asserts.isFalse(resolvedIndex == NOT_FOUND_INDEX, "Can not resolve aliased attributes from [{}]", mapping.annotation);
            return resolvedIndex;
        }

        /**
         * 通过{@link #resolve}方法确认最终值，并更新相关的字段映射
         */
        void resolveAndUpdate(AttributeMappedAnnotationMapping mapping) {
            int resolved = resolve(mapping);
            for (int idx : indexes) {
                mapping.mappedAttributes[idx] = resolved;
                mapping.mappedAttributeValueSources[idx] = mapping;
            }
        }
    }

    /**
     * 属性映射对象
     */
    record AttributeMapping(Method method, int idx, AttributeMappedAnnotationMapping mapping) {

        /**
         * 获取属性值
         *
         * @return 属性值
         */
        public <T> T getValue() {
            Asserts.isNotNull(mapping.annotation, "Cannot get value from type mapping which not bound to annotation: {}", mapping.getAnnotationType());
            return ReflectUtils.invokeRaw(mapping.annotation, method);
        }
    }
}
