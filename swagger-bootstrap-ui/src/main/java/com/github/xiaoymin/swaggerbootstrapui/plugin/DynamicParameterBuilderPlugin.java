/*
 * Copyright (C) 2018 Zhejiang xiaominfo Technology CO.,LTD.
 * All rights reserved.
 * Official Web Site: http://www.xiaominfo.com.
 * Developer Web Site: http://open.xiaominfo.com.
 */

package com.github.xiaoymin.swaggerbootstrapui.plugin;

import com.fasterxml.classmate.TypeResolver;
import com.github.xiaoymin.swaggerbootstrapui.util.CommonUtils;
import com.google.common.base.Optional;
import io.swagger.annotations.ApiOperationSupport;
import io.swagger.annotations.DynamicParameter;
import io.swagger.annotations.DynamicParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;

import java.util.HashMap;
import java.util.Map;

/***
 *
 * @since:swagger-bootstrap-ui 1.0
 * @author <a href="mailto:xiaoymin@foxmail.com">xiaoymin@foxmail.com</a> 
 * 2019/06/09 15:30
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+10)
public class DynamicParameterBuilderPlugin implements ParameterBuilderPlugin {

    @Autowired
    private TypeResolver typeResolver;

    private final Map<String,String> cacheGenModelMaps=new HashMap<>();

    @Override
    public void apply(ParameterContext parameterContext) {
        ResolvedMethodParameter resolvedMethodParameter=parameterContext.resolvedMethodParameter();
        if (resolvedMethodParameter!=null&&resolvedMethodParameter.getParameterType()!=null&&resolvedMethodParameter.getParameterType().getErasedType()!=null){
            if (Map.class.isAssignableFrom(resolvedMethodParameter.getParameterType().getErasedType())){
                //查询注解
                Optional<ApiOperationSupport> supportOptional=parameterContext.getOperationContext().findAnnotation(ApiOperationSupport.class);
                if (supportOptional.isPresent()){
                    ApiOperationSupport support=supportOptional.get();
                    //判断是否包含自定义注解
                    addDynamicParameter(support.params(),parameterContext);
                }else{
                    Optional<DynamicParameters> dynamicParametersOptional=parameterContext.getOperationContext().findAnnotation(DynamicParameters.class);
                    if (dynamicParametersOptional.isPresent()){
                        addDynamicParameter(dynamicParametersOptional.get(),parameterContext);
                    }
                }
            }
        }
    }


    private void addDynamicParameter(DynamicParameters dynamicParameters,ParameterContext parameterContext){
        if (dynamicParameters!=null){
            //name是否包含
            String name=dynamicParameters.name();
            if (name==null||"".equals(name)){
                //gen
                name=genClassName(parameterContext);
            }
            //判断是否存在
            if (cacheGenModelMaps.containsKey(name)){
                //存在,以方法名称作为ClassName
                name=genClassName(parameterContext);
            }
            cacheGenModelMaps.put(name,name);
            DynamicParameter[] dynamics=dynamicParameters.properties();
            Class<?> clazz= CommonUtils.createDynamicModelClass(name,dynamics);
            parameterContext.getDocumentationContext()
                    .getAdditionalModels()
                    .add(typeResolver.resolve(clazz));
            parameterContext.parameterBuilder()  //修改Map参数的ModelRef为我们动态生成的class
                    .parameterType("body")
                    .modelRef(new ModelRef(name))
                    .name(name);
        }
    }


    public String genClassName(ParameterContext parameterContext){
        //gen
        String name=parameterContext.getOperationContext().getName();
        if (name!=null&&"".equals(name)){
            if (name.length()==1){
                name=name.toUpperCase();
            }else{
                name=name.substring(0,1).toUpperCase()+name.substring(1);
            }
        }
        return name;
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}
