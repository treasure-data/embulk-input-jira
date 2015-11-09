[![Build Status](https://travis-ci.org/treasure-data/embulk-input-jira.svg)](https://travis-ci.org/treasure-data/embulk-input-jira)
[![Code Climate](https://codeclimate.com/github/treasure-data/embulk-input-jira/badges/gpa.svg)](https://codeclimate.com/github/treasure-data/embulk-input-jira)
[![Test Coverage](https://codeclimate.com/github/treasure-data/embulk-input-jira/badges/coverage.svg)](https://codeclimate.com/github/treasure-data/embulk-input-jira/coverage)
[![Gem Version](https://badge.fury.io/rb/embulk-input-jira.svg)](https://badge.fury.io/rb/embulk-input-jira)

# Jira input plugin for Embulk

embulk-input-jira is the Embulk input plugin for [JIRA](https://www.atlassian.com/software/jira).

## Overview

Required Embulk version >= 0.6.12

* **Plugin type**: input
* **Resume supported**: no
* **Cleanup supported**: no
* **Guess supported**: yes

## Configuration

- **username** JIRA username (string, required)
- **password** JIRA password (string, required)
- **uri** JIRA API endpoint (string, required)
- **jql** [JQL](https://confluence.atlassian.com/display/JIRA/Advanced+Searching) for extract target issues (string, required)
- **columns** target issue attributes. You can generate this configuration by `guess` command (array, required)

## Example

```yaml
in:
  type: jira
  username: USERNAME
  password: PASSWORD
  uri: http://localhost:8090
  jql: project = PRO AND summary~Fix
  columns:
    - {name: id, type: long}
    - {name: key, type: string}
    - {name: project.name, type: string}
    - {name: summary, type: string}
    - {name: assignee.name, type: string}
```

## Build

```
$ bundle exec rake build
```
