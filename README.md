[![Build Status](https://travis-ci.org/treasure-data/embulk-input-jira.svg)](https://travis-ci.org/treasure-data/embulk-input-jira)
[![Code Climate](https://codeclimate.com/github/treasure-data/embulk-input-jira/badges/gpa.svg)](https://codeclimate.com/github/treasure-data/embulk-input-jira)
[![Test Coverage](https://codeclimate.com/github/treasure-data/embulk-input-jira/badges/coverage.svg)](https://codeclimate.com/github/treasure-data/embulk-input-jira/coverage)
[![Gem Version](https://badge.fury.io/rb/embulk-input-jira.svg)](https://badge.fury.io/rb/embulk-input-jira)

# Jira input plugin for Embulk

embulk-input-jira is the Embulk input plugin for [JIRA](https://www.atlassian.com/software/jira).

## Overview

Required Embulk version >= 0.9.20

* **Plugin type**: input
* **Resume supported**: no
* **Cleanup supported**: no
* **Guess supported**: yes

## Configuration
**Since JIRA is going to deprecate the basic authentication with passwords and cookie-based authentication to their APIs, we highly recommend you to use email and API key to authenticate to JIRA APIs. [Deprecated notice](https://developer.atlassian.com/cloud/jira/platform/deprecation-notice-basic-auth-and-cookie-based-auth/)**

- **username** JIRA username or email (string, required)
- **password** JIRA password or API keys (string, required)
- **uri** JIRA API endpoint (string, required)
- **jql** [JQL](https://confluence.atlassian.com/display/JIRA/Advanced+Searching) for extract target issues (string, required)
- **columns** target issue attributes. You can generate this configuration by `guess` command (array, required)
- **retry_initial_wait_sec**: Wait seconds for exponential backoff initial value (integer, default: 1)
- **retry_limit**: Try to retry this times (integer, default: 5)
- **expand_json** The boolean value is to enable/disable json expanding. (boolean, default: true)

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
$ ./gradlew gem       # -t to watch change of files and rebuild continuously
```