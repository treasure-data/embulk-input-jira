Embulk::JavaPlugin.register_input(
  "jira", "org.embulk.input.jira.JiraInputPlugin",
  File.expand_path('../../../../classpath', __FILE__))
