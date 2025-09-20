def call(Map configmap){
    pipeline {
        agent {
            label 'AGENT-1'
        }
        environment{
            COURSE='Jenkins'
            NAME= configmap.get.('name')
            SURNAME= configmap.get.('surname')
        }
        options{
        //  timeout(time:10, unit:'SECONDS')
            disableConcurrentBuilds()
        }
        stages{
            stage('Build'){
                steps{
                    script {
                    echo "Hello ${NAME}-${SURNAME}"
                    demo()
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

def demo() {
   echo "Running demo from shared library"
}