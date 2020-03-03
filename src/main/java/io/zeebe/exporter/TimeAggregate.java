package io.zeebe.exporter;

public class TimeAggregate {

  private long sumTime = 0;
  private long minTime = 0;
  private long maxTime = 0;
  private long count = 0;
  private String recordName;
  
  public TimeAggregate(String recordName, long time) {
    this.setRecordName(recordName);
    sumTime = minTime = maxTime = time;
    count = 1;
  }

  public void add(long time) {
    sumTime += time;
    if (time < minTime) {
      minTime = time;
    }
    if (time > maxTime) {
      maxTime = time;
    }
    ++count;
  }
  
  @Override
  public String toString() {
    return recordName + " avg=" + sumTime/count + ", count=" + count + ", min=" + minTime + ", max=" + maxTime;
  }

  public long getSumTime() {
    return sumTime;
  }
  public void setSumTime(long sumTime) {
    this.sumTime = sumTime;
  }
  public long getMinTime() {
    return minTime;
  }
  public void setMinTime(long minTime) {
    this.minTime = minTime;
  }
  public long getMaxTime() {
    return maxTime;
  }
  public void setMaxTime(long maxTime) {
    this.maxTime = maxTime;
  }
  public long getCount() {
    return count;
  }
  public void setCount(long count) {
    this.count = count;
  }

  public String getRecordName() {
    return recordName;
  }

  public void setRecordName(String recordName) {
    this.recordName = recordName;
  }
}
