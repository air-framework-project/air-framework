package cn.airframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>用于将属性与其他属性进行绑定，从而构成联动关系。
 *
 * @author huangchengxing
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Alias {

    /**
     * <p>要覆写的注解属性名称，当不指定时，默认为当前被注解属性的名称 <br/>
     * <b>注意：</b>{@code attribute}与{@code value()}任填其一即可，
     * 当同时指定时，优先使用{@code attribute}。
     *
     * @return 属性名称
     */
    @Alias("value")
    String attribute() default "";

    /**
     * <p>要覆写的注解属性名称，当不指定时，默认为当前被注解属性的名称 <br/>
     * <b>注意：</b>{@code attribute}与{@code value()}任填其一即可，
     * 当同时指定时，优先使用{@code attribute}。
     *
     * @return 属性名称
     */
    @Alias("attribute")
    String value() default "";

    /**
     * 要绑定的属性所在的注解类型，不指定时，默认绑定当前注解中的属性
     *
     * @return 注解类
     */
    Class<? extends Annotation> annotation() default Annotation.class;
}
