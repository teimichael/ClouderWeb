package stu.napls.clouderweb.controller;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import stu.napls.clouderweb.auth.annotation.Auth;
import stu.napls.clouderweb.auth.util.AuthToolbox;
import stu.napls.clouderweb.config.GlobalConstant;
import stu.napls.clouderweb.core.dictionary.ItemCode;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.core.response.Response;
import stu.napls.clouderweb.core.response.ResponseCode;
import stu.napls.clouderweb.model.Item;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.service.ItemService;
import stu.napls.clouderweb.util.GCPToolbox;
import stu.napls.clouderweb.util.SessionGetter;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/item")
public class ItemController {

    @Resource
    private ItemService itemService;

    @Resource
    private SessionGetter sessionGetter;

    @Resource
    private AuthToolbox authToolbox;

    @Resource
    private GCPToolbox gcpToolbox;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/download")
    public Response getObjectUrl(@ApiParam(value = "The path of the file. Make sure it ends with '/'.", example = "image/", required = true) @RequestParam String itemPath, @RequestParam String itemName, @ApiIgnore HttpSession httpSession) throws IOException {
        User user = sessionGetter.getUser(httpSession);
        Storage storage = gcpToolbox.getStorage();
        URL signedUrl = storage.signUrl(BlobInfo.newBuilder(user.getUuid(), itemPath + itemName).build(),
                ItemCode.LINK_VALID_DAY, TimeUnit.DAYS, Storage.SignUrlOption.signWith(ServiceAccountCredentials.fromStream(
                        new FileInputStream(GlobalConstant.GCP_CREDENTIAL_PATH))));
        Assert.notNull(signedUrl, "Failed getting download URL.");
        return Response.success("ok", signedUrl.toString());
    }

    @GetMapping("/preview")
    public byte[] getItemPreview(@ApiParam(value = "The path of the file. Make sure it ends with '/'.", example = "image/", required = true) @RequestParam String itemPath, @RequestParam String itemName, @RequestParam String token) {
        Storage storage = gcpToolbox.getStorage();
        String uuid = authToolbox.verifyToken(token);
        Blob blob = storage.get(BlobId.of(uuid, itemPath + itemName));
        Assert.notNull(blob, "The item does not exist.");
        return blob.getContent();
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header"),
            @ApiImplicitParam(name = "size", value = "Size of a page", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Page number starting from 0", dataType = "string", paramType = "query")
    })
    @Auth
    @GetMapping("/list")
    public Response getItemListByFolder(@ApiParam(value = "The path of folder. Make sure it ends with '/'.", example = "image/", required = true) @RequestParam String folderPath, @ApiIgnore Pageable pageable, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);

        return Response.success(itemService.findAllByDepositoryId(user.getDepository().getId(), pageable));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/delete")
    public Response deleteItem(@ApiParam(value = "The path of the file. Make sure it ends with '/'.", example = "image/", required = true) @RequestParam String itemPath, @RequestParam String itemName, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Storage storage = gcpToolbox.getStorage();
        Assert.isTrue(storage.delete(user.getUuid(), itemPath + itemName), "Failed deleting");
        Item item = itemService.findByPathAndName(itemPath, itemName);
        Assert.notNull(item, ResponseCode.NONEXISTENCE, "Item does not exist.");
        itemService.delete(item);
        return Response.success("Delete successfully.");
    }
}
