package org.talend.dataprep.qa.api;

import com.github.valfirst.jbehave.junit.monitoring.JUnitReportingRunner;
import org.jbehave.core.Embeddable;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.failures.FailingUponPendingStep;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.spring.SpringStepsFactory;
import org.junit.runner.RunWith;
import org.talend.dataprep.qa.api.util.Springs;

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
@RunWith(JUnitReportingRunner.class)
public class AllStoriesTest extends JUnitStories {

    public AllStoriesTest() {
        configuredEmbedder()//
                .embedderControls()//
                .doGenerateViewAfterStories(true)//
                .doIgnoreFailureInStories(false)//
                .doIgnoreFailureInView(true)//
                .doVerboseFailures(true)//
                .useThreads(2);
    }

    @Override
    public Configuration configuration() {
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
                .useStoryReporterBuilder(storyReporter)
                .usePendingStepStrategy(new FailingUponPendingStep());
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        return new SpringStepsFactory(configuration(),
                Springs.createAnnotatedContextFromBasePackages("org.talend.dataprep.qa.api"));
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
