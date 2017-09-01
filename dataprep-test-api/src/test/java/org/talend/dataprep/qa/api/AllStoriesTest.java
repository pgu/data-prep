package org.talend.dataprep.qa.api;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jbehave.core.Embeddable;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.configuration.spring.SpringStoryControls;
import org.jbehave.core.failures.FailingUponPendingStep;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.spring.SpringStepsFactory;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;
import static org.jbehave.core.reporters.Format.CONSOLE;
import static org.jbehave.core.reporters.Format.HTML_TEMPLATE;

/**
 * <p>
 * {@link Embeddable} class to run multiple textual stories via JUnit.
 * </p>
 * <p>
 * Stories are specified in classpath and correspondingly the {@link LoadFromClasspath} story loader is configured.
 * </p>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ImportResource({"classpath:/application.properties"})
public class AllStoriesTest extends JUnitStories {

    private static final Logger LOGGER = LogManager.getLogger(AllStoriesTest.class);

    public AllStoriesTest() {
        super();
        System.out.println(this.getClass().getName() + ".AllStoriesTest()");
        // clear report folder ?
        // configure => not necessary
        configuredEmbedder()//
                .embedderControls()//
                .doGenerateViewAfterStories(true)//
                .doIgnoreFailureInStories(false)//
                .doIgnoreFailureInView(true)//
                .doVerboseFailures(true)//
                .useThreads(1);
    }


    @Override
    public Configuration configuration() {
        System.out.println(this.getClass().getName() + ".configuration()");
        Class<? extends Embeddable> embeddableClass = this.getClass();
        URL codeLocation = codeLocationFromClass(embeddableClass);
        StoryReporterBuilder storyReporter = //
                new StoryReporterBuilder() //
                        .withCodeLocation(codeLocation) //
                        .withDefaultFormats() //
                        .withFormats(CONSOLE, //
                                HTML_TEMPLATE) //
                        .withFailureTrace(true) //
                        .withFailureTraceCompression(true);

        return new MostUsefulConfiguration()
                .useStoryLoader(new UTF8StoryLoader(embeddableClass))
                .useStoryControls(new SpringStoryControls()
                        .doResetStateBeforeScenario(false)
                        .doSkipScenariosAfterFailure(false)
                        .doDryRun(false))
                .useStoryReporterBuilder(storyReporter)
                .usePendingStepStrategy(new FailingUponPendingStep());
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext("org.talend.dataprep.qa.api", "org.talend.dataprep.qa.api.steps");
        System.out.println("------------------");
        System.out.println(ctx.getEnvironment().toString());

        System.out.println("------------------");
        return new SpringStepsFactory(configuration(), ctx); // TODO : Add Step classes
    }

    // TODO use Java 8 lambdas
    @Override
    protected List<String> storyPaths() {
        List<String> storiesToRun;
        String resPath = "src/test/resources";
        StoryFinder sf = new StoryFinder();
        String storyProperty = System.getProperty("stories"); // TODO : put into string const
        if (storyProperty == null || storyProperty.isEmpty()) {
            storiesToRun = sf.findPaths(resPath, "**/TDP_*.story", "");
        } else {
            storiesToRun = new ArrayList<>();
            String[] storyNames = storyProperty.split(",");
            for (String storyName : storyNames) {
                storiesToRun.addAll(sf.findPaths(resPath, "**/" + storyName, ""));
            }
        }
        return storiesToRun;
    }
}
