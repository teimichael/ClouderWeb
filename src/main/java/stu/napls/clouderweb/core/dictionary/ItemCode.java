package stu.napls.clouderweb.core.dictionary;

import java.util.concurrent.TimeUnit;

public interface ItemCode {

    int VALID_SHARE_TIME = 1;

    TimeUnit VALID_SHARE_TIME_UNIT = TimeUnit.DAYS;

    int VALID_DOWNLOAD_TIME = 1;

    TimeUnit VALID_DOWNLOAD_TIME_UNIT = TimeUnit.HOURS;

    int VALID_PREVIEW_TIME = 15;

    TimeUnit VALID_PREVIEW_TIME_UNIT = TimeUnit.MINUTES;
}
