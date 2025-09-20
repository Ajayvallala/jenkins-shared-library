def call(Map configmap){
    pipeline{
        agent{
            label 'AGENT-1'
        }
        environment{
            appVersion = ''
            REGION= 'us-east-1'
            PROJECT= configmap.get('project')
            COMPONENT= configmap.get('component')
            ACCOUNTNO= '448049818055'
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value to Deploy')
        }

        stages{
            stage('Read package.json'){
                steps{
                    script {
                        def packageJSON = readJSON file: 'package.json'
                        appVersion = packageJSON.version
                        echo "appVersion:${appVersion}"
                    }
                }
            }
            stage('Install Dependencies'){
                steps {
                    script {
                        sh """
                        npm install
                        """
                    }
                }
            }
            /* stage('sonar scan'){
                environment{
                    ScannerHome = tool 'sonar-7.2'
                }
                steps{
                    script{
                    withSonarQubeEnv(installationName: 'sonar-7.2'){
                    sh "${ScannerHome}/bin/sonar-scanner"
                    }
                    }
                    
                }
            }
            stage("Quality Gate") {
            steps {
            timeout(time: 5, unit: 'MINUTES') {
                waitForQualityGate abortPipeline: true
                }
            }
            } */
            /* stage('Check Dependabot Alerts') {
                environment { 
                    GITHUB_TOKEN = credentials('github-token')
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
            } */
            stage('Docker Build'){
                steps{
                    script{
                        withAWS(credentials: 'aws-creds', region: "${REGION}"){
                        sh """
                        aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACCOUNTNO}.dkr.ecr.${REGION}.amazonaws.com

                        docker build -t ${ACCOUNTNO}.dkr.ecr.${REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .

                        docker push ${ACCOUNTNO}.dkr.ecr.${REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                        """    
                    }
                    }
                }
            }
            stage('Scan Image '){
                steps{
                    script{

                    }
                }
            }
            stage('Check Scan Results') {
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
            stage('Trigger CD'){
                when{
                    expression { params.deploy }
                }
                steps {
                    script {
                        build job: "${COMPONENT}-cd",
                        parameters: [
                            string(name: 'appVersion', value: "${appVersion}"),
                            string(name: 'deploy_to', value: 'dev')
                        ],
                        propagate: false,  // even SG fails VPC will not be effected
                        wait: false // VPC will not wait for SG pipeline completion
                    }
                }
            } 
            
        }

        post{
        always {
            echo "Pipeline exection completed"
            deleteDir()
        }
        success{
            echo "Success"
        }
        failure{
            echo "Failure"
        }
      }
   }

}