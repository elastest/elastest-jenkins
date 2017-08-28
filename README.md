ElasTest Plugin
=======================

This plugin adds support for sending the console log of a Job Build of Jenkins to ElasTest Platform, so that you can store, compare and analyze it.

Install
=======

* Download the sources from GitHub Repository `https://github.com/elastest/elastest-jenkins.git`
* Generate the `hpi` file with the command: `mvn package`
* Put the `hpi` file in the directory `$JENKINS_HOME/plugins`
* Restart jenkins

Configure
=========
1. To configure this plugin, you must go to "Manage Jenkins => Global Tool Configuration" and in the section `ElasTest Plugin` fill the `ElasTest URL` field (e.g. http://localhost:8090/).   

2. Check the 



Pipeline
========

Logstash plugin can be used as a publisher in pipeline jobs:

```Groovy
 node('master') {
        sh'''
        echo 'Hello, world!'
        '''
        elastest{
            ......
        } 
 }
```

License
=======

The Logstash Plugin is licensed under the MIT License.



