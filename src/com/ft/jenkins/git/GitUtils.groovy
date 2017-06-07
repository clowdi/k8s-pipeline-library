package com.ft.jenkins.git

import groovy.json.JsonSlurper

import java.util.regex.Matcher

import static com.ft.jenkins.git.GitUtilsConstants.TAG_BRANCHES_PREFIX

final class GitUtilsConstants {
  public static final String TAG_BRANCHES_PREFIX = "tags/"
}

public boolean isTag(String checkedOutBranchName) {
  return checkedOutBranchName.startsWith(TAG_BRANCHES_PREFIX)
}

public String getTagNameFromBranchName(String checkedOutBranchName) {
  String[] values = checkedOutBranchName.split('/')
  return values[values.length - 1]
}


public String getCurrentRepoName() {
  String gitUrl = scm.getUserRemoteConfigs()[0].url
  Matcher matcher = (gitUrl =~ /.*\/(.*).git/)
  /*  get the value matching the group */
  return matcher[0][1]
}

public GithubReleaseInfo getGithubReleaseInfo(String tagName, String repoName) {
  /*  fetch the release info*/
  def releaseResponse = httpRequest(acceptType: 'APPLICATION_JSON',
                                    authentication: 'ft.github.credentials',
                                    url: "https://api.github.com/repos/Financial-Times/${repoName}/releases/tags/${tagName}")

  def releaseInfoJson = new JsonSlurper().parseText(releaseResponse.content)
  GithubReleaseInfo releaseInfo = new GithubReleaseInfo()
  releaseInfo.title = releaseInfoJson.name
  releaseInfo.description = releaseInfoJson.body
  releaseInfo.url = releaseInfoJson.html_url
  releaseInfo.authorName = releaseInfoJson.author.login
  releaseInfo.authorUrl = releaseInfoJson.author.html_url
  releaseInfo.authorAvatar = releaseInfoJson.author.avatar_url
  releaseInfo.isPreRelease = Boolean.valueOf(releaseInfoJson.prerelease)
  releaseInfo.tagName = tagName
  return releaseInfo
}

