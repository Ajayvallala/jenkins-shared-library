def call(){
    pipeline {
        agent {
            label 'AGENT-1'
        }
        environment{
            COURSE='Jenkins'
        }
        options{
        //  timeout(time:10, unit:'SECONDS')
            disableConcurrentBuilds()
        }
        parameters{
            string(name: 'PERSON', defaultValue: 'Ajay', description: 'Who should I say hello to?')
            text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')
        }
        stages{
            stage('Build'){
                steps{
                    script {
                    echo 'Building'  
                    echo "Hello ${params.PERSON}"
                    echo "Biography: ${params.BIOGRAPHY}"
                    } 
                }
            }

            stage('Test'){
                steps{
                    script{
                    echo 'Testing'
                    }
                    
                }
            }
        }

        post {
            always{
                echo 'I will say hello always'
            }
            success{
                echo 'I will say hello when success'
            }
            failure{
                echo 'I will say hello when failure'  
            }
        }
    }
}

def demo(){
    pipeline {
        agent {
            label 'AGENT-1'
        }
        environment{
            COURSE='Jenkins'
        }
        stages{
            stage('Build'){
                steps{
                    script {
                    echo "Learning ${COURSE}"
                    }
                }
            }  
        }
    }
}