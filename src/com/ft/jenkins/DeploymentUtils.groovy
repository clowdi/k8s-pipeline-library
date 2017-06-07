package com.ft.jenkins

import java.util.regex.Matcher

import static DeploymentUtilsConstants.APPS_CONFIG_FOLDER
import static DeploymentUtilsConstants.CREDENTIALS_DIR
import static DeploymentUtilsConstants.DEFAULT_HELM_VALUES_FILE
import static DeploymentUtilsConstants.HELM_CONFIG_FOLDER
import static DeploymentUtilsConstants.K8S_CLI_IMAGE

final class DeploymentUtilsConstants {
  public static String CREDENTIALS_DIR = "credentials"
  public static String K8S_CLI_IMAGE = "coco/k8s-cli-utils:latest"
  public static String HELM_CONFIG_FOLDER = "helm"
  public static String APPS_CONFIG_FOLDER = "app-configs"
  public static final String DEFAULT_HELM_VALUES_FILE = "values.yaml"
  public static final String OPTION_ALL = "All"

}

/**
 * Deploys the application(s) in the current workspace using helm. It expects the helm chart to be defined in the {@link DeploymentUtilsConstants#HELM_CONFIG_FOLDER} folder.
 *
 * @param imageVersion the version of the docker image to deploy
 * @param env the environment name where it will be deployed.
 * @return the list of applications deployed
 */
public List<String> deployAppWithHelm(String imageVersion, Environment env, Cluster cluster, String region = null) {
  List<String> appsToDeploy = getAppNamesInRepo()
  runWithK8SCliTools(env, cluster, region, {
    def chartName = getHelmChartFolderName()

    for (String app : appsToDeploy) {
      sh "helm upgrade ${app} ${HELM_CONFIG_FOLDER}/${chartName} -i -f ${HELM_CONFIG_FOLDER}/${chartName}/${APPS_CONFIG_FOLDER}/${app}.yaml --set image.version=${imageVersion}"
    }
  })
  return appsToDeploy
}

/**
 * Retrieves the repository of the Docker image configured in the Helm chart in the current folder.
 *
 * @return the Docker image repository. Example: "coco/people-rw-neo4j"
 */
public String getDockerImageRepository() {
  String chartFolderName = getHelmChartFolderName()
  String valuesContents = readFile("${HELM_CONFIG_FOLDER}/${chartFolderName}/${DEFAULT_HELM_VALUES_FILE}")
  Matcher matcher = (valuesContents =~ /repository: (.*)\s/)
  /*  get the value matching the group */
  return matcher[0][1]
}

public List<String> getAppNamesInRepo() {
  String chartFolderName = getHelmChartFolderName()
  List<String> appNames = []
  def foundConfigFiles = findFiles(glob: "${HELM_CONFIG_FOLDER}/${chartFolderName}/${APPS_CONFIG_FOLDER}/*.yaml")

  for (def configFile :foundConfigFiles) {
    /*  strip the .yaml extension from the files */
    String fileName = configFile.name
    appNames.add(fileName.substring(0, fileName.indexOf('.')))
  }

  return appNames
}

/**
 * Retrieves the folder name where the Helm chart is defined .
 */
private String getHelmChartFolderName() {
  def chartFile = findFiles(glob: "${HELM_CONFIG_FOLDER}/**/Chart.yaml")[0]
  String[] chartFilePathComponents = ((String) chartFile.path).split('/')
  /* return the parent folder of Chart.yaml */
  return chartFilePathComponents[chartFilePathComponents.size() - 2]
}

public void runWithK8SCliTools(Environment env, Cluster cluster, String region = null, Closure codeToRun) {
  prepareK8SCliCredentials()
  String currentDir = pwd()

  String apiServer = env.getApiServerForCluster(cluster, region)
  GString dockerRunArgs =
      "-v ${currentDir}/${CREDENTIALS_DIR}:/${CREDENTIALS_DIR} " +
      "-e 'K8S_API_SERVER=${apiServer}' " +
      "-e 'KUBECONFIG=${currentDir}/kubeconfig'"

  docker.image(K8S_CLI_IMAGE).inside(dockerRunArgs) {
    sh "/docker-entrypoint.sh"

    codeToRun.call()
  }
}

private void prepareK8SCliCredentials() {
  withCredentials([
      [$class: 'FileBinding', credentialsId: "ft.k8s.auth.client-certificate", variable: 'CLIENT_CERT'],
      [$class: 'FileBinding', credentialsId: "ft.k8s.auth.ca-cert", variable: 'CA_CERT'],
      [$class: 'FileBinding', credentialsId: "ft.k8s.auth.client-key", variable: 'CLIENT_KEY']]) {
    sh """
      mkdir -p ${CREDENTIALS_DIR}
      rm -f ${CREDENTIALS_DIR}/*
      cp ${env.CLIENT_CERT} ${CREDENTIALS_DIR}/
      cp ${env.CLIENT_KEY} ${CREDENTIALS_DIR}/
      cp ${env.CA_CERT} ${CREDENTIALS_DIR}/
    """
  }
}

String getTeamFromReleaseCandidateTag(String rcTag) {
  String[] tagComponents = rcTag.split("-")
  if (tagComponents.length > 1) {
    return tagComponents[1]
  }
  throw new IllegalArgumentException("The tag '${rcTag}' is not a release candidate tag")
}

