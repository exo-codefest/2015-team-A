package org.exoplatform.codefest.entity;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/6/15
 * Page object, for paging Meeting entries
 */
public class Page {
  private int limit;
  private int offset;
  private String sort; // time desc; asc

  public Page() { }

  public Page(int limit, int offset, String sort) {
    this.limit = limit;
    this.offset = offset;
    this.sort = sort;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
  }
}
