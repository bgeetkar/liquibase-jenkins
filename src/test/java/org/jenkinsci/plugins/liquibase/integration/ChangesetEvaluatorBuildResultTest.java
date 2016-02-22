package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.liquibase.builder.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.builder.ExecutedChangesetAction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class ChangesetEvaluatorBuildResultTest {

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluatorBuildResultTest.class);
    private static final String CHANGESET_FILENAME = "changeset.xml";
    private static final String SUNNY_DAY_CHANGESET_XML = "/sunny-day-changeset.xml";
    private static final String CHANGESET_WITH_ERROR_XML = "/changeset-with-error.xml";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        temporaryFolder.create();
    }

    @Test
    public void should_report_success_with_error_free_changeset() throws IOException, ExecutionException, InterruptedException {

        FreeStyleBuild freeStyleBuild = createAndBuildErrorFreeProject();

        assertThat(freeStyleBuild.getResult(), is(Result.SUCCESS));
}

    @Test
    public void should_indicate_unstable_build_when_changeset_has_error()
            throws IOException, ExecutionException, InterruptedException {
        File changesetFileWithError = createChangesetFileWithError(temporaryFolder);
        FreeStyleProject project = createProjectWithChangelogFile(changesetFileWithError);
        FreeStyleBuild build = launchBuildForProject(project);
        assertThat(build.getResult(), is(Result.UNSTABLE));
    }

    @Test
    public void should_have_expected_executed_changesets()
            throws InterruptedException, ExecutionException, IOException {

        FreeStyleBuild build = createAndBuildErrorFreeProject();
        ExecutedChangesetAction action = build.getActions(ExecutedChangesetAction.class).get(0);
        assertThat(action.getChangeSetDetails(), hasSize(3));
    }


    protected FreeStyleBuild createAndBuildErrorFreeProject()
            throws IOException, InterruptedException, ExecutionException {
        File changelogFile = createErrorFreeChangeset(temporaryFolder);
        FreeStyleProject project = createProjectWithChangelogFile(changelogFile);
        return launchBuildForProject(project);
    }


    protected FreeStyleProject createProjectWithChangelogFile(File changelogFile) throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        ChangesetEvaluator.ChangesetEvaluatorBuilder builder =
                new ChangesetEvaluator.ChangesetEvaluatorBuilder();

        builder.withChangeLogFile(changelogFile).withUrl("jdbc:h2:mem:test").withDatabaseEngine("H2");
        project.getBuildersList().add(builder.build());
        return project;
    }

    protected static FreeStyleBuild launchBuildForProject(FreeStyleProject project)
            throws InterruptedException, ExecutionException {
        return project.scheduleBuild2(0).get();
    }

    private File createErrorFreeChangeset(TemporaryFolder temporaryFolder) throws IOException {
        String changesetResourcePath = SUNNY_DAY_CHANGESET_XML;
        return createChangesetFile(temporaryFolder, changesetResourcePath);
    }

    private File createChangesetFileWithError(TemporaryFolder temporaryFolder) throws IOException {
        String changesetResourcePath = CHANGESET_WITH_ERROR_XML;
        return createChangesetFile(temporaryFolder, changesetResourcePath);
    }

    private File createChangesetFile(TemporaryFolder temporaryFolder,
                                     String changesetResourcePath) throws IOException {
        File changesetFile = temporaryFolder.newFile(CHANGESET_FILENAME);
        InputStream resourceAsStream = getClass().getResourceAsStream(changesetResourcePath);
        String changeset = IOUtils.toString(resourceAsStream);
        FileUtils.write(changesetFile, changeset);
        return changesetFile;
    }
}