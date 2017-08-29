package org.talend.dataprep.qa.api;

import com.github.valfirst.jbehave.junit.monitoring.JUnitReportingRunner;
import org.jbehave.core.Embeddable;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.i18n.LocalizedKeywords;
import org.jbehave.core.io.CodeLocations;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.model.ExamplesTableFactory;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.jbehave.core.steps.ParameterConverters;
import org.jbehave.core.steps.ParameterConverters.DateConverter;
import org.jbehave.core.steps.ParameterConverters.ExamplesTableConverter;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.talend.dataprep.qa.api.steps.Story1Steps;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;
import static org.jbehave.core.reporters.Format.*;

/**
 * <p>
 * {@link Embeddable} class to run multiple textual stories via JUnit.
 * </p>
 * <p>
 * Stories are specified in classpath and correspondingly the {@link LoadFromClasspath} story loader is configured.
 * </p>
 */
@RunWith(JUnitReportingRunner.class)
public class AllStoriesTest extends JUnitStories {


    public AllStoriesTest() {
        configuredEmbedder() //
                .embedderControls() //
                .doGenerateViewAfterStories(true) //
                .doIgnoreFailureInStories(true) //
                .doIgnoreFailureInView(true) //
                .useThreads(2);
    }

    /**
     * Spring configuration.
     *
     * @param basePackages
     * @return
     */
    public static AnnotationConfigApplicationContext createContextFromBasePackages(String... basePackages) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.scan(basePackages);
        applicationContext.refresh();
        return applicationContext;
    }

    @Override
    public Configuration configuration() {
        Class<? extends Embeddable> embeddableClass = this.getClass();
        // Start from default ParameterConverters instance
        ParameterConverters parameterConverters = new ParameterConverters();
        // factory to allow parameter conversion and loading from external resources (used by StoryParser too)
        ExamplesTableFactory examplesTableFactory = new ExamplesTableFactory(new LocalizedKeywords(), new LoadFromClasspath(embeddableClass), parameterConverters);
        // add custom converters
        parameterConverters.addConverters(new DateConverter(new SimpleDateFormat("yyyy-MM-dd")),
                new ExamplesTableConverter(examplesTableFactory));
        return new MostUsefulConfiguration()
                .useStoryLoader(new LoadFromClasspath(embeddableClass))
                .useStoryParser(new RegexStoryParser(examplesTableFactory))
                .useStoryReporterBuilder(new StoryReporterBuilder()
                        .withCodeLocation(CodeLocations.codeLocationFromClass(embeddableClass))
                        .withDefaultFormats()
                        .withFormats(CONSOLE, TXT, HTML, XML))
                .useParameterConverters(parameterConverters);
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        return new InstanceStepsFactory(configuration(), new Story1Steps());
    }

    @Override
    protected List<String> storyPaths() {
        List<String> storiesToRun;
        StoryFinder sf = new StoryFinder();
        URL baseUrl = codeLocationFromClass(this.getClass());
        String storyProperty = System.getProperty("story");
        if (storyProperty == null || storyProperty.isEmpty()) {
            storiesToRun = sf.findPaths(baseUrl, "**/*.story", "**/excluded*.story");
        } else {
            storiesToRun = new ArrayList<String>();
            String[] storyNames = storyProperty.split(",");
            for (String storyName : storyNames) {
                System.out.println(storyName);
                storiesToRun.addAll(sf.findPaths(baseUrl, "**/" + storyName, ""));
            }
        }
        return storiesToRun;
    }

}
