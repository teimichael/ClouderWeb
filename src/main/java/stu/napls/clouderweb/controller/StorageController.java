package stu.napls.clouderweb.controller;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;
import stu.napls.clouderweb.auth.annotation.Auth;
import stu.napls.clouderweb.core.dictionary.FolderCode;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.core.exception.SystemException;
import stu.napls.clouderweb.core.response.Response;
import stu.napls.clouderweb.core.response.ResponseCode;
import stu.napls.clouderweb.model.Depository;
import stu.napls.clouderweb.model.Folder;
import stu.napls.clouderweb.model.Item;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.service.DepositoryService;
import stu.napls.clouderweb.service.FolderService;
import stu.napls.clouderweb.service.ItemService;
import stu.napls.clouderweb.util.GCPToolbox;
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
    private FolderService folderService;

    @Resource
    private GCPToolbox gcpToolbox;

    @Resource
    private SessionGetter sessionGetter;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/upload")
    public Response uploadItem(@ApiParam(value = "The path of the file. Make sure it ends with '/'.", example = "image/", required = true) @RequestParam String path, @RequestParam MultipartFile file, @ApiIgnore HttpSession httpSession) throws IOException {
        User user = sessionGetter.getUser(httpSession);
        String itemName = file.getOriginalFilename();
        Assert.notNull(itemName, "File name cannot be null.");
        Assert.isTrue(path.endsWith("/"), "Path format is invalid.");
        Folder folder = folderService.findByUserIdAndPath(user.getId(), path);
        Assert.isTrue(folder != null || path.equals("/"), ResponseCode.NONEXISTENCE, "Path does not exist.");
        Assert.isTrue(file.getSize() <= (user.getDepository().getCapacity() - user.getDepository().getUsedSpace()), "The remaining space is not enough.");

        // Upload to GCP storage
        Storage storage = gcpToolbox.getStorage();
        // If not the root path
        if (!path.equals("/")) {
            itemName = path + itemName;
        }
        BlobId blobId = BlobId.of(user.getUuid(), itemName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try (WriteChannel writer = storage.writer(blobInfo)) {
            byte[] buffer = new byte[5 * 1024 * 1024];
            try (InputStream inputStream = file.getInputStream()) {
                int length;
                while ((length = inputStream.read(buffer)) >= 0)
                    writer.write(ByteBuffer.wrap(buffer, 0, length));
            } catch (Exception ex) {
                throw new SystemException("Upload failed.");
            }
        }
        Blob blob = storage.get(blobId);
        Item item = itemService.update(new Item(user.getDepository().getId(), path, blob));

        // Update the space of depository
        Depository depository = user.getDepository();
        depository.setUsedSpace(depository.getUsedSpace() + blob.getSize());
        depositoryService.update(depository);
        return Response.success("File uploaded successfully", item);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/create/folder")
    public Response createFolder(@ApiParam(value = "The path of folder. Make sure it ends with '/'.", example = "image/", required = true) @RequestParam String path, @RequestParam String folderName, @ApiIgnore HttpSession httpSession) {
        Assert.isTrue(!folderName.contains("/"), ResponseCode.ILLEGAL, "Folder name is invalid.");
        Assert.isTrue(path.endsWith("/"), ResponseCode.ILLEGAL, "Path format is invalid.");
        User user = sessionGetter.getUser(httpSession);
        folderName = folderName + "/";
        if (!path.equals("/")) {
            Assert.isNull(folderService.findByUserIdAndPath(user.getId(), path + folderName), ResponseCode.ILLEGAL, "Folder exists.");
        } else {
            Assert.isNull(folderService.findByUserIdAndPath(user.getId(), folderName), ResponseCode.ILLEGAL, "Folder exists.");
        }
        Folder parentFolder = folderService.findByUserIdAndPath(user.getId(), path);
        Assert.isTrue(parentFolder != null || path.equals("/"), ResponseCode.NONEXISTENCE, "Path does not exist.");

        Folder folder = new Folder();
        if (!path.equals("/")) {
            folder.setPath(path + folderName);
            folder.setParentFolderId(parentFolder.getId());
        } else {
            folder.setPath(folderName);
            folder.setParentFolderId(FolderCode.ROOT_ID);
        }
        folder.setUserId(user.getId());
        folder = folderService.create(folder);

        return Response.success("Create successfully.", folder);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/folder/list")
    public Response getFolderList(@ApiParam(value = "The path of folder. Make sure it ends with '/'.", example = "image/", required = true) @RequestParam String folderPath, @ApiIgnore HttpSession httpSession) {
        Assert.isTrue(folderPath.endsWith("/"), ResponseCode.ILLEGAL, "Path format is invalid.");
        User user = sessionGetter.getUser(httpSession);

        long parentFolderId = FolderCode.ROOT_ID;
        if (!folderPath.equals("/")) {
            Folder folder = folderService.findByUserIdAndPath(user.getId(), folderPath);
            Assert.notNull(folder, ResponseCode.NONEXISTENCE, "Folder does not exist.");
            parentFolderId = folder.getId();
        }
        return Response.success(folderService.findByUserIdAndParentFolderId(user.getId(), parentFolderId));
    }

}
