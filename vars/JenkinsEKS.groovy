def call(Map configMap){
    pipeline{
    // agent any 
    agent{
        label "AGENT-1"
    }
    environment{
        region="us-east-1"
        accountid ="832510228841"
        project= configMap.get("project")
        component = configMap.get("component")
        deploy_to = configMap.get("deploy_to")
        vesion= ""
    }
    options{
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds() 
        ansiColor('xterm')
    } 
    stages{
        stage("read the version"){
            steps{
                script{
                    def file= readJSON file: 'package.json'
                    version = file.version
                    echo "this is building space for catalogue"
                    echo "this is node version: ${version}"
                }     
            }
        }
        stage("build"){
            steps{
                withAWS(credentials: 'aws-credentials', region: "${env.region}" )
                {
                    sh """
                        set -e
                        aws eks update-kubeconfig --region ${region} --name ${project}-${deploy_to}
                        kubectl get nodes 
                        sed -i "s/IMAGE_VERSION/${vesion}/g" values.yaml
                        sed -i "s/ENV/${deploy_to}/g" values.yaml
                        helm upgrade --install ${component} -f values-${deploy_to}.yaml --atomic --wait --timeout=5m .
                    """
                }
            }
        }

    }
    post{
        always{
            echo "cleaning work space"
            cleanWs()
        }
        
    }
}
}