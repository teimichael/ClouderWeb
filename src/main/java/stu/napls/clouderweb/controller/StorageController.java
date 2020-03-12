package stu.napls.clouderweb.controller;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;
import stu.napls.clouderweb.auth.annotation.Auth;
import stu.napls.clouderweb.config.GlobalConstant;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.core.response.Response;
import stu.napls.clouderweb.model.Depository;
import stu.napls.clouderweb.model.Item;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.service.DepositoryService;
import stu.napls.clouderweb.service.ItemService;
import stu.napls.clouderweb.util.SessionGetter;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@RestController
@RequestMapping("/storage")
public class StorageController {

    @Resource
    private DepositoryService depositoryService;

    @Resource
    private ItemService itemService;

    @Resource
    private SessionGetter sessionGetter;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/upload")
    public Response uploadObject(@RequestParam String path, @RequestParam MultipartFile file, @ApiIgnore HttpSession httpSession) throws IOException {
        User user = sessionGetter.getUser(httpSession);
        Storage storage = StorageOptions.newBuilder().setProjectId(GlobalConstant.GCP_STORAGE_PROJECT_ID).build().getService();
        String objectName = file.getOriginalFilename();
        Assert.notNull(objectName, "File name cannot be null.");
        Assert.isTrue(file.getSize() <= (user.getDepository().getCapacity() - user.getDepository().getUsedSpace()), "The remaining space is not enough.");

        // Upload to GCP storage
        BlobId blobId = BlobId.of(user.getUuid(), objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try (WriteChannel writer = storage.writer(blobInfo)) {
            byte[] buffer = new byte[1024 * 1024];
            try (InputStream inputStream = file.getInputStream()) {
                int length;
                while ((length = inputStream.read(buffer)) >= 0)
                    writer.write(ByteBuffer.wrap(buffer, 0, length));
            } catch (Exception ex) {
                // handle exception
            }
        }
        Blob blob = storage.get(blobId);
        Depository depository = user.getDepository();
        depository.getItems().add(itemService.update(new Item(path, blob)));
        depositoryService.update(depository);
        return Response.success("File (" + blob.getName() + ") uploaded successfully.");
    }


}
