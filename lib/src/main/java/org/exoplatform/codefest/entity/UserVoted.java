package org.exoplatform.codefest.entity;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/6/15
 * List users who voted Meeting's options
 */
public class UserVoted {
  private String username;
  private String optionId;
  private int value;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getOptionId() {
    return optionId;
  }

  public void setOptionId(String optionId) {
    this.optionId = optionId;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public UserVoted(String username, String optionId, int value) {
    this.username = username;
    this.optionId = optionId;
    this.value = value;
  }
}
