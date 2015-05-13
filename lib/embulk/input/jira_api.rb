require "jiralicious"

class JiraApi
  def self.setup(&block)
    Jiralicious.configure(&block)
    new
  end

  def search(jql)
    Jiralicious.search(jql)
  end
end
