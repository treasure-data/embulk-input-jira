package org.embulk.input.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.embulk.GuiceBinder;
import org.embulk.RandomManager;
import org.embulk.TestPluginSourceModule;
import org.embulk.TestUtilityModule;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.config.ModelManager;
import org.embulk.exec.ExecModule;
import org.embulk.exec.ExtensionServiceLoaderModule;
import org.embulk.exec.SystemConfigModule;
import org.embulk.jruby.JRubyScriptingModule;
import org.embulk.plugin.BuiltinPluginSourceModule;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecSession;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Random;

/**
 * This is a clone from {@link org.embulk.EmbulkTestRuntime}, since there is no easy way to extend it.
 * The only modification is on the provided systemConfig, enable tests to run `embulk/guess/jira.rb`
 */
public class JiraPluginTestRuntime extends GuiceBinder
{
    private static ConfigSource getSystemConfig()
    {
        final ObjectNode configNode = JsonNodeFactory.instance.objectNode();
        configNode.set("jruby_load_path", JsonNodeFactory.instance.arrayNode().add("lib"));

        return new DataSourceImpl(new ModelManager(null, new ObjectMapper()), configNode);
    }

    public static class TestRuntimeModule implements Module
    {
        @Override
        public void configure(final Binder binder)
        {
            final ConfigSource systemConfig = getSystemConfig();
            new SystemConfigModule(systemConfig).configure(binder);
            new ExecModule(systemConfig).configure(binder);
            new ExtensionServiceLoaderModule(systemConfig).configure(binder);
            new BuiltinPluginSourceModule().configure(binder);
            new JRubyScriptingModule(systemConfig).configure(binder);
            new TestUtilityModule().configure(binder);
            new TestPluginSourceModule().configure(binder);
        }
    }

    private final ExecSession exec;

    public JiraPluginTestRuntime()
    {
        super(new TestRuntimeModule());
        final Injector injector = getInjector();
        final ConfigSource execConfig = new DataSourceImpl(injector.getInstance(ModelManager.class));
        this.exec = ExecSession.builder(injector).fromExecConfig(execConfig).build();
    }

    public ExecSession getExec()
    {
        return exec;
    }

    public BufferAllocator getBufferAllocator()
    {
        return getInstance(BufferAllocator.class);
    }

    public ModelManager getModelManager()
    {
        return getInstance(ModelManager.class);
    }

    public Random getRandom()
    {
        return getInstance(RandomManager.class).getRandom();
    }

    public PluginClassLoaderFactory getPluginClassLoaderFactory()
    {
        return getInstance(PluginClassLoaderFactory.class);
    }

    @Override
    public Statement apply(final Statement base, final Description description)
    {
        final Statement superStatement = JiraPluginTestRuntime.super.apply(base, description);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable
            {
                try {
                    Exec.doWith(exec, (ExecAction<Void>) () -> {
                        try {
                            superStatement.evaluate();
                        }
                        catch (final Throwable ex) {
                            throw new RuntimeExecutionException(ex);
                        }
                        return null;
                    });
                }
                catch (final RuntimeException ex) {
                    throw ex.getCause();
                }
                finally {
                    exec.cleanup();
                }
            }
        };
    }

    private static class RuntimeExecutionException extends RuntimeException
    {
        public RuntimeExecutionException(final Throwable cause)
        {
            super(cause);
        }
    }
}
