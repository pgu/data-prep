package org.talend.dataprep.qa.api.config;

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.failures.SilentlyAbsorbingFailure;
import org.jbehave.core.io.*;
import org.jbehave.core.junit.JUnitStory;
import org.jbehave.core.reporters.FilePrintStreamFactory;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.ParameterControls;
import org.jbehave.core.steps.spring.SpringStepsFactory;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@AcceptanceTest
public abstract class APIStory extends JUnitStory {

    @Autowired
    protected Environment environment;
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    public APIStory() {
        Embedder embedder = new Embedder();
        embedder.useEmbedderControls(embedderControls());
        embedder.useMetaFilters(Arrays.asList("+author *", "theme *", "-skip"));
        useEmbedder(embedder);
    }

    @Override
    public Configuration configuration() {
        return new MostUsefulConfiguration() //
                .useStoryPathResolver(storyPathResolver()) //
                .useStoryLoader(storyLoader()) //
                .useStoryReporterBuilder(storyReporterBuilder()) //
                .useFailureStrategy(new SilentlyAbsorbingFailure()) //
                .useParameterControls(parameterControls());
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        return new SpringStepsFactory(configuration(), applicationContext);
    }

    private EmbedderControls embedderControls() {
        return new EmbedderControls().doIgnoreFailureInView(true);
    }

    private ParameterControls parameterControls() {
        return new ParameterControls().useDelimiterNamedParameters(true);
    }

    private StoryPathResolver storyPathResolver() {
        return new CasePreservingResolver();
    }

    private StoryLoader storyLoader() {
        return new LoadFromClasspath();
    }

    private StoryReporterBuilder storyReporterBuilder() {
        return new StoryReporterBuilder() //
                .withCodeLocation(CodeLocations.codeLocationFromClass(this.getClass())) //
                .withPathResolver(new FilePrintStreamFactory.ResolveToPackagedName()) //
                .withFailureTrace(true) //
                .withDefaultFormats();
    }
}
