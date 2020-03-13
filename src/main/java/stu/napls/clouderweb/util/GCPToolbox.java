package stu.napls.clouderweb.util;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Component;
import stu.napls.clouderweb.config.GlobalConstant;

@Component
public class GCPToolbox {

    public Storage getStorage() {
        return StorageOptions.newBuilder().setProjectId(GlobalConstant.GCP_STORAGE_PROJECT_ID).build().getService();
    }

}
