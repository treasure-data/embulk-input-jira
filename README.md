[![Build Status](https://travis-ci.org/treasure-data/embulk-input-jira.svg)](https://travis-ci.org/treasure-data/embulk-input-jira)

# Jira input plugin for Embulk

TODO: Write short description here and embulk-input-jira.gemspec file.

## Overview

* **Plugin type**: input
* **Resume supported**: yes
* **Cleanup supported**: yes
* **Guess supported**: no

## Configuration

- **property1**: description (string, required)
- **property2**: description (integer, default: default-value)

## Example

```yaml
in:
  type: jira
  property1: example1
  property2: example2
```


## Build

```
$ rake
```
