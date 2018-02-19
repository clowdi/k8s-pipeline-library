# k8s-pipeline-library

## Description
Jenkins shared pipeline library to be used for deployment in Kubernetes clusters.

## Documentation
[Deployment in k8s](https://docs.google.com/a/ft.com/document/d/15ecubJwkszH1B360Ah31uXy2UekpWlgfEmQeH9_wko8/edit?usp=sharing)

## What to do when adding a new environment
When provisioning a new environment, Jenkins needs to "see" it, in order to be able to deploy to it.
Here are the steps needed in order for Jenkins to "see" it.
1.  Create a new branch for this repository
1. Add the definition of the new environment in the EnvsRegistry.groovy. Here's an example:
    ```
          Environment prod = new Environment()
          prod.name = Environment.PROD_NAME
          prod.slackChannel = "#k8s-pipeline-notif"
          prod.regions = ["eu", "us"]
          prod.clusters = [Cluster.DELIVERY, Cluster.PUBLISHING, Cluster.NEO4J]
          prod.clusterToApiServerMap = [
              ("eu-" + Cluster.DELIVERY)  : "https://upp-prod-delivery-eu-api.ft.com",
              ("us-" + Cluster.DELIVERY)  : "https://upp-prod-delivery-us-api.ft.com",
              ("eu-" + Cluster.PUBLISHING): "https://upp-prod-publish-eu-api.ft.com",
              ("us-" + Cluster.PUBLISHING): "https://upp-prod-publish-us-api.ft.com",
              ("eu-" + Cluster.NEO4J): "https://upp-prod-neo4j-eu-api.ft.com",
              ("us-" + Cluster.NEO4J): "https://upp-prod-neo4j-us-api.ft.com"
          ]
    ```    
    Here are the characteristics of an Environment:    
      1. It has a name and a notifications slack channel. 
      1. It might be spread across multiple AWS regions
      1. In each region, it might have multiple clusters (stacks).
      1. For each cluster(stack) we must define the URL of the K8S APi server.
1. Don't forget to add the newly defined environment to the `envs` list in the EnvsRegistry class.
1. Define in [Jenkins](https://upp-k8s-jenkins.in.ft.com/job/k8s-deployment/credentials/store/folder/domain/_/) the credentials needed for accessing the K8S API servers. 
For each of the API servers in the environment Jenkins needs 3 keys in order to access it, therefore you need to create 3 Jenkins credentials / cluster that are of type `Secret File` with the following ids
    1. ft.k8s-auth.${cluster_label}-${env_name}[-${region}].ca-cert (example `ft.k8s-auth.delivery-staging-us.ca-cert`) -> this is the certificate of the CA used when generating the certificates -> ca.pem from the kubeconfig credentials
    1. ft.k8s-auth.${cluster_label}-${env_name}[-${region}].client-certificate (example `ft.k8s-auth.delivery-staging-us.client-certificate`) -> this is the certificate of the user used to authenticate in the k8s cluster -> admin.pem from the kubeconfig credentials
    1. ft.k8s-auth.${cluster_label}-${env_name}[-${region}].client-key (example `ft.k8s-auth.delivery-staging-us.client-key` ) -> this is the private key of the user used to authenticate in the k8s cluster -> admin-key.pem from the kubeconfig credentials
    
1. Push the branch and create a Pull Request.
1. After merge, add the new environment to the Jenkins jobs:
    1. [Deploys a helm chart from the upp repo](https://upp-k8s-jenkins.in.ft.com/job/k8s-deployment/job/utils/job/deploy-upp-helm-chart/)
    1. [Diff and Sync 2 k8s envs](https://upp-k8s-jenkins.in.ft.com/job/k8s-deployment/job/utils/job/diff-between-envs/)
    
