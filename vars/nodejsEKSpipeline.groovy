def call(Map configMap){
    pipeline {
        agent {
            label 'AGENT-1'
        }
        environment {
            appVersion=''
            COMPONENT=configMap.get('component')
            AWS_ACCOUNT_ID = "448049818055"
            PROJECT=configMap.get('project')
            REGION="us-east-1"
        }
        parameters{
            booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value to deply in DEV envirnoment')
        }
        stages{
            stage('Read Package.json'){
                steps{
                    script{
                        def packageJSON = readJSON file: 'package.json'
                        appVersion = packageJSON.version
                        echo "appVersion:${appVersion}"
                    }
                }
            }
            stage('Install Dependencies'){
                steps{
                    script{
                        sh """
                        npm install
                        """
                    }
                }
            }
            stage('Unit Testing '){
                steps{
                    script{
                        sh """
                        echo "Unit testing started"
                        """
                    }
                }
            }
/*             stage('sonar-scan'){
                environment {
                    ScannerHome = tool 'sonar-scanner'
                }
                steps{
                    script{
                        withSonarQubeEnv(installationName: 'sonar-server'){
                            sh "${ScannerHome}/bin/sonar-scanner"
                        }

                    }
                }
            }

            stage('Quality Gates'){
                steps{
                    timeout(time: 1, unit: 'HOURS'){
                        waitForQualityGate abortPipeline: true
                    }          
                }
            } */

            stage('Check Dependabot Alerts') {
                environment { 
                    GITHUB_TOKEN = credentials('git-token')
                }
                steps {
                    script {
                        // Fetch alerts from GitHub
                        def response = sh(
                            script: """
                                curl -s -H "Accept: application/vnd.github+json" \
                                    -H "Authorization: token ${GITHUB_TOKEN}" \
                                    https://api.github.com/repos/Ajayvallala/${COMPONENT}/dependabot/alerts
                            """,
                            returnStdout: true
                        ).trim()

                        // Parse JSON
                        def json = readJSON text: response

                        // Filter alerts by severity
                        def criticalOrHigh = json.findAll { alert ->
                            def severity = alert?.security_advisory?.severity?.toLowerCase()
                            def state = alert?.state?.toLowerCase()
                            return (state == "open" && (severity == "critical" || severity == "high"))
                        }

                        if (criticalOrHigh.size() > 0) {
                            error "❌ Found ${criticalOrHigh.size()} HIGH/CRITICAL Dependabot alerts. Failing pipeline!"
                        } else {
                            echo "✅ No HIGH/CRITICAL Dependabot alerts found."
                        }
                    }
                }
            }

            stage('Docker Image Build'){
                    steps{
                        script{
                        withAWS(credentials:'aws-creds', region: 'us-east-1'){
                            sh """
                            aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com

                            docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .

                            docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                            """
                        }
                    }
                }
            }
        /*  stage('Check Scan Results') {
                    steps {
                        script {
                            withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                            // Fetch scan findings
                                def findings = sh(
                                    script: """
                                        aws ecr describe-image-scan-findings \
                                        --repository-name ${PROJECT}/${COMPONENT} \
                                        --image-id imageTag=${appVersion} \
                                        --region ${REGION} \
                                        --output json
                                    """,
                                    returnStdout: true
                                ).trim()

                                // Parse JSON
                                def json = readJSON text: findings

                                def highCritical = json.imageScanFindings.findings.findAll {
                                    it.severity == "HIGH" || it.severity == "CRITICAL"
                                }

                                if (highCritical.size() > 0) {
                                    echo "❌ Found ${highCritical.size()} HIGH/CRITICAL vulnerabilities!"
                                    currentBuild.result = 'FAILURE'
                                    error("Build failed due to vulnerabilities")
                                } else {
                                    echo "✅ No HIGH/CRITICAL vulnerabilities found."
                                }
                            }
                        }
                    }
                }
            } */
            stage('trigger deploy'){
                when{
                    expression { params.deploy }
                }
                steps{
                    script{
                        build job: "${COMPONENT}-cd"
                        parameters[
                            string(name: 'appVersion', value: "${appVersion}"),
                            string(name: 'deploy_to', value: 'dev')
                        ]
                        propagate: false
                        wait: false
                    }
                }
            }


        }
        post {
            always{
                deleteDir()
            }
            success{
                echo "Build has been success"
            }
            failure{
                error "Build has been failed"
            }
        }



    }
}