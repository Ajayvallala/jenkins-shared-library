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
            booleanParam(name: 'TOGGLE', defaultValue: true, description: 'Toggle this value')
            choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')
            password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
        }
        stages{
            stage('Build'){
                steps{
                    script {
                    echo 'Building'  
                    echo "Hello ${params.PERSON}"
                    echo "Biography: ${params.BIOGRAPHY}"
                    echo "Toggle: ${params.TOGGLE}"
                    echo "Choice: ${params.CHOICE}"
                    echo "Password: ${params.PASSWORD}"
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
                deleteDir()
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