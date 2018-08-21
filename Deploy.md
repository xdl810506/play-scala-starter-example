Deploying your application:
https://www.playframework.com/documentation/2.6.x/Deploying

Configuring logging:
https://www.playframework.com/documentation/2.6.x/SettingsLogger#Using-a-custom-application-loader

Filters:
https://www.playframework.com/documentation/2.6.x/Filters

Understanding Play thread pools:
https://www.playframework.com/documentation/2.6.x/ThreadPools

Configuring the Akka HTTP server backend:
https://www.playframework.com/documentation/2.6.x/SettingsAkkaHttp

//step to update META-INF/build.json
stage("Version 2.6.${suffixBuildTag}") {
    sh "sed 's/\"build tag.*\"/\"build tag\" : \"2.6.${suffixBuildTag}\"/' conf/META-INF/build.json > temp.json"
    sh "cat temp.json && mv temp.json conf/META-INF/build.json"

    sh "git rev-parse HEAD > ./git_hash"
    sh "git_hash=\$(head -n 1 ./git_hash) && sed -i 's/\"git hash.*\"/\"git hash\" : \"'\$git_hash'\"/' conf/META-INF/build.json"
    sh "cat conf/META-INF/build.json"

    sh "sed -i 's/\"built.*\"/\"built\" : \"'\$(date -u +\"%Y-%m-%dT%H:%M:%SZ\")'\"/' conf/META-INF/build.json > temp.json"
    sh "cat conf/META-INF/build.json"
}