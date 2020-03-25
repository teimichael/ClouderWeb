package stu.napls.clouderweb.controller;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import stu.napls.clouderweb.auth.annotation.Auth;
import stu.napls.clouderweb.config.GCPStorageConfig;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.core.response.Response;
import stu.napls.clouderweb.core.response.ResponseCode;
import stu.napls.clouderweb.model.Depository;
import stu.napls.clouderweb.model.Folder;
import stu.napls.clouderweb.model.Item;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.model.vo.FolderContent;
import stu.napls.clouderweb.service.DepositoryService;
import stu.napls.clouderweb.service.FolderService;
import stu.napls.clouderweb.service.ItemService;
import stu.napls.clouderweb.util.GCPToolbox;
import stu.napls.clouderweb.util.SessionGetter;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/folder")
public class FolderController {

    @Resource
    private DepositoryService depositoryService;

    @Resource
    private FolderService folderService;

    @Resource
    private ItemService itemService;

    @Resource
    private SessionGetter sessionGetter;

    @Resource
    private GCPToolbox gcpToolbox;


    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/create/in/{parentFolderId}")
    public Response createFolder(@PathVariable("parentFolderId") Long parentFolderId, @RequestParam String folderName, @ApiIgnore HttpSession httpSession) {
        Assert.isTrue(!folderName.contains("/") && folderName.length() > 0 && folderName.length() <= 30, ResponseCode.ILLEGAL, "Folder name is invalid.");
        User user = sessionGetter.getUser(httpSession);
        Folder parentFolder = folderService.findById(parentFolderId);
        Assert.notNull(parentFolder, ResponseCode.NONEXISTENCE, "Parent folder does not exist.");
        Assert.isTrue(parentFolder.getDepositoryId().equals(user.getDepository().getId()), ResponseCode.UNAUTHORIZED, "Unauthorized operation.");
        Assert.isTrue(parentFolder.getPath().split("/").length < 10, ResponseCode.ILLEGAL, "Folder path is too deep.");
        Assert.isNull(folderService.findByParentFolderIdAndName(parentFolderId, folderName), ResponseCode.ILLEGAL, "Folder exists.");

        Folder folder = folderService.update(new Folder(user.getDepository().getId(), folderName, parentFolder.getPath() + folderName + "/", parentFolderId));

        return Response.success("Create successfully.", folder);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/rename/{folderId}")
    public Response renameFolder(@PathVariable("folderId") Long folderId, @RequestParam String name, @ApiIgnore HttpSession httpSession) {
        Assert.isTrue(!name.contains("/") && name.length() > 0 && name.length() <= 30, ResponseCode.ILLEGAL, "Folder name is invalid.");
        User user = sessionGetter.getUser(httpSession);
        Folder folder = getFolder(folderId, user.getDepository().getId());
        Assert.isTrue(folder.getParentFolderId() != 0, ResponseCode.ILLEGAL, "Cannot operate the root folder.");
        Assert.isNull(folderService.findByParentFolderIdAndName(folder.getParentFolderId(), name), ResponseCode.ILLEGAL, "Folder with the same name exists.");

        folder.setName(name);
        folder.setPath(renamePath(folder.getPath(), name));
        folder = folderService.update(folder);

        return Response.success("Create successfully.", folder);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/copy/{folderId}/to/{destFolderId}")
    public Response copyFolder(@PathVariable("folderId") Long folderId, @PathVariable("destFolderId") Long destFolderId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Folder folder = getFolder(folderId, depository.getId());
        Assert.isTrue(folder.getParentFolderId() != 0, ResponseCode.ILLEGAL, "Cannot operate the root folder.");
        Folder destFolder = getFolder(destFolderId, depository.getId());
        Assert.isTrue(destFolder.getPath().split("/").length < 10, ResponseCode.ILLEGAL, "Destination folder path is too deep.");
        Assert.isTrue(destFolder.getPath().indexOf(folder.getPath()) != 0, ResponseCode.ILLEGAL, "The destination folder is a subfolder of the source folder.");
        Assert.isNull(folderService.findByParentFolderIdAndName(destFolder.getId(), folder.getName()), ResponseCode.ILLEGAL, "Folder with the same name exists in the destination folder.");

        Folder copiedFolder = folderService.update(new Folder(depository.getId(), folder.getName(), destFolder.getPath() + folder.getName() + "/", destFolderId));

        List<Folder> subFolders = new ArrayList<>();
        // <id, uuid>
        Map<Long, String> oldFolderIdUuidMap = new HashMap<>();
        oldFolderIdUuidMap.put(folder.getId(), folder.getUuid());
        // <uuid, id>
        Map<String, Long> copiedFolderUuidIdMap = new HashMap<>();
        copiedFolderUuidIdMap.put(copiedFolder.getUuid(), copiedFolder.getId());

        // <old uuid, new uuid>
        Map<String, String> folderUuidMap = new HashMap<>();
        folderUuidMap.put(folder.getUuid(), copiedFolder.getUuid());
        List<Folder> copiedSubFolders = new ArrayList<>();

        List<Item> containingItems = new ArrayList<>();
        List<Item> copiedContainingItems = new ArrayList<>();

        recursiveFolderItemQuery(folder, subFolders, containingItems);

        for (Folder subFolder :
                subFolders) {
            oldFolderIdUuidMap.put(subFolder.getId(), subFolder.getUuid());
            Folder copiedSubFolder = new Folder(depository.getId(), subFolder.getName(), copiedFolder.getPath() + subFolder.getName() + "/", copiedFolder.getId());
            copiedSubFolders.add(copiedSubFolder);
            folderUuidMap.put(subFolder.getUuid(), copiedSubFolder.getUuid());
        }
        copiedSubFolders = folderService.saveAll(copiedSubFolders);
        for (Folder copiedSubFolder :
                copiedSubFolders) {
            copiedFolderUuidIdMap.put(copiedSubFolder.getUuid(), copiedSubFolder.getId());
        }

        Storage storage = gcpToolbox.getStorage();
        Blob blob;
        String copiedFolderUuid;
        long usedSpace = depository.getUsedSpace();
        for (Item containingItem :
                containingItems) {
            copiedFolderUuid = folderUuidMap.get(oldFolderIdUuidMap.get(containingItem.getFolderId()));
            blob = storage.get(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), oldFolderIdUuidMap.get(containingItem.getFolderId()), containingItem.getName())));
            CopyWriter copyWriter = blob.copyTo(BlobId.of(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), copiedFolderUuid, containingItem.getName())));

            copiedContainingItems.add(new Item(depository.getId(), copiedFolderUuidIdMap.get(copiedFolderUuid), containingItem.getName(), copyWriter.getResult()));
            usedSpace += containingItem.getSize();
        }

        itemService.saveAll(copiedContainingItems);

        depository.setUsedSpace(usedSpace);
        depositoryService.update(depository);

        return Response.success("Copy successfully.", copiedFolder);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/move/{folderId}/to/{destFolderId}")
    public Response moveFolder(@PathVariable("folderId") Long folderId, @PathVariable("destFolderId") Long destFolderId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Folder folder = getFolder(folderId, depository.getId());
        Assert.isTrue(folder.getParentFolderId() != 0, ResponseCode.ILLEGAL, "Cannot operate the root folder.");
        Folder destFolder = getFolder(destFolderId, depository.getId());
        Assert.isTrue(destFolder.getPath().split("/").length < 10, ResponseCode.ILLEGAL, "Destination folder path is too deep.");
        Assert.isTrue(destFolder.getPath().indexOf(folder.getPath()) != 0, ResponseCode.ILLEGAL, "The destination folder is a subfolder of the source folder.");
        Assert.isNull(folderService.findByParentFolderIdAndName(destFolder.getId(), folder.getName()), ResponseCode.ILLEGAL, "Folder with the same name exists in the destination folder.");

        folder.setPath(destFolder.getPath() + folder.getName() + "/");
        folder.setParentFolderId(destFolderId);
        Folder movedFolder = folderService.update(folder);

        return Response.success("Move successfully.", movedFolder);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/{folderId}")
    public Response getFolder(@PathVariable("folderId") Long folderId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);

        Folder folder = getFolder(folderId, user.getDepository().getId());

        return Response.success(folder);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header"),
            @ApiImplicitParam(name = "size", value = "Size of a page", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Page number starting from 0", dataType = "string", paramType = "query")
    })
    @Auth
    @GetMapping("/{folderId}/content")
    public Response getFolderContent(@PathVariable("folderId") Long folderId, @ApiIgnore Pageable pageable, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Folder folder = getFolder(folderId, user.getDepository().getId());

        return Response.success(new FolderContent(folderService.findByParentFolderId(folder.getId()), itemService.findAllByFolderId(folder.getId(), pageable)));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header"),
            @ApiImplicitParam(name = "size", value = "Size of a page", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "Page number starting from 0", dataType = "string", paramType = "query")
    })
    @Auth
    @GetMapping("/content/by/path")
    public Response getFolderContentByPath(@RequestParam String folderPath, @ApiIgnore Pageable pageable, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Folder folder = getFolderByPath(folderPath, user.getDepository().getId());

        return Response.success(new FolderContent(folderService.findByParentFolderId(folder.getId()), itemService.findAllByFolderId(folder.getId(), pageable)));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/list/{parentFolderId}")
    public Response getFolderList(@PathVariable("parentFolderId") Long parentFolderId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);

        Folder parentFolder = folderService.findById(parentFolderId);
        Assert.notNull(parentFolder, ResponseCode.NONEXISTENCE, "Parent folder does not exist.");
        Assert.isTrue(parentFolder.getDepositoryId().equals(user.getDepository().getId()), ResponseCode.UNAUTHORIZED, "Unauthorized operation.");

        return Response.success(folderService.findByParentFolderId(parentFolderId));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/delete/{folderId}")
    public Response deleteFolder(@PathVariable("folderId") Long folderId, @ApiIgnore HttpSession httpSession) {
        User user = sessionGetter.getUser(httpSession);
        Depository depository = user.getDepository();
        Folder folder = folderService.findById(folderId);
        Assert.notNull(folderId, ResponseCode.NONEXISTENCE, "Folder does not exist.");
        Assert.isTrue(folder.getParentFolderId() != 0, ResponseCode.ILLEGAL, "Cannot operate the root folder.");
        Assert.isTrue(folder.getDepositoryId().equals(user.getDepository().getId()), ResponseCode.UNAUTHORIZED, "Unauthorized operation.");

        Long usedSpace = depository.getUsedSpace();

        Storage storage = gcpToolbox.getStorage();

        List<Folder> deleteFolders = new ArrayList<>();
        List<Long> deleteFolderIds = new ArrayList<>();
        deleteFolders.add(folder);

        List<Item> deleteItems = new ArrayList<>();
        List<Long> deleteItemIds = new ArrayList<>();
        recursiveFolderItemQuery(folder, deleteFolders, deleteItems);

        for (Item deleteItem :
                deleteItems) {
            storage.delete(GCPStorageConfig.STANDARD_BUCKET_ID, gcpToolbox.getGCPPath(user.getUuid(), folder.getPath(), deleteItem.getName()));
            deleteItemIds.add(deleteItem.getId());
            usedSpace -= deleteItem.getSize();
        }
        itemService.deleteByIds(deleteItemIds);

        for (Folder deleteFolder :
                deleteFolders) {
            deleteFolderIds.add(deleteFolder.getId());
        }
        folderService.deleteByIds(deleteFolderIds);


        depository.setUsedSpace(usedSpace);
        depositoryService.update(depository);

        return Response.success("Delete successfully.");
    }

    /**
     * Find all folders and items given a parent folder
     *
     * @param parentFolder Parent folder
     * @param folders
     * @param items
     */
    private void recursiveFolderItemQuery(Folder parentFolder, List<Folder> folders, List<Item> items) {
        items.addAll(itemService.findAllByFolderId(parentFolder.getId()));

        List<Folder> subFolders = folderService.findByParentFolderId(parentFolder.getId());
        // If sub folders exist
        if (subFolders != null) {
            for (Folder subFolder :
                    subFolders) {
                folders.add(subFolder);
                recursiveFolderItemQuery(subFolder, folders, items);
            }
        }
    }

    /**
     * Rename the last subpath of 'path' with 'name'
     *
     * @param path
     * @param name
     * @return
     */
    private String renamePath(String path, String name) {
        String[] subPaths = path.split("/");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < subPaths.length - 1; i++) {
            stringBuilder.append(subPaths[i]).append("/");
        }
        stringBuilder.append(name).append("/");
        return stringBuilder.toString();
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

    private Folder getFolderByPath(String folderPath, long depositoryId) {
        Folder folder = folderService.findByPathAndDepositoryId(folderPath, depositoryId);
        Assert.notNull(folder, ResponseCode.NONEXISTENCE, "Folder does not exist.");
        return folder;
    }

}
