package stu.napls.clouderweb.controller;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import stu.napls.clouderweb.auth.annotation.Auth;
import stu.napls.clouderweb.auth.util.AuthToolbox;
import stu.napls.clouderweb.config.GCPStorageConfig;
import stu.napls.clouderweb.core.dictionary.ItemCode;
import stu.napls.clouderweb.core.dictionary.ItemType;
import stu.napls.clouderweb.core.dictionary.PreviewType;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.core.exception.SystemException;
import stu.napls.clouderweb.core.response.Response;
import stu.napls.clouderweb.core.response.ResponseCode;
import stu.napls.clouderweb.model.Depository;
import stu.napls.clouderweb.model.Folder;
import stu.napls.clouderweb.model.Item;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.model.vo.PreviewVO;
import stu.napls.clouderweb.service.DepositoryService;
import stu.napls.clouderweb.service.FolderService;
import stu.napls.clouderweb.service.ItemService;
import stu.napls.clouderweb.service.UserService;
import stu.napls.clouderweb.util.FileChecker;
import stu.napls.clouderweb.util.GCPToolbox;
import stu.napls.clouderweb.util.SessionGetter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

@RestController
@RequestMapping("/item")
public class ItemController {

    @Resource
    private ItemService itemService;

    @Resource
    private DepositoryService depositoryService;

    @Resource
    private UserService userService;

    @Resource
    private SessionGetter sessionGetter;

    @Resource
    private FolderService folderService;

    @Resource
    private AuthToolbox authToolbox;

    @Resource
    private GCPToolbox gcpToolbox;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/upload/check")
    public Response uploadCheck(@RequestParam Long folderId, @RequestParam String name, @RequestParam Long size, @ApiIgnore HttpSession httpSession) {
        checkItemName(name);
        Assert.isTrue(size != null && size > 0, ResponseCode.ILLEGAL, "File size is invalid.");
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();

        Folder destFolder = getFolder(folderId, depository.getId());

        Assert.isTrue(size <= (depository.getCapacity() - depository.getUsedSpace()), ResponseCode.ILLEGAL, "The remaining space is not enough.");

        checkItemNameDuplication(name, destFolder.getId());

        return Response.success("ok");
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header"),
            @ApiImplicitParam(name = "file", required = true, dataType = "__file", paramType = "form")})
    @Auth
    @PostMapping("/upload/to/{folderId}")
    public Response uploadItem(@PathVariable("folderId") Long folderId, @ApiIgnore HttpServletRequest request, @ApiIgnore HttpSession httpSession) throws IOException, FileUploadException {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();

        Folder destFolder = getFolder(folderId, depository.getId());

        String itemName = null;
        String contentType = null;
        Blob blob = null;

        ServletFileUpload upload = new ServletFileUpload();
        long remainingSpace = depository.getCapacity() - depository.getUsedSpace();
        upload.setFileSizeMax(remainingSpace);
        upload.setSizeMax(remainingSpace);
        FileItemIterator iterator = upload.getItemIterator(request);
        while (iterator.hasNext()) {
            FileItemStream fileItemStream = iterator.next();
            String name = fileItemStream.getFieldName();
            InputStream stream = fileItemStream.openStream();
            if (!fileItemStream.isFormField()) {
                if ("file".equals(name)) {
                    itemName = fileItemStream.getName();
                    contentType = fileItemStream.getContentType();
                    checkItemName(itemName);
                    checkItemNameDuplication(itemName, destFolder.getId());

                    Assert.notNull(folderId, ResponseCode.ILLEGAL, "Folder ID cannot be null.");

                    blob = uploadToGCP(user.getUuid(), destFolder.getUuid(), itemName, contentType, stream);
                }
            }
        }

        Assert.notNull(blob, "Upload to GCP failed.");

        Item item = itemService.update(new Item(user.getDepository().getId(), folderId, itemName, blob));
        // Update the space of depository
        depository.setUsedSpace(depository.getUsedSpace() + blob.getSize());
        depositoryService.update(depository);
        return Response.success("File uploaded successfully", item);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/download/{itemId}")
    public Response downloadItem(@PathVariable("itemId") Long itemId, @ApiIgnore HttpSession httpSession) throws IOException {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Item item = getItem(itemId, depository.getId());
        Folder folder = getFolder(item.getFolderId(), depository.getId());

        Storage storage = gcpToolbox.getStorage();
        String gcpPath = gcpToolbox.getGCPPath(user.getUuid(), folder.getUuid(), item.getName());

        Blob blob = storage.get(GCPStorageConfig.STANDARD_BUCKET_ID, gcpPath);
        Assert.notNull(blob, ResponseCode.NONEXISTENCE, "Item does not exist.");
        blob.toBuilder().setContentDisposition("attachment").build().update();

        URL signedUrl = storage.signUrl(BlobInfo.newBuilder(GCPStorageConfig.STANDARD_BUCKET_ID, gcpPath).build(),
                ItemCode.VALID_DOWNLOAD_TIME, ItemCode.VALID_DOWNLOAD_TIME_UNIT, Storage.SignUrlOption.signWith(ServiceAccountCredentials.fromStream(
                        new FileInputStream(GCPStorageConfig.GCP_CREDENTIAL_PATH))));
        Assert.notNull(signedUrl, "Failed getting download URL.");

        return Response.success("ok", signedUrl.toString());
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/share/{itemId}")
    public Response shareItem(@PathVariable("itemId") Long itemId, @ApiIgnore HttpSession httpSession) throws IOException {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Item item = getItem(itemId, depository.getId());

        Storage storage = gcpToolbox.getStorage();
        String gcpPath = gcpToolbox.getGCPPath(user.getUuid(), folderService.findById(item.getFolderId()).getUuid(), item.getName());

        Blob blob = storage.get(GCPStorageConfig.STANDARD_BUCKET_ID, gcpPath);
        Assert.notNull(blob, ResponseCode.NONEXISTENCE, "Item does not exist.");
        blob.toBuilder().setContentDisposition("").build().update();

        URL signedUrl = storage.signUrl(BlobInfo.newBuilder(GCPStorageConfig.STANDARD_BUCKET_ID, gcpPath).build(),
                ItemCode.VALID_SHARE_TIME, ItemCode.VALID_SHARE_TIME_UNIT, Storage.SignUrlOption.signWith(ServiceAccountCredentials.fromStream(
                        new FileInputStream(GCPStorageConfig.GCP_CREDENTIAL_PATH))));
        Assert.notNull(signedUrl, "Failed getting sharing URL.");

        return Response.success("The link is valid for one day.", signedUrl.toString());
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/preview/{itemId}")
    public Response getItemPreview(@PathVariable("itemId") Long itemId, @ApiIgnore HttpSession httpSession) throws IOException {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Item item = getItem(itemId, depository.getId());
        Folder folder = getFolder(item.getFolderId(), depository.getId());

        int previewType = getPreviewType(item.getSuffix());
        Assert.isTrue(previewType != PreviewType.NO_TYPE, ResponseCode.UNSUPPORTED, "Unsupported file preview.");

        Storage storage = gcpToolbox.getStorage();
        String gcpPath = gcpToolbox.getGCPPath(user.getUuid(), folder.getUuid(), item.getName());

        Blob blob = storage.get(GCPStorageConfig.STANDARD_BUCKET_ID, gcpPath);
        Assert.notNull(blob, ResponseCode.NONEXISTENCE, "Item does not exist.");
        blob.toBuilder().setContentDisposition("").build().update();

        URL signedUrl = storage.signUrl(BlobInfo.newBuilder(GCPStorageConfig.STANDARD_BUCKET_ID, gcpPath).build(),
                ItemCode.VALID_PREVIEW_TIME, ItemCode.VALID_PREVIEW_TIME_UNIT, Storage.SignUrlOption.signWith(ServiceAccountCredentials.fromStream(
                        new FileInputStream(GCPStorageConfig.GCP_CREDENTIAL_PATH))));
        Assert.notNull(signedUrl, ResponseCode.NONEXISTENCE, "Failed getting preview URL.");

        return Response.success(new PreviewVO(previewType, item.getContentType(), signedUrl.toString()));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header"),
            @ApiImplicitParam(name = "size", value = "Size of a page", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Page number starting from 0", dataType = "string", paramType = "query")
    })
    @Auth
    @GetMapping("/get/{itemId}")
    public Response getItem(@PathVariable("itemId") Long itemId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        return Response.success(getItem(itemId, user.getDepository().getId()));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header"),
            @ApiImplicitParam(name = "size", value = "Size of a page", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Page number starting from 0", dataType = "string", paramType = "query")
    })
    @Auth
    @GetMapping("/in/{folderId}/list")
    public Response getItemListByFolderId(@PathVariable("folderId") Long folderId, @ApiIgnore Pageable pageable, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Folder folder = getFolder(folderId, user.getDepository().getId());
        return Response.success(itemService.findAllByFolderId(folder.getId(), pageable));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header"),
            @ApiImplicitParam(name = "size", value = "Size of a page", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Page number starting from 0", dataType = "string", paramType = "query")
    })
    @Auth
    @GetMapping("/{itemType}/list")
    public Response getItemListByItemType(@PathVariable("itemType") Integer itemType, @ApiIgnore Pageable pageable, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        return Response.success(itemService.findAllByDepositoryIdAndType(user.getDepository().getId(), itemType, pageable));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/copy/{itemId}/to/{destFolderId}")
    public Response copyItem(@PathVariable("itemId") Long itemId, @PathVariable("destFolderId") Long destFolderId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Item item = getItem(itemId, depository.getId());

        Folder destFolder = getFolder(destFolderId, depository.getId());

        checkItemNameDuplication(item.getName(), destFolderId);

        Storage storage = gcpToolbox.getStorage();
        Blob blob = storage.get(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), folderService.findById(item.getFolderId()).getUuid(), item.getName())));
        CopyWriter copyWriter = blob.copyTo(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), destFolder.getUuid(), item.getName())));
        Blob copiedBlob = copyWriter.getResult();

        Item newItem = itemService.update(new Item(user.getDepository().getId(), destFolderId, item.getName(), copiedBlob));
        // Update the space of depository
        depository.setUsedSpace(depository.getUsedSpace() + copiedBlob.getSize());
        depositoryService.update(depository);
        return Response.success("Copy successfully", newItem);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/move/{itemId}/to/{destFolderId}")
    public Response moveItem(@PathVariable("itemId") Long itemId, @PathVariable("destFolderId") Long destFolderId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();

        Item item = getItem(itemId, depository.getId());
        Folder destFolder = getFolder(destFolderId, depository.getId());

        checkItemNameDuplication(item.getName(), destFolderId);

        Folder itemFolder = folderService.findById(item.getFolderId());

        Storage storage = gcpToolbox.getStorage();
        Blob blob = storage.get(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), itemFolder.getUuid(), item.getName())));
        item.setFolderId(destFolderId);
        CopyWriter copyWriter = blob.copyTo(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), destFolder.getUuid(), item.getName())));
        Blob copiedBlob = copyWriter.getResult();
        blob.delete();

        return Response.success("Move successfully.", itemService.update(item));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/rename/{itemId}")
    public Response renameItem(@PathVariable("itemId") Long itemId, @RequestParam String name, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Item item = getItem(itemId, depository.getId());

        checkItemNameDuplication(name, item.getFolderId());

        Folder itemFolder = folderService.findById(item.getFolderId());

        Storage storage = gcpToolbox.getStorage();
        Blob blob = storage.get(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), itemFolder.getUuid(), item.getName())));
        item.setName(name);
        CopyWriter copyWriter = blob.copyTo(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), itemFolder.getUuid(), item.getName())));
        Blob copiedBlob = copyWriter.getResult();
        blob.delete();

        return Response.success("Rename successfully.", itemService.update(item));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/delete/{itemId}")
    public Response deleteItem(@PathVariable("itemId") Long itemId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Item item = getItem(itemId, depository.getId());

        Storage storage = gcpToolbox.getStorage();
        Assert.isTrue(storage.delete(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), folderService.findById(item.getFolderId()).getUuid(), item.getName())), "Failed deleting");
        itemService.delete(item);

        // Release the space
        depository.setUsedSpace(depository.getUsedSpace() - item.getSize());
        depositoryService.update(depository);
        return Response.success("Delete successfully.");
    }

    /**
     * Upload item to GCP Storage
     *
     * @param userUUID
     * @param folderUUID
     * @param itemName
     * @param stream
     * @return
     * @throws IOException
     */
    private Blob uploadToGCP(String userUUID, String folderUUID, String itemName, String contentType, InputStream stream) throws IOException {
        Storage storage = gcpToolbox.getStorage();
        BlobId blobId = BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(userUUID, folderUUID, itemName));
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).setContentDisposition("attachment").build();

        try (WriteChannel writer = storage.writer(blobInfo)) {
            byte[] buffer = new byte[10 * 1024 * 1024];
            try (InputStream inputStream = stream) {
                int length;
                while ((length = inputStream.read(buffer)) >= 0)
                    writer.write(ByteBuffer.wrap(buffer, 0, length));
            } catch (Exception ex) {
                throw new SystemException("Upload failed.");
            }
        }
        return storage.get(blobId);
    }

    /**
     * Get item object with checking
     *
     * @param itemId
     * @param depositoryId
     * @return
     */
    private Item getItem(long itemId, long depositoryId) {
        Item item = itemService.findById(itemId);
        Assert.notNull(item, ResponseCode.NONEXISTENCE, "Item does not exist.");
        Assert.isTrue(item.getDepositoryId().equals(depositoryId), ResponseCode.UNAUTHORIZED, "Unauthorized operation.");
        return item;
    }

    /**
     * Get folder object with checking
     *
     * @param folderId
     * @param depositoryId
     * @return
     */
    private Folder getFolder(long folderId, long depositoryId) {
        Folder folder = folderService.findById(folderId);
        Assert.notNull(folder, ResponseCode.NONEXISTENCE, "Folder does not exist.");
        Assert.isTrue(folder.getDepositoryId().equals(depositoryId), ResponseCode.UNAUTHORIZED, "Unauthorized operation.");
        return folder;
    }

    private int getPreviewType(String suffix) {
        int previewType = PreviewType.NO_TYPE;
        switch (FileChecker.getItemType(suffix)) {
            case ItemType.IMAGE: {
                previewType = PreviewType.IMAGE;
                break;
            }
            case ItemType.VIDEO: {
                previewType = PreviewType.VIDEO;
                break;
            }
            case ItemType.DOCUMENT: {
                if ("pdf".equals(suffix)) {
                    previewType = PreviewType.PDF;
                }
                break;
            }
        }

        return previewType;
    }

    private void checkItemName(String itemName) {
        Assert.isTrue(itemName != null && !itemName.contains("/") && itemName.length() > 0 && itemName.length() < 95, ResponseCode.ILLEGAL, "File name is invalid.");
    }

    private void checkItemNameDuplication(String itemName, long folderId) {
        Assert.isNull(itemService.findByFolderIdAndName(folderId, itemName), ResponseCode.ILLEGAL, "Item exists.");
    }

}
