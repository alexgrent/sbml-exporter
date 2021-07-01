package org.reactome.server.tools.sbml.config;

import org.aspectj.lang.Aspects;
import org.reactome.server.graph.aop.LazyFetchAspect;
import org.reactome.server.graph.config.GraphCoreNeo4jConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = {"org.reactome.server.graph"})
@EntityScan(basePackages = {"org.reactome.server.graph.domain.model"})
@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = {"org.reactome.server.graph.repository"})
@EnableSpringConfigured
@EnableAutoConfiguration
public class ReactomeNeo4jConfig extends GraphCoreNeo4jConfig {

    /**
     * This is needed to get hold of the instance of the aspect which is created outside of the spring container,
     * and make it available for autowiring.
     */
    @Bean
    public LazyFetchAspect lazyFetchAspect() {
        return Aspects.aspectOf(LazyFetchAspect.class);
    }

}